package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;

/**
 * A named {@code {variable}} in an address and its Java type.
 */
public record DestinationParameter(@NonNull String name, @NonNull PayloadType type) {

    private static final PayloadType STRING = PayloadType.of(String.class);

    public DestinationParameter {
        if (name.isBlank()) {
            throw new IllegalArgumentException("destination parameter name must not be blank");
        }
        name = name.strip();
    }

    public static DestinationParameter untyped(String name) {
        return new DestinationParameter(name, STRING);
    }
}
