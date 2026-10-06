package io.github.hyeonsulee.wsdocs.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.hyeonsulee.wsdocs.fixture.WebSocketConfig;
import io.github.hyeonsulee.wsdocs.fixture.chat.ChatController;
import io.github.hyeonsulee.wsdocs.internal.asyncapi.AsyncApiGenerator;
import io.github.hyeonsulee.wsdocs.internal.scan.EndpointScanner;
import io.github.hyeonsulee.wsdocs.internal.web.WsDocsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;

class WsDocsAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WsDocsAutoConfiguration.class));

    @Test
    void registersBeansWithoutAnyConfiguration() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(EndpointScanner.class);
            assertThat(context).hasSingleBean(AsyncApiGenerator.class);
            assertThat(context).hasSingleBean(WsDocsController.class);
            assertThat(context.getBean(WsDocsProperties.class).getPath()).isEqualTo("/ws-docs");
        });
    }

    @Test
    void registersNothingWhenDisabled() {
        runner.withPropertyValues("websocket.docs.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EndpointScanner.class);
                    assertThat(context).doesNotHaveBean(AsyncApiGenerator.class);
                    assertThat(context).doesNotHaveBean(WsDocsController.class);
                    assertThat(context).doesNotHaveBean(WsDocsProperties.class);
                });
    }

    @Test
    void registersNothingOutsideServletWebApplications() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WsDocsAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(WsDocsController.class));
    }

    @Test
    void producesEmptyDocumentWithoutBrokerConfiguration() {
        runner.run(context -> {
            var doc = context.getBean(AsyncApiGenerator.class).document();
            assertThat(doc.get("asyncapi").asText()).isEqualTo("3.0.0");
            assertThat(doc.get("operations").isEmpty()).isTrue();
            assertThat(doc.get("channels").isEmpty()).isTrue();
        });
    }

    @Test
    void userProvidedBeansAreRespected() {
        runner.withUserConfiguration(CustomScannerConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EndpointScanner.class);
                    assertThat(context.getBean(EndpointScanner.class)).isSameAs(context.getBean("customScanner"));
                });
    }

    @Test
    void schemasFollowTheApplicationObjectMapper() {
        runner.withUserConfiguration(SnakeCaseConfig.class, WebSocketConfig.class)
                .withBean(ChatController.class)
                .run(context -> {
                    var schema = context.getBean(AsyncApiGenerator.class).document()
                            .get("components").get("schemas").get("UserJoinedEvent");
                    assertThat(schema.get("properties").has("user_name")).isTrue();
                    assertThat(schema.get("properties").has("userName")).isFalse();
                });
    }

    @Test
    void registersNothingWithoutSpringMessagingStomp() {
        runner.withClassLoader(new FilteredClassLoader(SimpAnnotationMethodMessageHandler.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WsDocsController.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class SnakeCaseConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomScannerConfig {

        @Bean
        EndpointScanner customScanner(ListableBeanFactory beanFactory) {
            return new EndpointScanner(beanFactory.getBeanProvider(SimpAnnotationMethodMessageHandler.class),
                    beanFactory.getBeanProvider(UserDestinationMessageHandler.class), beanFactory, "/custom");
        }
    }
}
