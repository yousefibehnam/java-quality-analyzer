package io.github.yousefibehnam.jqa.build;

import io.github.yousefibehnam.jqa.Fixtures;
import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.FieldInfo;
import io.github.yousefibehnam.jqa.model.MethodCall;
import io.github.yousefibehnam.jqa.model.MethodInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import io.github.yousefibehnam.jqa.model.TypeKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelBuilderTest {

    private final ProjectModel model = Fixtures.model("structure");

    private MethodInfo method(String type, String name) {
        return model.find(type).orElseThrow().methods().stream()
                .filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void memberTypesAreModelledAndAnonymousClassesAreNot() {
        assertThat(model.classes()).extracting(ClassInfo::qualifiedName)
                .containsExactly("fx.model.Outer", "fx.model.Outer.Inner", "fx.model.Outer.Nested", "fx.model.Pair");
        assertThat(model.find("fx.model.Pair").orElseThrow().kind()).isEqualTo(TypeKind.RECORD);
    }

    @Test
    void anonymousClassBodyBelongsToEnclosingMethod() {
        assertThat(method("fx.model.Outer", "supplier").accessedFields()).containsExactly("hits");
        assertThat(model.find("fx.model.Outer").orElseThrow().methods())
                .extracting(MethodInfo::name).doesNotContain("get");
    }

    @Test
    void innerClassDoesNotOwnOuterFields() {
        assertThat(method("fx.model.Outer.Inner", "read").accessedFields()).isEmpty();
    }

    @Test
    void staticImportsResolveCallOwner() {
        assertThat(method("fx.model.Outer", "name").calls())
                .extracting(MethodCall::key).containsExactly("java.util.Objects#requireNonNull/1");
    }

    @Test
    void recordComponentsAreFieldsAndCompactConstructorIsAConstructor() {
        ClassInfo pair = model.find("fx.model.Pair").orElseThrow();
        assertThat(pair.fields()).extracting(FieldInfo::name).containsExactly("left", "right");
        assertThat(pair.methods()).filteredOn(MethodInfo::isConstructor).hasSize(1);
        assertThat(method("fx.model.Pair", "size").accessedFields()).containsExactlyInAnyOrder("left", "right");
        assertThat(method("fx.model.Pair", "size").calls())
                .extracting(MethodCall::key).containsOnly("java.lang.String#length/0");
    }
}
