package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.Fixtures;
import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class WmcTest {

    private final ProjectModel model = Fixtures.model("wmc");

    @Test
    void complexityOfEachMethodMatchesHandCount() {
        ClassInfo branchy = model.find("fx.wmc.Branchy").orElseThrow();
        Map<String, Integer> byName = branchy.methods().stream()
                .collect(Collectors.toMap(MethodInfo::name, MethodInfo::cyclomaticComplexity));

        assertThat(byName).containsExactlyInAnyOrderEntriesOf(Map.of(
                "Branchy", 2,
                "straight", 1,
                "ifElse", 2,
                "loops", 5,
                "classicSwitch", 4,
                "arrowSwitch", 4,
                "logic", 3,
                "ternaryCatch", 3));
    }

    @Test
    void wmcSumsAllMethodsAndConstructors() {
        assertThat(Wmc.of(model.find("fx.wmc.Branchy").orElseThrow())).isEqualTo(24);
    }

    @Test
    void methodsWithoutBodyWeighOne() {
        assertThat(Wmc.of(model.find("fx.wmc.Shape").orElseThrow())).isEqualTo(3);
    }
}
