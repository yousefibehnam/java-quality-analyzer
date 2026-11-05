package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodInfo;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Lack of Cohesion in Methods.
 *
 * <p>Both variants look at the methods declared in the class that have a body
 * (constructors excluded, static methods included) and at the fields declared in the
 * class (static and instance). A method uses a field when it reads or writes it by
 * simple name, through {@code this}, or through the class name.
 */
public final class Lcom {

    private Lcom() {
    }

    /**
     * The Chidamber and Kemerer (1994) definition: P is the number of method pairs
     * that share no field and Q the number that share at least one;
     * LCOM = P - Q when P &gt; Q, otherwise 0.
     */
    public static int ck(ClassInfo type) {
        List<Set<String>> uses = fieldUsePerMethod(type);
        int sharing = 0;
        int disjoint = 0;
        for (int i = 0; i < uses.size(); i++) {
            for (int j = i + 1; j < uses.size(); j++) {
                Set<String> common = new HashSet<>(uses.get(i));
                common.retainAll(uses.get(j));
                if (common.isEmpty()) {
                    disjoint++;
                } else {
                    sharing++;
                }
            }
        }
        return Math.max(disjoint - sharing, 0);
    }

    /**
     * The Henderson-Sellers (1996) normalised variant:
     * LCOM* = (mean(mu(A)) - m) / (1 - m), where m is the number of methods, a the
     * number of fields and mu(A) the number of methods using field A.
     *
     * <p>0 means every method uses every field; 1 means each field is used by exactly
     * one method; values up to 2 occur when fields are used by no method at all.
     *
     * @return the value, or empty when undefined (fewer than two methods, or no fields)
     */
    public static OptionalDouble hendersonSellers(ClassInfo type) {
        List<MethodInfo> methods = cohesionMethods(type);
        int m = methods.size();
        int a = type.fields().size();
        if (m < 2 || a == 0) {
            return OptionalDouble.empty();
        }
        Set<String> fieldNames = new HashSet<>();
        type.fields().forEach(f -> fieldNames.add(f.name()));
        long uses = 0;
        for (MethodInfo method : methods) {
            uses += method.accessedFields().stream().filter(fieldNames::contains).count();
        }
        double meanUsers = (double) uses / a;
        return OptionalDouble.of((meanUsers - m) / (1 - m));
    }

    static List<MethodInfo> cohesionMethods(ClassInfo type) {
        return type.methods().stream()
                .filter(m -> !m.isConstructor() && m.hasBody())
                .toList();
    }

    private static List<Set<String>> fieldUsePerMethod(ClassInfo type) {
        return cohesionMethods(type).stream()
                .map(m -> (Set<String>) new HashSet<>(m.accessedFields()))
                .toList();
    }
}
