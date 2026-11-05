package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodCall;
import io.github.yousefibehnam.jqa.model.MethodInfo;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import io.github.yousefibehnam.jqa.resolve.TypeResolver;

import java.util.Properties;

/**
 * How much of a project the name resolution could follow. These numbers qualify the
 * RFC and CBO values: an unresolved call or type reference can only lower CBO and can
 * merge distinct callees in RFC.
 *
 * @param files              parsed source files
 * @param skippedFiles       files that failed to parse
 * @param types              modelled types
 * @param callSites          method and constructor call sites
 * @param resolvedCallSites  call sites whose declaring type was determined
 * @param typeReferences     distinct (type, referenced name) pairs
 * @param unresolvedTypeReferences references that could not be resolved to a qualified name
 */
public record ResolutionStats(int files, int skippedFiles, int types,
                              long callSites, long resolvedCallSites,
                              long typeReferences, long unresolvedTypeReferences) {

    public static ResolutionStats of(ProjectModel model, int files, int skippedFiles) {
        long calls = 0;
        long resolved = 0;
        long refs = 0;
        long unresolvedRefs = 0;
        for (ClassInfo type : model.classes()) {
            for (MethodInfo method : type.methods()) {
                for (MethodCall call : method.calls()) {
                    calls++;
                    resolved += call.isResolved() ? 1 : 0;
                }
            }
            for (MethodCall call : type.initializerCalls()) {
                calls++;
                resolved += call.isResolved() ? 1 : 0;
            }
            for (String ref : type.referencedTypes()) {
                refs++;
                unresolvedRefs += TypeResolver.isUnresolved(ref) ? 1 : 0;
            }
        }
        return new ResolutionStats(files, skippedFiles, model.size(), calls, resolved, refs, unresolvedRefs);
    }

    public double callResolutionRate() {
        return callSites == 0 ? 1.0 : (double) resolvedCallSites / callSites;
    }

    public double typeResolutionRate() {
        return typeReferences == 0 ? 1.0 : 1.0 - (double) unresolvedTypeReferences / typeReferences;
    }

    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty("files", Integer.toString(files));
        p.setProperty("skipped_files", Integer.toString(skippedFiles));
        p.setProperty("types", Integer.toString(types));
        p.setProperty("call_sites", Long.toString(callSites));
        p.setProperty("resolved_call_sites", Long.toString(resolvedCallSites));
        p.setProperty("type_references", Long.toString(typeReferences));
        p.setProperty("unresolved_type_references", Long.toString(unresolvedTypeReferences));
        return p;
    }

    public static ResolutionStats fromProperties(Properties p) {
        return new ResolutionStats(
                Integer.parseInt(p.getProperty("files")),
                Integer.parseInt(p.getProperty("skipped_files")),
                Integer.parseInt(p.getProperty("types")),
                Long.parseLong(p.getProperty("call_sites")),
                Long.parseLong(p.getProperty("resolved_call_sites")),
                Long.parseLong(p.getProperty("type_references")),
                Long.parseLong(p.getProperty("unresolved_type_references")));
    }
}
