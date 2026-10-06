/**
 * Public annotations for describing STOMP endpoints in the generated AsyncAPI document.
 *
 * <p>Everything a Spring application needs is in this package: {@link io.github.hyeonsulee.wsdocs.api.WsOperation}
 * adds text to a {@code @MessageMapping}/{@code @SubscribeMapping} handler,
 * {@link io.github.hyeonsulee.wsdocs.api.WsPublication} declares a message the server publishes, and
 * {@link io.github.hyeonsulee.wsdocs.api.WsHidden} excludes a class or method from the document.
 *
 * <p>Annotations are optional. Handlers, request payloads, return values and {@code @SendTo}/{@code @SendToUser}
 * targets are discovered from the Spring container without them; the annotations add descriptions, tags and
 * publications that cannot be inferred (for example messages sent through {@code SimpMessagingTemplate}).
 *
 * <p>Packages under {@code io.github.hyeonsulee.wsdocs.internal} are implementation details and may change in
 * any release.
 */
package io.github.hyeonsulee.wsdocs.api;
