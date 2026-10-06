package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Picks the handler parameter that receives the message body, following Spring's argument resolution.
 */
@CommonsLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RequestPayloadResolver {

    private static final Set<String> SECURITY_ANNOTATIONS = Set.of(
            "org.springframework.security.core.annotation.AuthenticationPrincipal",
            "org.springframework.security.core.annotation.CurrentSecurityContext"
    );

    // A STOMP frame has exactly one body. Spring converts it into every un-annotated parameter, so documenting
    // more than one would claim several messages where there is one; @Payload wins, otherwise the first candidate.
    static Optional<PayloadType> resolve(final BeanMethod method) {
        final var parameters = method.parameters();
        final var explicit = parameters.stream().filter(RequestPayloadResolver::isExplicitPayload).toList();
        final var candidates = explicit.isEmpty()
                ? parameters.stream().filter(RequestPayloadResolver::isImplicitPayload).toList()
                : explicit;

        warnIfAmbiguous(method, candidates);

        return candidates.stream()
                .findFirst()
                .map(RequestPayloadResolver::payloadType);
    }

    private static void warnIfAmbiguous(final BeanMethod method, final List<MethodParameter> candidates) {
        if (candidates.size() > 1) {
            log.warn(String.format("WebSocket docs: %s has %d payload parameters; documenting only the first. "
                    + "Mark the intended one with @Payload.", method.method(), candidates.size()));
        }
    }

    private static PayloadType payloadType(final MethodParameter parameter) {
        final var type = ResolvableType.forMethodParameter(parameter);
        final var body = Message.class.isAssignableFrom(type.toClass()) ? type.as(Message.class).getGeneric(0) : type;

        return new PayloadType(body);
    }

    private static boolean isExplicitPayload(final MethodParameter parameter) {
        return hasAnnotation(parameter, Payload.class);
    }

    private static boolean isImplicitPayload(final MethodParameter parameter) {
        if (isExplicitPayload(parameter)
                || hasAnnotation(parameter, DestinationVariable.class)
                || hasAnnotation(parameter, Header.class)
                || hasAnnotation(parameter, Headers.class)
                || hasSecurityAnnotation(parameter)) {
            return false;
        }
        final var type = parameter.getParameterType();
        return !(Principal.class.isAssignableFrom(type)
                || MessageHeaders.class.isAssignableFrom(type)
                || MessageHeaderAccessor.class.isAssignableFrom(type));
    }

    private static boolean hasAnnotation(final MethodParameter parameter, final Class<? extends Annotation> type) {
        return AnnotatedElementUtils.hasAnnotation(parameter.getParameter(), type);
    }

    // Compared by name so spring-security stays optional on the classpath.
    private static boolean hasSecurityAnnotation(final MethodParameter parameter) {
        return Arrays.stream(parameter.getParameterAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getName)
                .anyMatch(SECURITY_ANNOTATIONS::contains);
    }
}
