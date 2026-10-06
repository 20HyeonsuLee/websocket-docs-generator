package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * A handler the server receives messages on, with the publications it triggers.
 */
public record ReceiveOperation(
        @NonNull Address address,
        @NonNull Frame frame,
        @NonNull List<PayloadType> payloads,
        @NonNull DestinationParameters variables,
        @NonNull List<Publication> publications,
        @NonNull Documentation documentation,
        @NonNull Source source
) {

    public ReceiveOperation {
        payloads = List.copyOf(new LinkedHashSet<>(payloads));
        publications = List.copyOf(publications.stream()
                .collect(Collectors.toMap(Publication::target, Function.identity(), Publication::merge, LinkedHashMap::new))
                .values());
    }

    public Optional<Publication> reply() {
        return publications.stream().findFirst();
    }
}
