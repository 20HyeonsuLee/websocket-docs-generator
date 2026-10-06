package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hyeonsulee.wsdocs.internal.model.Address;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * {@code $ref} nodes for the pointers used inside the document.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class JsonPointers {

    static ObjectNode channelRef(Address address) {
        return ref("#/channels/" + escape(address.value()));
    }

    static ObjectNode channelMessageRef(Address address, PayloadType payload) {
        return ref("#/channels/" + escape(address.value()) + "/messages/" + escape(payload.key()));
    }

    static ObjectNode componentMessageRef(PayloadType payload) {
        return ref("#/components/messages/" + escape(payload.key()));
    }

    private static ObjectNode ref(String pointer) {
        return JsonNodeFactory.instance.objectNode().put("$ref", pointer);
    }

    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }
}
