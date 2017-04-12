package marytts.io;

import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Phone;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.data.Utterance;
import marytts.io.MaryIOException;
import marytts.data.SupportedSequenceType;


import java.util.Hashtable;
import java.util.Map;
import java.io.File;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DefaultHTSLabelSerializer implements Serializer
{
    protected Hashtable<String, String> alphabet_converter;
    protected Hashtable<String, String> pos_converter;
    public static final String UNDEF = "x";

    public DefaultHTSLabelSerializer()
    {
        initPhConverter();
        initPOSConverter();
    }

    public Utterance load(File file)
        throws MaryIOException
    {
        throw new UnsupportedOperationException();
    }

    public void save(File file, Utterance utt)
        throws MaryIOException
    {
        throw new UnsupportedOperationException();
    }

    public String toString(Utterance utt)
        throws MaryIOException
    {
        if (!utt.hasSequence(SupportedSequenceType.FEATURES))
        {
            throw new MaryIOException("Current utterance doesn't have any features. Check the module sequence", null);
        }
        Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES);
        String output = "";
        for (FeatureMap map: seq_features)
        {
            output += format(map);
            output += "\n";
        }

        return output;
    }

    public Utterance fromString(String content)
        throws MaryIOException
    {
        throw new UnsupportedOperationException();
    }


    /* ==========================================================================================
     * # Conversion helpers
     * ========================================================================================== */
    protected void initPhConverter()
    {
        alphabet_converter = new Hashtable<String, String>();

        // Vowels
        alphabet_converter.put("A", "aa");
        alphabet_converter.put("AI", "ay");
        alphabet_converter.put("E", "eh");
        alphabet_converter.put("EI", "ey");
        alphabet_converter.put("I", "ih");
        alphabet_converter.put("O", "ao");
        alphabet_converter.put("OI", "oy");
        alphabet_converter.put("U", "uh");
        alphabet_converter.put("aU", "aw");
        alphabet_converter.put("i", "iy");
        alphabet_converter.put("u", "uw");
        alphabet_converter.put("@", "ax");
        alphabet_converter.put("@U", "ow");
        alphabet_converter.put("V", "ah");
        alphabet_converter.put("{", "ae");

        alphabet_converter.put("j", "y");

        alphabet_converter.put("D", "dh");
        alphabet_converter.put("N", "ng");
        alphabet_converter.put("S", "sh");
        alphabet_converter.put("T", "th");
        alphabet_converter.put("Z", "zh");
        alphabet_converter.put("b", "b");
        alphabet_converter.put("d", "d");
        alphabet_converter.put("dZ", "jh"); // FIXME: what it is ?
        alphabet_converter.put("f", "f");
        alphabet_converter.put("g", "g");
        alphabet_converter.put("h", "hh");
        alphabet_converter.put("k", "k");
        alphabet_converter.put("l", "l");
        alphabet_converter.put("m", "m");
        alphabet_converter.put("n", "n");
        alphabet_converter.put("p", "p");
        alphabet_converter.put("r", "r");
        alphabet_converter.put("r=", "r"); // FIXME: sure ?
        alphabet_converter.put("s", "s");
        alphabet_converter.put("t", "t");
        alphabet_converter.put("tS", "ch");
        alphabet_converter.put("v", "v");
        alphabet_converter.put("w", "w");
        alphabet_converter.put("z", "z");

        alphabet_converter.put("_", "pau");

        alphabet_converter.put("2", "eu");
        alphabet_converter.put("4", "dx");
        alphabet_converter.put("6", "er");
        alphabet_converter.put("9", "oe");
        alphabet_converter.put("?", "dt");
    }

    protected String convertPh(String ph)
    {
        String fest_ph = alphabet_converter.get(ph);
        if (fest_ph != null)
            return fest_ph;

        return ph;
    }


    protected boolean isNSS(FeatureMap feature_map)
    {
        if (feature_map.get("phone").getStringValue().equals("_"))
            return true;

        return false;
    }



    protected void initPOSConverter()
    {
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

        // content => default do nothing
    }

    protected String convertPOS(Hashtable<String, String> cur_wrd)
    {
        String fest_pos = pos_converter.get(cur_wrd.get("label"));

        if (fest_pos != null)
            return fest_pos;

        return "content";
    }

    protected String convertPOS(String pos)
    {
        String fest_pos = pos_converter.get(pos);
        if (fest_pos != null)
            return fest_pos;

        return "content";
    }

    protected String format(FeatureMap feature_map)
    {
        // Check if current phone is nss ?
        boolean is_nss = isNSS(feature_map);

        // Phoneme format
        String format ="%s^%s-%s+%s=%s@%s_%s";
        String cur_lab = String.format(format,
                                       // Phoneme
                                       feature_map.get("prev_prev_phone") == null ? UNDEF : convertPh(feature_map.get("prev_prev_phone").getStringValue()),
                                       feature_map.get("prev_phone") == null ? UNDEF : convertPh(feature_map.get("prev_phone").getStringValue()),
                                       convertPh(feature_map.get("phone").getStringValue()),
                                       feature_map.get("next_phone") == null ? UNDEF : convertPh(feature_map.get("next_phone").getStringValue()),
                                       feature_map.get("next_next_phone") == null ? UNDEF : convertPh(feature_map.get("next_next_phone").getStringValue()),
                                       is_nss? "x" : UNDEF,
                                       is_nss? "x" : UNDEF);


        // Syllable format
        format = "/A:%s_%s_%s/B:%s-%s-%s@%s-%s&%s-%s#%s-%s$%s-%s!%s-%s;%s-%s|%s/C:%s+%s+%s";
        if (is_nss)
        {
            cur_lab += String.format(format,

                                     // Previous
                                     UNDEF, UNDEF, UNDEF,

                                     // Current
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,
                                     UNDEF,

                                     // Next
                                     UNDEF, UNDEF, UNDEF);
        }
        else
        {

            cur_lab += String.format(format,

                                     // Previous
                                     UNDEF, UNDEF, UNDEF,

                                     // Current
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

                                     // Next
                                     UNDEF, UNDEF, UNDEF);
        }

        // Word format
        format = "/D:%s_%s/E:%s+%s@%s+%s&%s+%s#%s+%s/F:%s_%s";
        if (is_nss)
        {
            cur_lab += String.format(format,
                                     // Previous
                                     UNDEF, UNDEF,

                                     // Current
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

                                     // Next
                                     UNDEF, UNDEF);
        }
        else
        {
            cur_lab += String.format(format,
                                     // Previous
                                     UNDEF, UNDEF,

                                     // Current
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

                                     // Next
                                     UNDEF, UNDEF);
        }

        // Phrase format
        format = "/G:%s_%s/H:%s=%s^%s=%s|%s/I:%s_%s";
        if (is_nss)
        {
            cur_lab += String.format(format,
                                     // Previous
                                     UNDEF, UNDEF,

                                     // Current
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

                                     // Next
                                     UNDEF, UNDEF);
        }
        else
        {
            cur_lab += String.format(format,
                                     // Previous
                                     UNDEF, UNDEF,

                                     // Current
                                     UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

                                     // Next
                                     UNDEF, UNDEF);
        }

        // Utterance format
        format = "/J:%s+%s-%s";
        cur_lab += String.format(format,
                                 feature_map.get("sentence_numsyllables").getStringValue(),
                                 feature_map.get("sentence_numwords").getStringValue(),
                                 feature_map.get("sentence_numphrases").getStringValue());
        return cur_lab;
    }
}


/* HTSLabelSerializer.java ends here */
