package generator.config;

import generator.AsyncApiGenerator;
import generator.controller.DocsController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(DocsProperties.class)
public class DocsAutoConfiguration {

    @Bean
    public AsyncApiGenerator asyncApiGenerator(DocsProperties properties) {
        return new AsyncApiGenerator(properties);
    }

    @Bean
    public DocsController docsController(AsyncApiGenerator generator, DocsProperties properties) {
        return new DocsController(generator, properties);
    }
}
