package marytts.features.featureprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;

import marytts.data.item.prosody.Phrase;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class NbToPhraseEnd implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws Exception {
        if (!(item instanceof Phrase)) {

            Sequence<? extends Item> seq_item = item.getSequence();
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

            int nb = item_indexes[item_indexes.length - 1] - item_idx + 1;

            return new Feature(nb);
        }

        throw new Exception();
    }
}
