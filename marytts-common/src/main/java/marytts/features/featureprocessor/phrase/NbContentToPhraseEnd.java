package marytts.features.featureprocessor.phrase;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;
import marytts.data.item.Item;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.item.linguistic.Word;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

import java.util.Hashtable;

// CC Coordinating conjunction
// CD Cardinal number
// DT Determiner
// EX Existential there
// FW Foreign word
// IN Preposition or subordinating conjunction
// JJ Adjective
// JJR Adjective, comparative
// JJS Adjective, superlative
// LS List item marker
// MD Modal
// NN Noun, singular or mass
// NNS Noun, plural
// NNP Proper noun, singular
// NNPS Proper noun, plural
// PDT Predeterminer
// POS Possessive ending
// PRP Personal pronoun
// PRP$ Possessive pronoun
// RB Adverb
// RBR Adverb, comparative
// RBS Adverb, superlative
// RP Particle
// SYM Symbol
// TO to
// UH Interjection
// VB Verb, base form
// VBD Verb, past tense
// VBG Verb, gerund or present participle
// VBN Verb, past participle
// VBP Verb, non­3rd person singular present
// VBZ Verb, 3rd person singular present
// WDT Wh­determiner
// WP Wh­pronoun
// WP$ Possessive wh­pronoun
// WRB Wh­adverb

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class NbContentToPhraseEnd implements FeatureProcessor {

    /** the POS tag conversion */
    protected final static Hashtable<String, String> pos_converter;

    public NbContentToPhraseEnd() {
    }

    static {
        pos_converter = new Hashtable<String, String>();

        // aux
        pos_converter.put("is", "aux");
        pos_converter.put("am", "aux");
        pos_converter.put("are", "aux");
        pos_converter.put("was", "aux");
        pos_converter.put("were", "aux");
        pos_converter.put("has", "aux");
        pos_converter.put("have", "aux");
        pos_converter.put("had", "aux");
        pos_converter.put("be", "aux");

        // cc
        pos_converter.put("and", "cc");
        pos_converter.put("but", "cc");
        pos_converter.put("or", "cc");
        pos_converter.put("plus", "cc");
        pos_converter.put("yet", "cc");
        pos_converter.put("nor", "cc");

        // det
        pos_converter.put("the", "det");
        pos_converter.put("a", "det");
        pos_converter.put("an", "det");
        pos_converter.put("no", "det");
        pos_converter.put("some", "det");
        pos_converter.put("this", "det");
        pos_converter.put("that", "det");
        pos_converter.put("each", "det");
        pos_converter.put("another", "det");
        pos_converter.put("those", "det");
        pos_converter.put("every", "det");
        pos_converter.put("all", "det");
        pos_converter.put("any", "det");
        pos_converter.put("these", "det");
        pos_converter.put("both", "det");
        pos_converter.put("neither", "det");
        pos_converter.put("no", "det");
        pos_converter.put("many", "det");

        // in
        pos_converter.put("in", "in");

        // md
        pos_converter.put("will", "md");
        pos_converter.put("may", "md");
        pos_converter.put("would", "md");
        pos_converter.put("can", "md");
        pos_converter.put("could", "md");
        pos_converter.put("must", "md");
        pos_converter.put("ought", "md");
        pos_converter.put("might", "md");

        // pps
        pos_converter.put("her", "pps");
        pos_converter.put("his", "pps");
        pos_converter.put("their", "pps");
        pos_converter.put("its", "pps");
        pos_converter.put("our", "pps");
        pos_converter.put("their", "pps");
        pos_converter.put("mine", "pps");

        // to
        pos_converter.put("to", "to");

        // wp
        pos_converter.put("who", "wp");
        pos_converter.put("what", "wp");
        pos_converter.put("where", "wp");
        pos_converter.put("when", "wp");
        pos_converter.put("how", "wp");

        // punc
        pos_converter.put(".", "punc");
        pos_converter.put(",", "punc");
        pos_converter.put(":", "punc");
        pos_converter.put(";", "punc");
        pos_converter.put("\"", "punc");
        pos_converter.put("'", "punc");
        pos_converter.put("(", "punc");
        pos_converter.put("?", "punc");
        pos_converter.put(")", "punc");
        pos_converter.put("!", "punc");
    }


    /**
     * The POS tag to convert.
     *
     * @param pos
     *            the original POS tag
     * @return the converted POS tag
     */
    protected String convertPOS(String pos) {
        String fest_pos = pos_converter.get(pos);
        if (fest_pos != null) {
            return fest_pos;
        }

        return "content";
    }

    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof marytts.data.item.linguistic.Word) {


            Sequence<Word> seq_item = (Sequence<Word>) item.getSequence();
            Relation rel = utt.getRelation(seq_item, utt.getSequence(SupportedSequenceType.PHRASE));
            int item_idx = seq_item.indexOf(item);

            // Find the related phrase
            int[] phr_indexes = rel.getRelatedIndexes(item_idx);
            if (phr_indexes.length <= 0) {
                return Feature.UNDEF_FEATURE;
            }

            // Finding the items related to the related phrase
            int[] item_indexes = rel.getSourceRelatedIndexes(phr_indexes[0]);
            if (item_indexes.length <= 0) {
                return Feature.UNDEF_FEATURE;
            }

            int nb = 0;
            for (int i = item_idx + 1; i < item_indexes[item_indexes.length - 1]; i++) {
                Word cur_item = seq_item.get(i);

		if (cur_item.getText() != null) {
		    String lc_word = cur_item.getText().toLowerCase();

		    if (convertPOS(lc_word).equals("content"))
			nb++;
		}
            }

            return new Feature(nb);
        }

        throw new MaryException("Only a word is accepted not an item of type " + item.getClass().toString() +
                            " ("
                            + item.toString() + ")");
    }
}
