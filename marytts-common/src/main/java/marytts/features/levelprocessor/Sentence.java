package marytts.features.levelprocessor;

import java.util.ArrayList;

import marytts.data.Utterance;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;

import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

import marytts.features.LevelProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Sentence implements LevelProcessor {
    public ArrayList<? extends Item> get(Utterance utt, Item item) throws Exception {
        if (item instanceof marytts.data.item.linguistic.Sentence) {
            ArrayList<Item> list_items = new ArrayList<Item>();
            list_items.add(item);
            return list_items;
        }

        Sequence<? extends Item> seq = item.getSequence();
        Relation relation = utt.getRelation(seq, utt.getSequence(SupportedSequenceType.SENTENCE));

        int item_idx = seq.indexOf(item);
        return relation.getRelatedItems(item_idx);
    }
}
