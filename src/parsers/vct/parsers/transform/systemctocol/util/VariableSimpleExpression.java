package vct.parsers.transform.systemctocol.util;

public class VariableSimpleExpression implements SimpleExpression {
    private final Object value;

    public VariableSimpleExpression(Object value) {
        this.value = value;
    }

    public SimpleExpression simplify() {
        return this;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VariableSimpleExpression)) {
            return false;
        }
        VariableSimpleExpression expr = (VariableSimpleExpression) other;
        return this.value.equals(expr.value);
    }
}
