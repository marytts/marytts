package marytts.data.item;

import java.util.HashSet;
import marytts.data.Sequence;

/**
 * Abstract class representing an item.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public abstract class Item {
    /** The sequence which is containing the item */
	protected Sequence<? extends Item> m_sequence_reference;

    /**
     * Default constructor to set to null the sequence reference. Protected as it is an abstract
     * class
     *
     */
	protected Item() {
		m_sequence_reference = null;
	}

    /**
     * Method to set the sequence which contains the item
     *
     * @param seq the sequence which contains the item
     * @return true if the reference was null before
     */
	public boolean setSequenceReference(Sequence<? extends Item> seq) {
		if (m_sequence_reference == null) {
			m_sequence_reference = seq;
			return true;
		} else {
			m_sequence_reference = seq;
			return false;
		}
	}

    /**
     * Unset the sequence. The item is therefore assumed to be orphan.
     *
     */
	public void unsetSequenceReference() {
		m_sequence_reference = null;
	}

    /**
     * Check if the current item is in the given sequence
     *
     * @param seq the given sequence
     * @return true if the current item is in the given sequence
     */
	public boolean isInSequence(Sequence<? extends Item> seq) {
		return (m_sequence_reference == seq);
	}

    /**
     * Get the current item sequence
     *
     * @return the item sequence
     */
	public Sequence<? extends Item> getSequence() {
		return m_sequence_reference;
	}
}
