package io.github.yousefibehnam.jqa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A top-level or member type declared in the analysed source code.
 *
 * <p>Nested member types are separate {@code ClassInfo} instances whose fully
 * qualified name uses dots, e.g. {@code com.acme.Outer.Inner}. Anonymous and
 * local classes are not modelled separately; their bodies belong to the
 * enclosing method.
 */
public final class ClassInfo {

    private final String qualifiedName;
    private final String packageName;
    private final TypeKind kind;
    private final String sourceFile;
    private String superclass;
    private final List<String> interfaces = new ArrayList<>();
    private final List<FieldInfo> fields = new ArrayList<>();
    private final List<MethodInfo> methods = new ArrayList<>();
    private final List<MethodCall> initializerCalls = new ArrayList<>();
    private final Set<String> referencedTypes = new LinkedHashSet<>();

    public ClassInfo(String qualifiedName, String packageName, TypeKind kind, String sourceFile) {
        this.qualifiedName = qualifiedName;
        this.packageName = packageName;
        this.kind = kind;
        this.sourceFile = sourceFile;
    }

    public String qualifiedName() {
        return qualifiedName;
    }

    public String simpleName() {
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? qualifiedName : qualifiedName.substring(dot + 1);
    }

    public String packageName() {
        return packageName;
    }

    public TypeKind kind() {
        return kind;
    }

    public String sourceFile() {
        return sourceFile;
    }

    /** The direct superclass; {@code null} for interfaces and annotation types. */
    public Optional<String> superclass() {
        return Optional.ofNullable(superclass);
    }

    public void setSuperclass(String superclass) {
        this.superclass = superclass;
    }

    /** Implemented interfaces (classes) or extended interfaces (interfaces). */
    public List<String> interfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    public void addInterface(String name) {
        interfaces.add(name);
    }

    public List<FieldInfo> fields() {
        return Collections.unmodifiableList(fields);
    }

    public void addField(FieldInfo field) {
        fields.add(field);
    }

    public Optional<FieldInfo> field(String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst();
    }

    public List<MethodInfo> methods() {
        return Collections.unmodifiableList(methods);
    }

    public void addMethod(MethodInfo method) {
        methods.add(method);
    }

    /** Calls made from field initializers and initializer blocks. */
    public List<MethodCall> initializerCalls() {
        return Collections.unmodifiableList(initializerCalls);
    }

    public void addInitializerCall(MethodCall call) {
        initializerCalls.add(call);
    }

    /** Resolved names of all other types this type refers to (never includes itself). */
    public Set<String> referencedTypes() {
        return Collections.unmodifiableSet(referencedTypes);
    }

    public void addReferencedType(String typeName) {
        if (typeName != null && !typeName.equals(qualifiedName)) {
            referencedTypes.add(typeName);
        }
    }

    @Override
    public String toString() {
        return kind + " " + qualifiedName;
    }
}
