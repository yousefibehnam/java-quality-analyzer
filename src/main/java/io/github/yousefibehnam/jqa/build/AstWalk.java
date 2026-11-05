package io.github.yousefibehnam.jqa.build;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Small AST traversal helpers shared by the model builder. */
final class AstWalk {

    private AstWalk() {
    }

    /**
     * Visits {@code root} and its descendants in pre-order, without entering member
     * type declarations: those are modelled as classes of their own. Local and
     * anonymous classes are entered, because they belong to the enclosing code.
     */
    static void ownNodes(Node root, Consumer<Node> visitor) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            visitor.accept(node);
            var children = node.getChildNodes();
            for (int i = children.size() - 1; i >= 0; i--) {
                Node child = children.get(i);
                if (!isMemberType(child)) {
                    stack.push(child);
                }
            }
        }
    }

    static boolean isMemberType(Node node) {
        return node instanceof TypeDeclaration<?>
                && node.getParentNode().filter(p -> p instanceof TypeDeclaration<?>).isPresent();
    }

    /** Names of type variables declared by {@code node}'s ancestors, stopping at {@code stop}. */
    static Set<String> typeParametersInScope(Node node, Node stop) {
        Set<String> names = new HashSet<>();
        Node current = node;
        while (current != null) {
            if (current instanceof NodeWithTypeParameters<?> generic) {
                generic.getTypeParameters().forEach(tp -> names.add(tp.getNameAsString()));
            }
            if (current == stop) {
                break;
            }
            current = current.getParentNode().orElse(null);
        }
        return names;
    }
}
