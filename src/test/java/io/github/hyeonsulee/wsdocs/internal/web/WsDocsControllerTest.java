package io.github.hyeonsulee.wsdocs.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hyeonsulee.wsdocs.fixture.TestWsApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = TestWsApp.class, properties = "websocket.docs.server-url=http://localhost:8080/ws")
@AutoConfigureMockMvc
class WsDocsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesDocumentationPageWithInjectedConfig() throws Exception {
        MvcResult result = mockMvc.perform(get("/ws-docs"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/html;charset=UTF-8"))
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("window.WS_DOCS_CONFIG = {");
        assertThat(html).contains("\"specUrl\":\"/ws-docs/asyncapi.json\"");
        assertThat(html).contains("\"websocketUrl\":\"http://localhost:8080/ws\"");
        assertThat(html).contains("href=\"/ws-docs/style.css\"");
        assertThat(html).contains("src=\"/ws-docs/vendor/sockjs.min.js\"");
        assertThat(html).doesNotContain("__WS_DOCS_BASE__").doesNotContain("__WS_DOCS_CONFIG__");
        assertThat(html).doesNotContain("th:").doesNotContain("cdn.jsdelivr.net");
    }

    @Test
    void servesAsyncApiJsonAndYaml() throws Exception {
        mockMvc.perform(get("/ws-docs/asyncapi.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.asyncapi").value("3.0.0"))
                .andExpect(jsonPath("$.operations['/app/chat/join/{roomId}'].action").value("receive"));

        MvcResult yaml = mockMvc.perform(get("/ws-docs/asyncapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/yaml;charset=UTF-8"))
                .andReturn();
        assertThat(yaml.getResponse().getContentAsString()).startsWith("asyncapi: 3.0.0");
    }

    @Test
    void servesBundledAssetsAndRejectsUnknownVendorFiles() throws Exception {
        mockMvc.perform(get("/ws-docs/style.css"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/css;charset=UTF-8"));
        mockMvc.perform(get("/ws-docs/vendor/sockjs.min.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/javascript;charset=UTF-8"));
        mockMvc.perform(get("/ws-docs/vendor/stomp.umd.min.js")).andExpect(status().isOk());
        mockMvc.perform(get("/ws-docs/vendor/evil.js")).andExpect(status().isNotFound());
    }
}
