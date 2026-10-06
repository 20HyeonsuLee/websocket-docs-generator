package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.api.WsHidden;
import io.github.hyeonsulee.wsdocs.api.WsOperation;
import io.github.hyeonsulee.wsdocs.api.WsPublication;
import io.github.hyeonsulee.wsdocs.internal.model.Documentation;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import io.github.hyeonsulee.wsdocs.internal.model.Publication;
import io.github.hyeonsulee.wsdocs.internal.model.Source;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Reads the public {@code @Ws*} annotations into model objects.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class WsAnnotationReader {

    static final List<Class<? extends Annotation>> MARKERS =
            List.of(WsOperation.class, WsPublication.class, WsPublication.List.class);

    private final BrokerSettings settings;

    static boolean isHidden(final AnnotatedElement element) {
        return AnnotatedElementUtils.hasAnnotation(element, WsHidden.class);
    }

    static boolean isMarked(final Method method) {
        return MARKERS.stream().anyMatch(marker -> AnnotatedElementUtils.hasAnnotation(method, marker));
    }

    static boolean declaresPublication(final Method method) {
        return operation(method).map(WsOperation::publishes).filter(publishes -> publishes.length > 0).isPresent()
                || !standalonePublications(method).isEmpty();
    }

    Documentation readDocumentation(final BeanMethod method) {
        return operation(method.method())
                .map(operation -> Documentation.of(operation.summary(), operation.description(), operation.tags()))
                .orElse(Documentation.EMPTY);
    }

    List<Publication> readPublications(final BeanMethod method) {
        final var nested = operation(method.method()).stream().flatMap(operation -> Arrays.stream(operation.publishes()));
        final var standalone = standalonePublications(method.method()).stream();

        return Stream.concat(nested, standalone)
                .map(annotation -> publication(annotation, method.source()))
                .toList();
    }

    private static Optional<WsOperation> operation(final Method method) {
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, WsOperation.class));
    }

    private static List<WsPublication> standalonePublications(final Method method) {
        return List.copyOf(AnnotatedElementUtils.findMergedRepeatableAnnotations(method, WsPublication.class));
    }

    private Publication publication(final WsPublication annotation, final Source source) {
        return Publication.of(
                settings.destination(annotation.destination()),
                PayloadType.of(annotation.payload()),
                Documentation.of(annotation.summary(), annotation.description(), annotation.tags()),
                true,
                source
        );
    }
}
