package marytts.features.featureprocessor;

import java.util.ArrayList;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.item.Item;
import marytts.data.item.phonology.Syllable;
import marytts.data.item.phonology.Phoneme;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

import marytts.phonetic.IPA;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class SyllableVowel implements FeatureProcessor {
    public Feature generate(Utterance utt, Item item) throws MaryException {

        if (item instanceof marytts.data.item.phonology.Syllable) {
            Relation rel = utt.getRelation(item.getSequence(), utt.getSequence(SupportedSequenceType.PHONE));
	    int idx = item.getSequence().indexOf(item);
	    ArrayList<Phoneme> phonemes = (ArrayList<Phoneme>)rel.getRelatedItems(idx);
	    String vowel = "";
	    for (Phoneme ph: phonemes) {
            if (IPA.ipa_cat_map.get(ph.getLabel().charAt(0)) == null)
                throw new MaryException("the following characters is not ipa == " +
                        ph.getLabel().charAt(0) + " ==");
		if (IPA.ipa_cat_map.get(ph.getLabel().charAt(0)).contains("vowel")) {
		    vowel = ph.getLabel();
		    break;
		}

	    }
            return new Feature(vowel);
        }

        throw new MaryException("The item is not a syllable");
    }
}
