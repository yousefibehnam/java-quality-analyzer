package io.github.yousefibehnam.jqa.build;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.resolve.TypeResolver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Everything needed to resolve names inside one type declaration. */
record TypeContext(ClassInfo info, Node declaration, TypeResolver resolver,
                   List<String> enclosingTypes, Set<String> typeParameters) {

    /** Resolves a declared type to its qualified name; {@code null} for primitives and type variables. */
    String resolve(Type type, Node site) {
        Type element = type instanceof ArrayType array ? array.getElementType() : type;
        if (!(element instanceof ClassOrInterfaceType classType)) {
            return null;
        }
        return resolveName(classType.getNameWithScope(), site);
    }

    String resolveName(String name, Node site) {
        Set<String> inScope = new HashSet<>(typeParameters);
        inScope.addAll(AstWalk.typeParametersInScope(site, declaration));
        return resolver.resolve(name, enclosingTypes, inScope);
    }

    /** A readable type name for signatures: the qualified name, or the source text for primitives. */
    String signatureName(Type type, Node site) {
        String resolved = resolve(type, site);
        if (resolved == null) {
            return type.asString();
        }
        int dims = type instanceof ArrayType array ? array.getArrayLevel() : 0;
        return resolved + "[]".repeat(dims);
    }
}
