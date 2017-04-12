package marytts.features.featureprocessor;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.phonology.Syllable;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class StressLevel implements FeatureProcessor
{
    public Feature generate(Utterance utt, Item item) throws Exception
    {
        if (item instanceof marytts.data.item.phonology.Syllable)
        {
            Syllable syl = (Syllable) item;
            return new Feature(syl.getStressLevel());
        }

        throw new Exception();
    }
}
