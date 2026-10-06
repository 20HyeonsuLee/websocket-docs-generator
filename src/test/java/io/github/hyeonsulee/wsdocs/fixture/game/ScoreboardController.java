package io.github.hyeonsulee.wsdocs.fixture.game;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@SendTo("/topic/scoreboard")
public class ScoreboardController {

    @MessageMapping("/scoreboard/submit/{gameId}")
    public GameState submit(@DestinationVariable String gameId) {
        return null;
    }

    @MessageMapping("/scoreboard/mine")
    @SendToUser("/queue/scoreboard")
    public GameState mine() {
        return null;
    }

    @MessageMapping("/scoreboard/share/{gameId}")
    @SendTo("/topic/scoreboard/{gameId}")
    @SendToUser("/queue/scoreboard/shared")
    public GameState share(@DestinationVariable String gameId) {
        return null;
    }
}
