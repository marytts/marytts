package marytts.data.item.phonology;

/**
 * A phone representation. A phone is a phoneme with a position in the signal
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Phone extends Phoneme {
    /** start position of the phone in second */
    private double m_start;

    /** duration of the phone in second */
    private double m_duration;

    /**
     * Instantiates a new phone.
     *
     * @param label
     *            the label of the phone
     * @param start
     *            the start timestamp
     * @param duration
     *            the duration of the phonee
     */
    public Phone(String label, double start, double duration) {
        super(label);
        setStart(start);
        setDuration(duration);
    }

    /**
     * Instantiates a new phone based on a known phoneme
     *
     * @param phoneme
     *            the phoneme which will get the label
     * @param start
     *            the start timestamp
     * @param duration
     *            the duration of the phonee
     */
    public Phone(Phoneme phoneme, double start, double duration) {
        super(phoneme);
        setStart(start);
        setDuration(duration);
    }

    /**
     * Instantiates a new phone. The start is considered as undefined.
     *
     * @param label
     *            the label
     * @param duration
     *            the duration
     */
    public Phone(String label, double duration) {
        super(label);
        setStart(-1);
        setDuration(duration);
    }

    /**
     * Instantiates a new phone based on a known phoneme. The start is
     * considered as undefined.
     *
     * @param phoneme
     *            the phoneme which will get the label
     * @param duration
     *            the duration
     */
    public Phone(Phoneme phoneme, double duration) {
        super(phoneme);
        setStart(-1);
        setDuration(duration);
    }

    /**
     * Gets the start timestamp of the phone
     *
     * @return the start timestamp of the phone
     */
    public double getStart() {
        return m_start;
    }

    /**
     * Sets the start timestamp of the phone
     *
     * @param start
     *            the new start timestamp of the phone
     */
    public void setStart(double start) {
        m_start = start;
    }

    /**
     * Gets the duration of the phone
     *
     * @return the duration of the phone
     */
    public double getDuration() {
        return m_duration;
    }

    /**
     * Sets the duration of the phone
     *
     * @param duration
     *            the new duration of the phone
     */
    protected void setDuration(double duration) {
        m_duration = duration;
    }

    /*
     * (non-Javadoc)
     *
     * @see marytts.data.item.phonology.Segment#toString()
     */
    @Override
    public String toString() {
        return getLabel() + " : " + getDuration();
    }
}
