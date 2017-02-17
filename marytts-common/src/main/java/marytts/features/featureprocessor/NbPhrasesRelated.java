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
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class NbPhrasesRelated implements FeatureProcessor
{

    public Feature generate(Utterance utt, Item item) throws Exception
    {
        if (item instanceof Phrase)
            throw new Exception();

        Sequence<Item> seq_item = (Sequence<Item>) item.getSequence();
        Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.PHRASE));
        int item_idx = seq_item.indexOf(item);

        // Find the related sylase
        int[] phr_indexes = rel.getRelatedIndexes(item_idx);
        return new Feature(phr_indexes.length);
    }
}
