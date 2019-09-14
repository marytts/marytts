package marytts.features.feature.processor.syllable;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.phonology.Syllable;

import marytts.features.Feature;
import marytts.features.feature.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class IsAccented implements FeatureProcessor {
    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof marytts.data.item.phonology.Syllable) {
            Syllable syl = (Syllable) item;
	    return new Feature(syl.getAccent() != null);
        }

        throw new MaryException("The item is not a syllable");
    }
}
