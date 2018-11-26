package marytts.features.levelprocessor;

import marytts.MaryException;

import java.util.ArrayList;

import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.Item;

import marytts.features.LevelProcessor;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class PhoneNSS implements LevelProcessor {
    public ArrayList<? extends Item> get(Utterance utt, Item item) throws MaryException {
        if (item instanceof marytts.data.item.phonology.Phoneme) {
            ArrayList<Item> list_items = new ArrayList<Item>();
            list_items.add(item);
            return list_items;
        }


        if (item instanceof marytts.data.item.phonology.NSS) {
            ArrayList<Item> list_items = new ArrayList<Item>();
            list_items.add(item);
            return list_items;
        }

        Sequence<? extends Item> seq = item.getSequence();
        int item_idx = seq.indexOf(item);

        // Try with phone
        Relation relation = utt.getRelation(seq, utt.getSequence(SupportedSequenceType.PHONE));
        ArrayList<? extends Item> ret_list = relation.getRelatedItems(item_idx);

        // If nothing try with NSS
        if (ret_list.size() == 0) {
            relation = utt.getRelation(seq, utt.getSequence(SupportedSequenceType.NSS));
            ret_list = relation.getRelatedItems(item_idx);
        }

        // System.out.println(relation.getRelatedItems(item_idx));
        return ret_list;
    }
}
