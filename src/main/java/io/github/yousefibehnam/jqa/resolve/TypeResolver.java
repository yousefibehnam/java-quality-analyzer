package io.github.yousefibehnam.jqa.resolve;

import com.github.javaparser.ast.ImportDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves type names written in one compilation unit to fully qualified names.
 *
 * <p>The lookup order follows the Java scoping rules closely enough for metrics:
 * <ol>
 *   <li>the enclosing types themselves and their member types (innermost first);</li>
 *   <li>single-type imports;</li>
 *   <li>types in the same package (as found in the analysed sources);</li>
 *   <li>on-demand ({@code .*}) imports, checked against the project and the JDK;</li>
 *   <li>{@code java.lang}.</li>
 * </ol>
 * Member types inherited from supertypes are not searched. Names that cannot be
 * resolved are returned with an {@link #UNRESOLVED_PREFIX} so callers can tell
 * them apart from real qualified names.
 */
public final class TypeResolver {

    public static final String UNRESOLVED_PREFIX = "?";

    private static final Set<String> NON_REFERENCE_TYPES = Set.of(
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void", "var");

    private final Set<String> projectTypes;
    private final String packageName;
    private final Map<String, String> singleImports = new HashMap<>();
    private final List<String> onDemandImports = new ArrayList<>();

    /**
     * @param projectTypes qualified names of every type in the analysed sources
     * @param packageName  package of the compilation unit ("" for the default package)
     * @param imports      the compilation unit's import declarations
     */
    public TypeResolver(Set<String> projectTypes, String packageName, List<ImportDeclaration> imports) {
        this.projectTypes = projectTypes;
        this.packageName = packageName;
        for (ImportDeclaration imp : imports) {
            if (imp.isStatic()) {
                continue;
            }
            String name = imp.getNameAsString();
            if (imp.isAsterisk()) {
                onDemandImports.add(name);
            } else {
                singleImports.put(simpleNameOf(name), name);
            }
        }
    }

    public static boolean isUnresolved(String name) {
        return name != null && name.startsWith(UNRESOLVED_PREFIX);
    }

    /**
     * Resolves a possibly dotted type name such as {@code Map.Entry} or {@code java.util.List}.
     *
     * @param name           the name as written, without type arguments
     * @param enclosingTypes qualified names of the enclosing types, innermost first
     * @param typeParameters type variables in scope (they are not types to resolve)
     * @return the qualified name; {@code null} for primitives, {@code void}, {@code var}
     *         and type variables; or an {@link #UNRESOLVED_PREFIX}ed name
     */
    public String resolve(String name, List<String> enclosingTypes, Set<String> typeParameters) {
        if (NON_REFERENCE_TYPES.contains(name)) {
            return null;
        }
        int dot = name.indexOf('.');
        String first = dot < 0 ? name : name.substring(0, dot);
        String rest = dot < 0 ? "" : name.substring(dot + 1);

        if (rest.isEmpty() && typeParameters.contains(name)) {
            return null;
        }
        String head = resolveSimple(first, enclosingTypes);
        if (head != null) {
            return rest.isEmpty() ? head : head + "." + rest;
        }
        if (!rest.isEmpty() && isKnown(name)) {
            return name; // already fully qualified
        }
        if (!rest.isEmpty()) {
            String qualified = longestKnownPrefix(name);
            if (qualified != null) {
                return qualified;
            }
        }
        return UNRESOLVED_PREFIX + name;
    }

    private String resolveSimple(String simple, List<String> enclosingTypes) {
        for (String enclosing : enclosingTypes) {
            if (simpleNameOf(enclosing).equals(simple)) {
                return enclosing;
            }
            String member = enclosing + "." + simple;
            if (projectTypes.contains(member)) {
                return member;
            }
        }
        String imported = singleImports.get(simple);
        if (imported != null) {
            return imported;
        }
        String samePackage = packageName.isEmpty() ? simple : packageName + "." + simple;
        if (projectTypes.contains(samePackage)) {
            return samePackage;
        }
        for (String pkg : onDemandImports) {
            String candidate = pkg + "." + simple;
            if (isKnown(candidate)) {
                return candidate;
            }
        }
        String javaLang = "java.lang." + simple;
        if (JdkTypes.exists(javaLang)) {
            return javaLang;
        }
        return null;
    }

    /** For {@code a.b.C.D} where {@code a.b.C} is known, returns {@code a.b.C.D}. */
    private String longestKnownPrefix(String dotted) {
        String candidate = dotted;
        int dot;
        while ((dot = candidate.lastIndexOf('.')) > 0) {
            candidate = candidate.substring(0, dot);
            if (isKnown(candidate)) {
                return dotted;
            }
        }
        return null;
    }

    private boolean isKnown(String qualifiedName) {
        return projectTypes.contains(qualifiedName) || JdkTypes.exists(qualifiedName);
    }

    static String simpleNameOf(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? qualifiedName : qualifiedName.substring(dot + 1);
    }
}
