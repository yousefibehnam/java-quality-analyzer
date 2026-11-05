package io.github.yousefibehnam.jqa.resolve;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TypeResolverTest {

    private static final Set<String> PROJECT = Set.of(
            "app.Order", "app.Order.Line", "app.Customer", "lib.Customer", "lib.Tool");

    private static TypeResolver resolverFor(String header) {
        CompilationUnit unit = StaticJavaParser.parse(header + "\nclass X {}");
        String pkg = unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        return new TypeResolver(PROJECT, pkg, unit.getImports());
    }

    private static String resolve(TypeResolver resolver, String name) {
        return resolver.resolve(name, List.of("app.Order"), Set.of("T"));
    }

    @Test
    void primitivesVoidVarAndTypeVariablesAreNotTypes() {
        TypeResolver r = resolverFor("package app;");
        assertThat(resolve(r, "int")).isNull();
        assertThat(resolve(r, "void")).isNull();
        assertThat(resolve(r, "var")).isNull();
        assertThat(resolve(r, "T")).isNull();
    }

    @Test
    void memberTypesOfEnclosingTypeComeFirst() {
        assertThat(resolve(resolverFor("package app;"), "Line")).isEqualTo("app.Order.Line");
    }

    @Test
    void singleImportShadowsSamePackage() {
        TypeResolver r = resolverFor("package app; import lib.Customer;");
        assertThat(resolve(r, "Customer")).isEqualTo("lib.Customer");
        assertThat(resolve(resolverFor("package app;"), "Customer")).isEqualTo("app.Customer");
    }

    @Test
    void onDemandImportsCheckProjectAndJdk() {
        TypeResolver r = resolverFor("package app; import lib.*; import java.util.*;");
        assertThat(resolve(r, "Tool")).isEqualTo("lib.Tool");
        assertThat(resolve(r, "List")).isEqualTo("java.util.List");
    }

    @Test
    void javaLangIsImplicit() {
        assertThat(resolve(resolverFor("package app;"), "String")).isEqualTo("java.lang.String");
    }

    @Test
    void dottedNamesResolveTheirFirstSegment() {
        TypeResolver r = resolverFor("package app; import java.util.Map;");
        assertThat(resolve(r, "Map.Entry")).isEqualTo("java.util.Map.Entry");
        assertThat(resolve(r, "java.util.concurrent.TimeUnit")).isEqualTo("java.util.concurrent.TimeUnit");
    }

    @Test
    void unknownNamesAreMarkedUnresolved() {
        String name = resolve(resolverFor("package app; import org.other.*;"), "Mystery");
        assertThat(name).isEqualTo("?Mystery");
        assertThat(TypeResolver.isUnresolved(name)).isTrue();
    }

    @Test
    void staticImportsAreRecorded() {
        TypeResolver r = resolverFor("package app; import static java.lang.Math.max; import static lib.Tool.*;");
        assertThat(r.staticImportOwner("max")).isEqualTo("java.lang.Math");
        assertThat(r.staticOnDemandOwners()).containsExactly("lib.Tool");
    }
}
