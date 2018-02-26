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
public class Arpabet extends Alphabet
{
    public Arpabet() {
	super();
	this.addIpaCorrespondance("AA", "ɑ");
	this.addIpaCorrespondance("AE", "æ");
	this.addIpaCorrespondance("AH", "ʌ");
	this.addIpaCorrespondance("AO", "ɔ");
	this.addIpaCorrespondance("AW", "aʊ");
	this.addIpaCorrespondance("AX", "ə");
	this.addIpaCorrespondance("AXR", "ɚ");
	this.addIpaCorrespondance("AY", "aɪ");
	this.addIpaCorrespondance("EH", "ɛ");
	this.addIpaCorrespondance("ER", "ɚ");
	this.addIpaCorrespondance("EY", "eɪ");
	this.addIpaCorrespondance("IH", "ɪ");
	this.addIpaCorrespondance("IX", "ɨ");
	this.addIpaCorrespondance("IY", "i");
	this.addIpaCorrespondance("OW", "oʊ");
	this.addIpaCorrespondance("OY", "ɔɪ");
	this.addIpaCorrespondance("UH", "ʊ");
	this.addIpaCorrespondance("UW", "u");
	this.addIpaCorrespondance("UX", "ʉ");
	this.addIpaCorrespondance("B", "b");
	this.addIpaCorrespondance("CH", "tʃ");
	this.addIpaCorrespondance("D", "d");
	this.addIpaCorrespondance("DH", "ð");
	this.addIpaCorrespondance("DX", "ɾ");
	this.addIpaCorrespondance("EL", "l̩");
	this.addIpaCorrespondance("EM", "m̩");
	this.addIpaCorrespondance("EN", "n̩");
	this.addIpaCorrespondance("F", "f");
	this.addIpaCorrespondance("G", "ɡ");
	this.addIpaCorrespondance("HH", "h");
	this.addIpaCorrespondance("JH", "dʒ");
	this.addIpaCorrespondance("K", "k");
	this.addIpaCorrespondance("L", "l");
	this.addIpaCorrespondance("M", "m");
	this.addIpaCorrespondance("N", "n");
	this.addIpaCorrespondance("NG", "ŋ");
	this.addIpaCorrespondance("NX", "ɾ̃");
	this.addIpaCorrespondance("P", "p");
	this.addIpaCorrespondance("Q", "ʔ");
	this.addIpaCorrespondance("R", "ɹ");
	this.addIpaCorrespondance("S", "s");
	this.addIpaCorrespondance("SH", "ʃ");
	this.addIpaCorrespondance("T", "t");
	this.addIpaCorrespondance("TH", "θ");
	this.addIpaCorrespondance("V", "v");
	this.addIpaCorrespondance("W", "w");
	this.addIpaCorrespondance("WH", "ʍ");
	this.addIpaCorrespondance("Y", "j");
	this.addIpaCorrespondance("Z", "z");
    }
}
