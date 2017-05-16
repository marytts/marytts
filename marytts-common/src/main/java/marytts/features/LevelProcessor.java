package marytts.features;

import marytts.data.Utterance;
import marytts.data.item.Item;
import java.util.ArrayList;

/**
 * Interface of a level processor. A level processor is meant to find a list of
 * items in a different sequence than the source one (vertical context).
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public interface LevelProcessor {
	/**
	 * Get a list of potential items related to the source item (see the
	 * description of the implementing class)
	 *
	 * @param utt
	 *            the utterance which contains the sequences and the relations
	 *            needed to find the corresponding items
	 * @param item
	 *            the source item
	 * @return the list of potential corresponding items
	 * @throws Exception
	 *             if something is going wrong
	 */
	public ArrayList<? extends Item> get(Utterance utt, Item item) throws Exception;
}
