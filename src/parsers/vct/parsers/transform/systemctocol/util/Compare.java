package vct.parsers.transform.systemctocol.util;

/**
 * Class representing an (in)equation.
 */
public class Compare {
    private Comparison op;
    private LinearExpression left;
    private LinearExpression right;

    /**
     * Default constructor.
     */
    public Compare() {
        this.op = Comparison.EQ;
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
    public Compare(Comparison op, LinearExpression left, LinearExpression right) {
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
    public void reduce() {
        LinearExpression sum = this.left.add(LinearExpression.minus(this.right));
        this.left = sum;
        this.right = new LinearExpression();
    }

    /**
     * Reduce an expression.
     * 
     * @param comp the expression to reduce
     * @return the resulting expression
     */
    public static Compare reduce(Compare comp) {
        Compare result = new Compare(comp);
        result.reduce();
        return result;
    }

    @Override 
    public String toString() {
        return this.left.toString() + " " + this.op.toString() + " " + this.right.toString();
    }
}