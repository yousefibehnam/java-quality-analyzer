package io.github.yousefibehnam.jqa.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** All types found in one analysed source tree, keyed by fully qualified name. */
public final class ProjectModel {

    private final Map<String, ClassInfo> classes = new TreeMap<>();

    public void add(ClassInfo info) {
        ClassInfo previous = classes.putIfAbsent(info.qualifiedName(), info);
        if (previous != null) {
            throw new IllegalStateException("duplicate type " + info.qualifiedName()
                    + " in " + previous.sourceFile() + " and " + info.sourceFile());
        }
    }

    public boolean contains(String qualifiedName) {
        return classes.containsKey(qualifiedName);
    }

    public Optional<ClassInfo> find(String qualifiedName) {
        return Optional.ofNullable(classes.get(qualifiedName));
    }

    /** Types in alphabetical order of their qualified name. */
    public Collection<ClassInfo> classes() {
        return Collections.unmodifiableCollection(classes.values());
    }

    public int size() {
        return classes.size();
    }

    /** Project types whose direct supertype edge (as used by DIT) points to {@code qualifiedName}. */
    public List<ClassInfo> directSubtypes(String qualifiedName) {
        return classes.values().stream()
                .filter(c -> c.kind().isInterfaceLike()
                        ? c.interfaces().contains(qualifiedName)
                        : c.superclass().map(qualifiedName::equals).orElse(false))
                .toList();
    }
}
