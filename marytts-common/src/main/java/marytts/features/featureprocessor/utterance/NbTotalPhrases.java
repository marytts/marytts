package marytts.features.featureprocessor.utterance;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.item.Item;
import marytts.data.SupportedSequenceType;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;


/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class NbTotalPhrases implements FeatureProcessor {

    public Feature generate(Utterance utt, Item item) throws MaryException {
        Sequence<? extends Item> seq = utt.getSequence(SupportedSequenceType.PHRASE);

        return new Feature(seq.size());
    }
}
