package io.github.yousefibehnam.jqa.build;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import io.github.yousefibehnam.jqa.model.FieldInfo;
import io.github.yousefibehnam.jqa.model.MethodCall;
import io.github.yousefibehnam.jqa.resolve.JdkTypes;
import io.github.yousefibehnam.jqa.resolve.TypeResolver;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Finds the method and constructor calls in a body and determines, as far as
 * possible without a compiler, which type declares the called method.
 *
 * <p>The receiver of a call is typed from: {@code this}/{@code super}; local
 * variables and parameters; fields of the type, its project superclasses and its
 * enclosing types; type names (static calls); {@code new} expressions; casts;
 * string literals; and the declared return type of project methods in call chains.
 * Anything else is recorded with the owner {@link MethodCall#UNKNOWN_OWNER}.
 * Method references ({@code Foo::bar}) are not counted as calls.
 */
final class CallCollector {

    private static final Set<String> OBJECT_METHODS = Set.of(
            "equals", "hashCode", "toString", "getClass", "clone", "finalize", "notify", "notifyAll", "wait");

    private final TypeContext context;
    private final MemberLookup lookup;
    private final BodyAnalyzer analyzer;

    CallCollector(TypeContext context, MemberLookup lookup) {
        this.context = context;
        this.lookup = lookup;
        this.analyzer = new BodyAnalyzer(context);
    }

    void collect(Node body, Consumer<MethodCall> sink) {
        Map<String, String> locals = analyzer.localVariables(body);
        AstWalk.ownNodes(body, node -> {
            if (node instanceof MethodCallExpr call) {
                sink.accept(new MethodCall(ownerOf(call, locals), call.getNameAsString(), call.getArguments().size()));
            } else if (node instanceof ObjectCreationExpr creation) {
                String type = context.resolve(creation.getType(), creation);
                sink.accept(new MethodCall(type == null ? MethodCall.UNKNOWN_OWNER : type,
                        MethodCall.CONSTRUCTOR, creation.getArguments().size()));
            } else if (node instanceof ExplicitConstructorInvocationStmt explicit) {
                String owner = explicit.isThis()
                        ? self()
                        : context.info().superclass().orElse(MethodCall.UNKNOWN_OWNER);
                sink.accept(new MethodCall(owner, MethodCall.CONSTRUCTOR, explicit.getArguments().size()));
            }
        });
    }

    private String ownerOf(MethodCallExpr call, Map<String, String> locals) {
        String name = call.getNameAsString();
        int args = call.getArguments().size();
        Optional<Expression> scope = call.getScope();
        if (scope.isEmpty() || isPlainThis(scope.get())) {
            return unqualifiedOwner(name, args);
        }
        String receiver = typeOf(scope.get(), locals);
        if (receiver == null) {
            return MethodCall.UNKNOWN_OWNER;
        }
        if (lookup.isProjectType(receiver)) {
            return lookup.findMethod(receiver, name, args).map(MemberLookup.Found::owner).orElse(receiver);
        }
        return receiver;
    }

    /** An unqualified call: own hierarchy, then enclosing types, then static imports. */
    private String unqualifiedOwner(String name, int args) {
        for (String type : context.enclosingTypes()) {
            Optional<MemberLookup.Found> found = lookup.findMethod(type, name, args);
            if (found.isPresent() && found.get().method() != null) {
                return found.get().owner();
            }
        }
        TypeResolver resolver = context.resolver();
        String imported = resolver.staticImportOwner(name);
        if (imported != null) {
            return imported;
        }
        for (String owner : resolver.staticOnDemandOwners()) {
            if (lookup.isProjectType(owner) && lookup.findMethod(owner, name, args)
                    .filter(f -> f.method() != null).isPresent()) {
                return owner;
            }
        }
        Optional<MemberLookup.Found> external = lookup.findMethod(self(), name, args);
        if (external.isPresent()) {
            return external.get().owner();
        }
        if (resolver.staticOnDemandOwners().size() == 1) {
            return resolver.staticOnDemandOwners().get(0);
        }
        return OBJECT_METHODS.contains(name) ? "java.lang.Object" : MethodCall.UNKNOWN_OWNER;
    }

    /** The static type of an expression used as a call receiver, or {@code null}. */
    private String typeOf(Expression expr, Map<String, String> locals) {
        if (expr instanceof ThisExpr self) {
            return self.getTypeName()
                    .map(t -> context.resolveName(t.asString(), self))
                    .orElse(self());
        }
        if (expr instanceof SuperExpr) {
            return context.info().superclass().orElse(null);
        }
        if (expr instanceof EnclosedExpr enclosed) {
            return typeOf(enclosed.getInner(), locals);
        }
        if (expr instanceof CastExpr cast) {
            return usable(context.resolve(cast.getType(), cast));
        }
        if (expr instanceof StringLiteralExpr || expr instanceof TextBlockLiteralExpr) {
            return "java.lang.String";
        }
        if (expr instanceof ObjectCreationExpr creation) {
            return usable(context.resolve(creation.getType(), creation));
        }
        if (expr instanceof NameExpr name) {
            return typeOfName(name, locals);
        }
        if (expr instanceof FieldAccessExpr access) {
            return typeOfFieldAccess(access, locals);
        }
        if (expr instanceof MethodCallExpr call) {
            String owner = ownerOf(call, locals);
            if (!lookup.isProjectType(owner)) {
                return JdkTypes.returnType(owner, call.getNameAsString(), call.getArguments().size()).orElse(null);
            }
            return lookup.findMethod(owner, call.getNameAsString(), call.getArguments().size())
                    .map(MemberLookup.Found::method)
                    .map(m -> usable(m.returnType()))
                    .orElse(null);
        }
        return null;
    }

    private String typeOfName(NameExpr name, Map<String, String> locals) {
        String id = name.getNameAsString();
        if (locals.containsKey(id)) {
            return usable(locals.get(id));
        }
        for (String type : context.enclosingTypes()) {
            Optional<FieldInfo> field = lookup.findField(type, id);
            if (field.isPresent()) {
                return usable(field.get().typeName());
            }
        }
        String asType = context.resolveName(id, name);
        return asType == null || TypeResolver.isUnresolved(asType) ? null : asType;
    }

    private String typeOfFieldAccess(FieldAccessExpr access, Map<String, String> locals) {
        String scopeType = typeOf(access.getScope(), locals);
        if (scopeType != null && lookup.isProjectType(scopeType)) {
            Optional<FieldInfo> field = lookup.findField(scopeType, access.getNameAsString());
            if (field.isPresent()) {
                return usable(field.get().typeName());
            }
            String member = scopeType + "." + access.getNameAsString();
            if (lookup.isProjectType(member)) {
                return member;
            }
        }
        if (scopeType != null && JdkTypes.isJdkName(scopeType)) {
            Optional<String> jdkField = JdkTypes.fieldType(scopeType, access.getNameAsString());
            if (jdkField.isPresent()) {
                return jdkField.get();
            }
        }
        // A qualified type name such as java.util.Collections or Outer.Inner. A static
        // field such as System.out is not a type: its type is unknown without the JDK sources.
        String asType = context.resolveName(access.toString(), access);
        return asType != null && (lookup.isProjectType(asType) || JdkTypes.exists(asType)) ? asType : null;
    }

    private static boolean isPlainThis(Expression expr) {
        return expr instanceof ThisExpr self && self.getTypeName().isEmpty();
    }

    private String self() {
        return context.info().qualifiedName();
    }

    /** Arrays, primitives and unresolved names cannot be used to find a declaring type. */
    private static String usable(String typeName) {
        if (typeName == null || typeName.endsWith("]") || typeName.endsWith("...")
                || TypeResolver.isUnresolved(typeName) || !typeName.contains(".") && isPrimitive(typeName)) {
            return null;
        }
        return typeName;
    }

    private static boolean isPrimitive(String name) {
        return Set.of("boolean", "byte", "char", "short", "int", "long", "float", "double").contains(name);
    }
}
