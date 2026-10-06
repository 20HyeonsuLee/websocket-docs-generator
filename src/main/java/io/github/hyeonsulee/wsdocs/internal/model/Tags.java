package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;

/**
 * An ordered, deduplicated set of tags.
 */
public record Tags(@NonNull List<Tag> values) {

    public static final Tags EMPTY = new Tags(List.of());

    public Tags {
        values = List.copyOf(new LinkedHashSet<>(values));
    }

    public static Tags of(String... names) {
        return new Tags(Arrays.stream(names)
                .filter(name -> name != null && !name.isBlank())
                .map(Tag::new)
                .toList()
        );
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<String> names() {
        return values.stream().map(Tag::name).toList();
    }

    public Tags union(@NonNull Tags other) {
        if (other.isEmpty()) {
            return this;
        }
        Set<Tag> merged = new LinkedHashSet<>(values);
        merged.addAll(other.values);
        return new Tags(List.copyOf(merged));
    }
}
