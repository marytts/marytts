package marytts.features.featureprocessor;

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
public class NbStressFromPhraseStart implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof Syllable) {

            Sequence<Syllable> seq_item = (Sequence<Syllable>) item.getSequence();
            Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.PHRASE));
            int item_idx = seq_item.indexOf(item);

            // Find the related phrase
            int[] phr_indexes = rel.getRelatedIndexes(item_idx);
            if (phr_indexes.length <= 0) {
                return Feature.UNDEF_FEATURE;
            }

            // Finding the items related to the related phrase
            int[] item_indexes = rel.getSourceRelatedIndexes(phr_indexes[0]);
            if (item_indexes.length <= 0) {
                return Feature.UNDEF_FEATURE;
            }

            int nb = 0;
            for (int i = item_indexes[0]; i < item_idx; i++) {
                Syllable cur_item = seq_item.get(i);
                if (cur_item.getStressLevel() > 0) {
                    nb++;
                }
            }

            return new Feature(nb);
        }

        throw new MaryException("The item is not a syllable");
    }
}
