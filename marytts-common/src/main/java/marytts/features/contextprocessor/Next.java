package marytts.features.contextprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.features.ContextProcessor;

/**
 * Context processor to get the next item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Next implements ContextProcessor {

    /**
     * Return the next item of the given item in the sequence
     *
     * @param item the given item
     * @return the next item of the given item or null if there is no such things.
     * @throws Exception (actually NotInSequenceException) if the given item is not in a sequence
     */
	public Item get(Item item) throws Exception {
		Sequence<? extends Item> seq = item.getSequence();

        // FIXME: Should be replace by a "notinsequence" exception
		if (seq == null)
			throw new Exception();

		int idx = seq.indexOf(item);
		if (idx >= (seq.size() - 1))
			return null;

		return seq.get(idx + 1);
	}
}
