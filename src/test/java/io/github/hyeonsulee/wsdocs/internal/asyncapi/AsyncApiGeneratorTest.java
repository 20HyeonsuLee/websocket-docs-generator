package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hyeonsulee.wsdocs.fixture.TestWsApp;
import io.github.hyeonsulee.wsdocs.internal.model.ApiInfo;
import io.github.hyeonsulee.wsdocs.internal.scan.EndpointScanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestWsApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncApiGeneratorTest {

    private static final Path SNAPSHOT = Path.of("src/test/resources/expected/asyncapi.yaml");

    @Autowired
    private EndpointScanner scanner;

    private AsyncApiGenerator generator;
    private ObjectNode doc;

    @BeforeAll
    void generate() {
        generator = new AsyncApiGenerator(scanner, ApiInfo.of("Fixture API", "9.9.9", "Snapshot test"));
        doc = generator.document();
    }

    @Test
    void yamlMatchesSnapshot() throws IOException {
        String actual = generator.toYaml();
        if (Boolean.getBoolean("wsdocs.updateSnapshot")) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, actual, StandardCharsets.UTF_8);
        }
        String expected = Files.readString(SNAPSHOT, StandardCharsets.UTF_8);
        assertThat(actual)
                .as("AsyncAPI YAML differs from the snapshot. If the change is intended, refresh it with -Dwsdocs.updateSnapshot=true.")
                .isEqualTo(expected);
    }

    @Test
    void jsonAndYamlDescribeTheSameDocument() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode fromJson = mapper.readTree(generator.toJson());
        Object fromYaml = new org.yaml.snakeyaml.Yaml().load(generator.toYaml());
        JsonNode yamlTree = mapper.valueToTree(fromYaml);
        assertThat(yamlTree).isEqualTo(fromJson);
        assertThat(fromJson.get("asyncapi").asText()).isEqualTo("3.0.0");
        assertThat(fromJson.get("info").get("title").asText()).isEqualTo("Fixture API");
    }

    @Test
    void actionsFollowTheApplicationPerspectiveOfAsyncApi3() {
        assertThat(doc.get("operations").get("/app/chat/join/{roomId}").get("action").asText()).isEqualTo("receive");
        assertThat(doc.get("operations").get("/topic/room/{roomId}").get("action").asText()).isEqualTo("send");
        assertThat(doc.get("operations").get("/user/queue/whoami").get("action").asText()).isEqualTo("send");
    }

    @Test
    void stompFrameIsExposedAsExtension() {
        assertThat(doc.get("operations").get("/app/chat/join/{roomId}").get("x-stomp-frame").asText()).isEqualTo("SEND");
        JsonNode init = doc.get("operations").get("/app/chat/init/{roomId}");
        assertThat(init.get("x-stomp-frame").asText()).isEqualTo("SUBSCRIBE");
        assertThat(init.get("reply").get("channel").get("$ref").asText()).isEqualTo("#/channels/~1app~1chat~1init~1{roomId}");
        assertThat(doc.get("operations").get("/topic/room/{roomId}").has("x-stomp-frame")).isFalse();
    }

    @Test
    void hiddenEndpointsAreAbsentEverywhere() {
        String json = generator.toJson();
        assertThat(json).doesNotContain("/admin/").doesNotContain("/reset");
    }

    @Test
    void receiveOperationWithoutReplyIsPresent() {
        JsonNode op = doc.get("operations").get("/app/chat/bulk/{roomId}");
        assertThat(op).isNotNull();
        assertThat(op.get("action").asText()).isEqualTo("receive");
        assertThat(op.has("reply")).isFalse();
        assertThat(op.get("messages").get(0).get("$ref").asText())
                .isEqualTo("#/channels/~1app~1chat~1bulk~1{roomId}/messages/List_ChatMessageRequest");
    }

    @Test
    void channelParametersComeFromTheTemplate() {
        JsonNode channel = doc.get("channels").get("/app/chat/join/{roomId}");
        assertThat(channel.get("address").asText()).isEqualTo("/app/chat/join/{roomId}");
        assertThat(channel.get("parameters").fieldNames()).toIterable().containsExactly("roomId");
        assertThat(channel.get("messages").get("JoinRequest").get("$ref").asText())
                .isEqualTo("#/components/messages/JoinRequest");
    }

    @Test
    void everyRefResolves() {
        java.util.List<String> refs = new java.util.ArrayList<>();
        collectRefs(doc, refs);
        assertThat(refs).isNotEmpty();
        for (String ref : refs) {
            assertThat(resolvePointer(doc, ref)).as("dangling $ref: " + ref).isNotNull();
        }
    }

    @Test
    void publicationsToTheSameDestinationAreMergedIntoOneSendOperation() {
        JsonNode op = doc.get("operations").get("/topic/game/{gameId}/state");
        assertThat(op.get("action").asText()).isEqualTo("send");
        assertThat(op.get("summary").asText()).isEqualTo("Game state changed");
        assertThat(op.get("description").asText()).isEqualTo("Final player list when the game ends");
        assertThat(op.get("tags")).extracting(t -> t.get("name").asText()).containsExactly("start", "game");
        assertThat(op.get("messages")).hasSize(2);
    }

    @Test
    void everyPublicationIsAlsoASendOperation() {
        JsonNode reply = doc.get("operations").get("/topic/room/{roomId}");
        assertThat(reply.get("action").asText()).isEqualTo("send");
        assertThat(reply.get("summary").asText()).isEqualTo("Member joined");
        assertThat(reply.has("description")).isFalse();
        assertThat(doc.get("operations").get("/app/chat/join/{roomId}").get("reply").get("channel").get("$ref").asText())
                .isEqualTo("#/channels/~1topic~1room~1{roomId}");

        assertThat(doc.get("operations").get("/topic/echo").get("action").asText()).isEqualTo("send");
        assertThat(doc.get("operations").get("/topic/chat/ping").get("action").asText()).isEqualTo("send");
    }

    @Test
    void arrayPayloadIsAnArrayOfSchemaRefs() {
        JsonNode message = doc.get("components").get("messages").get("UserArray");
        assertThat(message.get("title").asText()).isEqualTo("User[]");
        assertThat(message.get("payload").get("type").asText()).isEqualTo("array");
        assertThat(message.get("payload").get("items").get("$ref").asText()).isEqualTo("#/components/schemas/User");

        JsonNode list = doc.get("components").get("messages").get("List_ChatMessage");
        assertThat(list.get("title").asText()).isEqualTo("List<ChatMessage>");
        assertThat(list.get("payload").get("items").get("$ref").asText()).isEqualTo("#/components/schemas/ChatMessage");
        assertThat(doc.get("components").get("schemas").has("List")).isFalse();
        assertThat(doc.get("components").get("schemas").has("List_ChatMessage")).isFalse();
    }

    @Test
    void optionalPayloadUsesInnerSchemaAndMapPayloadUsesAdditionalProperties() {
        JsonNode optional = doc.get("components").get("messages").get("Optional_User").get("payload");
        assertThat(optional.get("$ref").asText()).isEqualTo("#/components/schemas/User");

        JsonNode map = doc.get("components").get("messages").get("Map_String_Integer").get("payload");
        assertThat(map.get("type").asText()).isEqualTo("object");
        assertThat(map.get("additionalProperties").get("type").asText()).isEqualTo("integer");
        assertThat(doc.get("components").get("schemas").has("Optional")).isFalse();
        assertThat(doc.get("components").get("schemas").has("Map")).isFalse();
    }

    @Test
    void scalarPayloadIsInlined() {
        JsonNode string = doc.get("components").get("messages").get("String").get("payload");
        assertThat(string.get("type").asText()).isEqualTo("string");
        assertThat(doc.get("components").get("schemas").has("String")).isFalse();
    }

    @Test
    void enumAnnotationAndEnumFieldsProduceEnumSchemas() {
        JsonNode join = doc.get("components").get("schemas").get("JoinRequest");
        assertThat(join.get("properties").get("userType").get("enum"))
                .extracting(JsonNode::asText).containsExactly("GUEST", "MEMBER", "ADMIN");

        JsonNode request = doc.get("components").get("schemas").get("ChatMessageRequest");
        assertThat(request.get("properties").get("type").get("enum"))
                .extracting(JsonNode::asText).containsExactly("TEXT", "IMAGE", "FILE");
        assertThat(request.get("required")).extracting(JsonNode::asText).containsExactly("content");
    }

    @Test
    void nestedDtoAndDateTimeAreDescribed() {
        JsonNode event = doc.get("components").get("schemas").get("UserJoinedEvent");
        assertThat(event.get("properties").get("joinedAt").get("format").asText()).isEqualTo("date-time");

        JsonNode state = doc.get("components").get("schemas").get("GameState");
        assertThat(state.get("properties").get("players").get("type").asText()).isEqualTo("array");
        assertThat(state.get("properties").get("players").get("items").get("$ref").asText()).isEqualTo("#/components/schemas/Player");
        assertThat(state.get("properties").get("settings").get("$ref").asText()).isEqualTo("#/components/schemas/GameSettings");
        assertThat(doc.get("components").get("schemas").get("Player").get("properties").has("nickname")).isTrue();
        assertThat(doc.get("components").get("schemas").get("GameSettings").get("properties").has("rounds")).isTrue();
    }

    @Test
    void documentIsCachedUntilRefreshed() {
        assertThat(generator.document()).isSameAs(doc);
        generator.refresh();
        ObjectNode regenerated = generator.document();
        assertThat(regenerated).isNotSameAs(doc).isEqualTo(doc);
    }

    private static void collectRefs(JsonNode node, List<String> out) {
        if (node.isObject()) {
            node.properties().forEach(e -> {
                if (e.getKey().equals("$ref")) {
                    out.add(e.getValue().asText());
                } else {
                    collectRefs(e.getValue(), out);
                }
            });
        } else if (node.isArray()) {
            node.forEach(n -> collectRefs(n, out));
        }
    }

    private static JsonNode resolvePointer(JsonNode root, String ref) {
        JsonNode current = root;
        for (String token : ref.substring(2).split("/")) {
            String key = token.replace("~1", "/").replace("~0", "~");
            current = current.get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}
