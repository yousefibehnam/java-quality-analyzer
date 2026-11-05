package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.Fixtures;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CboTest {

    private final Map<String, ClassMetrics> metrics = Fixtures.metrics("coupling");

    @ParameterizedTest(name = "{0}: CBO={1}")
    @CsvSource({
        "Order,    4",
        "Customer, 1",
        "Line,     2",
        "Product,  1",
        "Money,    1",
        "Report,   1",
        "Notifier, 2",
        "Hub,      2",
        "Channel,  2",
    })
    void matchesHandDerivedValues(String type, int cbo) {
        assertThat(metrics.get("fx.cbo." + type).cbo()).isEqualTo(cbo);
    }

    @Test
    void couplingCountsBothDirections() {
        ProjectModel model = Fixtures.model("coupling");
        Cbo cbo = new Cbo(model, false);
        assertThat(cbo.coupledClasses(model.find("fx.cbo.Order").orElseThrow()))
                .containsExactly("fx.cbo.Customer", "fx.cbo.Line", "fx.cbo.Money", "fx.cbo.Report");
    }

    @Test
    void methodCallsThroughChainsCreateCoupling() {
        ProjectModel model = Fixtures.model("coupling");
        Cbo cbo = new Cbo(model, false);
        assertThat(cbo.coupledClasses(model.find("fx.cbo.Notifier").orElseThrow()))
                .containsExactly("fx.cbo.Channel", "fx.cbo.Hub");
    }

    @Test
    void jdkTypesCountOnlyWhenRequested() {
        Map<String, ClassMetrics> withJdk = Fixtures.metrics("coupling", true);
        assertThat(withJdk.get("fx.cbo.Order").cbo()).isEqualTo(6);
    }
}
