package marytts.data.item.phonology;

import marytts.data.item.Item;

/**
 * Class which represents a non speech sound
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class NSS extends Item {

    /** The label of the nss. */
    private String m_label;

    /**
     * Instantiates a new nss.
     *
     * @param label
     *            the label
     */
    public NSS(String label) {
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

    /**
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getLabel();
    }
}
