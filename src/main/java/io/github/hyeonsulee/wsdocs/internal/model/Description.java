package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;

/**
 * Free-text description; blank means absent.
 */
public record Description(@NonNull String value) {

    public static final Description EMPTY = new Description("");

    public Description {
        value = value.strip();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public Description or(Description other) {
        return isEmpty() ? other : this;
    }
}
