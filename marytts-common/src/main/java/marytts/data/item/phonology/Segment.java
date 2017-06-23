package marytts.data.item.phonology;

import marytts.data.item.Item;

/**
 * Class which represents a segment
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Segment extends Item {

    /** The label of the segment. */
    private String m_label;

    /**
     * Instantiates a new segment.
     *
     * @param label
     *            the label of the segment
     */
    public Segment(String label) {
        super();
        setLabel(label);
    }

    /**
     * Gets the label of the segment.
     *
     * @return the label of the segment
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * Sets the label of the segment.
     *
     * @param label
     *            the new label of the segment
     */
    protected void setLabel(String label) {
        m_label = label;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getLabel();
    }
}
