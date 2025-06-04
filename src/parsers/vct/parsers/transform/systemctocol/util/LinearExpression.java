package vct.parsers.transform.systemctocol.util;

import java.util.Map;
import java.util.HashMap;

/**
 * Class representing a linear expression. A linear expression is a linear combination of
 * variables with integer coefficients and a constant integer term.
 */
public class LinearExpression {
    /**
     * Variable terms are stored as a item with items of the form <Variable, Coefficient>.
     * This ensures that a variable may only appear once in the linear expression.
     */
    private Map<Object, Bound> terms;
    private Bound constant;

    /**
     * Construct a linear expression that is the constant 0.
     */
    public LinearExpression() {
        this.terms = new HashMap<>();
        this.constant = new Bound(0);
    }

    /**
     * Construct a new linear expression.
     * 
     * @param terms the variables and coefficients of the expression
     * @param constant the constant term
     */
    public LinearExpression(Map<Object, Bound> terms, Bound constant) {
        this.terms = terms;
        this.constant = constant;
    }

    /**
     * Copy constructor.
     */
    public LinearExpression(LinearExpression other) {
        this.terms = new HashMap<>(other.getTerms());
        this.constant = new Bound(other.getConstant());
    }

    /**
     * Add a variable with coefficient to an expressin. If the expression did not contain
     * this terms before, then it is simply added. If the expression contains the variable
     * already, then the coefficients are added.
     * 
     * @param expr the expression to mutate
     * @param var the variable to add
     * @param coeff the coefficient of the variable
     * @return a new linear expression representing the result
     */
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

    /**
     * Add a variable with coefficient to an expressin. If the expression did not contain
     * this terms before, then it is simply added. If the expression contains the variable
     * already, then the coefficients are added.
     * 
     * @param expr the expression to mutate
     * @param var the variable to add
     * @param coeff the coefficient of the variable
     * @return a new linear expression representing the result
     */
    public static LinearExpression add(LinearExpression expr, Object var, Integer coeff) {
        return add(expr, var, new Bound(coeff));
    }

    /**
     * Add a variable with coefficient to the expressin. If the expression did not contain
     * this terms before, then it is simply added. If the expression contains the variable
     * already, then the coefficients are added.
     * 
     * @param var the variable to add
     * @param coeff the coefficient of the variable
     * @return a new linear expression representing the result
     */
    public LinearExpression add(Object var, Bound coeff) {
        return add(this, var, coeff);
    }

    /**
     * Add a variable with coefficient to the expressin. If the expression did not contain
     * this terms before, then it is simply added. If the expression contains the variable
     * already, then the coefficients are added.
     * 
     * @param var the variable to add
     * @param coeff the coefficient of the variable
     * @return a new linear expression representing the result
     */
    public LinearExpression add(Object var, Integer coeff) {
        return add(this, var, coeff);
    }

    /**
     * Add a constant term to an expression.
     * 
     * @param expr the expression to mutate
     * @param constant the term to add
     * @return a new linear expression representing the result
     */
    public static LinearExpression add(LinearExpression expr, Bound constant) {
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound prevConstant = expr.getConstant();
        return new LinearExpression(terms, prevConstant.add(constant));
    }

    /**
     * Add a constant term to an expression.
     * 
     * @param expr the expression to mutate
     * @param constant the term to add
     * @return a new linear expression representing the result
     */
    public static LinearExpression add(LinearExpression expr, Integer constant) {
        return add(expr, new Bound(constant));
    }

    /**
     * Add a constant term to the expression.
     * 
     * @param expr the expression to mutate
     * @param constant the term to add
     * @return a new linear expression representing the result
     */
    public LinearExpression add(Bound constant) {
        return add(this, constant);
    }

    /**
     * Add a constant term to the expression.
     * 
     * @param expr the expression to mutate
     * @param constant the term to add
     * @return a new linear expression representing the result
     */
    public LinearExpression add(Integer constant) {
        return add(this, constant);
    }

    /**
     * Add two linear expressions. Like terms are collected together.
     * 
     * @param a the first expression
     * @param b the second expression
     * @return a new linear expression representing the result
     */
    public static LinearExpression add(LinearExpression a, LinearExpression b) {
        LinearExpression result = new LinearExpression(a);
        for (Object var : b.getTerms().keySet()) {
            Bound coeff = b.getTerms().get(var);
            result = add(result, var, coeff);
        }
        result = add(result, b.getConstant());
        return result;
    }

