package vct.parsers.transform.systemctocol.util;

/**
 * Class representing an integer interval abstract domain.
 */
public class Interval {
    private Bound lower;
    private Bound upper;

    /**
     * Create a new interval with different bounds.
     * 
     * @param lower the lower bound. if null then it is negative infinity.
     * @param upper the upper bound. if null then it is positive infinity.
     */
    public Interval(Integer lower, Integer upper) {
        this.setLower(lower);
        this.setUpper(upper);
    }

    /**
     * Create a new interval with the same bounds.
     * 
     * @param value the interval bound. if null then the resulting interval is [-inf, inf]
     */
    public Interval(Integer value)
    {
        this(value, value);
    }

    /**
     * Create a new interval with different bounds.
     * 
     * @param lower the lower bound. if null then it is negative infinity.
     * @param upper the upper bound. if null then it is positive infinity.
     */
    public Interval(Bound lower, Bound upper) {
        this.setLower(lower);
        this.setUpper(upper);
    }

    /**
     * Create a new interval with the same bounds.
     * 
     * @param value the interval bound. if null then the resulting interval is [-inf, inf]
     */
    public Interval(Bound value) {
        this(value, value);
    }

    /**
     * Build an empty interval.
     */
    private Interval() {
        this.lower = null;
        this.upper = null;
    }

    /**
     * Copy constructor.
     */
    public Interval(Interval other) {
        this.lower = other.lower;
        this.upper = other.upper;
    }

    /**
     * Empty interval.
     */
    public static final Interval EMPTY = new Interval();

    /**
     * Check if the interval is lower bounded.
     */
    public boolean isLowerBounded() {
        return this.lower.isFinite();
    }

    /**
     * Check if the interval is upper bounded.
     */
    public boolean isUpperBounded() {
        return this.upper.isFinite();
    }

    /**
     * Check if the interval is bounded (i.e. both lower and upper bounded).
     */
    public boolean isBounded() {
        return this.isLowerBounded() && this.isUpperBounded();
    }

    /**
     * Add a value to both bounds of the interval.
     * 
     * @param value the value to be added
     * @return a newly constructed interval with the calculated bounds
     */
    public Interval add(Bound value) {
        return new Interval(
            this.lower.add(value),
            this.upper.add(value)
        );
    }

    /**
     * Add a value to both bounds of the interval.
     * 
     * @param value the value to be added
     * @return a newly constructed interval with the calculated bounds
     */
    public Interval add(Integer value) {
        return new Interval(
            this.lower.add(new Bound(value)),
            this.upper.add(new Bound(value))
        );
    }

    /**
     * Join two intervals. The result is the smallest interval that contains both.
     * 
     * @param a the first interval
     * @param b the second interval
     * @return the resulting interval
     */
    public static Interval join(Interval a, Interval b) {
        return new Interval(
            Bound.min(a.lower, b.lower),
            Bound.max(a.upper, b.upper)
        );
    }

    /**
     * Join an interval to this one. The result is the smallest interval that contains both.
     * 
     * @param other the other interval
     * @return the resulting interval
     */
    public Interval join(Interval other) {
        return join(this, other);
    }

    /**
     * Widen two intervals together. If the second interval is wider than the first,
     * then the result has an infinite bound on that side of the interval.
     * 
     * @param a the first interval
     * @param b the second interval
     * @return the resulting interval
     */
    public static Interval widen(Interval a, Interval b) {
        Bound lower = b.lower.compareTo(a.lower) < 0 ? Bound.MINUS_INFINITY : a.lower;
        Bound upper = b.upper.compareTo(a.upper) > 0 ? Bound.INFINITY : a.upper;
        return new Interval(lower, upper);
    }

    /**
     * Widen two intervals together. If the second interval is wider than the first,
     * then the result has an infinite bound on that side of the interval.
     * 
     * @param other the other interval
     * @return the resulting interval
     */
    public Interval widen(Interval other) {
        return widen(this, other);
    }

    /**
     * Intersect two intervals.
     * 
     * @param a the first interval
     * @param b the second interval
     * @return the resulting interval
     */
    public static Interval intersect(Interval a, Interval b) {
        if (a.upper.compareTo(b.lower) < 0 || 
            b.upper.compareTo(a.lower) < 0) {
                return EMPTY;
        }
        return new Interval(Bound.max(a.lower, b.lower), Bound.min(a.upper, b.upper));
    }

    /**
     * Intersect an interval with this one
     * 
     * @param other the other interval
     * @return the resulting interval
     */
    public Interval intersect(Interval other) {
        return intersect(this, other);
    }

    public Bound getLower() {
        return this.lower;
    }

    public Bound getUpper() {
        return this.upper;
    }

    public Integer getLowerValue() {
        if (lower.isFinite()) {
            return lower.getValue();
        }
        return null;
    }

    public Integer getUpperValue() {
        if (upper.isFinite()) {
            return upper.getValue();
        }
        return null;
    }

    public void setLower(Integer lower) {
        if (lower != null) {
            this.lower = new Bound(lower);
        } else {
            this.lower = Bound.MINUS_INFINITY;
        }
    }

    public void setLower(Bound lower) {
        this.lower = lower;
    }

    public void setUpper(Integer upper) {
        if (upper != null) {
            this.upper = new Bound(upper);
        } else {
            this.upper = Bound.INFINITY;
        }
    }

    public void setUpper(Bound upper) {
        this.upper = upper;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Interval)) {
            return false;
        }
        Interval other_interval = (Interval) other;
        return this.lower.equals(other_interval.lower) && this.upper.equals(other_interval.upper);
    }

    @Override
    public String toString() {
        return "[" + this.lower.toString() + ", " + this.upper.toString() + "]";
    }
}