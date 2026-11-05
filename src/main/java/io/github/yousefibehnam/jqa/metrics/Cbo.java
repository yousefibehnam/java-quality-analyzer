package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import io.github.yousefibehnam.jqa.resolve.JdkTypes;
import io.github.yousefibehnam.jqa.resolve.TypeResolver;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Coupling Between Object classes (Chidamber and Kemerer, 1994).
 *
 * <p>Two classes are coupled when one uses the other. A class uses another when it
 * names it anywhere in its body (field, parameter, return, local variable, generic
 * argument, {@code new}, cast, {@code instanceof}, {@code catch}, {@code throws},
 * class literal, supertype, static member access) or when it calls a method whose
 * declaring type was resolved to it.
 *
 * <p>CBO counts the distinct classes coupled in <em>either</em> direction: the types
 * this class uses (efferent) plus the project types that use it (afferent), as in
 * the original definition. The class itself, its use of unresolvable names and, by
 * default, JDK types ({@code java.*}, {@code javax.*}, ...) are excluded. Afferent
 * couplings can only be seen from inside the analysed sources.
 */
public final class Cbo {

    private final boolean includeJdk;
    private final Map<String, Set<String>> afferent = new HashMap<>();

    public Cbo(ProjectModel model, boolean includeJdk) {
        this.includeJdk = includeJdk;
        for (ClassInfo user : model.classes()) {
            for (String used : efferent(user)) {
                if (model.contains(used)) {
                    afferent.computeIfAbsent(used, k -> new LinkedHashSet<>()).add(user.qualifiedName());
                }
            }
        }
    }

    public int of(ClassInfo type) {
        return coupledClasses(type).size();
    }

    /** The coupled classes in alphabetical order, useful for explaining a value. */
    public Set<String> coupledClasses(ClassInfo type) {
        Set<String> coupled = new TreeSet<>(efferent(type));
        coupled.addAll(afferent.getOrDefault(type.qualifiedName(), Set.of()));
        coupled.remove(type.qualifiedName());
        return coupled;
    }

    private Set<String> efferent(ClassInfo type) {
        Set<String> result = new LinkedHashSet<>();
        for (String name : type.referencedTypes()) {
            if (TypeResolver.isUnresolved(name) || name.equals(type.qualifiedName())) {
                continue;
            }
            if (!includeJdk && JdkTypes.isJdkName(name)) {
                continue;
            }
            result.add(name);
        }
        return result;
    }
}
