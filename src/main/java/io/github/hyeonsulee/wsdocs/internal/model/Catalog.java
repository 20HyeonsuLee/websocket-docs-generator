package io.github.hyeonsulee.wsdocs.internal.model;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;

/**
 * Everything found in one scan, sorted so the document is deterministic.
 */
public record Catalog(@NonNull List<ReceiveOperation> receives, @NonNull List<SendOperation> sends) {

    public Catalog {
        receives = receives.stream()
                .sorted(Comparator.comparing(ReceiveOperation::address).thenComparing(ReceiveOperation::source))
                .toList();
        sends = sends.stream().sorted(Comparator.comparing(SendOperation::address)).toList();
    }

    // Only brokered publications become send operations; a @SubscribeMapping reply goes straight to the subscriber.
    public static Catalog of(List<ReceiveOperation> receives, List<Publication> standalone) {
        Map<Address, SendOperation> sends = Stream.concat(
                        receives.stream().flatMap(r -> r.publications().stream()).filter(Publication::brokered),
                        standalone.stream())
                .sorted(Comparator.comparing(Publication::address)
                        .thenComparing(Publication::payload)
                        .thenComparing(Publication::source))
                .collect(Collectors.toMap(Publication::address, SendOperation::of, SendOperation::merge, TreeMap::new));
        return new Catalog(receives, List.copyOf(sends.values()));
    }

    public List<Channel> channels() {
        Stream<Channel> fromReceives = receives.stream().map(Channel::of);
        Stream<Channel> fromDirectReplies = receives.stream()
                .flatMap(r -> r.publications().stream())
                .filter(p -> !p.brokered())
                .map(Channel::of);
        Stream<Channel> fromSends = sends.stream().map(Channel::of);
        Map<Address, Channel> channels = Stream.of(fromReceives, fromDirectReplies, fromSends).flatMap(s -> s)
                .collect(Collectors.toMap(Channel::address, c -> c, Channel::merge, TreeMap::new));
        return List.copyOf(channels.values());
    }

    public Set<PayloadType> payloadTypes() {
        return channels().stream().flatMap(c -> c.payloads().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
