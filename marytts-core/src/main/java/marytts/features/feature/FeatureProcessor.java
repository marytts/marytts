package marytts.features.feature;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.features.Feature;

/**
 * From the given item generate the corresponding feature
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public interface FeatureProcessor {
    /**
     * From the given item generate the corresponding feature
     *
     * @param utt
     *            the utterance which contains everything to compute the feature
     * @param item
     *            the item which is the source of the feature
     * @return the computed feature
     * @throws Exception
     *             if something is going wrong
     */
    public Feature generate(Utterance utt, Item item) throws MaryException;
}
