package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;

/**
 * All publications to one address merged into the operation the document shows.
 */
public record SendOperation(
        @NonNull Destination destination,
        @NonNull List<PayloadType> payloads,
        @NonNull Documentation documentation,
        @NonNull DestinationParameters variables
) {

    public SendOperation {
        payloads = List.copyOf(new LinkedHashSet<>(payloads));
    }

    public static SendOperation of(Publication publication) {
        return new SendOperation(publication.destination(), List.of(publication.payload()),
                publication.documentation(), publication.variables());
    }

    public Address address() {
        return destination.address();
    }

    public SendOperation merge(@NonNull SendOperation other) {
        Set<PayloadType> merged = new LinkedHashSet<>(payloads);
        merged.addAll(other.payloads);
        return new SendOperation(destination, List.copyOf(merged), documentation.merge(other.documentation),
                variables.union(other.variables));
    }
}
