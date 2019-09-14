package marytts.features.feature.processor.paragraph;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;

import marytts.data.item.linguistic.Paragraph;

import marytts.features.Feature;
import marytts.features.feature.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class NbToParagraphEnd implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof Paragraph) {
            throw new MaryException("The item is not a paragraph");
        }

        Sequence<Item> seq_item = (Sequence<Item>) item.getSequence();
        Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.PARAGRAPH));
        int item_idx = seq_item.indexOf(item);

        // Find the related sentase
        int[] sent_indexes = rel.getRelatedIndexes(item_idx);
        if (sent_indexes.length <= 0) {
            return Feature.UNDEF_FEATURE;
        }

        // Finding the itemlables related to the related sentase
        int[] item_indexes = rel.getSourceRelatedIndexes(sent_indexes[0]);
        if (item_indexes.length <= 0) {
            return Feature.UNDEF_FEATURE;
        }

        int nb = item_indexes[item_indexes.length - 1] - item_idx + 1;

        return new Feature(nb);
    }
}
