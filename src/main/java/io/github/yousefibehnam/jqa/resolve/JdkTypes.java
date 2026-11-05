package io.github.yousefibehnam.jqa.resolve;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers questions about JDK types by loading them (without initialisation)
 * through the platform class loader of the running JVM.
 */
public final class JdkTypes {

    private static final String[] JDK_PREFIXES = {
        "java.", "javax.", "jdk.", "sun.", "com.sun.", "org.w3c.dom.", "org.xml.sax.", "org.ietf.jgss."
    };

    private static final Map<String, Optional<Class<?>>> CACHE = new ConcurrentHashMap<>();

    private JdkTypes() {
    }

    /** True when the name lies in a package shipped with the JDK. */
    public static boolean isJdkName(String qualifiedName) {
        for (String prefix : JDK_PREFIXES) {
            if (qualifiedName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads a JDK type by source-style name. Nested types may be written with
     * dots ({@code java.util.Map.Entry}); binary names are tried automatically.
     */
    public static Optional<Class<?>> load(String qualifiedName) {
        if (!isJdkName(qualifiedName)) {
            return Optional.empty();
        }
        return CACHE.computeIfAbsent(qualifiedName, JdkTypes::tryLoad);
    }

    public static boolean exists(String qualifiedName) {
        return load(qualifiedName).isPresent();
    }

    private static Optional<Class<?>> tryLoad(String sourceName) {
        String candidate = sourceName;
        while (true) {
            try {
                return Optional.of(Class.forName(candidate, false, ClassLoader.getPlatformClassLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                int dot = candidate.lastIndexOf('.');
                if (dot < 0) {
                    return Optional.empty();
                }
                // Retry treating the last segment as a nested type: a.b.C.D -> a.b.C$D
                candidate = candidate.substring(0, dot) + "$" + candidate.substring(dot + 1);
                if (candidate.indexOf('.') < 0) {
                    return Optional.empty();
                }
            }
        }
    }

    /**
     * Depth of a JDK type in its inheritance tree.
     *
     * <p>{@code java.lang.Object} has depth 0 and a class extending it directly has
     * depth 1. An interface with no super-interfaces has depth 1 (it is rooted at
     * {@code Object} like every reference type); otherwise its depth is one more
     * than its deepest super-interface.
     */
    public static int depth(Class<?> type) {
        if (type == Object.class) {
            return 0;
        }
        if (type.isInterface()) {
            int deepest = 0;
            for (Class<?> parent : type.getInterfaces()) {
                deepest = Math.max(deepest, depth(parent));
            }
            return deepest + 1;
        }
        Class<?> parent = type.getSuperclass();
        return parent == null ? 1 : depth(parent) + 1;
    }
}
