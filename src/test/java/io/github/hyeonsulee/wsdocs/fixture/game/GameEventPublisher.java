package io.github.hyeonsulee.wsdocs.fixture.game;

import io.github.hyeonsulee.wsdocs.api.WsOperation;
import io.github.hyeonsulee.wsdocs.api.WsPublication;
import org.springframework.stereotype.Component;

@Component
public class GameEventPublisher {

    @WsPublication(destination = "/topic/game/{gameId}/state", payload = GameState.class, summary = "Game state changed", tags = "game")
    public void publishState(String gameId) {
    }

    @WsOperation(summary = "Must be ignored", description = "Must be ignored",
            publishes = @WsPublication(destination = "/topic/game/{gameId}/state", payload = Player[].class,
                    description = "Final player list when the game ends"))
    @WsPublication(destination = "/topic/game/{gameId}/players", payload = Player[].class)
    public void publishFinal(String gameId) {
    }

    @WsOperation(summary = "Ignored with a warning")
    public void housekeeping() {
    }
}
