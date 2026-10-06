package io.github.hyeonsulee.wsdocs.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hyeonsulee.wsdocs.internal.asyncapi.AsyncApiGenerator;
import io.github.hyeonsulee.wsdocs.internal.model.ApiInfo;
import io.github.hyeonsulee.wsdocs.internal.scan.EndpointScanner;
import io.github.hyeonsulee.wsdocs.internal.web.WsDocsController;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;

/**
 * Registers the scanner, the generator and the docs controller unless the application defines its own.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SimpAnnotationMethodMessageHandler.class)
@ConditionalOnProperty(prefix = "websocket.docs", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WsDocsProperties.class)
@ImportRuntimeHints(WsDocsRuntimeHints.class)
public class WsDocsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EndpointScanner wsDocsEndpointScanner(ObjectProvider<SimpAnnotationMethodMessageHandler> handlers,
                                                 ObjectProvider<UserDestinationMessageHandler> userDestinations,
                                                 ListableBeanFactory beanFactory, WsDocsProperties properties) {
        return new EndpointScanner(handlers, userDestinations, beanFactory, properties.getDefaultDestinationPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncApiGenerator wsDocsAsyncApiGenerator(EndpointScanner scanner, WsDocsProperties properties,
                                                     ObjectProvider<ObjectMapper> objectMapper) {
        WsDocsProperties.Info info = properties.getInfo();
        return new AsyncApiGenerator(scanner, ApiInfo.of(info.getTitle(), info.getVersion(), info.getDescription()),
                objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public WsDocsController wsDocsController(AsyncApiGenerator generator, WsDocsProperties properties) {
        return new WsDocsController(generator, properties);
    }
}
