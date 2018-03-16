package marytts.features.featureprocessor;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class StringFeature implements FeatureProcessor {
    public Feature generate(Utterance utt, Item item) throws MaryException {
        return new Feature(item.toString());
    }
}
