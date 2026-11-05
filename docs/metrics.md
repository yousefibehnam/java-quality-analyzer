# Metric definitions

The Chidamber–Kemerer (CK) metrics are defined for "classes" in general, so every tool
that implements them for Java has to make choices. This page lists the choices made by
`jqa`, so that its numbers can be compared with other tools and interpreted correctly.
Each definition is checked by a unit test against a small hand-written fixture whose
expected values were worked out by hand. The calculation is written as a comment at the
top of each fixture in [`src/test/resources/fixtures/`](../src/test/resources/fixtures/).

## What is measured

- **Types.** Every top-level and member type declaration: classes, interfaces, enums,
  records and annotation types. A member type such as `Outer.Inner` is a type of its own.
- **Anonymous and local classes** are *not* separate types. Their code is part of the
  method that contains them, as are lambdas.
- **Methods.** Methods and constructors written in the source. Implicit members, such as
  a default constructor or the accessors of a record, are not counted.
- **Fields.** Fields declared in the type, plus the components of a record. Enum
  constants are not counted as fields.

## Name resolution

`jqa` reads source code only. It does not compile it or load the project's
dependencies, so it resolves names itself, in this order: the enclosing types and their
member types, single-type imports, the same package, on-demand imports, then
`java.lang`. JDK types are checked against the running JDK through reflection. A name
that cannot be resolved is kept as unresolved (shown as `?Name`) and does not count
towards CBO. Member types inherited from a supertype are not searched.

To find out which type declares a called method, the receiver of each call is typed
from:

- `this`, `super`, and qualified `Outer.this`;
- parameters and local variables, including lambda, `catch` and pattern variables;
  `var` is typed only when it is initialised with `new`;
- fields of the type, of its project superclasses and of its enclosing types;
- type names, for static calls; `new` expressions; casts; string literals;
- the declared return type of the previous call in a chain. This works for project
  methods, and for JDK methods when every overload with that arity returns the same type.

The method is then looked up along the project's supertype chain, so a call to an
inherited method is attributed to the class that declares it. Statically imported
methods are attributed to the type they are imported from. On the three case-study
projects, 97.7–98.2% of call sites were resolved; see [`results/summary.md`](../results/summary.md).

Local scopes are flattened. A parameter or local variable declared *anywhere* in a
method hides a field with the same name for the *whole* method.

## WMC: Weighted Methods per Class

**Definition (CK 1994):** the sum of the complexities of the methods of a class.

**In `jqa`:** the sum of the McCabe cyclomatic complexity of every declared method and
constructor. The complexity of a method is 1 plus one for each `if`, `for`, enhanced
`for`, `while`, `do`, `case` label, `catch`, `?:`, `&&` and `||`. A `default` label
and `else` add nothing. A method without a body (abstract, or an interface method
without `default`) weighs 1.

If you want unit weights instead (WMC = number of methods), the CSV has a separate
`methods` column.

*Fixture:* `wmc/` gives `Branchy` = 24 and `Shape` = 3.

## DIT: Depth of Inheritance Tree

**Definition:** the length of the path from the class to the root of the inheritance tree.

**In `jqa`:** `java.lang.Object` has depth 0.

- A class without `extends` has DIT 1. Enums have DIT 2 because they extend
  `java.lang.Enum`, and records have DIT 2 because they extend `java.lang.Record`.
- Classes follow their superclass. Interfaces follow their deepest super-interface,
  and a root interface has DIT 1.
- Implementing an interface does not add depth.
- JDK supertypes are measured by reflection on the running JDK. For example,
  `extends ArrayList` gives DIT 4. The answer can depend on the JDK version: since
  JDK 21, `List` extends `SequencedCollection`.
- A supertype that is neither in the project nor in the JDK is assumed to extend
  `Object` directly, and the row is marked `dit_exact=false`. In that case the
  reported DIT is a lower bound.

*Fixture:* `inheritance/`.

## NOC: Number of Children

**Definition:** the number of immediate subclasses.

