package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.TypeKind;

import java.util.OptionalDouble;

/**
 * All metric values for one type.
 *
 * @param className  fully qualified name
 * @param kind       class, interface, enum, record or annotation
 * @param sourceFile path relative to the analysed root
 * @param methods    declared methods (constructors excluded)
 * @param fields     declared fields (record components included)
 * @param wmc        weighted methods per class
 * @param dit        depth of inheritance tree
 * @param ditExact   false when DIT is a lower bound (a supertype was outside the JDK and the sources)
 * @param noc        number of children
 * @param cbo        coupling between object classes
 * @param rfc        response for a class
 * @param lcom       lack of cohesion in methods (Chidamber-Kemerer)
 * @param lcomStar   LCOM* (Henderson-Sellers), empty when undefined
 */
public record ClassMetrics(String className, TypeKind kind, String sourceFile,
                           int methods, int fields,
                           int wmc, int dit, boolean ditExact, int noc, int cbo, int rfc,
                           int lcom, OptionalDouble lcomStar) {
}
