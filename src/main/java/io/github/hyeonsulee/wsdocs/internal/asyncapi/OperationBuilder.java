package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hyeonsulee.wsdocs.internal.model.Address;
import io.github.hyeonsulee.wsdocs.internal.model.Catalog;
import io.github.hyeonsulee.wsdocs.internal.model.Documentation;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import io.github.hyeonsulee.wsdocs.internal.model.Publication;
import io.github.hyeonsulee.wsdocs.internal.model.ReceiveOperation;
import io.github.hyeonsulee.wsdocs.internal.model.SendOperation;
import io.github.hyeonsulee.wsdocs.internal.model.Tag;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Emits the {@code operations} section with the STOMP extensions ({@code x-stomp-frame}, {@code x-stomp-scope}).
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
final class OperationBuilder {

    ObjectNode build(Catalog catalog) {
        TreeMap<String, ObjectNode> operations = new TreeMap<>();
        for (ReceiveOperation receive : catalog.receives()) {
            operations.put(receive.address().value(), receiveOperation(receive));
        }
        for (SendOperation send : catalog.sends()) {
            operations.put(send.address().value(), sendOperation(send));
        }
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        operations.forEach(node::set);
        return node;
    }

    private ObjectNode receiveOperation(ReceiveOperation receive) {
        ObjectNode operation = JsonNodeFactory.instance.objectNode();
        operation.put("action", "receive");
        operation.set("channel", JsonPointers.channelRef(receive.address()));
        operation.put("x-stomp-frame", receive.frame().name());
        applyDocumentation(operation, receive.documentation());

        if (!receive.payloads().isEmpty()) {
            operation.set("messages", messages(receive.address(), receive.payloads()));
        }
        receive.reply().ifPresent(reply -> operation.set("reply", reply(reply)));
        return operation;
    }

    private ObjectNode sendOperation(SendOperation send) {
        ObjectNode operation = JsonNodeFactory.instance.objectNode();
        operation.put("action", "send");
        operation.set("channel", JsonPointers.channelRef(send.address()));
        operation.put("x-stomp-scope", send.destination().scope().name().toLowerCase(Locale.ROOT));
        applyDocumentation(operation, send.documentation());

        operation.set("messages", messages(send.address(), send.payloads()));
        return operation;
    }

    private ObjectNode reply(Publication reply) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("channel", JsonPointers.channelRef(reply.address()));
        node.set("messages", messages(reply.address(), List.of(reply.payload())));
        return node;
    }

    private ArrayNode messages(Address address, List<PayloadType> payloads) {
        ArrayNode messages = JsonNodeFactory.instance.arrayNode();
        for (PayloadType payload : payloads) {
            messages.add(JsonPointers.channelMessageRef(address, payload));
        }
        return messages;
    }

    private void applyDocumentation(ObjectNode operation, Documentation documentation) {
        if (!documentation.summary().isEmpty()) {
            operation.put("summary", documentation.summary().value());
        }
        if (!documentation.description().isEmpty()) {
            operation.put("description", documentation.description().value());
        }
        if (!documentation.tags().isEmpty()) {
            ArrayNode tags = JsonNodeFactory.instance.arrayNode();
            for (Tag tag : documentation.tags().values()) {
                tags.add(JsonNodeFactory.instance.objectNode().put("name", tag.name()));
            }
            operation.set("tags", tags);
        }
    }
}
