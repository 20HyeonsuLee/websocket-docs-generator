package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.Catalog;
import io.github.hyeonsulee.wsdocs.internal.model.Publication;
import io.github.hyeonsulee.wsdocs.internal.model.ReceiveOperation;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;

/**
 * Entry point of a scan: reads the Spring container and produces a {@link Catalog}.
 */
@CommonsLog
@RequiredArgsConstructor
public class EndpointScanner {

    @NonNull
    private final ObjectProvider<SimpAnnotationMethodMessageHandler> handlers;

    @NonNull
    private final ObjectProvider<UserDestinationMessageHandler> userDestinations;

    @NonNull
    private final ListableBeanFactory beanFactory;

    private final String defaultDestinationPrefix;

    // Collaborators are built per scan: the handler registry and user-destination prefix only exist once the
    // broker configuration has run, and refresh() must observe the current container state.
    public Catalog scan() {
        final var settings = BrokerSettings.from(userDestinations, defaultDestinationPrefix);
        final var annotationReader = new WsAnnotationReader(settings);
        final var receiveOperations = receives(settings, annotationReader);
        final var publications = standalonePublications(annotationReader);
        final var catalog = Catalog.of(receiveOperations, publications);

        log.debug(String.format("WebSocket docs: %d receive and %d send operation(s)",
                catalog.receives().size(), catalog.sends().size()));

        return catalog;
    }

    private List<ReceiveOperation> receives(final BrokerSettings settings, final WsAnnotationReader annotationReader) {
        final var returnValueRouter = new ReturnValueRouter(settings);
        final var receiveOperationFactory = new ReceiveOperationFactory(returnValueRouter, annotationReader);
        final var handlerMethods = new HandlerMethodProvider(handlers).findHandlerMethods();

        return handlerMethods.stream()
                .flatMap(handler -> receiveOperationFactory.create(handler).stream())
                .toList();
    }

    private List<Publication> standalonePublications(final WsAnnotationReader annotationReader) {
        final var publisherMethods = new PublisherMethodProvider(beanFactory).findPublisherMethods();

        return publisherMethods.stream()
                .flatMap(method -> annotationReader.readPublications(method).stream())
                .toList();
    }
}
