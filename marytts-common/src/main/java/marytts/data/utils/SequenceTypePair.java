package marytts.data.utils;

import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class SequenceTypePair
{
    private SupportedSequenceType left; //left member of pair
    private SupportedSequenceType right; //right member of pair

    public SequenceTypePair(SupportedSequenceType left,
                            SupportedSequenceType right)
    {
        this.left = left;
        this.right = right;
    }

    public SupportedSequenceType getLeft() {
        return left;
    }

    public SupportedSequenceType getRight() {
        return right;
    }

    /**************************************************************************************
     ** Object overriding part
     **************************************************************************************/
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        return ((getLeft() == ((SequenceTypePair) obj).getLeft()) &&
                (getRight() == (((SequenceTypePair) obj).getRight())));
    }

    /**
     * <p>Returns a suitable hash code.
     * The hash code follows the definition in {@code Map.Entry}.</p>
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        // see Map.Entry API specification
        return (getLeft() == null ? 0 : getLeft().hashCode()) ^
            (getRight() == null ? 0 : getRight().hashCode());
    }

    @Override
    public String toString()
    {
        return "(" + getLeft().toString() + ", " + getRight().toString() + ")";
    }
}
