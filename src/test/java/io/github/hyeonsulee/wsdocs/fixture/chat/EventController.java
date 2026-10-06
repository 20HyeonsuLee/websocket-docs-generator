package io.github.hyeonsulee.wsdocs.fixture.chat;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;

public abstract class EventController<T> {

    @MessageMapping("/events/{roomId}")
    public T relay(@DestinationVariable String roomId, Message<T> event) {
        return event.getPayload();
    }
}
