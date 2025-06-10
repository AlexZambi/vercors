package vct.parsers.transform.systemctocol.util;

public class SymbolicInterval {
    private LinearExpression lower;
    private LinearExpression upper;

    public SymbolicInterval(LinearExpression lower, LinearExpression upper) {
        this.lower = new LinearExpression(lower);
        this.upper = new LinearExpression(upper);
    }

    public SymbolicInterval(LinearExpression expr) {
        this(expr, expr);
    }

    public SymbolicInterval(Bound lower, Bound upper) {
        this(new LinearExpression().add(lower), new LinearExpression().add(upper));
    }

    public SymbolicInterval(Bound value) {
        this(value, value);
    }

    public SymbolicInterval(Integer lower, Integer upper) {
        this(new Bound(lower), new Bound(upper));
    }

    public SymbolicInterval(Integer value) {
        this(value, value);
    }

    public SymbolicInterval(Object lower, Object upper) {
        this(new LinearExpression().add(lower, 1), new LinearExpression().add(upper, 1));
    }

    public SymbolicInterval(Object var) {
        this(var, var);
    }

    public SymbolicInterval(Interval interval) {
        this.lower = new LinearExpression().add(interval.getLower());
        this.upper = new LinearExpression().add(interval.getUpper());
    }

    public SymbolicInterval(SymbolicInterval interval) {
        this(interval.lower, interval.upper);
    }

    public static SymbolicInterval add(SymbolicInterval interval, LinearExpression expr) {
        SymbolicInterval result = new SymbolicInterval(interval);
        result.lower = result.lower.add(expr);
        result.upper = result.upper.add(expr);
        return result;
    }

    public SymbolicInterval add(LinearExpression expr) {
        return add(this, expr);
    }

    public static SymbolicInterval add(SymbolicInterval interval, Bound constant) {
        return add(interval, new LinearExpression().add(constant));
    }

    public SymbolicInterval add(Bound constant) {
        return add(this, constant);
    }

    public static SymbolicInterval add(SymbolicInterval interval, Integer constant) {
        return add(interval, new LinearExpression().add(constant));
    }

    public SymbolicInterval add(Integer constant) {
        return add(this, constant);
    }

    public static SymbolicInterval add(SymbolicInterval interval, Object var, Bound coeff) {
        return add(interval, new LinearExpression().add(var, coeff));
    }

    public SymbolicInterval add(Object var, Bound coeff) {
        return add(this, var, coeff);
    }

    public static SymbolicInterval add(SymbolicInterval interval, Object var, Integer coeff) {
        return add(interval, new LinearExpression().add(var, coeff));
    }

    public SymbolicInterval add(Object var, Integer coeff) {
        return add(this, var, coeff);
    }

    public static SymbolicInterval apply(SymbolicInterval interval, LinearExpression expr, Object var) {
        LinearExpression lower = expr.replace(var, interval.lower);
        LinearExpression upper = expr.replace(var, interval.upper);
        // we check if the resulting bounds should be swapped
        Compare comp = new Compare(ComparisonType.LESSER_EQ, lower, upper);
        if (comp.canEvaluate()) {
            // this means that the upper bound is actually lower, so switch
            if (!comp.evaluate()) {
                return new SymbolicInterval(upper, lower);
            }
        }
        // in any other case we just return
        return new SymbolicInterval(lower, upper);
    }

    public SymbolicInterval apply(LinearExpression expr, Object var) {
        return apply(this, expr, var);
    }

    public static SymbolicInterval evaluate(SymbolicInterval interval, Object var, Bound value) {
        LinearExpression lower = interval.lower.evaluate(var, value);
        LinearExpression upper = interval.upper.evaluate(var, value);
        // we check if the resulting bounds should be swapped
        Compare comp = new Compare(ComparisonType.LESSER_EQ, lower, upper);
        if (comp.canEvaluate()) {
            // this means that the upper bound is actually lower, so switch
            if (!comp.evaluate()) {
                return new SymbolicInterval(upper, lower);
            }
        }
        // in any other case we just return
        return new SymbolicInterval(lower, upper);
    }

    public SymbolicInterval evaluate(Object var, Bound value) {
        return evaluate(this, var, value);
    }

    public static SymbolicInterval evaluate(SymbolicInterval interval, Object var, Integer value) {
        return evaluate(interval, var, new Bound(value));
    }

    public SymbolicInterval evaluate(Object var, Integer value) {
        return evaluate(this, var, new Bound(value));
    }

    public static SymbolicInterval replace(SymbolicInterval interval, Object var, LinearExpression expr) {
        LinearExpression lower = interval.lower.replace(var, expr);
        LinearExpression upper = interval.upper.replace(var, expr);
        // we check if the resulting bounds should be swapped
        Compare comp = new Compare(ComparisonType.LESSER_EQ, lower, upper);
        if (comp.canEvaluate()) {
            // this means that the upper bound is actually lower, so switch
            if (!comp.evaluate()) {
                return new SymbolicInterval(upper, lower);
            }
        }
        // in any other case we just return
        return new SymbolicInterval(lower, upper);
    }

    public SymbolicInterval replace(Object var, LinearExpression expr) {
        return replace(this, var, expr);
    }

    public static SymbolicInterval join(SymbolicInterval a, SymbolicInterval b) {
        Compare comp_lower = new Compare(ComparisonType.LESSER_EQ, a.lower, b.lower);
        Compare comp_upper = new Compare(ComparisonType.LESSER_EQ, a.upper, b.upper);
        if (!comp_lower.canEvaluate() || !comp_upper.canEvaluate()) {
            // if we cannot evaluate either bound, just return null
            return null;
        }
        LinearExpression lower = comp_lower.evaluate() ? a.lower : b.lower;
        LinearExpression upper = comp_upper.evaluate() ? b.upper : a.upper;
        return new SymbolicInterval(lower, upper);
    }

    public SymbolicInterval join(SymbolicInterval other) {
        return join(this, other);
    }

    public LinearExpression getLower() {return this.lower;}

    public LinearExpression getUpper() {return this.upper;}

    public boolean isConstant() {
        return this.lower.isConstant() && this.upper.isConstant();
    }

    @Override
    public String toString() {
        return "[" + this.lower.toString() + ", " + this.upper.toString() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SymbolicInterval)) {
            return false;
        }
        SymbolicInterval other = (SymbolicInterval) obj;
        return this.lower.equals(other.lower) && this.upper.equals(other.upper);
    }
}
