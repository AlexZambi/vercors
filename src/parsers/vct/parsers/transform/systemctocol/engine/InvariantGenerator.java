package vct.parsers.transform.systemctocol.engine;

import scala.Option;
import scala.reflect.ClassTag$;
import vct.col.ast.*;
import vct.col.ref.DirectRef;
import vct.col.ref.LazyRef;
import vct.col.ref.Ref;
import scala.math.BigInt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tub.pes.syscir.sc_model.SCVariable;
import de.tub.pes.syscir.sc_model.expressions.*;
import de.tub.pes.syscir.sc_model.variables.SCClassInstance;
import vct.parsers.transform.systemctocol.colmodel.COLSystem;
import vct.parsers.transform.systemctocol.util.*;
import vct.parsers.transform.systemctocol.util.MinMaxExpression.MinMaxType;

public class InvariantGenerator<T> {
    private COLSystem<T> col_system;

    public InvariantGenerator(COLSystem<T> col_system) {
        this.col_system = col_system;
    }

    public Expr<T> generateInvariant(LinearExpression init_value, Compare cond, LinearExpression transfer_function, Expr<T> var) {
        
        SymbolicState state = new SymbolicState();
        state.add("n", new SymbolicInterval(5));
        state.add("i", new SymbolicInterval(0));
        state.add("j", new SymbolicInterval(1));
        Assignment assign = new Assignment("i", new LinearExpression().add("i", 1).add(1));
        List<Assignment> body = new ArrayList<>();
        body.add(assign);
        assign = new Assignment("j", new LinearExpression().add("i", 1).add(-1));
        body.add(assign);
        System.out.println("before: " + state.toString());
        for (int i = 0; i < 5; i++) {
            state = executeAndJoin(body, state);
            System.out.println("at iteration " + String.valueOf(i) + ": " + state.toString());
        }
        System.out.println("finally: " + state.toString());

        System.out.println();
        System.out.println();
        System.out.println();

        // if constant loop, get bounds
        if (init_value.isConstant() && cond.getRight().isConstant() && transfer_function.isUnivariate()) {
            return getConstantBoundInvariant(init_value, cond, transfer_function, var);
        }

        // if we have a variable guard, but constant initial value
        // if (init_value.isConstant()) {
            
        // }

        Expr<T> result = getVariableGuardInvariant(init_value, cond, transfer_function, var);
        Expr<T> guard_relation = getGuardTransferRelationInvariant(cond, transfer_function, var);
        if (guard_relation != null) {
            result = new And<>(result, guard_relation, OriGen.create());
        }

        return result;

        // return null;
    }

    public SymbolicState executeAndJoin(List<Assignment> body, SymbolicState initial_state) {
        return executeBody(body, initial_state).join(initial_state).reduce();
    }

    public SymbolicState executeBody(List<Assignment> body, SymbolicState initial_state) {
        SymbolicState current_state = new SymbolicState(initial_state);
        for (Assignment assignment : body) {
            Object var = assignment.var;
            LinearExpression transform = assignment.expr;
            // if we don't have this symbol in the state, then it doesn't change the state
            if (!current_state.hasSymbol(var)) {
                continue;
            }
            if (!assignment.isRecurrent()) {
                current_state.add(var, new SymbolicInterval(transform));
            } else {
                current_state.update(var, transform);
            }
            current_state = current_state.reduce();
        }
        return current_state;
    }

    public List<Assignment> parseBody(List<Expression> expressions, SCClassInstance sc_inst) {
        List<Assignment> result = new ArrayList<>();
        for (Expression expression : expressions) {
            if (!(expression instanceof BinaryExpression)) {
                continue;
            }
            BinaryExpression expr = (BinaryExpression) expression;
            if (!Arrays.asList("=", "+=", "-=", "*=").contains(expr.getOp())) {
                // unsupported expression, best to just return null
                return null;
            }
            if (!(expr.getLeft() instanceof SCVariableExpression)) {
                // not sure if this can even happen, but to be sure
                return null;
            }
            Expr<T> var = getVariableFromExpression((SCVariableExpression) expr.getLeft(), sc_inst);
            LinearExpression assign = getLinearExpression(expr.getRight(), sc_inst);
            if (assign == null) {
                return null;
            }
            if (expr.getOp().equals("+=")) {
                assign = assign.add(var, 1);
            } else if (expr.getOp().equals("-=")) {
                assign = assign.add(var, -1);
            } else if (expr.getOp().equals("*=")) {
                // we don't support multipliying symbols together
                if (!assign.isConstant()) {
                    return null;
                }
                assign = new LinearExpression().add(var, assign.getConstant());
            }
            result.add(new Assignment(var, assign));
        }
        return result;
    }

