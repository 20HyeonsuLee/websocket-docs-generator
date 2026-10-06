package io.github.hyeonsulee.wsdocs.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code websocket.docs}.
 */
@Data
@ConfigurationProperties(prefix = "websocket.docs")
public class WsDocsProperties {

    private boolean enabled = true;

    private String path = "/ws-docs";

    private String defaultDestinationPrefix = "/topic";

    private String serverUrl = "";

    private Info info = new Info();

    @Data
    public static class Info {

        private String title = "WebSocket API Documentation";
        private String version = "1.0.0";
        private String description = "";
    }
}
