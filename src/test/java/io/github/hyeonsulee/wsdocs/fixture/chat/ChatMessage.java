package io.github.hyeonsulee.wsdocs.fixture.chat;

import java.util.List;
import lombok.Data;

@Data
public class ChatMessage {

    private String content;
    private User sender;
    private List<String> mentions;
}
