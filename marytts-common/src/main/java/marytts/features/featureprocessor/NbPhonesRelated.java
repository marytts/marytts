package marytts.features.featureprocessor;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;

import marytts.data.item.phonology.Phoneme;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class NbPhonesRelated implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws MaryException {
        Sequence<Item> seq_item = (Sequence<Item>) item.getSequence();
        Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.PHONE));
        int item_idx = seq_item.indexOf(item);

        // Find the related sylase
        int[] ph_indexes = rel.getRelatedIndexes(item_idx);
        return new Feature(ph_indexes.length);
    }
}
