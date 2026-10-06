package io.github.hyeonsulee.wsdocs.internal.model;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.core.ResolvableType;

/**
 * A message payload type with stable keys for use as message and schema names.
 */
public record PayloadType(@NonNull ResolvableType type) implements Comparable<PayloadType> {

    public PayloadType {
        type = concrete(type);
    }

    public static PayloadType of(Type type) {
        return new PayloadType(ResolvableType.forType(type));
    }

    public Type javaType() {
        return type.getType();
    }

    public String key() {
        return key(type);
    }

    public String displayName() {
        return displayName(type);
    }

    public boolean isEnum() {
        return type.toClass().isEnum();
    }

    public List<String> enumNames() {
        return Arrays.stream(type.toClass().getEnumConstants()).map(constant -> ((Enum<?>) constant).name()).toList();
    }

    public PayloadType unwrapOptional() {
        return Optional.class.equals(type.toClass()) ? new PayloadType(type.getGeneric(0)) : this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PayloadType other && key().equals(other.key());
    }

    @Override
    public int hashCode() {
        return key().hashCode();
    }

    @Override
    public int compareTo(PayloadType other) {
        return key().compareTo(other.key());
    }

    // Wildcards and type variables are replaced by their bounds so equal payloads get equal keys regardless of
    // the declaring method's generics.
    private static ResolvableType concrete(ResolvableType t) {
        if (t.isArray()) {
            return ResolvableType.forArrayComponent(concrete(t.getComponentType()));
        }
        Class<?> raw = t.resolve(Object.class);
        if (!t.hasGenerics() || t.getType() instanceof Class) {
            return ResolvableType.forClass(raw);
        }
        ResolvableType[] generics = Arrays.stream(t.getGenerics()).map(PayloadType::concrete).toArray(ResolvableType[]::new);
        return ResolvableType.forClassWithGenerics(raw, generics);
    }

    private static String key(ResolvableType t) {
        if (t.isArray()) {
            return key(t.getComponentType()) + "Array";
        }
        String name = t.toClass().getSimpleName();
        return t.hasGenerics()
                ? name + "_" + Arrays.stream(t.getGenerics()).map(PayloadType::key).collect(Collectors.joining("_"))
                : name;
    }

    private static String displayName(ResolvableType t) {
        if (t.isArray()) {
            return displayName(t.getComponentType()) + "[]";
        }
        String name = t.toClass().getSimpleName();
        return t.hasGenerics()
                ? name + "<" + Arrays.stream(t.getGenerics()).map(PayloadType::displayName).collect(Collectors.joining(", ")) + ">"
                : name;
    }
}
