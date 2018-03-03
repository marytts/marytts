package marytts.features.featureprocessor;

import marytts.MaryException;

import java.util.Hashtable;
import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

import marytts.phonetic.AlphabetFactory;
import marytts.phonetic.converter.Alphabet;
import marytts.MaryException;
import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class ArpaLabel implements FeatureProcessor {

    Alphabet ipa2arp;
    public ArpaLabel() throws MaryException {
	ipa2arp = AlphabetFactory.getAlphabet("arpabet");
    }

    protected String convertPh(String ph) throws MaryException{
	if (ph.equals("sil") || ph.equals("_"))
	    return "pau";

        return ipa2arp.getLabelFromIPA(ph);
    }

    public Feature generate(Utterance utt, Item item) throws MaryException {
        return new Feature(convertPh(item.toString()));
    }
}
