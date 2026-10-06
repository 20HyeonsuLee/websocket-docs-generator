package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.DestinationPrefix;
import io.github.hyeonsulee.wsdocs.internal.model.Frame;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.simp.SimpMessageMappingInfo;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;

/**
 * Lists the STOMP handlers Spring registered, translated into scan types.
 */
@CommonsLog
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class HandlerMethodProvider {

    record RegisteredHandler(
            @NonNull DestinationPrefix applicationPrefix,
            @NonNull List<String> patterns,
            @NonNull Frame frame,
            @NonNull BeanMethod method
    ) {
    }

    private final ObjectProvider<SimpAnnotationMethodMessageHandler> handlers;

    List<RegisteredHandler> findHandlerMethods() {
        final var registered = handlers.orderedStream()
                .flatMap(HandlerMethodProvider::registeredHandlers)
                .toList();

        if (registered.isEmpty()) {
            log.warn("WebSocket docs: no STOMP handler found. Without @EnableWebSocketMessageBroker no "
                    + "@MessageMapping handler is registered, so none is documented.");
        }
        return registered;
    }

    private static Stream<RegisteredHandler> registeredHandlers(final SimpAnnotationMethodMessageHandler handler) {
        final var prefix = firstPrefix(handler.getDestinationPrefixes());

        return handler.getHandlerMethods().entrySet().stream()
                .map(entry -> registered(prefix, entry.getKey(), entry.getValue()));
    }

    private static RegisteredHandler registered(
            final DestinationPrefix applicationPrefix,
            final SimpMessageMappingInfo mapping,
            final HandlerMethod handlerMethod
    ) {
        final var frame = frame(mapping.getMessageTypeMessageCondition().getMessageType());
        final var patterns = List.copyOf(mapping.getDestinationConditions().getPatterns());
        final var method = BeanMethod.of(handlerMethod.getBeanType(), handlerMethod.getMethod());

        return new RegisteredHandler(applicationPrefix, patterns, frame, method);
    }

    private static Frame frame(final SimpMessageType messageType) {
        return messageType == SimpMessageType.SUBSCRIBE ? Frame.SUBSCRIBE : Frame.SEND;
    }

    // Several application prefixes are legal in Spring; only the first is used for addresses so far.
    private static DestinationPrefix firstPrefix(final Collection<String> prefixes) {
        if (prefixes.isEmpty()) {
            return DestinationPrefix.NONE;
        }
        return new DestinationPrefix(prefixes.iterator().next());
    }
}
