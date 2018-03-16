package marytts.phonetic.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;


/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class Disc extends Alphabet
{
    public Disc() {
	super();

	// consonants
	this.addIpaCorrespondance("p", "p");
	this.addIpaCorrespondance("b", "b");
	this.addIpaCorrespondance("t", "t");
	this.addIpaCorrespondance("d", "d");
	this.addIpaCorrespondance("k", "k");
	this.addIpaCorrespondance("x", "ɡ");
	this.addIpaCorrespondance("g", "ɡ");
	this.addIpaCorrespondance("N", "ŋ");
	this.addIpaCorrespondance("m", "m");
	this.addIpaCorrespondance("n", "n");
	this.addIpaCorrespondance("l", "l");
	this.addIpaCorrespondance("r", "ɹ");
	this.addIpaCorrespondance("f", "f");
	this.addIpaCorrespondance("v", "v");
	this.addIpaCorrespondance("T", "θ");
	this.addIpaCorrespondance("D", "ð");
	this.addIpaCorrespondance("s", "s");
	this.addIpaCorrespondance("z", "z");
	this.addIpaCorrespondance("S", "ʃ");
	this.addIpaCorrespondance("Z", "ʒ");
	this.addIpaCorrespondance("j", "j");
	this.addIpaCorrespondance("h", "h");
	this.addIpaCorrespondance("w", "w");

	// affricates
	this.addIpaCorrespondance("J", "tʃ");
	this.addIpaCorrespondance("_", "dʒ");

	// syllabic consonants- these are coded just to their consonant counterpart in arpabet
	this.addIpaCorrespondance("C", "n");
	this.addIpaCorrespondance("F", "m");
	this.addIpaCorrespondance("H", "n");
	this.addIpaCorrespondance("P", "l");
	this.addIpaCorrespondance("R", "ɹ");

	// long vowels
	this.addIpaCorrespondance("i", "i");
	this.addIpaCorrespondance("u", "u");
	this.addIpaCorrespondance("#", "ɑ");
	this.addIpaCorrespondance("$", "ɔ");
	this.addIpaCorrespondance("3", "ɚ");

	// short vowels a few of these in DISC code to the same in ARPAbet
	this.addIpaCorrespondance("I", "ɪ");
	this.addIpaCorrespondance("E", "ɛ");
	this.addIpaCorrespondance("{", "æ");
	this.addIpaCorrespondance("Q", "ɑ");
	this.addIpaCorrespondance("V", "ʌ");
	this.addIpaCorrespondance("U", "ʊ");
	this.addIpaCorrespondance("@", "ɚ");

	// borrowed vowels dont have a separate encoding in arpabet and in both DISC and arpabet the subsequent consonant sound is coded
	this.addIpaCorrespondance("c", "ɪ");
	this.addIpaCorrespondance("q", "ɑ");
	this.addIpaCorrespondance("0", "ɑ");
	this.addIpaCorrespondance("~", "ʌ");

	// dipthongs oddly arpabet lacks something for 8 e.g. "pair" which is clearly a dipthong...
	this.addIpaCorrespondance("1", "eɪ");
	this.addIpaCorrespondance("2", "aɪ");
	this.addIpaCorrespondance("4", "ɔɪ");
	this.addIpaCorrespondance("5", "oʊ");
	this.addIpaCorrespondance("6", "aʊ");
	this.addIpaCorrespondance("7", "ɪ");
	this.addIpaCorrespondance("8", "ɛ");
	this.addIpaCorrespondance("9", "u");
    }
}
