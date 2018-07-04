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
public class Arpabet extends Alphabet
{
    public Arpabet() {
	super();


	// consonants
	this.addIpaCorrespondance("B", "b");
	this.addIpaCorrespondance("D", "d");
	this.addIpaCorrespondance("F", "f");
	this.addIpaCorrespondance("G", "ɡ");
	this.addIpaCorrespondance("K", "k");
	this.addIpaCorrespondance("L", "l");
	this.addIpaCorrespondance("M", "m");
	this.addIpaCorrespondance("N", "n");
	this.addIpaCorrespondance("SH", "ʃ");
	this.addIpaCorrespondance("S", "s");
	this.addIpaCorrespondance("P", "p");
	this.addIpaCorrespondance("T", "t");
	this.addIpaCorrespondance("TH", "θ");
	this.addIpaCorrespondance("V", "v");
	this.addIpaCorrespondance("W", "w");
	this.addIpaCorrespondance("Z", "z");
	this.addIpaCorrespondance("ZH", "ʒ");

	// affricates
	this.addIpaCorrespondance("TS", "ts"); // FIXME: oupsy
	this.addIpaCorrespondance("CH", "tʃ");
	this.addIpaCorrespondance("JH", "dʒ");

	// Long vowels
	this.addIpaCorrespondance("AA", "ɑ");
	this.addIpaCorrespondance("IY", "i");
	this.addIpaCorrespondance("UW", "u");
	this.addIpaCorrespondance("AO", "ɔ");
	this.addIpaCorrespondance("AXR", "ɚ");

	// Short vowels
	this.addIpaCorrespondance("AE", "æ");
	this.addIpaCorrespondance("AH", "ʌ");
	this.addIpaCorrespondance("AX", "ə");
	this.addIpaCorrespondance("EH", "ɛ");
	this.addIpaCorrespondance("ER", "ɚ");
	this.addIpaCorrespondance("IH", "ɪ");
	this.addIpaCorrespondance("IX", "ɨ");
	this.addIpaCorrespondance("UH", "ʊ");
	this.addIpaCorrespondance("UX", "ʉ");
	this.addIpaCorrespondance("DH", "ð");
	this.addIpaCorrespondance("DX", "ɾ");

	// Stressed
	this.addIpaCorrespondance("EL", "l̩");
	this.addIpaCorrespondance("EM", "m̩");
	this.addIpaCorrespondance("EN", "n̩");


	// dipthongs
	this.addIpaCorrespondance("EY", "eɪ");
	this.addIpaCorrespondance("AY", "aɪ");
	this.addIpaCorrespondance("AW", "aʊ");
	this.addIpaCorrespondance("OW", "oʊ");
	this.addIpaCorrespondance("OY", "ɔɪ");


	// FIXME: Not sorted (yet)
	this.addIpaCorrespondance("H", "h");
	this.addIpaCorrespondance("HH", "h");
	this.addIpaCorrespondance("NG", "ŋ");
	this.addIpaCorrespondance("NX", "ɾ̃");
	this.addIpaCorrespondance("Q", "ʔ");
	this.addIpaCorrespondance("R", "ɹ");
	this.addIpaCorrespondance("WH", "ʍ");
	this.addIpaCorrespondance("Y", "j");
    }
}
