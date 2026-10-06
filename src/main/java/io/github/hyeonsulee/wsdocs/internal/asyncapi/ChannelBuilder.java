package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hyeonsulee.wsdocs.internal.model.Catalog;
import io.github.hyeonsulee.wsdocs.internal.model.Channel;
import io.github.hyeonsulee.wsdocs.internal.model.DestinationParameter;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Emits the {@code channels} section: one entry per address with its messages and parameters.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ChannelBuilder {

    private final SchemaRegistry schemas;

    ObjectNode build(Catalog catalog) {
        ObjectNode channels = JsonNodeFactory.instance.objectNode();
        for (Channel channel : catalog.channels()) {
            channels.set(channel.address().value(), channel(channel));
        }
        return channels;
    }

    private ObjectNode channel(Channel source) {
        ObjectNode channel = JsonNodeFactory.instance.objectNode();
        channel.put("address", source.address().value());

        if (!source.payloads().isEmpty()) {
            ObjectNode messages = JsonNodeFactory.instance.objectNode();
            for (PayloadType payload : source.payloads()) {
                messages.set(payload.key(), JsonPointers.componentMessageRef(payload));
            }
            channel.set("messages", messages);
        }

        if (!source.variables().isEmpty()) {
            ObjectNode parameters = JsonNodeFactory.instance.objectNode();
            for (DestinationParameter variable : source.variables().values()) {
                parameters.set(variable.name(), parameter(variable));
            }
            channel.set("parameters", parameters);
        }
        return channel;
    }

    private ObjectNode parameter(DestinationParameter variable) {
        ObjectNode parameter = JsonNodeFactory.instance.objectNode();
        parameter.put("description", "Destination variable '" + variable.name() + "'");
        if (variable.type().isEnum()) {
            ArrayNode names = JsonNodeFactory.instance.arrayNode();
            variable.type().enumNames().forEach(names::add);
            parameter.set("enum", names);
        } else {
            // AsyncAPI 3 parameters only allow enum/default/description/examples/location, hence an extension.
            parameter.set("x-schema", schemas.reference(variable.type()));
        }
        return parameter;
    }
}
