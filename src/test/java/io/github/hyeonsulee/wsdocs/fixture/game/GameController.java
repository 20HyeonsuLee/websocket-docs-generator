package io.github.hyeonsulee.wsdocs.fixture.game;

import io.github.hyeonsulee.wsdocs.api.WsOperation;
import io.github.hyeonsulee.wsdocs.api.WsPublication;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@MessageMapping("/game")
public class GameController {

    @WsOperation(summary = "Move", tags = "game")
    @MessageMapping("/move/{gameId}")
    public void move(@DestinationVariable String gameId, @Payload MoveRequest move,
                     @Header("simpSessionId") String sessionId) {
    }

    @MessageMapping("/raw")
    public void raw(Message<MoveRequest> message) {
    }

    @MessageMapping("/start/{gameId}")
    @SendTo("/topic/game/{gameId}/state")
    @WsPublication(destination = "/topic/game/{gameId}/state", payload = GameState.class, tags = "start")
    @WsPublication(destination = "/topic/game/{gameId}/players", payload = Player[].class, summary = "Participants at game start")
    public GameState start(@DestinationVariable String gameId) {
        return null;
    }
}
