package io.github.yousefibehnam.jqa.build;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.FieldInfo;
import io.github.yousefibehnam.jqa.model.MethodInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import io.github.yousefibehnam.jqa.model.TypeKind;
import io.github.yousefibehnam.jqa.parser.SourceScanner.ParsedFile;
import io.github.yousefibehnam.jqa.resolve.TypeResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a {@link ProjectModel} from parsed compilation units in two passes:
 * the first pass collects the qualified names of all declared types so that the
 * second pass can resolve references between them.
 */
public final class ModelBuilder {

    public ProjectModel build(List<ParsedFile> files, Path root) {
        Set<String> projectTypes = new HashSet<>();
        for (ParsedFile file : files) {
            String pkg = packageOf(file.unit());
            for (TypeDeclaration<?> type : file.unit().getTypes()) {
                collectTypeNames(type, pkg, projectTypes);
            }
        }

        ProjectModel model = new ProjectModel();
        for (ParsedFile file : files) {
            CompilationUnit unit = file.unit();
            String pkg = packageOf(unit);
            TypeResolver resolver = new TypeResolver(projectTypes, pkg, unit.getImports());
            String source = relativeSource(root, file.path());
            for (TypeDeclaration<?> type : unit.getTypes()) {
                buildType(type, pkg, List.of(), Set.of(), resolver, source, model);
            }
        }
        return model;
    }

