package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DitNocTest {

    private final Map<String, ClassMetrics> metrics = Fixtures.metrics("inheritance");

    @ParameterizedTest(name = "{0}: DIT={1}, NOC={2}")
    @CsvSource({
        "Animal, 1, 2",
        "Dog,    2, 1",
        "Cat,    2, 0",
        "Puppy,  3, 0",
        "MyList, 4, 0",
        "Widget, 2, 0",
        "Color,  2, 0",
        "Point,  2, 0",
        "Named,  1, 1",
        "Titled, 2, 0",
        "Book,   1, 0",
    })
    void matchesHandDerivedValues(String type, int dit, int noc) {
        ClassMetrics m = metrics.get("fx.inh." + type);
        assertThat(m.dit()).as("DIT").isEqualTo(dit);
        assertThat(m.noc()).as("NOC").isEqualTo(noc);
    }

    @Test
    void ditIsExactOnlyWhenEverySupertypeIsKnown() {
        assertThat(metrics.get("fx.inh.MyList").ditExact()).isTrue();
        assertThat(metrics.get("fx.inh.Puppy").ditExact()).isTrue();
        assertThat(metrics.get("fx.inh.Widget").ditExact()).isFalse();
    }
}
