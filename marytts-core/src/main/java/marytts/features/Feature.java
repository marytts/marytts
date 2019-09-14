package marytts.features;

import marytts.data.item.Item;

/**
 * Class to model a feature. A feature is a specific item with a specific value.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Feature extends Item {
    /** The value of the feature */
    Object m_value;

    /**
     * Constructor
     *
     * @param value
     *            the value of the feature
     */
    public Feature(Object value) {
        m_value = value;
    }

    /**
     * Get the value of the feature
     *
     * @return the value of the feature
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Get the value of the feature in String format.
     *
     * @return an empty string if the value is not defined, value.toString()
     *         else
     */
    public String getStringValue() {
        if (m_value == null) {
            return "";
        }
        return m_value.toString();
    }

    /**
     * Method to compare if a given objet is a feature which is equal to the
     * current one
     *
     * @param o
     *            the given object
     * @return true if o is a feature whose value equals the value of the
     *         current feature.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Feature)) {
            return false;
        }

        return (getValue() == ((Feature) o).getValue());
    }

    @Override
    public String toString() {
        return getStringValue();
    }

    /** Constant to represent an undefined feature */
    public static final Feature UNDEF_FEATURE = new Feature(null);
}