    /**
     * Add a linear expression to this one. Like terms are collected together.
     * 
     * @param a the first expression
     * @param b the second expression
     * @return a new linear expression representing the result
     */
    public LinearExpression add(LinearExpression other) {
        return add(this, other);
    }

    /**
     * Multiply a linear expression by a constant factor.
     * 
     * @param expr the expression to mutate
     * @param value the scalar factor
     * @return the resuling linear expression
     */
    public static LinearExpression mult(LinearExpression expr, Bound value) {
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound constant = new Bound(expr.getConstant());
        for (Object var : terms.keySet()) {
            terms.put(var, value.mult(terms.get(var)));
        }
        constant = constant.mult(value);
        return new LinearExpression(terms, constant);
    }

    /**
     * Multiply a linear expression by a constant factor.
     * 
     * @param expr the expression to mutate
     * @param value the scalar factor
     * @return the resuling linear expression
     */
    public static LinearExpression mult(LinearExpression expr, Integer value) {
        return mult(expr, new Bound(value));
    }

    /**
     * Multiply the linear expression by a constant factor.
     * 
     * @param value the scalar factor
     * @return the resuling linear expression
     */
    public LinearExpression mult(Bound value) {
        return mult(this, value);
    }

    /**
     * Multiply the linear expression by a constant factor.
     * 
     * @param value the scalar factor
     * @return the resuling linear expression
     */
    public LinearExpression mult(Integer value) {
        return mult(this, value);
    }

    /**
     * Give the negation of an expression (i.e. all coefficients and the constant are multiplied by -1).abstract
     * 
     * @param expr the expression to mutate
     * @return the resulting linear expression
     */
    public static LinearExpression minus(LinearExpression expr) {
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound constant = new Bound(expr.getConstant());
        for (Object var : terms.keySet()) {
            terms.put(var, Bound.minus(terms.get(var)));
        }
        constant = Bound.minus(constant);
        return new LinearExpression(terms, constant);
    }

    /**
     * Replace a variable in a linear expression. If the variable being replaced is not present
     * in the original expression, then the original expression is returned
     * 
     * @param expr the expression to mutate
     * @param oldVar the variable to be replaced
     * @param newVar the variable to replace with
     * @return a new linear expression representing the result
     */
    public static LinearExpression replace(LinearExpression expr, Object oldVar, Object newVar) {
        if (!expr.getTerms().containsKey(oldVar)) {
            return new LinearExpression(expr);
        }
        Map<Object, Bound> terms = new HashMap<>(expr.getTerms());
        Bound constant = new Bound(expr.getConstant());
        if (!expr.getTerms().containsKey(newVar)) {
            terms.put(newVar, terms.get(oldVar));
            terms.remove(oldVar);
            return new LinearExpression(terms, constant);
        }
        Bound oldCoeff = terms.remove(oldVar);
        LinearExpression result = new LinearExpression(terms, constant);
        return result.add(newVar, oldCoeff);
    }

    /**
     * Replace a variable in the linear expression. If the variable being replaced is not present
     * in the original expression, then the original expression is returned
     * 
     * @param oldVar the variable to be replaced
     * @param newVar the variable to replace with
     * @return a new linear expression representing the result
     */
    public LinearExpression replace(Object oldVar, Object newVar) {
        return replace(this, oldVar, newVar);
    }

    /**
     * Replace a variable with a linear expression. If the variable being replaced is not present
     * in the original expression, then the original expression is returned.
     * 
     * @param expr the expression to mutate
     * @param var the variable to be replaced
     * @param rep the expression to replace with
     * @return a new linear expression representing the result
     */
    public static LinearExpression replace(LinearExpression expr, Object var, LinearExpression rep) {
        if (!expr.getTerms().containsKey(var)) {
            return new LinearExpression(expr);
        }
        LinearExpression result = new LinearExpression(expr);
        Bound coeff = result.getTerms().remove(var);
        LinearExpression scaled = rep.mult(coeff);
        return result.add(scaled);
    }

    /**
     * Replace a variable with a linear expression. If the variable being replaced is not present
     * in the original expression, then the original expression is returned.
     * 
     * @param expr the expression to mutate
     * @param var the variable to be replaced
     * @param rep the expression to replace with
     * @return a new linear expression representing the result
     */
    public LinearExpression replace(Object var, LinearExpression rep) {
        return replace(this, var, rep);
    }

