package vct.parsers.transform.systemctocol.util;

public class ConstantSimpleExpression implements SimpleExpression {
    private final Bound value;

    public ConstantSimpleExpression(Bound value) {
        this.value = value;
    }

    public ConstantSimpleExpression(Integer value) {
        this(new Bound(value));
    }

    public SimpleExpression simplify() {
        return this;
    }

    public Bound getBound() {
        return this.value;
    }

    public Integer getValue() {
        return this.value.getValue();
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConstantSimpleExpression)) {
            return false;
        }
        ConstantSimpleExpression expr = (ConstantSimpleExpression) other;
        return this.value.equals(expr.value);
    }
}
