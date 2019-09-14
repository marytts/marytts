package marytts.features.feature.processor.word;

import marytts.MaryException;

import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

import marytts.features.Feature;
import marytts.features.feature.FeatureProcessor;

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
public class POS implements FeatureProcessor {

    public String convertPOS(String orig_pos) {
        if (orig_pos.equals(".")) {
            return "FULLSTOP";
        }
        if (orig_pos.equals(",")) {
            return "COMMA";
        }
        if (orig_pos.equals(":")) {
            return "COLON";
        }
        if (orig_pos.equals(";")) {
            return "SEMICOLON";
        }
        if (orig_pos.equals("'")) {
            return "APOSTROPHE";
        }
        if (orig_pos.equals("!")) {
            return "EXCLAM";
        }
        if (orig_pos.equals("?")) {
            return "QUESTION";
        }
        if (orig_pos.equals("-")) {
            return "HYPHEN";
        }
        if (orig_pos.equals("...")) {
            return "ELLIPSIS";
        }
        if (orig_pos.equals("``")) {
            return "OPENQUOTES";
        }
        if (orig_pos.equals("''")) {
            return "CLOSEQUOTES";
        }

        return orig_pos;

    }

    public Feature generate(Utterance utt, Item item) throws MaryException {
        if (item instanceof marytts.data.item.linguistic.Word) {
            if ((((Word) item).getPOS()) == null) {
                return Feature.UNDEF_FEATURE;
            } else {
                return new Feature(convertPOS(((Word) item).getPOS()));
            }
        }

        throw new MaryException("Only a word is accepted not an item of type " + item.getClass().toString() +
                            " ("
                            + item.toString() + ")");
    }
}
