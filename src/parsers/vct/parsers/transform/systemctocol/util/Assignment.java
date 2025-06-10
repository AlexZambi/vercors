package vct.parsers.transform.systemctocol.util;

public class Assignment {
    public Object var;
    public LinearExpression expr;

    public Assignment(Object var, LinearExpression expr) {
        this.var = var;
        this.expr = expr;
    }

    public boolean isRecurrent() {
        return expr.hasVariable(var);
    }
}
