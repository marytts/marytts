package marytts.data.item.phonology;

/**
 * Class which repreesents any kind of accent
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Accent {
    /** The label of the accent */
    private String m_label;

    /**
     * Instantiates a new accent. The label is mandatory
     *
     * @param label
     *            the label of the accent
     */
    public Accent(String label) {
        setLabel(label);
    }

    /**
     * Gets the label of the accent.
     *
     * @return the label of the accent
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * Sets the label of the accent.
     *
     * @param label
     *            the new label of
     */
    protected void setLabel(String label) {
        m_label = label;
    }

    /**
     * Method to compare an object to the current accent
     *
     * @param obj
     *            the object to compare
     * @return true if obj is a accent and the accent are equals, false else
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Accent)) {
            return false;
        }

        return getLabel().equals(((Accent) obj).getLabel());
    }
}
