package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.Catalog;
import io.github.hyeonsulee.wsdocs.internal.model.Destination;
import io.github.hyeonsulee.wsdocs.internal.model.DestinationParameters;
import io.github.hyeonsulee.wsdocs.internal.model.Frame;
import io.github.hyeonsulee.wsdocs.internal.model.Publication;
import io.github.hyeonsulee.wsdocs.internal.model.ReceiveOperation;
import io.github.hyeonsulee.wsdocs.internal.model.SendOperation;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.hyeonsulee.wsdocs.fixture.TestWsApp;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestWsApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndpointScannerTest {

    @Autowired
    private EndpointScanner scanner;

    private Catalog catalog;

    @BeforeAll
    void scan() {
        catalog = scanner.scan();
    }

    private ReceiveOperation receive(String address) {
        return catalog.receives().stream().filter(r -> r.address().value().equals(address)).findFirst()
                .orElseThrow(() -> new AssertionError("receive operation not found: " + address));
    }

    private static Publication reply(ReceiveOperation receive) {
        return receive.reply().orElseThrow(() -> new AssertionError("no reply on " + receive.address().value()));
    }

    private SendOperation send(String address) {
        return catalog.sends().stream().filter(s -> s.address().value().equals(address)).findFirst()
                .orElseThrow(() -> new AssertionError("send operation not found: " + address));
    }

    @Test
    void everyHandlerIsAReceiveOperationEvenWithoutReply() {
        ReceiveOperation bulk = receive("/app/chat/bulk/{roomId}");
        assertThat(bulk.reply()).isEmpty();
        assertThat(bulk.payloads()).extracting(PayloadType::key).containsExactly("List_ChatMessageRequest");
    }

    @Test
    void operationTextDescribesTheRequestAndNestedPublicationTextDescribesTheTopic() {
        ReceiveOperation join = receive("/app/chat/join/{roomId}");
        assertThat(join.payloads()).extracting(PayloadType::key).containsExactly("JoinRequest");
        assertThat(join.documentation().summary().value()).isEqualTo("Join a room");
        assertThat(join.documentation().tags().names()).containsExactly("chat", "room");
        assertThat(reply(join).address().value()).isEqualTo("/topic/room/{roomId}");
        assertThat(reply(join).payload().key()).isEqualTo("UserJoinedEvent");

        SendOperation topic = send("/topic/room/{roomId}");
        assertThat(topic.documentation().summary().value()).isEqualTo("Member joined");
        assertThat(topic.documentation().description().isEmpty()).isTrue();
        assertThat(topic.documentation().tags().names()).containsExactly("room");
        assertThat(topic.destination().scope()).isEqualTo(Destination.Scope.BROADCAST);
    }

    @Test
    void standalonePublicationOnAHandlerIsEquivalentToTheNestedForm() {
        SendOperation users = send("/topic/room/{roomId}/users");
        assertThat(users.documentation().isEmpty()).isTrue();
        assertThat(users.payloads()).extracting(PayloadType::displayName).containsExactly("User[]");
        assertThat(receive("/app/chat/users/{roomId}").documentation().summary().value()).isEqualTo("User list");
    }

    @Test
    void classLevelPrefixIsMergedAndHeaderParametersAreExcluded() {
        ReceiveOperation move = receive("/app/game/move/{gameId}");
        assertThat(move.payloads()).extracting(PayloadType::key).containsExactly("MoveRequest");
        assertThat(move.documentation().tags().names()).containsExactly("game");
        assertThat(move.reply()).isEmpty();
    }

    @Test
    void messageParameterUnwrapsToItsBodyType() {
        assertThat(receive("/app/game/raw").payloads()).extracting(PayloadType::key).containsExactly("MoveRequest");
    }

    @Test
    void inferredPublicationStaysTheReplyAndMatchingDeclarationOnlyAddsText() {
        ReceiveOperation start = receive("/app/game/start/{gameId}");
        assertThat(reply(start).address().value()).isEqualTo("/topic/game/{gameId}/state");
        assertThat(reply(start).payload().key()).isEqualTo("GameState");
        assertThat(reply(start).documentation().tags().names()).containsExactly("start");
        assertThat(start.publications()).hasSize(2);
        assertThat(start.publications().get(1).address().value()).isEqualTo("/topic/game/{gameId}/players");
    }

    @Test
    void returnValuesAreRoutedLikeSpringDoes() {
        ReceiveOperation echo = receive("/app/chat/echo");
        assertThat(reply(echo).address().value()).isEqualTo("/topic/echo");
        assertThat(reply(echo).payload().key()).isEqualTo("ChatMessage");

        ReceiveOperation ping = receive("/app/chat/ping");
        assertThat(ping.payloads()).isEmpty();
        assertThat(reply(ping).address().value()).isEqualTo("/topic/chat/ping");
        assertThat(reply(ping).payload().displayName()).isEqualTo("String");

        assertThat(reply(receive("/app/chat/find/{userId}")).payload().displayName()).isEqualTo("Optional<User>");
        assertThat(reply(receive("/app/chat/scores/{roomId}")).payload().displayName()).isEqualTo("Map<String, Integer>");
    }

    @Test
    void sendToUserTargetsTheUserPrefixAndDefaultsToQueuePlusRequestPath() {
        Publication whoami = reply(receive("/app/chat/whoami"));
        assertThat(whoami.address().value()).isEqualTo("/user/queue/whoami");
        assertThat(whoami.destination().scope()).isEqualTo(Destination.Scope.USER);
        assertThat(send("/user/queue/whoami").destination().scope()).isEqualTo(Destination.Scope.USER);

        assertThat(reply(receive("/app/chat/me")).address().value()).isEqualTo("/user/queue/chat/me");
    }

    @Test
    void classLevelSendToAppliesUnlessTheMethodRoutesItself() {
        assertThat(reply(receive("/app/scoreboard/submit/{gameId}")).address().value()).isEqualTo("/topic/scoreboard");
        assertThat(reply(receive("/app/scoreboard/mine")).address().value()).isEqualTo("/user/queue/scoreboard");
    }

    @Test
    void sendToAndSendToUserOnTheSameMethodPublishToBoth() {
        ReceiveOperation share = receive("/app/scoreboard/share/{gameId}");
        assertThat(share.publications()).extracting(p -> p.address().value())
                .containsExactly("/user/queue/scoreboard/shared", "/topic/scoreboard/{gameId}");
        assertThat(share.publications()).extracting(p -> p.destination().scope())
                .containsExactly(Destination.Scope.USER, Destination.Scope.BROADCAST);
        assertThat(send("/topic/scoreboard/{gameId}").variables().names()).containsExactly("gameId");
    }

    @Test
    void publicationsFromSeveralMethodsMergeIntoOneSendOperation() {
        SendOperation state = send("/topic/game/{gameId}/state");
        assertThat(state.payloads()).extracting(PayloadType::key).containsExactly("GameState", "PlayerArray");
        assertThat(state.documentation().summary().value()).isEqualTo("Game state changed");
        assertThat(state.documentation().description().value()).isEqualTo("Final player list when the game ends");
        assertThat(state.documentation().tags().names()).containsExactly("start", "game");
    }

    @Test
    void operationTextOnANonHandlerIsIgnoredButItsPublicationsCount() {
        assertThat(catalog.receives()).extracting(r -> r.documentation().summary().value())
                .doesNotContain("Must be ignored", "Ignored with a warning");
        assertThat(send("/topic/game/{gameId}/players").payloads()).extracting(PayloadType::key)
                .containsExactly("PlayerArray");
    }

    @Test
    void destinationVariablesAreReadFromHandlerParameters() {
        DestinationParameters find = receive("/app/chat/find/{userId}").variables();
        assertThat(find.names()).containsExactly("userId");
        assertThat(find.values().get(0).type().displayName()).isEqualTo("long");

        DestinationParameters join = receive("/app/chat/join/{roomId}").variables();
        assertThat(join.names()).containsExactly("roomId");
        assertThat(join.values().get(0).type().displayName()).isEqualTo("String");

        assertThat(receive("/app/chat/echo").variables().isEmpty()).isTrue();
    }

    @Test
    void publicationsCarryTheHandlerVariablesTheirAddressUses() {
        Publication reply = reply(receive("/app/chat/join/{roomId}"));
        assertThat(reply.variables().names()).containsExactly("roomId");
        assertThat(send("/topic/room/{roomId}").variables().names()).containsExactly("roomId");
        assertThat(send("/topic/game/{gameId}/state").variables().values().get(0).type().displayName()).isEqualTo("String");
    }

    @Test
    void typeVariablesAreResolvedAgainstTheConcreteControllerClass() {
        ReceiveOperation relay = receive("/app/events/{roomId}");
        assertThat(relay.payloads()).extracting(PayloadType::displayName).containsExactly("UserJoinedEvent");
        assertThat(reply(relay).payload().displayName()).isEqualTo("UserJoinedEvent");
        assertThat(reply(relay).address().value()).isEqualTo("/topic/events/{roomId}");
    }

    @Test
    void hiddenClassesAndMethodsAreSkipped() {
        assertThat(catalog.receives()).extracting(r -> r.address().value())
                .doesNotContain("/app/admin/shutdown", "/app/chat/reset/{roomId}");
        assertThat(catalog.sends()).extracting(s -> s.address().value())
                .doesNotContain("/topic/admin/events", "/topic/room/{roomId}/reset");
    }

    @Test
    void resultsAreSortedForDeterministicOutput() {
        assertThat(catalog.receives().stream().map(ReceiveOperation::address).toList()).isSorted();
        assertThat(catalog.sends().stream().map(SendOperation::address).toList()).isSorted();
        assertThat(catalog.receives()).extracting(r -> r.address().value()).allMatch(a -> a.startsWith("/app/"));
    }

    @Test
    void subscribeMappingRepliesDirectlyAndIsNotASendOperation() {
        ReceiveOperation init = receive("/app/chat/init/{roomId}");
        assertThat(init.frame()).isEqualTo(Frame.SUBSCRIBE);
        assertThat(init.documentation().summary().value()).isEqualTo("Recent messages");
        assertThat(reply(init).address().value()).isEqualTo("/app/chat/init/{roomId}");
        assertThat(reply(init).brokered()).isFalse();
        assertThat(reply(init).payload().displayName()).isEqualTo("List<ChatMessage>");
        assertThat(catalog.sends()).extracting(s -> s.address().value()).doesNotContain("/app/chat/init/{roomId}");
        assertThat(receive("/app/chat/join/{roomId}").frame()).isEqualTo(Frame.SEND);
    }
}
