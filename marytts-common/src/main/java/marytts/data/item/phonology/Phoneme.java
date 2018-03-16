package marytts.data.item.phonology;

import marytts.data.item.Item;

/**
 * Class which represents a phoneme
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phoneme extends Segment {

    /** Stress information of the phonem */
    private String m_stress;

    /**
     * Instantiates a new phoneme.
     *
     * @param label
     *            the label
     */
    public Phoneme(String label) {
        super(label);
        setStress(null);
    }

    /**
     * Instantiates a new phoneme by copying a phoneme
     *
     * @param phoneme
     *            the phoneme to copy
     */
    public Phoneme(Phoneme phoneme) {
        super(phoneme.getLabel());
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
}
