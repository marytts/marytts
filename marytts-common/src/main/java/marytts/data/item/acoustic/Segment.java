package marytts.data.item.acoustic;

import marytts.data.item.Item;

/**
 * A segment representation. A segment is identified by a starting position and qualified by a
 * duration.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Segment extends Item {
    /** Constant to indicate that the segment is starting stricktly when the previous one ends */
    public static final double NO_START = -1.0;

    /** Constant for the default duration */
    public static final double DEFAULT_DURATION = 1.0;

    /** Start position of the segment in second */
    private double m_start;

    /** Duration of the segment in second */
    private double m_duration;


    /**
     * Instantiates a new segment initialized with default values.
     *
     */
    public Segment() {
        super();
        setStart(NO_START);
        setDuration(DEFAULT_DURATION);
    }

    /**
     * Instantiates a new segment.
     *
     * @param start
     *            the start timestamp
     * @param duration
     *            the duration of the segmente
     */
    public Segment(double start, double duration) {
        super();
        setStart(start);
        setDuration(duration);
    }

    /**
     * Gets the start timestamp of the segment
     *
     * @return the start timestamp of the segment
     */
    public double getStart() {
        return m_start;
    }

    /**
     * Sets the start timestamp of the segment
     *
     * @param start
     *            the new start timestamp of the segment
     */
    public void setStart(double start) {
        m_start = start;
    }

    /**
     * Gets the duration of the segment
     *
     * @return the duration of the segment
     */
    public double getDuration() {
        return m_duration;
    }

    /**
     * Sets the duration of the segment
     *
     * @param duration
     *            the new duration of the segment
     */
    protected void setDuration(double duration) {
        m_duration = duration;
    }

    /**
     *  Equality test method.
     *
     *  @param obj the object to compare to the current one
     *  @return true if obj is a Segment and if obj.getStart() == this.getStart()
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Segment))
            return false;

        return ((Segment) obj).getStart() == getStart();
    }

    /**
     *  Method to get the string representation of the segment
     *
     *  @return String.format("Segment (%d, %d)", getStart(), getDuration());
     */
    @Override
    public String toString() {
        return String.format("Segment (%f, %f)", getStart(), getDuration());
    }
}
