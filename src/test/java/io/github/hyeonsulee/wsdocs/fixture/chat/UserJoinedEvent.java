package io.github.hyeonsulee.wsdocs.fixture.chat;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserJoinedEvent {

    private String userName;
    private String roomId;
    private LocalDateTime joinedAt;
}
