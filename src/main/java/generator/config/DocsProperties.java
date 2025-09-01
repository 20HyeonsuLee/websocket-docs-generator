package generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "websocket.docs")
public class DocsProperties {

    private boolean enabled = true;
    private Info info = new Info();
    private String basePackage = "";
    private String appPath = "/app";
    private String topicPath = "/topic";
    private String serverUrl = "";

    @Data
    public static class Info {
        private String title = "WebSocket API Documentation";
        private String version = "1.0.0";
        private String description = "WebSocket API 명세서";
    }
}
