package vct.parsers.transform.systemctocol.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Associative {
    public final SimpleOperation op;
    public final Bound identity;
    public final BiFunction<Bound, Bound, Bound> fold;

    public Associative(SimpleOperation op, Bound identity, BiFunction<Bound, Bound, Bound> fold) {
        this.op = op;
        this.identity = identity;
        this.fold = fold;
    }

    public static final Map<SimpleOperation, Associative> OPERATORS;

    static {
        OPERATORS = new HashMap<SimpleOperation, Associative>();
        OPERATORS.put(SimpleOperation.ADD, new Associative(SimpleOperation.ADD, new Bound(0), (a, b) -> Bound.add(a, b)));
        OPERATORS.put(SimpleOperation.MUL, new Associative(SimpleOperation.MUL, new Bound(1), (a, b) -> Bound.mult(a, b)));
        OPERATORS.put(SimpleOperation.MIN, new Associative(SimpleOperation.MIN, Bound.INFINITY, Bound::min));
        OPERATORS.put(SimpleOperation.MAX, new Associative(SimpleOperation.MAX, Bound.MINUS_INFINITY, Bound::max));
    }
}
