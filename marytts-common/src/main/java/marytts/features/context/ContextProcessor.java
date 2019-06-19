package marytts.features.context;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;

/**
 * Interface of a context processor. A context processor is meant to find an
 * item in the same sequence the given one.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public interface ContextProcessor {
    /**
     * Get the item from a source item in the implenting context (see the
     * description of the implementing class)
     *
     * @param utt the utterance
     * @param item
     *            the source item
     * @return the item corresponding to the context of the source item
     * @throws Exception
     *             if something is going wrong
     */
    public Item get(Utterance utt, Item item) throws MaryException;
}