    /**
     * Evaluate an expression by replacing a variable by a constant term.
     * 
     * @param expr the expression to mutate
     * @param var the variable to be replaced
     * @param value the value to replace with
     * @return a new linear expression representing the result
     */
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

    /**
     * Evaluate an expression by replacing a variable by a constant term.
     * 
     * @param expr the expression to mutate
     * @param var the variable to be replaced
     * @param value the value to replace with
     * @return a new linear expression representing the result
     */
    public static LinearExpression evaluate(LinearExpression expr, Object var, Integer value) {
        return evaluate(expr, var, new Bound(value));
    }

    /**
     * Evaluate the expression by replacing a variable by a constant term.
     * 
     * @param var the variable to be replaced
     * @param value the value to replace with
     * @return a new linear expression representing the result
     */
    public LinearExpression evaluate(Object var, Bound value) {
        return evaluate(this, var, value);
    }

    /**
     * Evaluate the expression by replacing a variable by a constant term.
     * 
     * @param var the variable to be replaced
     * @param value the value to replace with
     * @return a new linear expression representing the result
     */
    public LinearExpression evaluate(Object var, Integer value) {
        return evaluate(this, var, value);
    }

    /**
     * Evaluate an expression by replacing a set of variables by constant terms.
     * 
     * @param expr the expression to mutate
     * @param instantiation the map of variables to constant values
     * @return a new linear expression representing the result
     */
    public static LinearExpression evaluate(LinearExpression expr, Map<Object, Bound> instantiation) {
        LinearExpression result = new LinearExpression(expr);
        for (Object var : instantiation.keySet()) {
            Bound value = instantiation.get(var);
            result = evaluate(expr, var, value);
        }
        return result;
    }

    /**
     * Evaluate the expression by replacing a set of variables by constant terms.
     * 
     * @param expr the expression to mutate
     * @param instantiation the map of variables to constant values
     * @return a new linear expression representing the result
     */
    public LinearExpression evaluate(Map<Object, Bound> instantiation) {
        return evaluate(this, instantiation);
    }

    /**
     * Check if the expression is just a constant.
     * 
     * @return true if the expression if constant, false otherwise
     */
    public boolean isConstant() {
        return this.terms.size() == 0;
    }

    /**
     * Check if the expression is univariate (i.e. has exactly one variable).
     * 
     * @return true if the expression is univariate, false otherwise.
     */
    public boolean isUnivariate() {
        return this.terms.keySet().size() == 1;
    }

    /**
     * Check if the expression contains a certain variable.
     * 
     * @param var the variable
     * @return true if the expression contains the variable, false otherwise
     */
    public boolean hasVariable(Object var) {
        return this.terms.containsKey(var);
    }

    /**
     * Get the coefficient of a variable.
     * 
     * @param var the variable
     * @return the coeffcient of the variable if the variable is present, null  otherwise
     */
    public Bound getCoefficient(LinearExpression expr, Object var) {
        if (!hasVariable(var)) {
            return null;
        }
        return this.terms.get(var);
    }

    /**
     * Get the variable of a univariate expression.
     * 
     * @return the variable of the expression if it is univariate, null otherwise
     */
    public Object getVariable() {
        if (!isUnivariate()) {
            return null;
        }
        Object result = null;
        for (Object var : this.terms.keySet()) {
            result = var;
        }
        return result;
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinearExpression)) {
            return false;
        }
        LinearExpression other = (LinearExpression) obj;
        // other has all variables of this with the same coefficients
        for (Object var : this.terms.keySet()) {
            if (!other.terms.containsKey(var)) {
                return false;
            }
            Bound this_coeff = this.terms.get(var);
            Bound other_coeff = other.terms.get(var);
            if (!this_coeff.equals(other_coeff)) {
                return false;
            }
        }
        // this has all variables of other with the same coefficients
        for (Object var : other.terms.keySet()) {
            if (!this.terms.containsKey(var)) {
                return false;
            }
            Bound this_coeff = this.terms.get(var);
            Bound other_coeff = other.terms.get(var);
            if (!this_coeff.equals(other_coeff)) {
                return false;
            }
        }
        // we also have the same constant terms
        return this.constant.equals(other.constant);
    }
}
