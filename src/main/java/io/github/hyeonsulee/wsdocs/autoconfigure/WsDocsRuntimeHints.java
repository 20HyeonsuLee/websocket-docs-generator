package io.github.hyeonsulee.wsdocs.autoconfigure;

import io.github.hyeonsulee.wsdocs.api.WsHidden;
import io.github.hyeonsulee.wsdocs.api.WsOperation;
import io.github.hyeonsulee.wsdocs.api.WsPublication;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM hints for the public annotations and the bundled UI resources. Application DTOs must be registered by the application itself.
 */
class WsDocsRuntimeHints implements RuntimeHintsRegistrar {

    private static final Class<?>[] ANNOTATIONS = {
            WsOperation.class, WsPublication.class, WsPublication.List.class, WsHidden.class};

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources()
                .registerPattern("ws-docs/*")
                .registerPattern("ws-docs/vendor/*")
                .registerPattern("META-INF/third-party/*");
        for (Class<?> annotation : ANNOTATIONS) {
            hints.reflection().registerType(annotation, MemberCategory.INVOKE_DECLARED_METHODS);
            hints.proxies().registerJdkProxy(annotation);
        }
    }
}
