package io.github.hyeonsulee.wsdocs.internal.model;

import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * Where something was declared, as {@code Class#method}.
 */
public record Source(@NonNull String value) implements Comparable<Source> {

    public static Source of(Class<?> type, Method method) {
        return new Source(type.getSimpleName() + "#" + method.getName());
    }

    @Override
    public int compareTo(Source other) {
        return value.compareTo(other.value);
    }
}
