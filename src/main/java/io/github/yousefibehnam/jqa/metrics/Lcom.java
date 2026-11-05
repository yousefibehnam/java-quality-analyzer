package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodInfo;

import java.util.HashSet;
import java.util.List;
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
