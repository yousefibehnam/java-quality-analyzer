package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;

/**
 * Number of Children (Chidamber and Kemerer, 1994).
 *
 * <p>The number of immediate subtypes in the analysed sources, along the same edges
 * DIT uses: classes (including enums and records) count the classes that extend them,
 * and interfaces count the interfaces that extend them. Classes implementing an
 * interface are not its children, because implementation is not class inheritance.
 */
public final class Noc {

    private Noc() {
    }

    public static int of(ClassInfo type, ProjectModel model) {
        return model.directSubtypes(type.qualifiedName()).size();
    }
}
