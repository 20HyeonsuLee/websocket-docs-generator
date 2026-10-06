package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.Address;
import io.github.hyeonsulee.wsdocs.internal.model.Documentation;
import io.github.hyeonsulee.wsdocs.internal.model.Frame;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import io.github.hyeonsulee.wsdocs.internal.model.Publication;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;

/**
 * Infers where a handler's return value goes, mirroring Spring's {@code SendToMethodReturnValueHandler}.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ReturnValueRouter {

    private final BrokerSettings settings;

    List<Publication> route(final BeanMethod method, final String lookupPath, final Address address, final Frame frame) {
        if (method.returnsVoid()) {
            return List.of();
        }
        final var payload = new PayloadType(method.returnType());

        return targets(method, lookupPath, address, frame).stream()
                .map(target -> publication(target, payload, method))
                .toList();
    }

    private record Target(Address address, boolean brokered) {
    }

    private record Annotated(SendToUser sendToUser, SendTo sendTo) {

        boolean isEmpty() {
            return sendToUser == null && sendTo == null;
        }
    }

    // Same rules as SendToMethodReturnValueHandler: method-level annotations win over class-level ones, both
    // @SendToUser and @SendTo are honoured when present, and a @SubscribeMapping without either replies directly.
    private List<Target> targets(final BeanMethod method, final String lookupPath, final Address address, final Frame frame) {
        final var annotated = annotated(method);
        if (annotated.isEmpty()) {
            return frame == Frame.SUBSCRIBE
                    ? List.of(new Target(address, false))
                    : List.of(new Target(settings.defaultDestination(lookupPath), true));
        }
        return Stream.concat(userAddresses(annotated.sendToUser(), lookupPath), brokerAddresses(annotated.sendTo(), lookupPath))
                .map(target -> new Target(target, true))
                .toList();
    }

    private static Annotated annotated(final BeanMethod method) {
        final var onMethod = new Annotated(find(method.method(), SendToUser.class), find(method.method(), SendTo.class));
        if (!onMethod.isEmpty()) {
            return onMethod;
        }
        return new Annotated(find(method.type(), SendToUser.class), find(method.type(), SendTo.class));
    }

    private Stream<Address> userAddresses(final SendToUser sendToUser, final String lookupPath) {
        if (sendToUser == null) {
            return Stream.empty();
        }
        if (sendToUser.value().length == 0) {
            return Stream.of(settings.userQueue(lookupPath));
        }
        return Arrays.stream(sendToUser.value()).map(settings::userDestination);
    }

    private Stream<Address> brokerAddresses(final SendTo sendTo, final String lookupPath) {
        if (sendTo == null) {
            return Stream.empty();
        }
        if (sendTo.value().length == 0) {
            return Stream.of(settings.defaultDestination(lookupPath));
        }
        return Arrays.stream(sendTo.value()).map(Address::new);
    }

    private Publication publication(final Target target, final PayloadType payload, final BeanMethod method) {
        return Publication.of(settings.destination(target.address()), payload, Documentation.EMPTY, target.brokered(),
                method.source());
    }

    private static <A extends Annotation> A find(final AnnotatedElement element, final Class<A> type) {
        return AnnotatedElementUtils.findMergedAnnotation(element, type);
    }
}
