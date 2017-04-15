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
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ArpaLabel implements FeatureProcessor
{
    protected static Hashtable<String, String> alphabet_converter = null;

    public ArpaLabel()
    {
        if (alphabet_converter == null)
            initPhConverter();
    }

    protected void initPhConverter()
    {
        alphabet_converter = new Hashtable<String, String>();

        alphabet_converter.put("o","ao");
        alphabet_converter.put("aa","aa");
        alphabet_converter.put("ii","iy");
        alphabet_converter.put("uu","u");
        alphabet_converter.put("e","eh");
        alphabet_converter.put("i","ih");
        alphabet_converter.put("u","uh");
        alphabet_converter.put("uh","ah");
        alphabet_converter.put("@","ax");
        alphabet_converter.put("a","ae");
        alphabet_converter.put("ei","ei");
        alphabet_converter.put("ai","ay");
        alphabet_converter.put("oo","o");
        alphabet_converter.put("ou", "ow");
        alphabet_converter.put("Q", "er");
        alphabet_converter.put("au","aw");
        alphabet_converter.put("oi","oy");
        alphabet_converter.put("ch","ch");
        alphabet_converter.put("jh","jh");
        alphabet_converter.put("th","th");
        alphabet_converter.put("dh","dh");
        alphabet_converter.put("sh","sh");
        alphabet_converter.put("zh","zh");
        alphabet_converter.put("ng","ng");
        alphabet_converter.put("y","y");
        alphabet_converter.put("d", "d");
        alphabet_converter.put("m", "m");
        alphabet_converter.put("n", "n");
        alphabet_converter.put("r", "r");
        alphabet_converter.put("s", "s");
        alphabet_converter.put("t", "t");
        alphabet_converter.put("b", "b");
        alphabet_converter.put("f", "f");
        alphabet_converter.put("g", "g");
        alphabet_converter.put("h", "h");
        alphabet_converter.put("k", "k");
        alphabet_converter.put("l", "l");
        alphabet_converter.put("p", "p");
        alphabet_converter.put("v", "v");
        alphabet_converter.put("w", "w");
        alphabet_converter.put("z", "z");
        alphabet_converter.put("sil", "pau");
        alphabet_converter.put("_", "pau");
    }


    protected String convertPh(String ph)
    {
        String fest_ph = alphabet_converter.get(ph);
        if (fest_ph != null)
        {
            return fest_ph;
        }
        System.out.println(ph + " is not converted");

        return ph;
    }

    public Feature generate(Utterance utt, Item item) throws Exception
    {
        return new Feature(convertPh(item.toString()));
    }
}
