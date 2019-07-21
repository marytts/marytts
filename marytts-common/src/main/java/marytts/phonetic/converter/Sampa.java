package marytts.phonetic.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;


/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Sampa extends Alphabet
{
    public Sampa() {
	super();

	// consonants
	this.addIpaCorrespondance("b", "b");
	this.addIpaCorrespondance("d", "d");
	this.addIpaCorrespondance("f", "f");
	this.addIpaCorrespondance("g", "ɡ");
	this.addIpaCorrespondance("k", "k");
	this.addIpaCorrespondance("l", "l");
	this.addIpaCorrespondance("m", "m");
	this.addIpaCorrespondance("n", "n");
	this.addIpaCorrespondance("S", "ʃ");
	this.addIpaCorrespondance("s", "s");
	this.addIpaCorrespondance("p", "p");
	this.addIpaCorrespondance("t", "t");
	this.addIpaCorrespondance("T", "θ");
	this.addIpaCorrespondance("v", "v");
	this.addIpaCorrespondance("w", "w");
        this.addIpaCorrespondance("x", "x");
	this.addIpaCorrespondance("z", "z");
	this.addIpaCorrespondance("Z", "ʒ");

	// affricates
	this.addIpaCorrespondance("dS", "dʃ");
	this.addIpaCorrespondance("dZ", "dʒ");
	this.addIpaCorrespondance("pf", "pf");
	this.addIpaCorrespondance("ts", "ts");
	this.addIpaCorrespondance("tS", "tʃ");
        this.addIpaCorrespondance("C", "ç");

	// Long vowels
        this.addIpaCorrespondance("a", "a");
	this.addIpaCorrespondance("A", "ɑ");
	this.addIpaCorrespondance("i", "i");
        this.addIpaCorrespondance("e", "e");
        this.addIpaCorrespondance("y", "y");
	this.addIpaCorrespondance("u", "u");
	this.addIpaCorrespondance("O", "ɔ");
        this.addIpaCorrespondance("o", "o");
	// FIXME: try to delete this.addIpaCorrespondance("AXR", "ɚ");

	// Short vowels
	this.addIpaCorrespondance("{", "æ");
	this.addIpaCorrespondance("V", "ʌ");
	this.addIpaCorrespondance("@", "ə");
	this.addIpaCorrespondance("@U", "əʊ");
	this.addIpaCorrespondance("E", "ɛ");

	this.addIpaCorrespondance("I", "ɪ");
	this.addIpaCorrespondance("1", "ɨ");
	this.addIpaCorrespondance("U", "ʊ");
        this.addIpaCorrespondance("}", "ʉ");
	this.addIpaCorrespondance("D", "ð");
	this.addIpaCorrespondance("4", "ɾ");

	// Stressed
	this.addIpaCorrespondance("EL", "l̩");
	this.addIpaCorrespondance("EM", "m̩");
	this.addIpaCorrespondance("EN", "n̩");

	// dipthongs
	this.addIpaCorrespondance("EI", "eɪ");
	this.addIpaCorrespondance("aI", "aɪ");
	this.addIpaCorrespondance("AI", "aɪ");
	this.addIpaCorrespondance("aU", "aʊ");
	this.addIpaCorrespondance("OW", "oʊ");
	this.addIpaCorrespondance("OI", "ɔɪ");
	this.addIpaCorrespondance("OY", "ɔʏ");

        this.addIpaCorrespondance("9", "œ");
        this.addIpaCorrespondance("2", "ø");
        this.addIpaCorrespondance("6", "ɐ");

	// FIXME: Not sorted (yet)
	this.addIpaCorrespondance("h", "h");
	this.addIpaCorrespondance("N", "ŋ");
	this.addIpaCorrespondance("4~", "ɾ̃");
	this.addIpaCorrespondance("Q", "ʔ");
	this.addIpaCorrespondance("r", "r");
        this.addIpaCorrespondance("r=", "ɚ"); // NOTE: needed by british english!
	this.addIpaCorrespondance("r\\", "ɹ");
        this.addIpaCorrespondance("R", "ʁ");
	this.addIpaCorrespondance("W", "ʍ");
	this.addIpaCorrespondance("j", "j");
        this.addIpaCorrespondance("?", "ʔ");
        this.addIpaCorrespondance("Y", "ʏ");

        // Nasale!
        this.addIpaCorrespondance("a~", "ã");
        this.addIpaCorrespondance("o~", "õ");
        this.addIpaCorrespondance("e~", "ẽ");
        this.addIpaCorrespondance("9~", "œ̃");

        // Long modifier
        this.addIpaCorrespondance("i:", "iː");
        this.addIpaCorrespondance("y:", "yː");
        this.addIpaCorrespondance("e:", "eː");
        this.addIpaCorrespondance("E:", "ɛː");
        this.addIpaCorrespondance("2:", "øː");
        this.addIpaCorrespondance("u:", "uː");
        this.addIpaCorrespondance("o:", "oː");
        this.addIpaCorrespondance("a:", "aː");
    }
}
