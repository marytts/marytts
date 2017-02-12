package marytts.features.levelprocessor;

import java.util.ArrayList;

import marytts.data.Utterance;
import marytts.data.item.Item;

import marytts.features.LevelProcessor;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class CurrentLevel implements LevelProcessor
{
    public ArrayList<? extends Item> generate(Utterance utt, Item item)
        throws Exception
    {
        ArrayList<Item> list_items = new ArrayList<Item>();
        list_items.add(item);
        return list_items;
    }
}
