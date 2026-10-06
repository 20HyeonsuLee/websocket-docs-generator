package io.github.hyeonsulee.wsdocs.fixture.chat;

import io.github.hyeonsulee.wsdocs.api.WsHidden;
import io.github.hyeonsulee.wsdocs.api.WsPublication;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@WsHidden
@Controller
public class AdminController {

    @MessageMapping("/admin/shutdown")
    @WsPublication(destination = "/topic/admin/events", payload = String.class)
    public void shutdown() {
    }
}
