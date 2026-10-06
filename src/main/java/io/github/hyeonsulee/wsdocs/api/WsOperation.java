package io.github.hyeonsulee.wsdocs.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a STOMP handler method ({@code @MessageMapping} or {@code @SubscribeMapping}) in the generated document.
 *
 * <p>The handler itself, its request payload, its destination variables and the message it returns are read from
 * Spring; this annotation only adds text and extra publications. Without it the operation is still documented,
 * just without a summary, description or tags.
 *
 * <pre>{@code
 * @MessageMapping("/chat/join/{roomId}")
 * @SendTo("/topic/room/{roomId}")
 * @WsOperation(
 *         summary = "Join a room",
 *         description = "Announces the new member to everyone in the room.",
 *         tags = {"chat", "room"},
 *         publishes = @WsPublication(destination = "/topic/room/{roomId}", payload = UserJoinedEvent.class,
 *                 summary = "Member joined"))
 * public UserJoinedEvent join(@DestinationVariable String roomId, JoinRequest request) { ... }
 * }</pre>
 *
 * <p>A publication listed in {@link #publishes()} that targets the same destination and payload as the inferred
 * reply (here the {@code @SendTo} target and the return type) is merged into it and only contributes text.
 * Publications with other targets are added as additional messages the handler sends.
 *
 * <p>On a method that is not a STOMP handler the text is ignored; only {@link #publishes()} is used.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WsOperation {

    /** Short one-line summary shown as the operation title. */
    String summary() default "";

    /** Longer description; CommonMark is allowed by AsyncAPI viewers. */
    String description() default "";

    /** Tags used to group operations. Blank entries are ignored. */
    String[] tags() default {};

    /** Messages this handler publishes in addition to, or instead of, what can be inferred from its return value. */
    WsPublication[] publishes() default {};
}
