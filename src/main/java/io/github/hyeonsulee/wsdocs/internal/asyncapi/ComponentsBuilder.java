package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import java.util.Collection;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Emits the {@code components} section. Must run after every other builder because it drains the schema registry.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ComponentsBuilder {

    private final SchemaRegistry schemas;

    ObjectNode build(Collection<PayloadType> payloadTypes) {
        TreeMap<String, ObjectNode> messages = new TreeMap<>();
        for (PayloadType type : payloadTypes) {
            messages.put(type.key(), message(type));
        }
        ObjectNode messagesNode = JsonNodeFactory.instance.objectNode();
        messages.forEach(messagesNode::set);

        ObjectNode schemasNode = JsonNodeFactory.instance.objectNode();
        schemas.definitions().forEach(schemasNode::set);

        ObjectNode components = JsonNodeFactory.instance.objectNode();
        components.set("messages", messagesNode);
        components.set("schemas", schemasNode);
        return components;
    }

    private ObjectNode message(PayloadType type) {
        ObjectNode message = JsonNodeFactory.instance.objectNode();
        message.put("name", type.key());
        message.put("title", type.displayName());
        message.set("payload", schemas.reference(type));
        return message;
    }
}
