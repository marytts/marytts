package marytts.features.featureprocessor;

import java.util.Hashtable;
import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

import marytts.features.Feature;
import marytts.features.FeatureProcessor;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class ArpaLabel implements FeatureProcessor {
    protected static Hashtable<String, String> alphabet_converter = null;

    public ArpaLabel() {
        if (alphabet_converter == null) {
            initPhConverter();
        }
    }

    protected void initPhConverter() {
        alphabet_converter = new Hashtable<String, String>();

        alphabet_converter.put("A", "aa");
        alphabet_converter.put("O", "ao");
        alphabet_converter.put("u", "uw");
        alphabet_converter.put("i", "iy");

        alphabet_converter.put("{", "ae");
        alphabet_converter.put("V", "ah");
        alphabet_converter.put("E", "eh");
        alphabet_converter.put("I", "ih");
        alphabet_converter.put("U", "uh");

        alphabet_converter.put("@", "ah");
        alphabet_converter.put("r=", "er"); // FIXME: what is this one ?


        alphabet_converter.put("aU", "aw");
        alphabet_converter.put("OI", "oy");
        alphabet_converter.put("@U", "ah");
        alphabet_converter.put("EI", "ey");
        alphabet_converter.put("AI", "ay");

        alphabet_converter.put("p", "p");
        alphabet_converter.put("t", "t");
        alphabet_converter.put("k", "k");
        alphabet_converter.put("b", "b");
        alphabet_converter.put("d", "d");
        alphabet_converter.put("g", "g");

        alphabet_converter.put("tS", "ch");
        alphabet_converter.put("dZ", "jh");

        alphabet_converter.put("f", "f");
        alphabet_converter.put("v", "v");
        alphabet_converter.put("T", "th");
        alphabet_converter.put("D", "dh");
        alphabet_converter.put("s", "s");
        alphabet_converter.put("z", "z");
        alphabet_converter.put("S", "sh");
        alphabet_converter.put("Z", "zh");
        alphabet_converter.put("h", "h");

        alphabet_converter.put("l", "l");
        alphabet_converter.put("m", "m");
        alphabet_converter.put("n", "n");
        alphabet_converter.put("N", "ng");
        alphabet_converter.put("r", "r"); // FIXME: what is this one ?
        alphabet_converter.put("w", "w");
        alphabet_converter.put("j", "y");

        alphabet_converter.put("sil", "pau");
        alphabet_converter.put("_", "pau");
    }

    protected String convertPh(String ph) {
        String fest_ph = alphabet_converter.get(ph);
        if (fest_ph != null) {
            return fest_ph;
        }
        System.out.println(ph + " is not converted");

        return ph;
    }

    public Feature generate(Utterance utt, Item item) throws Exception {
        return new Feature(convertPh(item.toString()));
    }
}
