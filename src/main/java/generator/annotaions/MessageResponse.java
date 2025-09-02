package generator.annotaions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageResponse {
    String path();
    Class<?> returnType();
    Class<?> genericType() default Void.class; // 옵셔널 제네릭 타입
}
