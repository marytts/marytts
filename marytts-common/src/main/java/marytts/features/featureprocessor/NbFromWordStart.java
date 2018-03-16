package marytts.features.featureprocessor;

import marytts.MaryException;
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
public class NbFromWordStart implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof Word) {
            throw new MaryException("The item is not a word");
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

        int nb = item_idx - item_indexes[0] + 1;
        return new Feature(nb);
    }
}
