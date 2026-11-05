package io.github.yousefibehnam.jqa.model;

/**
 * A field declared directly in a type.
 *
 * @param name     the field name
 * @param typeName the resolved type name (fully qualified when resolvable)
 * @param isStatic whether the field is static
 */
public record FieldInfo(String name, String typeName, boolean isStatic) {
}
