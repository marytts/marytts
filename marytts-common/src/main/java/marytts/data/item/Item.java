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
	protected Sequence<? extends Item> m_sequence_reference;

	protected Item() {
		m_sequence_reference = null;
	}

	public boolean setSequenceReference(Sequence<? extends Item> seq) {
		if (m_sequence_reference == null) {
			m_sequence_reference = seq;
			return true;
		} else {
			m_sequence_reference = seq;
			return false;
		}
	}

	public void unsetSequenceReference() {
		m_sequence_reference = null;
	}

	public boolean isInSequence(Sequence<? extends Item> seq) {
		return (m_sequence_reference == seq);
	}

	public Sequence<? extends Item> getSequence() {
		return m_sequence_reference;
	}
}
