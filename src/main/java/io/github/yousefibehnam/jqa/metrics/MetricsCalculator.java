package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;

import java.util.ArrayList;
import java.util.List;

/** Computes every metric for every type of a project model. */
public final class MetricsCalculator {

    private final boolean includeJdkInCbo;

    /** @param includeJdkInCbo whether couplings to JDK types count towards CBO */
    public MetricsCalculator(boolean includeJdkInCbo) {
        this.includeJdkInCbo = includeJdkInCbo;
    }

    public MetricsCalculator() {
        this(false);
    }

    public List<ClassMetrics> calculate(ProjectModel model) {
        Cbo cbo = new Cbo(model, includeJdkInCbo);
        List<ClassMetrics> rows = new ArrayList<>();
        for (ClassInfo type : model.classes()) {
            Dit.Result dit = Dit.of(type, model);
            int methods = (int) type.methods().stream().filter(m -> !m.isConstructor()).count();
            rows.add(new ClassMetrics(
                    type.qualifiedName(), type.kind(), type.sourceFile(),
                    methods, type.fields().size(),
                    Wmc.of(type), dit.depth(), dit.exact(), Noc.of(type, model), cbo.of(type), Rfc.of(type),
                    Lcom.ck(type), Lcom.hendersonSellers(type)));
        }
        return rows;
    }
}
