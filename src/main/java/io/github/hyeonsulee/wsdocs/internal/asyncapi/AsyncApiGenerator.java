package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hyeonsulee.wsdocs.internal.model.ApiInfo;
import io.github.hyeonsulee.wsdocs.internal.model.Catalog;
import io.github.hyeonsulee.wsdocs.internal.scan.EndpointScanner;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Builds the AsyncAPI 3.0 document from a scan and caches it until {@link #refresh()}.
 */
@RequiredArgsConstructor
public class AsyncApiGenerator {

    public static final String ASYNCAPI_VERSION = "3.0.0";

    @NonNull
    private final EndpointScanner scanner;

    @NonNull
    private final ApiInfo info;

    @NonNull
    private final ObjectMapper mapper;

    private volatile Document cached;

    public AsyncApiGenerator(EndpointScanner scanner, ApiInfo info) {
        this(scanner, info, new ObjectMapper());
    }

    public ObjectNode document() {
        return cachedDocument().root();
    }

    public String toJson() {
        return cachedDocument().json();
    }

    public String toYaml() {
        return cachedDocument().yaml();
    }

    public void refresh() {
        cached = null;
    }

    private Document cachedDocument() {
        Document local = cached;
        if (local == null) {
            synchronized (this) {
                local = cached;
                if (local == null) {
                    local = generate();
                    cached = local;
                }
            }
        }
        return local;
    }

    private Document generate() {
        Catalog catalog = scanner.scan();
        SchemaRegistry schemas = new SchemaRegistry(mapper);
        // Order matters: the channel builder registers parameter schemas, and the components builder drains them.
        ObjectNode channels = new ChannelBuilder(schemas).build(catalog);
        ObjectNode operations = new OperationBuilder().build(catalog);
        ObjectNode components = new ComponentsBuilder(schemas).build(catalog.payloadTypes());

        ObjectNode root = mapper.createObjectNode();
        root.put("asyncapi", ASYNCAPI_VERSION);
        root.set("info", info());
        root.put("defaultContentType", "application/json");
        root.set("channels", channels);
        root.set("operations", operations);
        root.set("components", components);
        return Document.of(root, mapper);
    }

    private ObjectNode info() {
        ObjectNode node = mapper.createObjectNode();
        node.put("title", info.title());
        node.put("version", info.version());
        if (!info.description().isEmpty()) {
            node.put("description", info.description().value());
        }
        return node;
    }
}
