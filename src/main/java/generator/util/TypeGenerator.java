package generator.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeGenerator {

    public static Type generateType(Class<?> returnType, Class<?> genericType) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{genericType};
            }

            @Override
            public Type getRawType() {
                return returnType;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public String toString() {
                if (genericType == Void.class) {
                    return returnType.getSimpleName();
                }
                // JSON 호환성을 위해 <> 대신 _ 사용
                return returnType.getSimpleName() + "_" + genericType.getSimpleName();
            }
        };
    }
}
