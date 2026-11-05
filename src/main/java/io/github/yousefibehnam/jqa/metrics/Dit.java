package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import io.github.yousefibehnam.jqa.resolve.JdkTypes;
import io.github.yousefibehnam.jqa.resolve.TypeResolver;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Depth of Inheritance Tree (Chidamber and Kemerer, 1994).
 *
 * <p>{@code java.lang.Object} is the root with depth 0, so a class without an
 * {@code extends} clause has DIT 1. Enums and records have DIT 2 because they extend
 * {@code java.lang.Enum} and {@code java.lang.Record}. Classes follow their superclass
 * chain; interfaces (and annotation types) follow their deepest super-interface, a
 * root interface having DIT 1.
 *
 * <p>Supertypes inside the analysed sources are followed directly; JDK supertypes are
 * measured by reflection. A supertype that is neither (a third-party library or an
 * unresolvable name) is assumed to extend {@code Object} directly, and the result is
 * marked as not exact: the true depth is at least the reported value.
 */
public final class Dit {

    /** The depth and whether every supertype on the path was known. */
    public record Result(int depth, boolean exact) {
    }

    private Dit() {
    }

    public static Result of(ClassInfo type, ProjectModel model) {
        return depthOf(type, model, new HashSet<>());
    }

    private static Result depthOf(ClassInfo type, ProjectModel model, Set<String> visiting) {
        if (!visiting.add(type.qualifiedName())) {
            return new Result(0, false); // inheritance cycle in broken code
        }
        try {
            List<String> parents = type.kind().isInterfaceLike()
                    ? type.interfaces()
                    : type.superclass().map(List::of).orElse(List.of());
            if (parents.isEmpty()) {
                return new Result(1, true);
            }
            int deepest = 0;
            boolean exact = true;
            for (String parent : parents) {
                Result r = depthOfName(parent, model, visiting);
                deepest = Math.max(deepest, r.depth());
                exact &= r.exact();
            }
            return new Result(deepest + 1, exact);
        } finally {
            visiting.remove(type.qualifiedName());
        }
    }

    private static Result depthOfName(String name, ProjectModel model, Set<String> visiting) {
        Optional<ClassInfo> projectType = model.find(name);
        if (projectType.isPresent()) {
            return depthOf(projectType.get(), model, visiting);
        }
        if (!TypeResolver.isUnresolved(name)) {
            Optional<Class<?>> jdkType = JdkTypes.load(name);
            if (jdkType.isPresent()) {
                return new Result(JdkTypes.depth(jdkType.get()), true);
            }
        }
        return new Result(1, false);
    }
}
