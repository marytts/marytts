package marytts.features.contextprocessor;

import marytts.MaryException;
import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.features.ContextProcessor;

/**
 * Context processor to get the previous item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Previous implements ContextProcessor {
    /**
     * Return the previous item of the given item in the sequence
     *
     * @param utt the utterance
     * @param item
     *            the given item
     * @return the previous item of the given item or null if there is no such
     *         things.
     * @throws Exception
     *             (actually NotInSequenceException) if the given item is not in
     *             a sequence
     */
    public Item get(Utterance utt, Item item) throws MaryException {
        Sequence<? extends Item> seq = item.getSequence();

        // FIXME: Should be replace by a "notinsequence" exception
        if (seq == null) {
            throw new MaryException("The item is not in a sequence");
        }

        int idx = seq.indexOf(item);
        if (idx <= 0) {
            return null;
        }

        return seq.get(idx - 1);
    }
}
