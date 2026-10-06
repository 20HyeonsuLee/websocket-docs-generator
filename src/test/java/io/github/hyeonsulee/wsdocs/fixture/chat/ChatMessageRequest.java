package io.github.hyeonsulee.wsdocs.fixture.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatMessageRequest {

    @JsonProperty(required = true)
    private String content;
    private String sender;
    private MessageType type;
}
