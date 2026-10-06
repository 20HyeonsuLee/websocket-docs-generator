package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.Address;
import io.github.hyeonsulee.wsdocs.internal.model.DestinationParameter;
import io.github.hyeonsulee.wsdocs.internal.model.DestinationParameters;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.annotation.DestinationVariable;

/**
 * Reads {@code @DestinationVariable} parameters and completes them with the names found in the address.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DestinationParameterResolver {

    static DestinationParameters resolve(final BeanMethod method, final Address address) {
        final var declared = method.parameters().stream()
                .map(DestinationParameterResolver::declared)
                .flatMap(Optional::stream)
                .toList();

        return new DestinationParameters(declared).forAddress(address);
    }

    private static Optional<DestinationParameter> declared(final MethodParameter parameter) {
        final var annotation = AnnotatedElementUtils.findMergedAnnotation(parameter.getParameter(), DestinationVariable.class);

        return Optional.ofNullable(annotation)
                .map(variable -> new DestinationParameter(name(parameter, variable), type(parameter)));
    }

    private static String name(final MethodParameter parameter, final DestinationVariable annotation) {
        if (!annotation.value().isBlank()) {
            return annotation.value();
        }
        return Objects.requireNonNullElse(parameter.getParameterName(), parameter.getParameter().getName());
    }

    private static PayloadType type(final MethodParameter parameter) {
        return new PayloadType(ResolvableType.forMethodParameter(parameter));
    }
}
