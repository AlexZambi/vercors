package vct.parsers.transform.systemctocol.util;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

public class SymbolicState {
    private Map<Object, SymbolicInterval> symbols;

    public SymbolicState() {
        this.symbols = new TreeMap<>(new StringComparator());
    }

    public SymbolicState(SymbolicState state) {
        this.symbols = new TreeMap<>(new StringComparator());
        this.symbols.putAll(state.symbols);
    }

    public void add(Object var, SymbolicInterval interval) {
        this.symbols.put(var, interval);
    }

    public void add(Object var) {
        this.symbols.put(var, new SymbolicInterval(var));
    }

    public void remove(Object var) {
        if (this.symbols.containsKey(var)) {
            this.symbols.remove(var);
        }
    }

    public void update(Object var, LinearExpression expr) {
        if (!this.symbols.containsKey(var)) {
            return;
        }
        SymbolicInterval interval = this.symbols.get(var);
        interval = interval.apply(expr, var);
        this.symbols.put(var, interval);
    }

    public static SymbolicState join(SymbolicState a, SymbolicState b) {
        SymbolicState result = new SymbolicState();
        // first add all symbols from a
        for (Object var : a.symbols.keySet()) {
            SymbolicInterval interval = a.symbols.get(var);
            // if b also contains that symbol, then join the intervals
            if (b.symbols.containsKey(var)) {
                interval = interval.join(b.symbols.get(var));
            }
            result.add(var, interval);
        }
        // then add all symbols from b that have not already been added before
        for (Object var : b.symbols.keySet()) {
            if (result.symbols.containsKey(var)) {
                continue;
            }
            SymbolicInterval interval = b.symbols.get(var);
            result.add(var, interval);
        }
        return result;
    }

    public SymbolicState join(SymbolicState other) {
        return join(this, other);
    }

    public static Set<Object> getConstants(SymbolicState state) {
        Set<Object> constants = new TreeSet<>(new StringComparator());
        for (Object var : state.symbols.keySet()) {
            SymbolicInterval interval = state.symbols.get(var);
            if (interval.isConstant()) {
                constants.add(var);
            }
        }
        return constants;
    }

    public Set<Object> getConstants() {
        return getConstants(this);
    }

    public void addSymbols(SymbolicState other, Set<Object> symbols) {
        for (Object var : symbols) {
            if (!other.symbols.containsKey(var)) {
                continue;
            }
            add(var, other.symbols.get(var));
        }
    }

    public void removeSymbols(Set<Object> symbols) {
        for (Object var : symbols) {
            remove(var);
        }
    }

    public static SymbolicInterval evaluate(SymbolicInterval interval, SymbolicState state) {
        LinearExpression lower = new LinearExpression();
        LinearExpression upper = new LinearExpression();

        // compute lower bound
        for (Object var : interval.getLower().getTerms().keySet()) {
            Bound coeff = interval.getLower().getTerms().get(var);
            if (!state.symbols.containsKey(var)) {
                lower = lower.add(var, coeff);
                continue;
            }
            LinearExpression term;
            // if we subtract, then we take the biggest amount,
            // if we add, then we take the smallest amount
            if (coeff.compareTo(new Bound(0)) < 0) {
                term = state.symbols.get(var).getUpper();
            } else {
                term = state.symbols.get(var).getLower();
            }
            // add this expression multiplied by the previous coefficient
            lower = lower.add(term.mult(coeff));
        }
        // finally add the constant back
        lower = lower.add(interval.getLower().getConstant());

        // compute upper bound
        for (Object var : interval.getUpper().getTerms().keySet()) {
            Bound coeff = interval.getUpper().getTerms().get(var);
            if (!state.symbols.containsKey(var)) {
                upper = upper.add(var, coeff);
                continue;
            }
            LinearExpression term;
            // if we subtract, then we take the smallest amount,
            // if we add, then we take the biggest amount
            if (coeff.compareTo(new Bound(0)) > 0) {
                term = state.symbols.get(var).getUpper();
            } else {
                term = state.symbols.get(var).getLower();
            }
            // add this expression multiplied by the previous coefficient
            upper = upper.add(term.mult(coeff));
        }
        // finally add the constant back
        upper = upper.add(interval.getUpper().getConstant());

        return new SymbolicInterval(lower, upper);
    }