    private static void collectTypeNames(TypeDeclaration<?> type, String prefix, Set<String> out) {
        String name = qualify(prefix, type.getNameAsString());
        out.add(name);
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                collectTypeNames(nested, name, out);
            }
        }
    }

    private void buildType(TypeDeclaration<?> type, String prefix, List<String> outerTypes,
                           Set<String> outerTypeParameters, TypeResolver resolver,
                           String source, ProjectModel model) {
        String qualifiedName = qualify(prefix, type.getNameAsString());
        List<String> enclosing = new ArrayList<>();
        enclosing.add(qualifiedName);
        enclosing.addAll(outerTypes);
        Set<String> typeParameters = new HashSet<>(outerTypeParameters);
        if (type instanceof NodeWithTypeParameters<?> generic) {
            generic.getTypeParameters().forEach(tp -> typeParameters.add(tp.getNameAsString()));
        }

        String pkg = type.findCompilationUnit().map(ModelBuilder::packageOf).orElse("");
        ClassInfo info = new ClassInfo(qualifiedName, pkg, kindOf(type), source);
        TypeContext context = new TypeContext(info, type, resolver, List.copyOf(enclosing), Set.copyOf(typeParameters));

        addSupertypes(type, context);
        addFields(type, context);
        addMethods(type, context);
        addTypeReferences(type, context);
        model.add(info);

        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                buildType(nested, qualifiedName, enclosing, typeParameters, resolver, source, model);
            }
        }
    }

    private static TypeKind kindOf(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration c) {
            return c.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;
        }
        if (type instanceof EnumDeclaration) {
            return TypeKind.ENUM;
        }
        if (type instanceof RecordDeclaration) {
            return TypeKind.RECORD;
        }
        if (type instanceof AnnotationDeclaration) {
            return TypeKind.ANNOTATION;
        }
        throw new IllegalArgumentException("unsupported type declaration: " + type.getClass().getSimpleName());
    }

    private static void addSupertypes(TypeDeclaration<?> type, TypeContext context) {
        ClassInfo info = context.info();
        if (type instanceof ClassOrInterfaceDeclaration c) {
            if (c.isInterface()) {
                c.getExtendedTypes().forEach(t -> addInterface(info, context.resolve(t, t)));
            } else {
                info.setSuperclass(c.getExtendedTypes().isEmpty()
                        ? "java.lang.Object"
                        : context.resolve(c.getExtendedTypes(0), c.getExtendedTypes(0)));
                c.getImplementedTypes().forEach(t -> addInterface(info, context.resolve(t, t)));
            }
        } else if (type instanceof EnumDeclaration e) {
            info.setSuperclass("java.lang.Enum");
            e.getImplementedTypes().forEach(t -> addInterface(info, context.resolve(t, t)));
        } else if (type instanceof RecordDeclaration r) {
            info.setSuperclass("java.lang.Record");
            r.getImplementedTypes().forEach(t -> addInterface(info, context.resolve(t, t)));
        } else if (type instanceof AnnotationDeclaration) {
            info.addInterface("java.lang.annotation.Annotation");
        }
    }

    private static void addInterface(ClassInfo info, String name) {
        if (name != null) {
            info.addInterface(name);
        }
    }

    private static void addFields(TypeDeclaration<?> type, TypeContext context) {
        ClassInfo info = context.info();
        boolean implicitlyStatic = info.kind().isInterfaceLike();
        if (type instanceof RecordDeclaration record) {
            for (Parameter component : record.getParameters()) {
                info.addField(new FieldInfo(component.getNameAsString(),
                        context.signatureName(component.getType(), component), false));
            }
        }
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof FieldDeclaration field) {
                for (VariableDeclarator variable : field.getVariables()) {
                    info.addField(new FieldInfo(variable.getNameAsString(),
                            context.signatureName(variable.getType(), variable),
                            implicitlyStatic || field.isStatic()));
                }
            }
        }
    }

    private static void addMethods(TypeDeclaration<?> type, TypeContext context) {
        ClassInfo info = context.info();
        BodyAnalyzer analyzer = new BodyAnalyzer(context);
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof MethodDeclaration method) {
                MethodInfo m = new MethodInfo(method.getNameAsString(), parameterTypes(method.getParameters(), context),
                        false, method.isStatic(), method.getBody().isPresent());
                m.setReturnType(context.resolve(method.getType(), method));
                method.getBody().ifPresent(body -> {
                    m.setCyclomaticComplexity(ComplexityCounter.of(body));
                    analyzer.analyze(method, m);
                });
                info.addMethod(m);
            } else if (member instanceof ConstructorDeclaration constructor) {
                MethodInfo m = new MethodInfo(constructor.getNameAsString(),
                        parameterTypes(constructor.getParameters(), context), true, false, true);
                m.setCyclomaticComplexity(ComplexityCounter.of(constructor.getBody()));
                analyzer.analyze(constructor, m);
                info.addMethod(m);
            } else if (member instanceof CompactConstructorDeclaration compact
                    && type instanceof RecordDeclaration record) {
                MethodInfo m = new MethodInfo(compact.getNameAsString(),
                        parameterTypes(record.getParameters(), context), true, false, true);
                m.setCyclomaticComplexity(ComplexityCounter.of(compact.getBody()));
                analyzer.analyze(compact, m);
                info.addMethod(m);
            }
        }
    }

    private static List<String> parameterTypes(List<Parameter> parameters, TypeContext context) {
        List<String> types = new ArrayList<>();
        for (Parameter p : parameters) {
            String name = context.signatureName(p.getType(), p);
            types.add(p.isVarArgs() ? name + "..." : name);
        }
        return types;
    }

    /**
     * Records every type the declaration mentions: in declarations, generics, casts,
     * {@code instanceof}, {@code new}, {@code catch}, {@code throws}, class literals,
     * and capitalised names used as the receiver of a static call or field access.
     */
    private static void addTypeReferences(TypeDeclaration<?> type, TypeContext context) {
        ClassInfo info = context.info();
        Set<String> variableNames = new HashSet<>();
        type.findAll(Parameter.class).forEach(p -> variableNames.add(p.getNameAsString()));
        type.findAll(VariableDeclarator.class).forEach(v -> variableNames.add(v.getNameAsString()));

        AstWalk.ownNodes(type, node -> {
            if (node instanceof ClassOrInterfaceType classType && !isScopeOfAnotherType(classType)) {
                info.addReferencedType(context.resolveName(classType.getNameWithScope(), classType));
            } else if (node instanceof NameExpr name && looksLikeTypeName(name, variableNames)) {
                String resolved = context.resolveName(name.getNameAsString(), name);
                if (resolved != null && !TypeResolver.isUnresolved(resolved)) {
                    info.addReferencedType(resolved);
                }
            }
        });
    }

    private static boolean isScopeOfAnotherType(ClassOrInterfaceType type) {
        return type.getParentNode()
                .filter(p -> p instanceof ClassOrInterfaceType parent
                        && parent.getScope().filter(s -> s == type).isPresent())
                .isPresent();
    }

    private static boolean looksLikeTypeName(NameExpr name, Set<String> variableNames) {
        String id = name.getNameAsString();
        return Character.isUpperCase(id.charAt(0)) && !variableNames.contains(id);
    }

    static String packageOf(CompilationUnit unit) {
        return unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
    }

    private static String qualify(String prefix, String name) {
        return prefix.isEmpty() ? name : prefix + "." + name;
    }

    private static String relativeSource(Path root, Path file) {
        Path base = Files.isDirectory(root) ? root : root.getParent();
        if (base == null) {
            return file.toString();
        }
        return base.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString();
    }
}