    public Expr<T> getVariableFromExpression(SCVariableExpression expr, SCClassInstance sc_inst) {
        SCVariable var_raw = expr.getVar();
        VariableTransformer<T> variableTransformer = new VariableTransformer<>(sc_inst, col_system);
        InstanceField<T> var_field = variableTransformer.transform_variable_to_instance_field(var_raw);
        Ref<T, InstanceField<T>> var_ref = new DirectRef<>(var_field, ClassTag$.MODULE$.apply(InstanceField.class));
        return new Deref<>(col_system.THIS, var_ref, new GeneratedBlame<>(), OriGen.create());
    }

    public Compare getCompare(Expression expr, SCClassInstance sc_inst) {
        if (!(expr instanceof BinaryExpression)) {
            return null;
        }
        BinaryExpression bin = (BinaryExpression) expr;
        ComparisonType op = null;
        switch (bin.getOp()) {
            case "<": op = ComparisonType.LESSER; break;
            case "<=": op = ComparisonType.LESSER_EQ; break;
            case ">": op = ComparisonType.GREATER; break;
            case ">=": op = ComparisonType.GREATER_EQ; break;
            default: op = null; break;
        }
        if (op == null) {
            return null;
        }
        LinearExpression left = getLinearExpression(bin.getLeft(), sc_inst);
        LinearExpression right = getLinearExpression(bin.getRight(), sc_inst);
        if (left == null || right == null) {
            return null;
        }
        return new Compare(op, left, right);
    }

    public LinearExpression getLinearExpression(Expression expr, SCClassInstance sc_inst) {
        LinearExpression result = new LinearExpression();
        if (expr == null) {
            return null;
        } else if (expr instanceof ConstantExpression c) {
            return result.add(Integer.valueOf(c.getValue()));
        } else if (expr instanceof SCVariableExpression v) {
            Expr<T> var = getVariableFromExpression(v, sc_inst);
            return result.add(var, 1);
        } else if (expr instanceof BracketExpression b) {
            return getLinearExpression(b.getInBrackets(), sc_inst);
        } else if (expr instanceof BinaryExpression b) {
            LinearExpression left = getLinearExpression(b.getLeft(), sc_inst);
            if (left == null) return null;
            LinearExpression right = getLinearExpression(b.getRight(), sc_inst);
            if (right == null) return null;
            switch (b.getOp()) {
                case "+": return left.add(right);
                case "-": return left.add(LinearExpression.minus(right));
                case "*": {
                    // at least one of the operands must be a constant to ensure that
                    // the resulting expression is still a linear expression
                    if (!left.isConstant() && !right.isConstant()) {
                        return null;
                    }
                    if (left.isConstant()) {
                        return right.mult(left.getConstant());
                    } else {
                        return left.mult(right.getConstant());
                    }
                }
                default: return null;
            }
        } else {
            return null;
        }
    }

    public Expr<T> translateLinearExpression(LinearExpression expr) {
        // check if this is a constant expression
        int constant = expr.getConstant().getValue();
        if (expr.isConstant()) {
            return new IntegerValue<>(BigInt.apply(constant), OriGen.create()); 
        }

        // it is not constant, so build it
        Expr<T> result = null;
        for (Object obj : expr.getTerms().keySet()) {
            Expr<T> var = (Expr<T>) obj;
            int coefficient = expr.getTerms().get(obj).getValue();
            // build the operand based on its coefficient
            Expr<T> operand = null;
            // if the coefficient is just one, don't write it
            if (coefficient == 1 || coefficient == -1) {
                operand = var;
            } else if (coefficient > 0) {
                Expr<T> value = new IntegerValue<>(BigInt.apply(coefficient), OriGen.create());
                operand = new Mult<>(value, var, OriGen.create());
            } else {
                Expr<T> value = new IntegerValue<>(BigInt.apply(-coefficient), OriGen.create());
                operand = new Mult<>(value, var, OriGen.create());
            }
            // is this the first operand?
            if (result == null) {
                // add the sign properly
                if (coefficient < 0) {
                    result = new UMinus<>(operand, OriGen.create());
                } else {
                    result = operand;
                }
            } else {
                // this is not the first operand, add the sign properly
                if (coefficient < 0) {
                    result = new Minus<>(result, operand, OriGen.create());
                } else {
                    result = new Plus<>(result, operand, OriGen.create());
                }
            }
        }
        // finally we add the constant
        Expr<T> value;
        if (constant > 0) {
            value = new IntegerValue<>(BigInt.apply(constant), OriGen.create());
            result = new Plus<>(result, value, OriGen.create());
        } else if (constant < 0) {
            value = new IntegerValue<>(BigInt.apply(-constant), OriGen.create()); 
            result = new Minus<>(result, value, OriGen.create());
        }

        return result;
    }

