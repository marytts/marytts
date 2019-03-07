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
        this.addIpaCorrespondance("iy", "i");
        this.addIpaCorrespondance("iy:", "iː");
        this.addIpaCorrespondance("y", "y");
        this.addIpaCorrespondance("y:", "yː");
        this.addIpaCorrespondance("ih", "ɪ");
        this.addIpaCorrespondance("yh", "ʏ");
        this.addIpaCorrespondance("e", "e");
        this.addIpaCorrespondance("e:", "eː");
        this.addIpaCorrespondance("e~", "ẽ");
        this.addIpaCorrespondance("eu", "ø");
        this.addIpaCorrespondance("eu:", "øː");
        this.addIpaCorrespondance("eh", "ɛ");
        this.addIpaCorrespondance("eh:", "ɛː");
        this.addIpaCorrespondance("er", "œ");
        this.addIpaCorrespondance("er~", "œ̃"); // FIXME: added by force
        this.addIpaCorrespondance("ae", "æ");
        this.addIpaCorrespondance("a", "a");
        this.addIpaCorrespondance("a:", "aː");
        this.addIpaCorrespondance("a~", "ã");
        this.addIpaCorrespondance("ix", "ɨ");
        this.addIpaCorrespondance("ux", "ʉ");
        this.addIpaCorrespondance("ax", "ə");
        this.addIpaCorrespondance("axr", "ɚ");
        this.addIpaCorrespondance("oe", "ɐ");
        this.addIpaCorrespondance("uw", "u");
        this.addIpaCorrespondance("uw:", "uː");
        this.addIpaCorrespondance("uh", "ʊ");
        this.addIpaCorrespondance("o", "o");
        this.addIpaCorrespondance("o:", "oː");
        this.addIpaCorrespondance("o~", "õ");
        this.addIpaCorrespondance("ah", "ʌ");
        this.addIpaCorrespondance("ao", "ɔ");
        this.addIpaCorrespondance("aa", "ɑ");
        this.addIpaCorrespondance("ey", "eɪ");
        this.addIpaCorrespondance("ow", "oʊ");
        this.addIpaCorrespondance("oy", "ɔʏ");
        this.addIpaCorrespondance("oi", "ɔɪ");
        this.addIpaCorrespondance("ay", "aɪ");
        this.addIpaCorrespondance("aw", "aʊ");
        this.addIpaCorrespondance("p", "p");
        this.addIpaCorrespondance("q", "ʔ"); // FIXME: added by force
        this.addIpaCorrespondance("b", "b");
        this.addIpaCorrespondance("m", "m");
        this.addIpaCorrespondance("em", "m̩");
        this.addIpaCorrespondance("f", "f");
        this.addIpaCorrespondance("v", "v");
        this.addIpaCorrespondance("th", "θ");
        this.addIpaCorrespondance("ts", "ts"); // FIXME: added by force
        this.addIpaCorrespondance("dh", "ð");
        this.addIpaCorrespondance("t", "t");
        this.addIpaCorrespondance("d", "d");
        this.addIpaCorrespondance("n", "n");
        this.addIpaCorrespondance("en", "n̩");
        this.addIpaCorrespondance("dx", "ɾ");
        this.addIpaCorrespondance("nx", "ɾ̃");
        this.addIpaCorrespondance("s", "s");
        this.addIpaCorrespondance("z", "z");
        this.addIpaCorrespondance("r", "ɹ");
        this.addIpaCorrespondance("rr", "ʁ"); // FIXME: added by force
        this.addIpaCorrespondance("l", "l");
        this.addIpaCorrespondance("el", "l̩");
        this.addIpaCorrespondance("sh", "ʃ");
        this.addIpaCorrespondance("zh", "ʒ");
        this.addIpaCorrespondance("hv", "ç");
        this.addIpaCorrespondance("j", "j");
        this.addIpaCorrespondance("k", "k");
        this.addIpaCorrespondance("g", "ɡ");
        this.addIpaCorrespondance("ng", "ŋ");
        this.addIpaCorrespondance("hh", "h");
        this.addIpaCorrespondance("wh", "ʍ");
        this.addIpaCorrespondance("w", "w");
        this.addIpaCorrespondance("x", "x"); // FIXME: added by force
        this.addIpaCorrespondance("pf", "pf");
        this.addIpaCorrespondance("ch", "tʃ");
        this.addIpaCorrespondance("jh", "dʒ");
    }
}
