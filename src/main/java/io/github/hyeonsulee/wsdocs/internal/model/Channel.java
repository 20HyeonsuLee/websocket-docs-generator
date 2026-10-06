package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;

/**
 * One address with every message and parameter seen on it, from either direction.
 */
public record Channel(
        @NonNull Address address,
        @NonNull List<PayloadType> payloads,
        @NonNull DestinationParameters variables
) {

    public Channel {
        payloads = List.copyOf(new LinkedHashSet<>(payloads));
    }

    public static Channel of(ReceiveOperation receive) {
        return new Channel(receive.address(), receive.payloads(), receive.variables());
    }

    public static Channel of(Publication publication) {
        return new Channel(publication.address(), List.of(publication.payload()), publication.variables());
    }

    public static Channel of(SendOperation send) {
        return new Channel(send.address(), send.payloads(), send.variables());
    }

    public Channel merge(@NonNull Channel other) {
        Set<PayloadType> merged = new LinkedHashSet<>(payloads);
        merged.addAll(other.payloads);
        return new Channel(address, List.copyOf(merged), variables.union(other.variables));
    }
}
