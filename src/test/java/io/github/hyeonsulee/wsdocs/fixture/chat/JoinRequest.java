package io.github.hyeonsulee.wsdocs.fixture.chat;

import lombok.Data;

@Data
public class JoinRequest {

    private String userName;
    private UserType userType;
}
