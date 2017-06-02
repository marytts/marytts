package marytts.features.featureprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;

import marytts.data.item.linguistic.Word;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class NbToWordEnd implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws Exception {
        if (item instanceof Word) {
            throw new Exception();
        }

        Sequence<Item> seq_item = (Sequence<Item>) item.getSequence();
        Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.WORD));
        int item_idx = seq_item.indexOf(item);

        // Find the related wrdase
        int[] wrd_indexes = rel.getRelatedIndexes(item_idx);
        if (wrd_indexes.length <= 0) {
            return Feature.UNDEF_FEATURE;
        }

        // Finding the itemlables related to the related wrdase
        int[] item_indexes = rel.getSourceRelatedIndexes(wrd_indexes[0]);
        if (item_indexes.length <= 0) {
            return Feature.UNDEF_FEATURE;
        }

        int nb = item_indexes[item_indexes.length - 1] - item_idx;
        return new Feature(nb);
    }
}