    public SymbolicInterval evaluate(SymbolicInterval interval) {
        return evaluate(interval, this);
    }

    public static SymbolicInterval evaluate(LinearExpression expr, SymbolicState state) {
        return evaluate(new SymbolicInterval(expr), state);
    }

    public SymbolicInterval evaluate(LinearExpression expr) {
        return evaluate(expr, this);
    }

    public static SymbolicState reduce(SymbolicState state) {
        SymbolicState current_state = new SymbolicState(state);
        Set<Object> constants = getConstants(current_state);
        SymbolicState new_state;
        SymbolicState result = new SymbolicState();

        while (!constants.isEmpty()) {
            result.addSymbols(current_state, constants);
            current_state.removeSymbols(constants);
            new_state = new SymbolicState();
            for (Object var : current_state.symbols.keySet()) {
                SymbolicInterval interval = current_state.symbols.get(var);
                interval = evaluate(interval, result);
                new_state.add(var, interval);
            }
            current_state = new_state;
            constants = getConstants(current_state);
        }

        // add any leftover symbols
        result.addSymbols(current_state, current_state.symbols.keySet());

        return result;
    }

    public static boolean couldEvaluate(SymbolicState state, Compare cond) {
        Set<Object> variables = new TreeSet<>(new StringComparator());
        // add all variables of of the left side
        variables.addAll(cond.getLeft().getTerms().keySet());
        // then of the right side
        variables.addAll(cond.getRight().getTerms().keySet());
        // do we have all these variables in our state?
        return state.symbols.keySet().containsAll(variables);
    }

    public boolean couldEvaluate(Compare cond) {
        return couldEvaluate(this, cond);
    }

    public static boolean satifies(SymbolicState state, Compare cond) {
        SymbolicInterval left = state.evaluate(cond.getLeft());
        SymbolicInterval right = state.evaluate(cond.getRight());
        Compare comp;
        ComparisonType op = cond.getOp();
        switch (op) {
            case LESSER, LESSER_EQ: comp = new Compare(op, left.getUpper(), right.getLower()); break;
            case GREATER, GREATER_EQ: comp = new Compare(op, left.getLower(), right.getUpper()); break;
            default: return false;
        }
        if (!comp.canEvaluate()) {
            return false;
        }
        return comp.evaluate();
    }

    public boolean satifies(Compare cond) {
        return satifies(this, cond);
    }

    public SymbolicState reduce() {
        return reduce(this);
    }

    public boolean hasSymbol(Object var) {
        return this.symbols.containsKey(var);
    }

    public Map<Object, SymbolicInterval> getSymbols() {return this.symbols;}

    @Override
    public String toString() {
        List<Map.Entry<Object, SymbolicInterval>> symbol_entries = new ArrayList<>(this.symbols.entrySet());
        if (symbol_entries.size() < 1) {
            return "{}";
        }
        String result = "{ ";
        for (int i = 0; i < symbol_entries.size() - 1; i++) {
            Map.Entry<Object, SymbolicInterval> entry = symbol_entries.get(i);
            Object var = entry.getKey();
            SymbolicInterval interval = entry.getValue();
            result += var.toString() + ": " + interval.toString();
            result += "; ";
        }
        Map.Entry<Object, SymbolicInterval> entry = symbol_entries.get(symbol_entries.size() - 1);
        Object var = entry.getKey();
        SymbolicInterval interval = entry.getValue();
        result += var.toString() + ": " + interval.toString();
        result += " }";
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SymbolicState)) {
            return false;
        }
        SymbolicState other = (SymbolicState) obj;
        // other has all symbols of this with the same symbolic intervals
        for (Object var : this.symbols.keySet()) {
            if (!other.symbols.containsKey(var)) {
                return false;
            }
            SymbolicInterval this_interval = this.symbols.get(var);
            SymbolicInterval other_interval = other.symbols.get(var);
            if (!this_interval.equals(other_interval)) {
                return false;
            }
        }
        // this has all symbols of other with the same symbolic intervals
        for (Object var : other.symbols.keySet()) {
            if (!this.symbols.containsKey(var)) {
                return false;
            }
            SymbolicInterval this_interval = this.symbols.get(var);
            SymbolicInterval other_interval = other.symbols.get(var);
            if (!this_interval.equals(other_interval)) {
                return false;
            }
        }
        // if we reach here, then the states are the same
        return true;
    }
}