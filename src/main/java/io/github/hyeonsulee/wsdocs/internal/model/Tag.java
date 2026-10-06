package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;

/**
 * A non-blank tag name.
 */
public record Tag(@NonNull String name) {

    public Tag {
        if (name.isBlank()) {
            throw new IllegalArgumentException("tag name must not be blank");
        }
        name = name.strip();
    }
}
