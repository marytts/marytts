package marytts.features.featureprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Relation;
import marytts.data.item.linguistic.Word;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class InDirectSpeech implements FeatureProcessor {
    public Feature generate(Utterance utt, Item item) throws Exception {
        // Initialize everything
        Sequence<Item> seq_item = (Sequence<Item>) item.getSequence();
        Sequence<Word> seq_wrd = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);
        Relation rel = utt.getRelation(seq_item, seq_wrd);

        // Find item id
        int item_idx = seq_item.indexOf(item);

        // Find the related word indexes
        int[] wrd_indexes = rel.getRelatedIndexes(item_idx);
        int idx_wrd = wrd_indexes[0] - 1;
        boolean open_found = false;

        // Search for the
        while ((idx_wrd >= 0) && (!open_found)) {
            Word wrd = seq_wrd.get(idx_wrd);
            String pos = ((Word) wrd).getPOS();
            if (pos.equals("``")) {
                open_found = true;
            } else if (pos.equals("''")) {
                break;
            }

            idx_wrd--;
        }

        if (open_found) {
            return new Feature(true);
        }

        return new Feature(false);
    }
}
