package marytts.features.contextprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;

import marytts.features.ContextProcessor;

/**
 * Context processor to get the current item. This class is here to be
 * consistent with the whole feature processing architecture.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Current implements ContextProcessor {

    /**
     * Return the given item
     *
     * @param item
     *            the returned item
     * @return the item given in parameter
     * @throws Exception
     *             not throwed actually
     */
    public Item get(Item item) throws Exception {
        return item;
    }
}
