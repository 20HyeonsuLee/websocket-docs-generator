package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.Documentation;
import io.github.hyeonsulee.wsdocs.internal.model.PayloadType;
import io.github.hyeonsulee.wsdocs.internal.model.Publication;
import io.github.hyeonsulee.wsdocs.internal.model.ReceiveOperation;
import io.github.hyeonsulee.wsdocs.internal.scan.HandlerMethodProvider.RegisteredHandler;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Turns one registered handler into a {@link ReceiveOperation} per destination pattern.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ReceiveOperationFactory {

    private final ReturnValueRouter router;
    private final WsAnnotationReader annotations;

    List<ReceiveOperation> create(final RegisteredHandler handler) {
        final var method = handler.method();
        if (method.isHidden()) {
            return List.of();
        }
        final var payloads = RequestPayloadResolver.resolve(method).stream().toList();
        final var publications = annotations.readPublications(method);
        final var documentation = annotations.readDocumentation(method);

        return handler.patterns().stream()
                .map(pattern -> create(handler, pattern, payloads, publications, documentation))
                .toList();
    }

    private ReceiveOperation create(
            final RegisteredHandler handler,
            final String pattern,
            final List<PayloadType> payloads,
            final List<Publication> declared,
            final Documentation documentation
    ) {
        final var method = handler.method();
        final var address = handler.applicationPrefix().join(pattern);
        final var parameters = DestinationParameterResolver.resolve(method, address);
        final var inferred = router.route(method, pattern, address, handler.frame());
        final var publications = Stream.concat(inferred.stream(), declared.stream())
                .map(publication -> publication.withVariables(parameters.forAddress(publication.address())))
                .toList();

        return new ReceiveOperation(
                address,
                handler.frame(),
                payloads,
                parameters,
                publications,
                documentation,
                method.source()
        );
    }
}
