package vct.parsers.transform.systemctocol.util;

import java.util.Map;
import java.util.HashMap;

public class LinearExpression {
    private Map<Object, Bound> terms;
    private Bound constant;

    public LinearExpression() {
        this.terms = new HashMap<>();
        this.constant = new Bound(0);
    }

    public LinearExpression(Map<Object, Bound> terms, Bound constant) {
        this.terms = terms;
        this.constant = constant;
    }

    public LinearExpression(LinearExpression other) {
        this.terms = new HashMap<>(other.getTerms());
        this.constant = new Bound(other.getConstant());
    }

    public static LinearExpression add(LinearExpression expr, Object var, Bound coeff) {
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound constant = expr.getConstant();
        Bound prevCoeff = terms.getOrDefault(var, null);
        if (prevCoeff == null) {
            terms.put(var, coeff);
        } else {
            terms.put(var, prevCoeff.add(coeff));
        }
        return new LinearExpression(terms, constant);
    }

    public static LinearExpression add(LinearExpression expr, Object var, Integer coeff) {
        return add(expr, var, new Bound(coeff));
    }

    public LinearExpression add(Object var, Bound coeff) {
        return add(this, var, coeff);
    }

    public LinearExpression add(Object var, Integer coeff) {
        return add(this, var, coeff);
    }

    public static LinearExpression add(LinearExpression expr, Bound constant) {
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound prevConstant = expr.getConstant();
        return new LinearExpression(terms, prevConstant.add(constant));
    }

    public static LinearExpression add(LinearExpression expr, Integer constant) {
        return add(expr, new Bound(constant));
    }

    public LinearExpression add(Bound constant) {
        return add(this, constant);
    }

    public LinearExpression add(Integer constant) {
        return add(this, constant);
    }

    public static LinearExpression add(LinearExpression a, LinearExpression b) {
        LinearExpression result = new LinearExpression(a);
        for (Object var : b.getTerms().keySet()) {
            Bound coeff = b.getTerms().get(var);
            result = add(result, var, coeff);
        }
        result = add(result, b.getConstant());
        return result;
    }

    public LinearExpression add(LinearExpression other) {
        return add(this, other);
    }

    public static LinearExpression replace(LinearExpression expr, Object oldVar, Object newVar) {
        if (!expr.getTerms().containsKey(oldVar)) {
            return new LinearExpression(expr);
        }
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound constant = new Bound(expr.getConstant());
        terms.put(newVar, terms.get(oldVar));
        terms.remove(oldVar);
        return new LinearExpression(terms, constant);
    }

    public LinearExpression replace(Object oldVar, Object newVar) {
        return replace(this, oldVar, newVar);
    }

    public static LinearExpression evaluate(LinearExpression expr, Object var, Bound value) {
        if (!expr.getTerms().containsKey(var)) {
            return new LinearExpression(expr);
        }
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound constant = expr.getConstant();

        // take the coefficient and remove the variable from the expression
        Bound coeff = terms.remove(var);
        
        // evaluate the new constant term
        Bound newConstant = coeff.mult(value).add(constant);

        return new LinearExpression(terms, newConstant);
    }

    public static LinearExpression evaluate(LinearExpression expr, Object var, Integer value) {
        return evaluate(expr, var, new Bound(value));
    }

    public LinearExpression evaluate(Object var, Bound value) {
        return evaluate(this, var, value);
    }

    public LinearExpression evaluate(Object var, Integer value) {
        return evaluate(this, var, value);
    }

    public static LinearExpression evaluate(LinearExpression expr, Map<Object, Bound> instantiation) {
        LinearExpression result = new LinearExpression(expr);
        for (Object var : instantiation.keySet()) {
            Bound value = instantiation.get(var);
            result = evaluate(expr, var, value);
        }
        return result;
    }

    public LinearExpression evaluate(Map<Object, Bound> instantiation) {
        return evaluate(this, instantiation);
    }

    public Map<Object, Bound> getTerms() {return this.terms;}

    public Bound getConstant() {return this.constant;}

    @Override
    public String toString() {
        String output = "";
        boolean variables = false;
        for (Object var : this.terms.keySet()) {
            Bound coeff = this.terms.get(var);
            if (coeff.compareTo(new Bound(0)) >= 0) {
                output += "+";
            }
            if (!coeff.equals(new Bound(1))) {
                output += coeff.toString();
                output += "*";
            }
            output += var.toString();
            output += " ";
            variables = true;
        }
        if (!this.constant.equals(new Bound(0)) || !variables) {
            if (this.constant.compareTo(new Bound(0)) >= 0 && variables) {
                output += "+ ";
            }
            output += this.constant.toString();
        }
        return output;
    }
}
