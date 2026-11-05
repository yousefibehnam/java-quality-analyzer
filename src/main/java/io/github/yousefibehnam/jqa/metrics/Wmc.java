package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodInfo;

/**
 * Weighted Methods per Class (Chidamber and Kemerer, 1994).
 *
 * <p>The sum of the McCabe cyclomatic complexity of every method and constructor
 * declared in the class. A method without a body (abstract or interface method)
 * weighs 1. Implicit members, such as a default constructor or record accessors,
 * are not counted because they are not written in the source.
 */
public final class Wmc {

    private Wmc() {
    }

    public static int of(ClassInfo type) {
        return type.methods().stream().mapToInt(MethodInfo::cyclomaticComplexity).sum();
    }
}
