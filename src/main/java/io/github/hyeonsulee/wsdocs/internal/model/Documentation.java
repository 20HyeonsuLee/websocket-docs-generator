package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;

/**
 * Summary, description and tags attached to an operation.
 */
public record Documentation(
        @NonNull Summary summary,
        @NonNull Description description,
        @NonNull Tags tags
) {

    public static final Documentation EMPTY = new Documentation(Summary.EMPTY, Description.EMPTY, Tags.EMPTY);

    public static Documentation of(String summary, String description, String... tags) {
        return new Documentation(new Summary(summary), new Description(description), Tags.of(tags));
    }

    public boolean isEmpty() {
        return summary.isEmpty() && description.isEmpty() && tags.isEmpty();
    }

    public Documentation merge(@NonNull Documentation other) {
        if (other.isEmpty()) {
            return this;
        }
        return new Documentation(summary.or(other.summary), description.or(other.description), tags.union(other.tags));
    }
}
