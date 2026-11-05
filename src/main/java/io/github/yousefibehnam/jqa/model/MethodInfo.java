package io.github.yousefibehnam.jqa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A method or constructor declared directly in a type. */
public final class MethodInfo {

    private final String name;
    private final List<String> parameterTypes;
    private final boolean constructor;
    private final boolean isStatic;
    private final boolean hasBody;
    private int cyclomaticComplexity = 1;
    private final Set<String> accessedFields = new LinkedHashSet<>();
    private final List<MethodCall> calls = new ArrayList<>();
    private String returnType;

    public MethodInfo(String name, List<String> parameterTypes, boolean constructor,
                      boolean isStatic, boolean hasBody) {
        this.name = name;
        this.parameterTypes = List.copyOf(parameterTypes);
        this.constructor = constructor;
        this.isStatic = isStatic;
        this.hasBody = hasBody;
    }

    public String name() {
        return name;
    }

    public List<String> parameterTypes() {
        return parameterTypes;
    }

    public int parameterCount() {
        return parameterTypes.size();
    }

    public boolean isConstructor() {
        return constructor;
    }

    public boolean isStatic() {
        return isStatic;
    }

    /** False for abstract and interface methods without a default body. */
    public boolean hasBody() {
        return hasBody;
    }

    public int cyclomaticComplexity() {
        return cyclomaticComplexity;
    }

    public void setCyclomaticComplexity(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("cyclomatic complexity must be >= 1: " + value);
        }
        this.cyclomaticComplexity = value;
    }

    /** Names of fields of the declaring type that this method reads or writes. */
    public Set<String> accessedFields() {
        return Collections.unmodifiableSet(accessedFields);
    }

    public void addAccessedField(String fieldName) {
        accessedFields.add(fieldName);
    }

    public List<MethodCall> calls() {
        return Collections.unmodifiableList(calls);
    }

    public void addCall(MethodCall call) {
        calls.add(call);
    }

    /** Resolved return type, or {@code null} for constructors and {@code void}. */
    public String returnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /** Signature used to tell overloads apart, e.g. {@code add(int,java.lang.String)}. */
    public String signature() {
        return (constructor ? MethodCall.CONSTRUCTOR : name) + "(" + String.join(",", parameterTypes) + ")";
    }

    @Override
    public String toString() {
        return signature();
    }
}
