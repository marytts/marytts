package marytts.features.feature.processor;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

import marytts.features.Feature;
import marytts.features.feature.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class TextFeature implements FeatureProcessor {
    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof marytts.data.item.linguistic.Word) {
            return new Feature(((Word) item).getText());
        }

        throw new MaryException("The item is not a word");
    }
}
