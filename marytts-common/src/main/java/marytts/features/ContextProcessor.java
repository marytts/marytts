package marytts.features;

import marytts.data.Utterance;
import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public interface ContextProcessor
{
    public Item generate(Utterance utt, Item item) throws Exception;
}
