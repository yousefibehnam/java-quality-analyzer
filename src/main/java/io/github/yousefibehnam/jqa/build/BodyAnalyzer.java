package io.github.yousefibehnam.jqa.build;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import io.github.yousefibehnam.jqa.model.MethodInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analyses one callable body (method, constructor or initializer) of a type:
 * which of the type's own fields it touches.
 *
 * <p>Local scopes are flattened: every parameter, local variable, lambda parameter,
 * catch parameter and pattern variable declared anywhere in the body shadows a field
 * of the same name for the whole body. This is exact for the common case where a
 * name is not reused for both a local and a field inside one method.
 */
final class BodyAnalyzer {

    private final TypeContext context;
    private final Set<String> ownFields;

    BodyAnalyzer(TypeContext context) {
        this.context = context;
        this.ownFields = context.info().fields().stream()
                .map(f -> f.name())
                .collect(Collectors.toSet());
    }

    /** Collects field accesses of {@code body} into {@code method}. */
    void analyze(Node body, MethodInfo method) {
        Map<String, String> locals = localVariables(body);
        AstWalk.ownNodes(body, node -> {
            String field = accessedOwnField(node, locals);
            if (field != null) {
                method.addAccessedField(field);
            }
        });
    }

    /** Local names declared in {@code body}, mapped to their resolved type (or {@code null}). */
    Map<String, String> localVariables(Node body) {
        Map<String, String> locals = new HashMap<>();
        for (Parameter p : body.findAll(Parameter.class)) {
            locals.put(p.getNameAsString(), p.getType().isUnknownType() ? null : context.resolve(p.getType(), p));
        }
        for (VariableDeclarator v : body.findAll(VariableDeclarator.class)) {
            if (v.getParentNode().filter(p -> p instanceof com.github.javaparser.ast.body.FieldDeclaration).isPresent()
                    && v.getParentNode().get().getParentNode().filter(p -> p == context.declaration()).isPresent()) {
                continue; // a field of this very type, not a local
            }
            locals.put(v.getNameAsString(), declaredOrInferredType(v));
        }
        for (TypePatternExpr pattern : body.findAll(TypePatternExpr.class)) {
            locals.put(pattern.getNameAsString(), context.resolve(pattern.getType(), pattern));
        }
        return locals;
    }

    private String declaredOrInferredType(VariableDeclarator v) {
        if (!v.getType().isVarType()) {
            return context.resolve(v.getType(), v);
        }
        return v.getInitializer()
                .filter(init -> init instanceof ObjectCreationExpr)
                .map(init -> context.resolve(((ObjectCreationExpr) init).getType(), init))
                .orElse(null);
    }

    private String accessedOwnField(Node node, Map<String, String> locals) {
        if (node instanceof NameExpr name) {
            String id = name.getNameAsString();
            return ownFields.contains(id) && !locals.containsKey(id) ? id : null;
        }
        if (node instanceof FieldAccessExpr access && ownFields.contains(access.getNameAsString())
                && refersToThisType(access.getScope())) {
            return access.getNameAsString();
        }
        return null;
    }

    /** {@code this}, {@code Self.this} or the type's own simple name (static access). */
    boolean refersToThisType(Node scope) {
        String simpleName = context.info().simpleName();
        if (scope instanceof ThisExpr self) {
            return self.getTypeName().map(t -> t.getIdentifier().equals(simpleName)).orElse(true);
        }
        return scope instanceof NameExpr name && name.getNameAsString().equals(simpleName);
    }
}
