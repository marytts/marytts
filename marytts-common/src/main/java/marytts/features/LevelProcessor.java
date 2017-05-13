package marytts.features;

import marytts.data.Utterance;
import marytts.data.item.Item;
import java.util.ArrayList;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public interface LevelProcessor {
	public ArrayList<? extends Item> generate(Utterance utt, Item item) throws Exception;
}
