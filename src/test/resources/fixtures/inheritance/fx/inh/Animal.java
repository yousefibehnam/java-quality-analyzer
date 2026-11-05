package fx.inh;

/*
 * Hand-derived values (java.lang.Object has depth 0):
 *
 *   type      extends / implements              DIT          NOC
 *   Animal    (Object)                          1            2  (Dog, Cat)
 *   Dog       Animal                            2            1  (Puppy)
 *   Cat       Animal                            2            0
 *   Puppy     Dog                               3            0
 *   MyList    java.util.ArrayList               4  exact     0  (ArrayList -> AbstractList -> AbstractCollection -> Object)
 *   Widget    org.example.External (unknown)    2  lower bound
 *   Color     enum (java.lang.Enum)             2            0
 *   Point     record (java.lang.Record)         2            0
 *   Named     interface                         1            1  (Titled)
 *   Titled    interface extends Named           2            0
 *   Book      implements Titled                 1            0  (implementing is not inheriting)
 */
public class Animal {
}

class Dog extends Animal {
}

class Cat extends Animal {
}

class Puppy extends Dog {
}

class MyList extends java.util.ArrayList<String> {
    private static final long serialVersionUID = 1L;
}

class Widget extends org.example.External {
}

enum Color { RED, GREEN }

record Point(int x, int y) {
}

interface Named {
}

interface Titled extends Named {
}

class Book implements Titled {
}
