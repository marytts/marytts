package marytts.features.contextprocessor;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.features.ContextProcessor;

/**
 * Context processor to get the 2nd previous item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class PreviousPrevious implements ContextProcessor {

    /**
     * Return the 2nd previous item of the given item in the sequence
     *
     * @param utt the utterance
     * @param item
     *            the given item
     * @return the 2nd previous item of the given item or null if there is no
     *         such things.
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
        if (idx <= 1) {
            return null;
        }

        return seq.get(idx - 2);
    }
}
