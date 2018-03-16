package marytts.features.featureprocessor;

import marytts.MaryException;

import java.util.Hashtable;

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
public class NbPhrasesRelated implements FeatureProcessor {
    protected Hashtable<Item, Feature> cache;

    public NbPhrasesRelated() {
        cache = new Hashtable<Item, Feature>();
    }

    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof Phrase) {
            throw new MaryException("The item is not a phrase");
        }

        if (cache.containsKey(item)) {
            return cache.get(item);
        }

        Sequence<Item> seq_item = (Sequence<Item>) item.getSequence();
        Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.PHRASE));
        int item_idx = seq_item.indexOf(item);

        // Find the related phrase indexes
        int[] phr_indexes = rel.getRelatedIndexes(item_idx);
        Feature tmp = new Feature(phr_indexes.length);

        // Save in the cache
        cache.put(item, tmp);
        return tmp;
    }
}
