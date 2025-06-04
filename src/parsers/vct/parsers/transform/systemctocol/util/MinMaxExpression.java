package vct.parsers.transform.systemctocol.util;

public class MinMaxExpression {
    public enum MinMaxType {
        MIN {
            @Override
            public String toString() {
                return "min";
            }
        },
        MAX {
            @Override
            public String toString() {
                return "max";
            }
        }
    }

    private MinMaxType op;
    private LinearExpression left;
    private LinearExpression right;

    public MinMaxExpression(MinMaxType op, LinearExpression left, LinearExpression right) {
        this.op = op;
        // keep the lower lexicographic expression to the left
        if (left.toString().compareTo(right.toString()) < 0) {
            this.left = new LinearExpression(left);
            this.right = new LinearExpression(right);
        } else {
            this.left = new LinearExpression(right);
            this.right = new LinearExpression(left);
        }
    }

    public MinMaxExpression evaluate(Object var, Bound value) {
        LinearExpression new_left = this.left.evaluate(var, value);
        LinearExpression new_right = this.right.evaluate(var, value);
        return new MinMaxExpression(this.op, new_left, new_right);
    }

    public boolean isSolvable() {
        LinearExpression diff = this.left.add(LinearExpression.minus(this.right));
        if (diff.isConstant()) {
            return true;
        }
        return false;
    }

    public LinearExpression solve() {
        // if both sides are the same expression, then the result is that
        if (this.left.equals(this.right)) {
            return new LinearExpression(this.left);
        }
        // we can directly compare two constants
        if (this.left.isConstant() && this.right.isConstant()) {
            Bound lc = this.left.getConstant();
            Bound rc = this.right.getConstant();
            switch (this.op) {
                case MIN: return new LinearExpression().add(Bound.min(lc, rc));
                case MAX: return new LinearExpression().add(Bound.max(lc, rc));
            }
        }
        // if the difference between two expressions is a constant,
        // then we can decide which of them is smaller
        LinearExpression diff = this.left.add(LinearExpression.minus(this.right));
        if (diff.isConstant()) {
            // this is the value of left - right
            // if it is negative, then left < right
            Bound value = diff.getConstant();
            LinearExpression smaller;
            LinearExpression bigger;
            if (value.compareTo(new Bound(0)) < 0) {
                smaller = new LinearExpression(this.left);
                bigger = new LinearExpression(this.right);
            } else {
                smaller = new LinearExpression(this.right);
                bigger = new LinearExpression(this.left);
            }
            switch (this.op) {
                case MIN: return smaller;
                case MAX: return bigger;
            }
        }
        // if we cannot simplify in any way, then just return an expression
        // with this as the only term
        return new LinearExpression().add(this, 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MinMaxExpression)) {
            return false;
        }
        MinMaxExpression other = (MinMaxExpression) obj;
        if (this.op != other.op) {
            return false;
        }
        return (this.left.equals(other.left) && this.right.equals(other.right)) ||
                (this.left.equals(other.right) && this.right.equals(other.left));
    }

    @Override
    public String toString() {
        return this.op.toString() + "(" + this.left.toString() + ", " + this.right.toString() + ")";
    }
}
