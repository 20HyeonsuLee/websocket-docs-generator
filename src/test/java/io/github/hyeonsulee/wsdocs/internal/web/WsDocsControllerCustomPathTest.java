package io.github.hyeonsulee.wsdocs.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hyeonsulee.wsdocs.fixture.TestWsApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TestWsApp.class, properties = {
        "websocket.docs.path=/internal/docs/ws",
        "websocket.docs.default-destination-prefix=/sub"
})
@AutoConfigureMockMvc
class WsDocsControllerCustomPathTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pathAndDefaultPrefixAreConfigurable() throws Exception {
        mockMvc.perform(get("/ws-docs")).andExpect(status().isNotFound());

        String html = mockMvc.perform(get("/internal/docs/ws")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("\"specUrl\":\"/internal/docs/ws/asyncapi.json\"");

        String json = mockMvc.perform(get("/internal/docs/ws/asyncapi.json")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json).contains("/app/chat/join/{roomId}").contains("/sub/chat/ping").doesNotContain("/topic/chat/ping");
        assertThat(json).contains("/topic/room/{roomId}").contains("/topic/echo");
    }
}
