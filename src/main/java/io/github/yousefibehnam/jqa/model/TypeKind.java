package io.github.yousefibehnam.jqa.model;

/** The kind of a Java type declaration. */
public enum TypeKind {
    CLASS,
    INTERFACE,
    ENUM,
    RECORD,
    ANNOTATION;

    /** Interfaces and annotation types inherit through interface edges only. */
    public boolean isInterfaceLike() {
        return this == INTERFACE || this == ANNOTATION;
    }
}
