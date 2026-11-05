package io.github.yousefibehnam.jqa.build;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.FieldInfo;
import io.github.yousefibehnam.jqa.model.MethodInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Finds methods and fields along the supertype chains of project types. */
final class MemberLookup {

    /** Where a method was found: a project method, or only an external supertype name. */
    record Found(String owner, MethodInfo method) {
    }

    private final ProjectModel model;

    MemberLookup(ProjectModel model) {
        this.model = model;
    }

    /**
     * Searches {@code typeName} and its supertypes (breadth first, classes before
     * interfaces) for a method with this name that accepts {@code argCount} arguments.
     * If the search leaves the project before finding it, the first external
     * supertype other than {@code java.lang.Object} is returned as the owner, because
     * the method is most likely inherited from there.
     */
    Optional<Found> findMethod(String typeName, String name, int argCount) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(typeName);
        String firstExternal = null;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!seen.add(current)) {
                continue;
            }
            Optional<ClassInfo> info = model.find(current);
            if (info.isEmpty()) {
                if (firstExternal == null && !current.equals("java.lang.Object")) {
                    firstExternal = current;
                }
                continue;
            }
            for (MethodInfo method : info.get().methods()) {
                if (!method.isConstructor() && method.name().equals(name) && accepts(method, argCount)) {
                    return Optional.of(new Found(current, method));
                }
            }
            info.get().superclass().ifPresent(queue::add);
            queue.addAll(info.get().interfaces());
        }
        return Optional.ofNullable(firstExternal).map(owner -> new Found(owner, null));
    }

    /** Finds a field declared in {@code typeName} or one of its project superclasses. */
    Optional<FieldInfo> findField(String typeName, String name) {
        Set<String> seen = new HashSet<>();
        String current = typeName;
        while (current != null && seen.add(current)) {
            Optional<ClassInfo> info = model.find(current);
            if (info.isEmpty()) {
                return Optional.empty();
            }
            Optional<FieldInfo> field = info.get().field(name);
            if (field.isPresent()) {
                return field;
            }
            current = info.get().superclass().orElse(null);
        }
        return Optional.empty();
    }

    boolean isProjectType(String name) {
        return model.contains(name);
    }

    private static boolean accepts(MethodInfo method, int argCount) {
        int declared = method.parameterCount();
        boolean varArgs = declared > 0 && method.parameterTypes().get(declared - 1).endsWith("...");
        return varArgs ? argCount >= declared - 1 : argCount == declared;
    }
}
