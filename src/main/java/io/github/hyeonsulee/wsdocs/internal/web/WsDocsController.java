package io.github.hyeonsulee.wsdocs.internal.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hyeonsulee.wsdocs.internal.asyncapi.AsyncApiGenerator;
import io.github.hyeonsulee.wsdocs.autoconfigure.WsDocsProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves the documentation page, the AsyncAPI document and the bundled assets.
 */
@Controller
public class WsDocsController {

    private static final String RESOURCE_ROOT = "ws-docs/";
    private static final Map<String, String> VENDOR_FILES = Map.of(
            "sockjs.min.js", "sockjs.min.js",
            "stomp.umd.min.js", "stomp.umd.min.js");
    private static final MediaType TEXT_JAVASCRIPT = MediaType.parseMediaType("text/javascript;charset=UTF-8");
    private static final MediaType TEXT_CSS = MediaType.parseMediaType("text/css;charset=UTF-8");
    private static final MediaType APPLICATION_YAML = MediaType.parseMediaType("application/yaml;charset=UTF-8");

    private final AsyncApiGenerator generator;
    private final WsDocsProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String pageTemplate;
    private final String stylesheet;

    public WsDocsController(AsyncApiGenerator generator, WsDocsProperties properties) {
        this.generator = generator;
        this.properties = properties;
        this.pageTemplate = readResource("docs.html");
        this.stylesheet = readResource("style.css");
    }

    @GetMapping("${websocket.docs.path:/ws-docs}")
    public ResponseEntity<String> page(HttpServletRequest request) {
        String basePath = request.getContextPath() + properties.getPath();
        String html = pageTemplate
                .replace("__WS_DOCS_BASE__", basePath)
                .replace("__WS_DOCS_CONFIG__", pageConfig(basePath));
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .body(html);
    }

    @GetMapping("${websocket.docs.path:/ws-docs}/asyncapi.yaml")
    public ResponseEntity<String> asyncApiYaml() {
        return ResponseEntity.ok().contentType(APPLICATION_YAML).body(generator.toYaml());
    }

    @GetMapping("${websocket.docs.path:/ws-docs}/asyncapi.json")
    public ResponseEntity<String> asyncApiJson() {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(generator.toJson());
    }

    @GetMapping("${websocket.docs.path:/ws-docs}/style.css")
    public ResponseEntity<String> stylesheet() {
        return ResponseEntity.ok().contentType(TEXT_CSS).body(stylesheet);
    }

    @GetMapping("${websocket.docs.path:/ws-docs}/vendor/{file:[A-Za-z0-9._-]+}")
    public ResponseEntity<byte[]> vendor(@PathVariable String file) {
        String resource = VENDOR_FILES.get(file);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = new ClassPathResource(RESOURCE_ROOT + "vendor/" + resource, getClass().getClassLoader())
                    .getContentAsByteArray();
            return ResponseEntity.ok()
                    .contentType(TEXT_JAVASCRIPT)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
                    .body(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String pageConfig(String basePath) {
        ObjectNode config = mapper.createObjectNode();
        config.put("basePath", basePath);
        config.put("specUrl", basePath + "/asyncapi.json");
        config.put("yamlUrl", basePath + "/asyncapi.yaml");
        config.put("websocketUrl", properties.getServerUrl() == null ? "" : properties.getServerUrl());
        try {
            return mapper.writeValueAsString(config).replace("</", "<\\/");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readResource(String name) {
        try {
            return new ClassPathResource(RESOURCE_ROOT + name, WsDocsController.class.getClassLoader())
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read documentation resource " + name, e);
        }
    }
}
