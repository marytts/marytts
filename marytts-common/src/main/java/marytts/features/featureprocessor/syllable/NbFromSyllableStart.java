package marytts.features.featureprocessor.syllable;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;

import marytts.data.item.phonology.Syllable;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class NbFromSyllableStart implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof Syllable) {
            throw new MaryException("The item is not a syllable");
        }

        Sequence<Item> seq_item = (Sequence<Item>) item.getSequence();
        Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.SYLLABLE));
        int item_idx = seq_item.indexOf(item);

        // Find the related syllable
        int[] syl_indexes = rel.getRelatedIndexes(item_idx);
        if (syl_indexes.length <= 0) {
            return Feature.UNDEF_FEATURE;
        }

        // Finding the items related to the related syllable
        int[] item_indexes = rel.getSourceRelatedIndexes(syl_indexes[0]);
        if (item_indexes.length <= 0) {
            return Feature.UNDEF_FEATURE;
        }

        int nb = item_idx - item_indexes[0] + 1;
        return new Feature(nb);
    }
}
