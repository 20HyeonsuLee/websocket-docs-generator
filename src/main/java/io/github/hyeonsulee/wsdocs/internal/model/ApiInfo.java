package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.Objects;
import lombok.NonNull;

/**
 * The {@code info} block of the document.
 */
public record ApiInfo(
        @NonNull String title,
        @NonNull String version,
        @NonNull Description description
) {

    public static ApiInfo of(String title, String version, String description) {
        return new ApiInfo(
                Objects.requireNonNullElse(title, ""),
                Objects.requireNonNullElse(version, ""),
                new Description(Objects.requireNonNullElse(description, "")));
    }
}
