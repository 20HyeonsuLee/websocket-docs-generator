package io.github.hyeonsulee.wsdocs.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a message the server publishes to a STOMP destination.
 *
 * <p>Use it on any Spring bean method, typically one that calls {@code SimpMessagingTemplate.convertAndSend}, so
 * messages that are not handler return values still appear in the document. It can be repeated and can also be
 * nested in {@link WsOperation#publishes()}.
 *
 * <pre>{@code
 * @Service
 * public class GameEventPublisher {
 *
 *     @WsPublication(destination = "/topic/game/{gameId}/state", payload = GameState.class,
 *             summary = "Game state changed", tags = "game")
 *     @WsPublication(destination = "/topic/game/{gameId}/players", payload = Player[].class)
 *     public void broadcast(String gameId, GameState state) {
 *         template.convertAndSend("/topic/game/" + gameId + "/state", state);
 *         template.convertAndSend("/topic/game/" + gameId + "/players", state.players());
 *     }
 * }
 * }</pre>
 *
 * <p>Publications to the same destination from several methods are merged into one {@code send} operation
 * listing every payload. A destination under the user destination prefix (by default {@code /user}) is
 * documented as a user-scoped message.
 */
@Documented
@Repeatable(WsPublication.List.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WsPublication {

    /**
     * Absolute STOMP destination such as {@code /topic/room/{roomId}}. Path variables in braces become channel
     * parameters.
     */
    String destination();

    /**
     * Payload class. Use an array type, e.g. {@code User[].class}, to document a list of objects; generic
     * collections cannot be expressed in an annotation.
     */
    Class<?> payload();

    /** Short one-line summary shown as the operation title. */
    String summary() default "";

    /** Longer description; CommonMark is allowed by AsyncAPI viewers. */
    String description() default "";

    /** Tags used to group operations. Blank entries are ignored. */
    String[] tags() default {};

    /** Container for repeated {@link WsPublication} annotations; never used directly. */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {

        WsPublication[] value();
    }
}
