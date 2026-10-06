package io.github.hyeonsulee.wsdocs.internal.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Renders a JSON tree as block-style YAML.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class YamlWriter {

    private static final DumperOptions OPTIONS = new DumperOptions();

    static {
        OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        OPTIONS.setPrettyFlow(true);
        OPTIONS.setIndent(2);
        OPTIONS.setSplitLines(false);
        OPTIONS.setAllowUnicode(true);
    }

    static String write(JsonNode node, ObjectMapper mapper) {
        return new Yaml(OPTIONS).dump(mapper.convertValue(node, Object.class));
    }
}
