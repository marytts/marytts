package marytts.data.utils;

import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;

/**
 * Encapsulation of a pair of sequence type for hashing purpose
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class SequenceTypePair {
    /** the left member of the pair */
    private SupportedSequenceType left;

    /** the right member of the pair */
    private SupportedSequenceType right;

    /**
     * Constructor
     *
     * @param left
     *            the left member to set
     * @param right
     *            the right member to set
     */
    public SequenceTypePair(SupportedSequenceType left, SupportedSequenceType right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Get the left member of the pair
     *
     * @return the left member of the pair
     */

    public SupportedSequenceType getLeft() {
        return left;
    }

    /**
     * Get the right member of the pair
     *
     * @return the right member of the pair
     */
    public SupportedSequenceType getRight() {
        return right;
    }

    /**************************************************************************************
     ** Object overriding part
     **************************************************************************************/
    /**
     * Method to compare the current pair of sequence type to a given one
     *
     * @param obj
     *            the other pair of sequence type
     * @return true if obj is a pair of sequence type equals to the current one
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        return ((getLeft() == ((SequenceTypePair) obj).getLeft())
                && (getRight() == (((SequenceTypePair) obj).getRight())));
    }

    /**
     * Returns a suitable hash code. The hash code follows the definition in
     * {@code Map.Entry}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        // see Map.Entry API specification
        return (getLeft() == null ? 0 : getLeft().hashCode()) ^ (getRight() == null ? 0 :
                getRight().hashCode());
    }

    /**
     * Method to generate a string representation of the current pair
     *
     * @return the string representation of the current pair
     */
    @Override
    public String toString() {
        return "(" + getLeft().toString() + ", " + getRight().toString() + ")";
    }
}
