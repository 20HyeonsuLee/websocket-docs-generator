package generator.config;

import generator.controller.DocsController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DocsAutoConfiguration {

    @Bean
    public DocsController docsController() {
        return new DocsController();
    }
}
