package marytts.features.featureprocessor;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.phonology.Syllable;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

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
            if (syl.getAccent() == null) {
                return new Feature(Boolean.FALSE);
            } else {
                return new Feature(Boolean.TRUE);
            }
        }

        throw new MaryException("The item is not a syllable");
    }
}
