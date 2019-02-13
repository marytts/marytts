package marytts.data.item.phonology;

import marytts.data.item.Item;

/**
 * Class which represents a phoneme
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phoneme extends Item {

    /** Stress information of the phonem */
    private String m_stress;

    /** The label of the phoneme. */
    private String m_label;

    /**
     * Instantiates a new phoneme.
     *
     * @param label
     *            the label
     */
    public Phoneme(String label) {
        setLabel(label);
        setStress(null);
    }

    /**
     * Instantiates a new phoneme.
     *
     * @param label
     *            the label
     */
    public Phoneme(String label, String stress) {
        setLabel(label);
        setStress(stress);
    }

    /**
     * Instantiates a new phoneme by copying a phoneme
     *
     * @param phoneme
     *            the phoneme to copy
     */
    public Phoneme(Phoneme phoneme) {
        setLabel(phoneme.getLabel());
        setStress(phoneme.getStress());
    }

    /**
     * Gets the stress.
     *
     * @return the stress
     */
    public String getStress() {
        return m_stress;
    }

    /**
     * Sets the stress.
     *
     * @param stress
     *            the new stress
     */
    public void setStress(String stress) {
        m_stress = stress;
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
        if ((getStress() == null) || (getStress().isEmpty()))
            return getLabel();
        else
            return String.format("%s (%s)", getLabel(), getStress());
    }
}
