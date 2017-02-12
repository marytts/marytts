package marytts.features.contextprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;

import marytts.features.ContextProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Current implements ContextProcessor
{
    public Item generate(Utterance utt, Item item)
        throws Exception
    {
        return item;
    }
}
