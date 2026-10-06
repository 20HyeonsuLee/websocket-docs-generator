package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.Address;
import io.github.hyeonsulee.wsdocs.internal.model.Destination;
import io.github.hyeonsulee.wsdocs.internal.model.DestinationPrefix;
import java.util.Objects;
import lombok.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;

/**
 * Destination prefixes the broker configuration implies, read from the container at scan time.
 */
record BrokerSettings(
        @NonNull DestinationPrefix defaultDestinationPrefix,
        @NonNull DestinationPrefix userDestinationPrefix
) {

    static final DestinationPrefix DEFAULT_USER_DESTINATION_PREFIX = new DestinationPrefix("/user");

    private static final DestinationPrefix USER_QUEUE_PREFIX = new DestinationPrefix("/queue");

    BrokerSettings {
        if (userDestinationPrefix.isEmpty()) {
            userDestinationPrefix = DEFAULT_USER_DESTINATION_PREFIX;
        }
    }

    static BrokerSettings from(
            final ObjectProvider<UserDestinationMessageHandler> userDestinations,
            final String defaultDestinationPrefix
    ) {
        final var userPrefix = userDestinations.stream()
                .map(UserDestinationMessageHandler::getUserDestinationResolver)
                .filter(DefaultUserDestinationResolver.class::isInstance)
                .map(resolver -> ((DefaultUserDestinationResolver) resolver).getDestinationPrefix())
                .findFirst()
                .orElse(DEFAULT_USER_DESTINATION_PREFIX.value());

        return new BrokerSettings(
                new DestinationPrefix(Objects.requireNonNullElse(defaultDestinationPrefix, "")),
                new DestinationPrefix(Objects.requireNonNullElse(userPrefix, ""))
        );
    }

    Destination destination(final Address address) {
        return Destination.of(address, userDestinationPrefix);
    }

    Destination destination(final String address) {
        return destination(new Address(address));
    }

    Address defaultDestination(final String path) {
        return defaultDestinationPrefix.join(path);
    }

    Address userDestination(final String path) {
        return userDestinationPrefix.join(path);
    }

    Address userQueue(final String lookupPath) {
        return userDestination(USER_QUEUE_PREFIX.join(lookupPath).value());
    }
}
