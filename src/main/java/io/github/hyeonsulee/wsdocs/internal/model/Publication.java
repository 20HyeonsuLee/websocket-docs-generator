package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;
import lombok.With;

/**
 * One message the server sends, as declared or inferred from a single method.
 */
public record Publication(
        @NonNull Destination destination,
        @NonNull PayloadType payload,
        @NonNull @With Documentation documentation,
        @NonNull @With DestinationParameters variables,
        boolean brokered,
        @NonNull Source source
) {

    public record Target(@NonNull Address address, @NonNull PayloadType payload) {
    }

    public static Publication of(Destination destination, PayloadType payload, Documentation documentation,
                                 boolean brokered, Source source) {
        return new Publication(destination, payload, documentation, DestinationParameters.of(destination.address()),
                brokered, source);
    }

    public Address address() {
        return destination.address();
    }

    public Target target() {
        return new Target(address(), payload);
    }

    public Publication merge(@NonNull Publication other) {
        return withDocumentation(documentation.merge(other.documentation));
    }
}
