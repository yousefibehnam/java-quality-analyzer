package io.github.yousefibehnam.jqa.metrics;

import io.github.yousefibehnam.jqa.model.ClassInfo;
import io.github.yousefibehnam.jqa.model.MethodCall;
import io.github.yousefibehnam.jqa.model.MethodInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Response For a Class (Chidamber and Kemerer, 1994).
 *
 * <p>RFC = |M| + |R|, where M is the set of methods and constructors declared in the
 * class and R is the set of other methods called directly (one level deep) from M,
 * field initializers and initializer blocks. Called methods are identified by
 * declaring type, name and argument count; calls whose receiver could not be typed
 * are identified by name and argument count alone. Calls to the class's own methods
 * are already in M and are not counted twice.
 */
public final class Rfc {

    private Rfc() {
    }

    public static int of(ClassInfo type) {
        Set<String> own = new HashSet<>();
        for (MethodInfo method : type.methods()) {
            String name = method.isConstructor() ? MethodCall.CONSTRUCTOR : method.name();
            own.add(new MethodCall(type.qualifiedName(), name, method.parameterCount()).key());
        }
        Set<String> called = new HashSet<>();
        Stream.concat(type.methods().stream().map(MethodInfo::calls).flatMap(List::stream),
                        type.initializerCalls().stream())
                .map(MethodCall::key)
                .filter(key -> !own.contains(key))
                .forEach(called::add);
        return type.methods().size() + called.size();
    }
}
