package io.github.yousefibehnam.jqa.resolve;

import org.junit.jupiter.api.Test;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdkTypesTest {

    @Test
    void loadsNestedTypesWrittenWithDots() {
        assertThat(JdkTypes.exists("java.util.Map.Entry")).isTrue();
        assertThat(JdkTypes.exists("java.util.NoSuchThing")).isFalse();
        assertThat(JdkTypes.exists("com.example.Foo")).isFalse();
    }

    @Test
    void depthCountsFromObject() {
        assertThat(JdkTypes.depth(Object.class)).isZero();
        assertThat(JdkTypes.depth(AbstractCollection.class)).isEqualTo(1);
        assertThat(JdkTypes.depth(ArrayList.class)).isEqualTo(3);
    }

    @Test
    void interfaceDepthFollowsDeepestSuperInterface() {
        assertThat(JdkTypes.depth(Iterable.class)).isEqualTo(1);
        assertThat(JdkTypes.depth(Collection.class)).isEqualTo(2);
        // JDK 21 inserted SequencedCollection between Collection and List.
        int expected = Runtime.version().feature() >= 21 ? 4 : 3;
        assertThat(JdkTypes.depth(List.class)).isEqualTo(expected);
    }

    @Test
    void fieldAndReturnTypes() {
        assertThat(JdkTypes.fieldType("java.lang.System", "out")).contains("java.io.PrintStream");
        assertThat(JdkTypes.returnType("java.lang.StringBuilder", "append", 1)).contains("java.lang.StringBuilder");
        assertThat(JdkTypes.returnType("java.util.List", "get", 1)).isEmpty(); // returns a type variable
    }
}
