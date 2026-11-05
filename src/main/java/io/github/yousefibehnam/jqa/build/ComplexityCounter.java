package io.github.yousefibehnam.jqa.build;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;

/**
 * McCabe cyclomatic complexity of a method body: one plus the number of decision points.
 *
 * <p>Decision points are {@code if}, {@code for}, enhanced {@code for}, {@code while},
 * {@code do}, every {@code case} label (a {@code default} label adds nothing),
 * {@code catch}, the ternary operator {@code ?:}, and the short-circuit operators
 * {@code &&} and {@code ||}. {@code else} does not add a path. Lambdas and anonymous
 * classes inside the body are counted as part of the enclosing method.
 */
public final class ComplexityCounter {

    private ComplexityCounter() {
    }

    public static int of(Node body) {
        if (body == null) {
            return 1;
        }
        int[] decisions = {0};
        body.walk(node -> decisions[0] += decisionsAt(node));
        return 1 + decisions[0];
    }

    private static int decisionsAt(Node node) {
        if (node instanceof IfStmt || node instanceof ForStmt || node instanceof ForEachStmt
                || node instanceof WhileStmt || node instanceof DoStmt
                || node instanceof ConditionalExpr || node instanceof CatchClause) {
            return 1;
        }
        if (node instanceof SwitchEntry entry) {
            return entry.getLabels().size();
        }
        if (node instanceof BinaryExpr binary) {
            BinaryExpr.Operator op = binary.getOperator();
            return op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR ? 1 : 0;
        }
        return 0;
    }
}
