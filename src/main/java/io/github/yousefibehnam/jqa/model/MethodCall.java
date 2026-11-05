package io.github.yousefibehnam.jqa.model;

/**
 * A call site found in a method body.
 *
 * <p>Methods are identified by owner, name and argument count. Parameter types are
 * not compared because argument types are often not resolvable without a full
 * compiler; overloads with the same arity are therefore treated as one method.
 *
 * @param owner    fully qualified name of the declaring type; {@link #UNKNOWN_OWNER} when the
 *                 receiver could not be typed, or {@code ?Name} for an unresolvable type name
 * @param name     method name, or {@link #CONSTRUCTOR} for constructor invocations
 * @param argCount number of arguments at the call site
 */
public record MethodCall(String owner, String name, int argCount) {

    public static final String UNKNOWN_OWNER = "?";
    public static final String CONSTRUCTOR = "<init>";

    public boolean isResolved() {
        return !owner.startsWith(UNKNOWN_OWNER);
    }

    /** A stable key such as {@code com.acme.Order#total/0}. */
    public String key() {
        return owner + "#" + name + "/" + argCount;
    }
}
