package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;

/**
 * A generated document together with its JSON and YAML renderings.
 */
record Document(@NonNull ObjectNode root, @NonNull String json, @NonNull String yaml) {

    static Document of(ObjectNode root, ObjectMapper mapper) {
        return new Document(root, toJson(root, mapper), YamlWriter.write(root, mapper));
    }

    private static String toJson(ObjectNode root, ObjectMapper mapper) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize the AsyncAPI document to JSON", e);
        }
    }
}
