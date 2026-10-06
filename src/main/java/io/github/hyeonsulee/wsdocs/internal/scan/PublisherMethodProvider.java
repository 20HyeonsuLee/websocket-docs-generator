package io.github.hyeonsulee.wsdocs.internal.scan;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.util.ClassUtils;

/**
 * Finds non-handler bean methods that declare publications.
 */
@CommonsLog
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class PublisherMethodProvider {

    private final ListableBeanFactory beanFactory;

    // Spring keeps no index of annotated methods on arbitrary beans, so the container is walked by hand:
    // bean names -> user classes -> classes that can carry our annotations -> marked methods.
    List<BeanMethod> findPublisherMethods() {
        return Arrays.stream(beanFactory.getBeanDefinitionNames())
                .map(this::userType)
                .flatMap(Optional::stream)
                .filter(PublisherMethodProvider::isCandidateClass)
                .flatMap(PublisherMethodProvider::publisherMethods)
                .toList();
    }

    // allowInit=false avoids instantiating FactoryBeans just to learn their type, so the type can be unknown.
    // getUserClass strips CGLIB proxies (e.g. @Transactional services) so Source names the real class.
    private Optional<Class<?>> userType(final String beanName) {
        return Optional.ofNullable(beanFactory.getType(beanName, false)).map(ClassUtils::getUserClass);
    }

    // Cheap pre-check that skips JDK and framework classes before any method is inspected reflectively.
    private static boolean isCandidateClass(final Class<?> type) {
        return AnnotationUtils.isCandidateClass(type, WsAnnotationReader.MARKERS) && !WsAnnotationReader.isHidden(type);
    }

    // MethodIntrospector also finds inherited methods and annotations declared on interfaces.
    private static Stream<BeanMethod> publisherMethods(final Class<?> type) {
        return MethodIntrospector.selectMethods(type, PublisherMethodProvider::isCandidateMethod).stream()
                .map(method -> new BeanMethod(type, method))
                .filter(method -> !method.isHidden())
                .filter(PublisherMethodProvider::declaresPublication);
    }

    private static boolean isCandidateMethod(final Method method) {
        // Compiler-generated duplicates of generic overrides would document the same publication twice.
        if (method.isBridge() || method.isSynthetic()) {
            return false;
        }
        // Handlers come from the Spring registry; their publications are read on the receive side.
        if (AnnotatedElementUtils.hasAnnotation(method, MessageMapping.class)
                || AnnotatedElementUtils.hasAnnotation(method, SubscribeMapping.class)) {
            return false;
        }
        return WsAnnotationReader.isMarked(method);
    }

    private static boolean declaresPublication(final BeanMethod method) {
        if (WsAnnotationReader.declaresPublication(method.method())) {
            return true;
        }
        log.warn(String.format("WebSocket docs: @WsOperation on %s is ignored because the method is neither a "
                + "@MessageMapping/@SubscribeMapping handler nor declares any publication.", method.method()));
        return false;
    }
}
