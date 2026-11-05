package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.Fixtures;
import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LcomTest {

    private final ProjectModel model = Fixtures.model("cohesion");

    private ClassInfo type(String simpleName) {
        return model.find("fx.lcom." + simpleName).orElseThrow();
    }

    @Test
    void fieldAccessesPerMethodMatchHandAnalysis() {
        Map<String, Set<String>> uses = type("Counter").methods().stream()
                .collect(Collectors.toMap(MethodInfo::name, MethodInfo::accessedFields));
        assertThat(uses).containsEntry("increment", Set.of("count", "step"))
                .containsEntry("reset", Set.of("count"))
                .containsEntry("getLabel", Set.of("label"))
                .containsEntry("help", Set.of());
    }

    @Test
    void counter() {
        assertThat(Lcom.ck(type("Counter"))).isEqualTo(4);
        assertThat(Lcom.hendersonSellers(type("Counter")).getAsDouble()).isCloseTo(8.0 / 9.0, within(1e-9));
    }

    @Test
    void localVariableShadowsFieldOfTheSameName() {
        Map<String, Set<String>> uses = type("Vector2").methods().stream()
                .collect(Collectors.toMap(MethodInfo::name, MethodInfo::accessedFields));
        assertThat(uses.get("setX")).containsExactly("x");
        assertThat(Lcom.ck(type("Vector2"))).isZero();
        assertThat(Lcom.hendersonSellers(type("Vector2")).getAsDouble()).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void lcomStarIsUndefinedForOneMethodOrNoFields() {
        assertThat(Lcom.ck(type("Single"))).isZero();
        assertThat(Lcom.hendersonSellers(type("Single"))).isEmpty();
        assertThat(Lcom.ck(type("Stateless"))).isEqualTo(1);
        assertThat(Lcom.hendersonSellers(type("Stateless"))).isEmpty();
    }
}
