package io.github.hyeonsulee.wsdocs.internal.model;

import lombok.NonNull;

/**
 * An address plus whether it is broadcast or delivered to a single user.
 */
public record Destination(@NonNull Address address, @NonNull Scope scope) {

    public enum Scope {

        BROADCAST,
        USER
    }

    public static Destination of(Address address, DestinationPrefix userPrefix) {
        return new Destination(address, address.isUnder(userPrefix) ? Scope.USER : Scope.BROADCAST);
    }
}
