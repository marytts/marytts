package marytts.features.contextprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.features.ContextProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Previous implements ContextProcessor {
	public Item get(Item item) throws Exception {
		Sequence<? extends Item> seq = item.getSequence();
		if (seq == null)
			throw new Exception(); // FIXME: Should be replace by a
									// "notinsequence" exception

		int idx = seq.indexOf(item);
		if (idx <= 0)
			return null;

		return seq.get(idx - 1);
	}
}
