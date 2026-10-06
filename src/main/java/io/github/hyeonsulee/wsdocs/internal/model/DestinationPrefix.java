package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;

/**
 * A broker or application prefix such as {@code /app}; empty means none.
 */
public record DestinationPrefix(@NonNull String value) {

    public static final DestinationPrefix NONE = new DestinationPrefix("");

    public DestinationPrefix {
        value = value.strip().replaceAll("/+$", "");
        if (!value.isEmpty() && !value.startsWith("/")) {
            value = "/" + value;
        }
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public Address join(String path) {
        String p = path == null ? "" : path.strip();
        if (p.isEmpty()) {
            return new Address(value);
        }
        return new Address(value + (p.startsWith("/") ? p : "/" + p));
    }
}
