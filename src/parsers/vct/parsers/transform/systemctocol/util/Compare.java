package vct.parsers.transform.systemctocol.util;

/**
 * Class representing an (in)equation.
 */
public class Compare {
    private ComparisonType op;
    private LinearExpression left;
    private LinearExpression right;

    /**
     * Default constructor.
     */
    public Compare() {
        this.op = ComparisonType.EQ;
        this.left = new LinearExpression();
        this.right = new LinearExpression();
    }

    /**
     * Compare constructor.
     * 
     * @param op the operation of the expression.
     * @param left the left expression
     * @param right the right expression
     */
    public Compare(ComparisonType op, LinearExpression left, LinearExpression right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    /**
     * Copy constructor.
     */
    public Compare(Compare comp) {
        this.op = comp.op;
        this.left = new LinearExpression(comp.left);
        this.right = new LinearExpression(comp.right);
    }

    /**
     * Reduce this expression. The resulting expression is of the form
     * left - right *OP* 0, where the original expression is left *OP* right.
     */
    public Compare reduce() {
        return reduce(this);
    }

    /**
     * Reduce an expression.
     * 
     * @param comp the expression to reduce
     * @return the resulting expression
     */
    public static Compare reduce(Compare comp) {
        Compare result = new Compare(comp);
        result.left = result.left.add(LinearExpression.minus(result.right));
        result.right = new LinearExpression();
        return result;
    }

    /**
     * Check if the comparison can be concretely evaluated. This is true for expressions
     * that are either constant or can be reduced to constant.
     * 
     * @return true if the comparison can be evaluated, false otherwise
     */
    public boolean canEvaluate() {
        // by reducing the expression we create a new comparison
        // against 0. If the reduction (i.e. left side of the 
        // comparison) is constant, then the comparison can be evaluated.
        return this.reduce().left.isConstant();
    }

    /**
     * Concretely evaluate this comparison.
     * 
     * @return true if the comparison can be evaluated and is true, false
     * if either the comparison cannot be evaluated or the evaluation is false
     */
    public boolean evaluate() {
        if (!canEvaluate()) {
            return false;
        }
        Compare reduced = this.reduce();
        Bound zero = new Bound(0);
        Bound value = reduced.left.getConstant();
        switch (reduced.op) {
            case EQ: return value.equals(zero);
            case NEQ: return !value.equals(zero);
            case LESSER: return value.compareTo(zero) < 0;
            case LESSER_EQ: return value.compareTo(zero) <= 0;
            case GREATER: return value.compareTo(zero) > 0;
            case GREATER_EQ: return value.compareTo(zero) >= 0;
            default: return false;
        }
    }

    public LinearExpression getLeft() {
        return this.left;
    }

    public LinearExpression getRight() {
        return this.right;
    }

    public ComparisonType getOp() {
        return this.op;
    }

    @Override 
    public String toString() {
        return this.left.toString() + " " + this.op.toString() + " " + this.right.toString();
    }
}