    public Expr<T> translateCompare(Compare comp, boolean reduce) {
        Expr<T> left;
        Expr<T> right;
        if (reduce) {
            left = translateLinearExpression(comp.reduce().getLeft());
            right = translateLinearExpression(comp.reduce().getRight());
        } else {
            left = translateLinearExpression(comp.getLeft());
            right = translateLinearExpression(comp.getRight());
        }
        switch (comp.getOp()) {
            case EQ: return new Eq<>(left, right, OriGen.create());
            case NEQ: return new Neq<>(left, right, OriGen.create());
            case LESSER: return new Less<>(left, right, OriGen.create());
            case LESSER_EQ: return new LessEq<>(left, right, OriGen.create());
            case GREATER: return new Greater<>(left, right, OriGen.create());
            case GREATER_EQ: return new GreaterEq<>(left, right, OriGen.create());
            default: return null;
        }
    }

    public Expr<T> translateCompare(Compare comp) {
        return translateCompare(comp, false);
    }

    public Expr<T> getGuardTransferRelationInvariant(Compare cond, LinearExpression transfer_function, Expr<T> var) {
        ComparisonType op;
        switch (cond.getOp()) {
            case LESSER, LESSER_EQ: op = ComparisonType.LESSER_EQ; break;
            case GREATER, GREATER_EQ: op = ComparisonType.GREATER_EQ; break;
            default: return null;
        }
        LinearExpression last_step = getLastStep(cond);
        if (last_step == null) {
            return null;
        }
        LinearExpression symbolic_bound = transfer_function.replace(var, last_step);
        Compare relation = new Compare(op, cond.getRight(), symbolic_bound);
        relation = relation.reduce();
        // this just means that the relation has simplified to true,
        // so we discard it
        if (relation.getLeft().isConstant()) {
            return null;
        }
        return translateCompare(relation);
    }

    public LinearExpression getLastStep(Compare cond) {
        LinearExpression last_step = cond.getRight();
        switch (cond.getOp()) {
            case EQ, NEQ: return null;
            case LESSER: last_step = last_step.add(-1); break;
            case GREATER: last_step = last_step.add(1); break;
            default: return null;
        }
        return last_step;
    }

    public Expr<T> getVariableGuardInvariant(LinearExpression init_value, Compare cond, LinearExpression transfer_function, Expr<T> var) {
        LinearExpression last_step = getLastStep(cond);
        if (last_step == null) {
            return null;
        }
        LinearExpression symbolic_bound = transfer_function.replace(var, last_step);
        Compare lower;
        Compare upper;
        switch (cond.getOp()) {
            case EQ, NEQ: return null;
            case LESSER, LESSER_EQ: {
                lower = new Compare(ComparisonType.LESSER_EQ, init_value, new LinearExpression().add(var, 1));
                upper = new Compare(ComparisonType.LESSER_EQ, new LinearExpression().add(var, 1), symbolic_bound);
                break;
            }
            case GREATER, GREATER_EQ: {
                lower = new Compare(ComparisonType.LESSER_EQ, symbolic_bound, new LinearExpression().add(var, 1));
                upper = new Compare(ComparisonType.LESSER_EQ, new LinearExpression().add(var, 1), init_value);
                break;
            }
            default: return null;
        }
        return new And<>(translateCompare(lower), translateCompare(upper), OriGen.create());
    }

    public Expr<T> getConstantBoundInvariant(LinearExpression init_value, Compare cond, LinearExpression transfer_function, Expr<T> var) {
        Interval loop_bounds = getLoopBounds(init_value, transfer_function, cond, var);

        Compare lower = new Compare(ComparisonType.LESSER_EQ,
            new LinearExpression().add(loop_bounds.getLower()),
            new LinearExpression().add(var, 1));
        Compare upper = new Compare(ComparisonType.LESSER_EQ,
            new LinearExpression().add(var, 1),
            new LinearExpression().add(loop_bounds.getUpper()));

        return new And<>(translateCompare(lower), translateCompare(upper), OriGen.create());
    }

    public Interval getLoopBounds(LinearExpression init, LinearExpression transfer_function, Compare cond, Expr<T> var) {
        Interval bounds = new Interval(init.getConstant());
        Interval new_bounds = bounds;
        do {
            Bound lower = transfer_function.evaluate(var, new_bounds.getLower()).getConstant();
            Bound upper = transfer_function.evaluate(var, new_bounds.getUpper()).getConstant();
            Interval result = new Interval(lower, upper);
            Interval next = bounds.join(result);
            bounds = new_bounds;
            new_bounds = next;
        } while (!bounds.equals(new_bounds) && isGuarded(bounds, cond, var));
        return bounds;
    }

    public boolean isGuarded(Interval interval, Compare cond, Expr<T> var) {
        Compare cond_lower = new Compare(cond.getOp(), cond.getLeft().evaluate(var, interval.getLower()), cond.getRight());
        Compare cond_upper = new Compare(cond.getOp(), cond.getLeft().evaluate(var, interval.getUpper()), cond.getRight());
        return cond_lower.evaluate() && cond_upper.evaluate();
    }
}
