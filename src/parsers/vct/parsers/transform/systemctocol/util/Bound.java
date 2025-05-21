package vct.parsers.transform.systemctocol.util;

/**
 * Class representing an integer interval bound.
 */
public class Bound implements Comparable<Bound> {
    private final Integer value;
    private final boolean minus_inf;
    private final boolean plus_inf;
    private final boolean undef;

    /**
     * Create a new Bound with a given value.
     * 
     * @param value the value of the bound
     * @return the new bound
     */
    public Bound(int value) {
        this.value = value;
        this.minus_inf = false;
        this.plus_inf = false;
        this.undef = false;
    }

    /**
     * Create a new non-finite Bound.
     * 
     * @param minus_inf whether this bound is negative infinity
     * @param plus_inf whether this bound if positive infinity
     * @return the new non-finite bound
     */
    private Bound(boolean minus_inf, boolean plus_inf) {
        this.value = null;
        this.minus_inf = minus_inf;
        this.plus_inf = plus_inf;
        this.undef = false;
    }

    /**
     * Copy constructor.
     * 
     * @param bound the bound to copy. 
     */
    private Bound(Bound bound) {
        this.value = bound.value;
        this.minus_inf = bound.minus_inf;
        this.plus_inf = bound.plus_inf;
        this.undef = false;
    }

    /**
     * Constructor used to create an undefined bound.
     */
    private Bound() {
        this.value = null;
        this.minus_inf = false;
        this.plus_inf = false;
        this.undef = true;
    }

    /**
     * Bound representing negative infinity.
     */
    public static final Bound MINUS_INFINITY = new Bound(true, false);
    /**
     * Bound representing positive infinity.
     */
    public static final Bound INFINITY = new Bound(false, true);

    /**
     * Bound representing an undefined value.
     */
    public static final Bound UNDEFINED = new Bound();

    /**
     * Check if a bound has a finite value.
     * 
     * @return whether the bound is finite or not.
     */
    public boolean isFinite() {return this.value != null;}

    /**
     * Retrieve the bound value.
     * 
     * @throws IllegalStateException if the bound if non-finite
     * @return the bound value
     */
    public int getValue() {
        if (this.value == null) {
            throw new IllegalStateException("Cannot retrieve infinite value");
        } else {
            return this.value;
        }
    }

    /**
     * Add two bounds together. If both bounds are infinite but have opposing signs,
     * then the result is assumed to be positive infinity.
     * 
     * @param a
     * @param b
     * @return the newly computed bound
     */
    public static Bound add(Bound a, Bound b) {
        if (a.isFinite() && b.isFinite()) {
            return new Bound(a.getValue() + b.getValue());
        }
        if (a.isFinite()) {
            return new Bound(b);
        }
        if (b.isFinite()) {
            return new Bound(a);
        }
        if (a.plus_inf || b.plus_inf) {
            return INFINITY;
        }
        if (a.undef && b.undef) {
            return UNDEFINED;
        }
        return MINUS_INFINITY;
    }

    /**
     * Add another bound to this.
     * 
     * @param other the other bound
     * @return the newly computed bound
     */
    public Bound add(Bound other) {
        return add(this, other);
    }

    public static Bound min(Bound a, Bound b) {
        if (a.compareTo(b) < 0) {
            return a;
        } else {
            return b;
        }
    }

    public static Bound max(Bound a, Bound b) {
        if (a.compareTo(b) > 0) {
            return a;
        } else {
            return b;
        }
    }

    @Override
    public int compareTo(Bound other) {
        if (this.plus_inf) {
            return other.plus_inf ? 0 : 1;
        }
        if (this.minus_inf) {
            return other.minus_inf ? 0 : -1;
        }
        if (other.plus_inf) {
            return -1;
        }
        if (other.minus_inf) {
            return 1;
        }
        if (this.undef || other.undef) {
            return 0;
        }
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        if (this.minus_inf) {
            return "-inf";
        }
        if (this.plus_inf) {
            return "inf";
        }
        if (this.undef) {
            return "undefined";
        }
        return this.value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Bound)) {
            return false;
        }
        Bound other_bound = (Bound) other;
        if (this.minus_inf != other_bound.minus_inf) {
            return false;
        }
        if (this.plus_inf != other_bound.plus_inf) {
            return false;
        }
        if (this.undef != other_bound.undef) {
            return false;
        }
        if (!this.value.equals(other_bound.value)) {
            return false;
        }
        return true;
    }
}
