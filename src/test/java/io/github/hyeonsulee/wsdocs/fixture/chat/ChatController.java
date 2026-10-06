package io.github.hyeonsulee.wsdocs.fixture.chat;

import io.github.hyeonsulee.wsdocs.api.WsHidden;
import io.github.hyeonsulee.wsdocs.api.WsOperation;
import io.github.hyeonsulee.wsdocs.api.WsPublication;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @WsOperation(summary = "Join a room", description = "A user joins the chat room", tags = {"chat", "room"},
            publishes = @WsPublication(destination = "/topic/room/{roomId}", payload = UserJoinedEvent.class,
                    summary = "Member joined", tags = "room"))
    @MessageMapping("/chat/join/{roomId}")
    public void joinRoom(@DestinationVariable("roomId") String id, JoinRequest request, Principal principal,
                         SimpMessageHeaderAccessor accessor) {
    }

    @WsOperation(summary = "User list")
    @MessageMapping("/chat/users/{roomId}")
    @WsPublication(destination = "/topic/room/{roomId}/users", payload = User[].class)
    public void users(@DestinationVariable String roomId) {
    }

    @MessageMapping("/chat/bulk/{roomId}")
    public void bulk(@DestinationVariable String roomId, List<ChatMessageRequest> messages) {
    }

    @MessageMapping("/chat/echo")
    @SendTo("/topic/echo")
    public ChatMessage echo(ChatMessageRequest request) {
        return null;
    }

    @MessageMapping("/chat/ping")
    public String ping() {
        return "pong";
    }

    @MessageMapping("/chat/whoami")
    @SendToUser("/queue/whoami")
    public User whoami(Principal principal) {
        return null;
    }

    @MessageMapping("/chat/me")
    @SendToUser
    public User me(Principal principal) {
        return null;
    }

    @MessageMapping("/chat/find/{userId}")
    public Optional<User> find(@DestinationVariable long userId) {
        return Optional.empty();
    }

    @MessageMapping("/chat/scores/{roomId}")
    @SendTo("/topic/room/{roomId}/scores")
    public Map<String, Integer> scores(@DestinationVariable String roomId) {
        return Map.of();
    }

    @WsOperation(summary = "Recent messages", description = "Receives the recent message list once at subscription time")
    @SubscribeMapping("/chat/init/{roomId}")
    public List<ChatMessage> init(@DestinationVariable String roomId) {
        return List.of();
    }

    @WsHidden
    @MessageMapping("/chat/reset/{roomId}")
    @WsPublication(destination = "/topic/room/{roomId}/reset", payload = String.class)
    public void reset(@DestinationVariable String roomId) {
    }

    public void helper() {
    }
}
