package vct.parsers.transform.systemctocol.util;

import java.util.List;
import java.util.ArrayList;

public class BinarySimpleExpression implements SimpleExpression {
    private SimpleOperation op;
    private SimpleExpression left;
    private SimpleExpression right;

    public BinarySimpleExpression(SimpleOperation op, SimpleExpression left, SimpleExpression right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public BinarySimpleExpression(SimpleExpression left, SimpleOperation op, SimpleExpression right) {
        this(op, left, right);
    }

    public SimpleExpression simplify() {
        SimpleExpression l = this.left.simplify();
        SimpleExpression r = this.right.simplify();

        // solve constant expressions
        if (l instanceof ConstantSimpleExpression && r instanceof ConstantSimpleExpression) {
            return foldConstant(this.op, (ConstantSimpleExpression) l, (ConstantSimpleExpression) r);
        }

        // simplify min/max statements
        if (this.op == SimpleOperation.MIN || this.op == SimpleOperation.MAX) {
            return foldMinMax(this.op, l, r);
        }

        // simplify operations of the form x +- 0 and 0 + x
        if (this.op == SimpleOperation.ADD && l instanceof ConstantSimpleExpression expr && expr.getValue().equals(0)) {
            return r;
        }
        if (this.op == SimpleOperation.ADD && r instanceof ConstantSimpleExpression expr && expr.getValue().equals(0)) {
            return l;
        }
        if (this.op == SimpleOperation.SUB && r instanceof ConstantSimpleExpression expr && expr.getValue().equals(0)) {
            return l;
        }

        // simplify operations of the form x * 0 and 0 * x
        if (this.op == SimpleOperation.MUL && l instanceof ConstantSimpleExpression expr && expr.getValue().equals(0)) {
            return new ConstantSimpleExpression(0);
        }
        if (this.op == SimpleOperation.MUL && r instanceof ConstantSimpleExpression expr && expr.getValue().equals(0)) {
            return new ConstantSimpleExpression(0);
        }

        // apply associativity rules
        if (Associative.OPERATORS.containsKey(this.op)) {
            return foldAssociative(op, l, r);
        }

        // if all else fails, just return a binary expression with simplified terms
        return new BinarySimpleExpression(l, op, r);
    }

    private static SimpleExpression foldAssociative(SimpleOperation op, SimpleExpression l, SimpleExpression r) {
        Associative operator = Associative.OPERATORS.get(op);

        // recursively find all operands from this expression
        List<SimpleExpression> operands = new ArrayList<>();
        getOperands(op, l, operands);
        getOperands(op, r, operands);

        // try to simplify the operands if possible
        List<SimpleExpression> simplified = new ArrayList<>();
        for (SimpleExpression expr : operands) {
            simplified.add(expr.simplify());
        }

        // separate constants from variables
        List<SimpleExpression> constants = new ArrayList<>();
        List<SimpleExpression> variables = new ArrayList<>();
        for (SimpleExpression expr : simplified) {
            if (expr instanceof ConstantSimpleExpression) {
                constants.add(expr);
            } else {
                variables.add(expr);
            }
        }
        
        // combine constant terms
        Bound finalConstant = operator.identity;
        for (SimpleExpression expr : constants) {
            Bound constant = ((ConstantSimpleExpression) expr).getBound();
            finalConstant = operator.fold.apply(finalConstant, constant);
        }

        // if the final constant term is different from the identity, then we can add to the final expression
        List<SimpleExpression> terms = variables;
        if (!finalConstant.equals(operator.identity)) {
            terms.add(new ConstantSimpleExpression(finalConstant));
        }

        // finally build the expression
        SimpleExpression fold = terms.get(0);
        for (int i = 1; i < terms.size(); i++) {
            fold = new BinarySimpleExpression(fold, op, terms.get(i));
        }

        return fold;
    }

    private static void getOperands(SimpleOperation op, SimpleExpression expr, List<SimpleExpression> operands) {
        if (expr instanceof BinarySimpleExpression) {
            BinarySimpleExpression binaryExpression = (BinarySimpleExpression) expr;
            // the suboperation is of the same type, so get recursively
            if (binaryExpression.op == op) {
                getOperands(op, binaryExpression.left, operands);
                getOperands(op, binaryExpression.right, operands);
            }
        } else {
            operands.add(expr);
        }
    }

    private static SimpleExpression foldMinMax(SimpleOperation op, SimpleExpression a, SimpleExpression b) {
        if (a.equals(b)) {
            return a;
        }
        return new BinarySimpleExpression(a, op, b);
    }

    private static SimpleExpression foldConstant(SimpleOperation op, ConstantSimpleExpression a, ConstantSimpleExpression b) {
        switch (op) {
            case ADD:
                return new ConstantSimpleExpression(a.getBound().add(b.getBound()));
            case SUB:
                return new ConstantSimpleExpression(a.getBound().add(Bound.minus(b.getBound())));
            case MIN:
                return new ConstantSimpleExpression(Bound.min(a.getBound(), b.getBound()));
            case MAX:
                return new ConstantSimpleExpression(Bound.max(a.getBound(), b.getBound()));
            default:
                return new ConstantSimpleExpression(Bound.UNDEFINED);
        }
    }

    @Override
    public String toString() {
        String leftString = this.left.toString();
        String rightString = this.right.toString();
        String opString = this.op.toString();
        if (this.op == SimpleOperation.MIN || this.op == SimpleOperation.MAX) {
            return opString + "(" + leftString + ", " + rightString + ")";
        }
        return leftString + " " + opString + " " + rightString;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BinarySimpleExpression)) {
            return false;
        }
        BinarySimpleExpression expr = (BinarySimpleExpression) other;
        return this.op == expr.op && this.left.equals(expr.left) && this.right.equals(expr.right);
    }
}