**In `jqa`:** the immediate subtypes in the analysed sources, along the same edges DIT
uses. A class counts the classes that extend it, and an interface counts the
interfaces that extend it. Classes that implement an interface are not its children.

*Fixture:* `inheritance/`.

## CBO: Coupling Between Object classes

**Definition (CK 1994):** the number of other classes to which a class is coupled. Two
classes are coupled when one uses methods or instance variables of the other.

**In `jqa`:** class A *uses* class B when either of these is true:

- A names B anywhere: in a field, parameter, return type, local variable, generic
  argument, `new`, cast, `instanceof`, `catch`, `throws`, class literal, supertype,
  or as the receiver of a static call or field access.
- A calls a method that was resolved to B. This covers types that are only reached
  through call chains and never named.

CBO is the number of distinct classes coupled **in either direction**: the types A uses
plus the project types that use A. Excluded are:

- the class itself;
- unresolved names;
- by default, JDK types. Use `--include-jdk` to count them.

Annotation usages are not couplings. Couplings *from* code outside the analysed
sources cannot be seen.

*Fixture:* `coupling/`. `Order` = 4, or 6 with JDK types. `Notifier` is coupled to
`Channel` only through a call chain.

## RFC: Response For a Class

**Definition (CK 1994):** |RS|, where the response set RS = M ∪ R. M is the set of
methods of the class, and R is the set of methods those methods call.

**In `jqa`:**

- **M** is the declared methods and constructors.
- **R** is the methods called directly, one level deep, from M, from field
  initializers and from initializer blocks.
- A called method is identified by its declaring type, name and number of arguments.
  Overloads with the same arity therefore count once.
- A call whose receiver could not be typed is identified by name and arity alone.
- Calls to the class's own methods are already in M and are not counted twice.
- Calling a constructor (`new T(..)`, `this(..)`, `super(..)`) is a call to `T.<init>`.
- Method references (`Foo::bar`) are not counted.

*Fixture:* `response/`, where `Account` = 12.

## LCOM: Lack of Cohesion in Methods (CK)

**Definition (CK 1994):**

- P is the number of method pairs that share no instance variable.
- Q is the number of pairs that share at least one.
- LCOM = P − Q if P > Q, otherwise 0.

**In `jqa`:**

- **Methods:** declared methods with a body. Constructors are excluded; static methods
  are included.
- **Fields:** all fields declared in the class, static and instance.
- A method uses a field when it reads or writes it by simple name (if no local hides
  it), through `this.` (or `Self.this.`, naming the class itself), or through the class's own name.
- Fields reached through getters, or inherited fields, do not count.
- With fewer than two methods, LCOM is 0.

LCOM is not normalised: it grows roughly with the square of the number of methods.

*Fixture:* `cohesion/`, where `Counter` = 4.

## LCOM*: Henderson-Sellers

**Definition (Henderson-Sellers 1996):** LCOM* = (mean μ(A) − m) / (1 − m).

- m is the number of methods.
- a is the number of fields.
- μ(A) is the number of methods that use field A, and the mean is taken over all a fields.

**In `jqa`:** methods and fields are the same as for LCOM. Reading the value:

- 0 means every method uses every field.
- 1 means each field is used by exactly one method.
- Values up to 2 occur when fields are used by no method at all, for example fields
  that only constructors touch.

LCOM* is **undefined** (empty in CSV, `null` in JSON) when there are fewer than two
methods or no fields.

*Fixture:* `cohesion/`. `Counter` = 8/9, and `Vector2` = 0.25, where a local variable
hides a field.

## References

- S. R. Chidamber and C. F. Kemerer, "A Metrics Suite for Object Oriented Design,"
  *IEEE Transactions on Software Engineering*, 20(6), 1994.
- B. Henderson-Sellers, *Object-Oriented Metrics: Measures of Complexity*, Prentice Hall, 1996.
- T. J. McCabe, "A Complexity Measure," *IEEE Transactions on Software Engineering*, SE-2(4), 1976.
