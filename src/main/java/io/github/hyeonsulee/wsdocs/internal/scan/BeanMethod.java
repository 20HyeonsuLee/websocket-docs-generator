package io.github.hyeonsulee.wsdocs.internal.scan;

import io.github.hyeonsulee.wsdocs.internal.model.Source;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.IntStream;
import lombok.NonNull;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * A method on a user class (proxies unwrapped) with the reflection helpers the scan needs.
 */
record BeanMethod(@NonNull Class<?> type, @NonNull Method method) {

    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    static BeanMethod of(final Class<?> beanType, final Method method) {
        return new BeanMethod(ClassUtils.getUserClass(beanType), method);
    }

    Source source() {
        return Source.of(type, method);
    }

    boolean isHidden() {
        return WsAnnotationReader.isHidden(type) || WsAnnotationReader.isHidden(method);
    }

    boolean returnsVoid() {
        final var returnClass = method.getReturnType();
        return returnClass == void.class || returnClass == Void.class;
    }

    ResolvableType returnType() {
        return ResolvableType.forMethodReturnType(method, type);
    }

    List<MethodParameter> parameters() {
        return IntStream.range(0, method.getParameterCount())
                .mapToObj(this::parameter)
                .toList();
    }

    private MethodParameter parameter(final int index) {
        final var parameter = new MethodParameter(method, index).withContainingClass(type);
        parameter.initParameterNameDiscovery(PARAMETER_NAMES);
        return parameter;
    }
}
