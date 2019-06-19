package marytts.features.contextprocessor;

import java.util.ArrayList;

import marytts.MaryException;
import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.features.ContextProcessor;
import marytts.features.levelprocessor.PhoneNSS;

/**
 * Context processor to get the next item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class SegmentPreviousPrevious implements ContextProcessor {

    /**
     * Return the next item of the given item in the sequence
     *
     * @param utt the utterance
     * @param item
     *            the given item
     * @return the next item of the given item or null if there is no such
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

        // Got back to the segment level
        Sequence<? extends Item> seq_seg = utt.getSequence(SupportedSequenceType.SEGMENT);
        Relation rel = utt.getRelation(seq, seq_seg);

        int[] ret_list = rel.getRelatedIndexes(idx);
        if (ret_list.length == 0)
            return null;

        if ((ret_list[0] - 2) < 0)
            return null;

        Item seg = seq_seg.get(ret_list[0]-2);
        ArrayList<? extends Item> final_items = (new PhoneNSS()).get(utt, seg);

        if (final_items.size() == 0)
            return null;

        return final_items.get(0);
    }
}
