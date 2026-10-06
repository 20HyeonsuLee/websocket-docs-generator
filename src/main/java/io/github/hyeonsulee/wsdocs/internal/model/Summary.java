package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;

/**
 * One-line summary; blank means absent.
 */
public record Summary(@NonNull String value) {

    public static final Summary EMPTY = new Summary("");

    public Summary {
        value = value.strip();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public Summary or(Summary other) {
        return isEmpty() ? other : this;
    }
}
