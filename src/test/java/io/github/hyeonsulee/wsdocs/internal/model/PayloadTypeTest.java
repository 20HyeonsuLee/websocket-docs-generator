package io.github.hyeonsulee.wsdocs.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hyeonsulee.wsdocs.fixture.chat.User;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;

class PayloadTypeTest {

    @SuppressWarnings("unused")
    private static class Holder {
        List<User> users;
        Map<String, List<User>> scoreboard;
        Optional<String> nickname;
        String[] names;
        Set<Integer> ids;
        List<? extends User> wildcard;
    }

    private static Type fieldType(String name) throws NoSuchFieldException {
        return Holder.class.getDeclaredField(name).getGenericType();
    }

    @Test
    void namesGenericTypesWithSimpleNames() throws Exception {
        PayloadType type = PayloadType.of(fieldType("users"));
        assertThat(type.key()).isEqualTo("List_User");
        assertThat(type.displayName()).isEqualTo("List<User>");
    }

    @Test
    void nestedGenericsHaveNoDepthLimit() throws Exception {
        PayloadType type = PayloadType.of(fieldType("scoreboard"));
        assertThat(type.key()).isEqualTo("Map_String_List_User");
        assertThat(type.displayName()).isEqualTo("Map<String, List<User>>");
    }

    @Test
    void arraysAndCollections() throws Exception {
        assertThat(PayloadType.of(fieldType("names")).key()).isEqualTo("StringArray");
        assertThat(PayloadType.of(fieldType("names")).displayName()).isEqualTo("String[]");
        assertThat(PayloadType.of(fieldType("ids")).key()).isEqualTo("Set_Integer");
        assertThat(PayloadType.of(fieldType("nickname")).displayName()).isEqualTo("Optional<String>");
    }

    @Test
    void wildcardResolvesToUpperBound() throws Exception {
        PayloadType type = PayloadType.of(fieldType("wildcard"));
        assertThat(type.key()).isEqualTo("List_User");
        assertThat(type.displayName()).isEqualTo("List<User>");
    }

    @Test
    void enumDetection() {
        assertThat(PayloadType.of(io.github.hyeonsulee.wsdocs.fixture.chat.UserType.class).isEnum()).isTrue();
        assertThat(PayloadType.of(User.class).isEnum()).isFalse();
    }

    @Test
    void reflectedAndSynthesizedTypesAreEqual() throws Exception {
        PayloadType reflected = PayloadType.of(fieldType("users"));
        PayloadType synthesized = PayloadType.of(ResolvableType.forClassWithGenerics(List.class, User.class).getType());
        assertThat(reflected).isEqualTo(synthesized);
        assertThat(reflected.hashCode()).isEqualTo(synthesized.hashCode());
    }
}
