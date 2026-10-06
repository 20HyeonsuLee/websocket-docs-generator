package io.github.hyeonsulee.wsdocs.fixture.game;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class GameState {

    private String gameId;
    private List<Player> players;
    private GameSettings settings;
    private Map<String, Object> metadata;
}
