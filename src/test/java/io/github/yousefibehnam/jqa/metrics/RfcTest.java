package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.Fixtures;
import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodCall;
import io.github.yousefibehnam.jqa.model.MethodInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RfcTest {

    private final ProjectModel model = Fixtures.model("response");

    @Test
    void accountRfcMatchesHandCount() {
        assertThat(Rfc.of(model.find("fx.rfc.Account").orElseThrow())).isEqualTo(12);
    }

    @Test
    void classWithoutCallsHasRfcEqualToItsMethods() {
        assertThat(Rfc.of(model.find("fx.rfc.Ledger").orElseThrow())).isEqualTo(2);
    }

    @Test
    void callsAreAttributedToTheirDeclaringTypes() {
        ClassInfo account = model.find("fx.rfc.Account").orElseThrow();
        List<String> keys = Stream.concat(
                        account.methods().stream().map(MethodInfo::calls).flatMap(List::stream),
                        account.initializerCalls().stream())
                .map(MethodCall::key)
                .distinct()
                .toList();
        assertThat(keys).containsExactlyInAnyOrder(
                "fx.rfc.Account#<init>/1",
                "fx.rfc.Account#check/1",
                "fx.rfc.Ledger#<init>/0",
                "fx.rfc.Ledger#record/2",
                "fx.rfc.Ledger#flush/0",
                "java.lang.IllegalArgumentException#<init>/1",
                "java.lang.String#valueOf/1",
                "java.lang.String#trim/0");
    }
}
