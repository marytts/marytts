package marytts.io.serializer.label;

/* Utils part */
import java.util.Map;
import java.util.Hashtable;
import java.util.ArrayList;

/* Mary data part */
import marytts.data.Sequence;
import marytts.data.item.Item;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Phone;
import marytts.data.Relation;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;

/* IO Part */
import marytts.io.serializer.Serializer;
import marytts.io.MaryIOException;
import java.io.File;

/* Alphabet part */
import marytts.phonetic.converter.Alphabet;
import marytts.phonetic.AlphabetFactory;

/* */
import marytts.MaryException;

/**
 * This serializer is aimed to generate HTS compatible labels as Festival would
 * do.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class DefaultHTSLabelSerializer implements Serializer {

    /** The default undefined value constant */
    private static final String DEFAULT_UNDEF = "xx";

    /** The HTK step offset (ms => HTK unit) */
    private static final int HTK_STEP = 10000;

    /** The object undefined symbol */
    protected String undef_symbol;

    /** Ipa to arpabet conversion */
    protected Alphabet ipa2arp;

    /**
     * Constructor
     *
     */
    public DefaultHTSLabelSerializer() throws MaryException {
        setUndefSymbol(DEFAULT_UNDEF);
	ipa2arp = AlphabetFactory.getAlphabet("arpabet");
    }

    /**
     *  Get the object undefined symbol value
     *
     *  @return the undefined symbol
     */
    public String getUndefSymbol() {
        return undef_symbol;
    }

    public void setUndefSymbol(String undef_symbol) {
        this.undef_symbol = undef_symbol;
    }

    /**
     * Generate the HTS Labels from the given utterance
     *
     * @param utt
     *            the utterance
     * @return the HTS label content
     * @throws MaryIOException
     *             when something is going wrong
     */
    public Object export(Utterance utt) throws MaryIOException {
        if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
            throw new MaryIOException("Current utterance doesn't have any features. Check the module sequence",
                                      null);
        }

	try {
            Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(
                                                                                       SupportedSequenceType.FEATURES);
            Relation rel_feat_ph = utt.getRelation(SupportedSequenceType.FEATURES, SupportedSequenceType.PHONE);

	    String output = "";
	    for (int i=0; i<seq_features.size(); i++) {
		FeatureMap map = seq_features.get(i);
		ArrayList<Phoneme> phs = (ArrayList<Phoneme>) rel_feat_ph.getRelatedItems(i);
		if (phs.size() <= 0) {
		    continue;
		}
		if (phs.get(0) instanceof Phone)  {
		    Phone ph = (Phone) phs.get(0);
		    long start = (long) (ph.getStart() * HTK_STEP);
		    long end = (long) ((ph.getStart() + ph.getDuration()) * HTK_STEP);
		    output += String.format("%d\t%d\t", start, end);
		}
		output += format(map);
		output += "\n";
	    }

	    return output;
	} catch (MaryException ex) {
	    throw new MaryIOException("Couldn't format the label", ex);
	}
    }

    /**
     * Generate an utterance from the HTS labels. <b>Not supported for now</b>
     *
     * @param content
     *            the HTS labels
     * @return the utterance generated
     * @throws MaryIOException
     *             when something is going wrong
     */
    public Utterance load(String content) throws MaryIOException {
        throw new UnsupportedOperationException();
    }


    /************************************************************************************************
     * Conversion helpers
     ************************************************************************************************/

    /**
     * Method to convert the phoneme label to be compatible with HTS
     *
     * @param ph
     *            the phoneme to convert
     * @return the converted phoneme
     */
    protected String convertPh(String ph) throws MaryException {

	if ((ph == null) || (ph.isEmpty()))
	    return getUndefSymbol();
	if (ph.equals(getUndefSymbol()))
	    return getUndefSymbol();

	if (ph.equals("sil") || ph.equals("_"))
	    return "pau";

        return ipa2arp.getLabelFromIPA(ph).toLowerCase();
    }

    /**
     * Method to check if a segment is a NonSpeechSound (NSS)
     *
     * @param feature_map
     *            the map of features of the segment
     * @return true if it is a NSS
     */
    protected boolean isNSS(FeatureMap feature_map) {
        if (feature_map.get("phone").getStringValue().equals("pau")) {
            return true;
        }

        return false;
    }

    /**
     * Wrapper to get the value of specific feature
     *
     * @param feature_map
     *            the feature map
     * @param feature_name
     *            the feature name
     * @return the value of feature_name in feature_map or the UNDEF value if
     *         the feature_name is not existing in the map
     */
    protected final String getValue(FeatureMap feature_map, String feature_name) {

        if (! feature_map.containsKey(feature_name)) {
            return getUndefSymbol();
        }
        if (feature_map.get(feature_name) == Feature.UNDEF_FEATURE) {
            return getUndefSymbol();
        }

        String feat = feature_map.get(feature_name).getStringValue();

        if (feat.equals("false")) {
            return "0";
        }
        if (feat.equals("true")) {
            return "1";
        }

        return feat;
    }

    /**
     * Method to output the feature map in the HTS label format
     *
     * @param feature_map
     *            the feature map
     * @return the corresponding HTS label
     */
    protected String format(FeatureMap feature_map) throws MaryException {
        // Check if current phone is nss ?
        boolean is_nss = isNSS(feature_map);

        // Phoneme format
        String format = "%s^%s-%s+%s=%s@%s_%s";
        String cur_lab = String.format(format,
                                       // Phoneme
                                       convertPh(getValue(feature_map, "prev_prev_phone")), convertPh(getValue(feature_map, "prev_phone")),
                                       convertPh(getValue(feature_map, "phone")), convertPh(getValue(feature_map, "next_phone")),
                                       convertPh(getValue(feature_map, "next_next_phone")),
                                       getValue(feature_map, "ph_from_syl_start"),
                                       getValue(feature_map, "ph_from_syl_end"));

        // Syllable format
        format = "/A:%s_%s_%s/B:%s-%s-%s@%s-%s&%s-%s#%s-%s$%s-%s!%s-%s;%s-%s|%s/C:%s+%s+%s";
        if (is_nss) {
            cur_lab += String.format(format,

                                     // Previous
                                     getUndefSymbol(), getUndefSymbol(), getUndefSymbol(),

                                     // Current
                                     getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(),
				     getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(),
				     getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(),
				     getUndefSymbol(),

                                     // Next
                                     getUndefSymbol(), getUndefSymbol(), getUndefSymbol());
        } else {

            cur_lab += String.format(format,

                                     // Previous
                                     getValue(feature_map, "prev_syl_accent"), getValue(feature_map, "prev_syl_stress"), getValue(feature_map, "prev_syl_num_phones"),

                                     // Current
                                     getValue(feature_map, "syl_accent"), getValue(feature_map, "syl_stress"), getValue(feature_map, "syl_num_phones"),

                                     getValue(feature_map, "syls_from_word_start"), getValue(feature_map, "syls_from_word_end"),
                                     getValue(feature_map, "syls_from_phrase_start"), getValue(feature_map, "syls_from_phrase_end"),
                                     getValue(feature_map, "nb_stress_syls_from_phrase_start"), getValue(feature_map, "nb_stress_syls_to_phrase_end"),
                                     getValue(feature_map, "nb_accent_syls_from_phrase_start"), getValue(feature_map, "nb_accent_syls_to_phrase_end"),
				     getUndefSymbol(), getUndefSymbol(),
				     getUndefSymbol(), getUndefSymbol(),
				     convertPh(getValue(feature_map, "syl_vowel")),

                                     // Next
                                     getValue(feature_map, "next_syl_accent"), getValue(feature_map, "next_syl_stress"), getValue(feature_map, "next_syl_num_phones"));
        }

        // Word format
        format = "/D:%s_%s/E:%s+%s@%s+%s&%s+%s#%s+%s/F:%s_%s";
        if (is_nss) {
            cur_lab += String.format(format,
                                     // Previous
                                     getUndefSymbol(), getUndefSymbol(),

                                     // Current
                                     getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(),

                                     // Next
                                     getUndefSymbol(), getUndefSymbol());
        } else {
            cur_lab += String.format(format,
                                     // Previous
                                     getValue(feature_map, "prev_word_pos_festival"), getValue(feature_map, "prev_word_num_syls"),

                                     // Current
                                     getValue(feature_map, "word_pos_festival"), getValue(feature_map, "word_num_syls"),
                                     getValue(feature_map, "words_from_phrase_start"), getValue(feature_map, "words_from_phrase_end"),
                                     getValue(feature_map, "nb_content_words_from_phrase_start"), getValue(feature_map, "nb_content_words_to_phrase_end"),
                                     getUndefSymbol(), getUndefSymbol(),

                                     // Next
                                     getValue(feature_map, "next_word_pos_festival"), getValue(feature_map, "next_word_num_syls"));
        }

        // Phrase format
        format = "/G:%s_%s/H:%s=%s^%s=%s|%s/I:%s_%s";
        if (is_nss) {
            cur_lab += String.format(format,
                                     // Previous
                                     getUndefSymbol(), getUndefSymbol(),

                                     // Current
                                     getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(), getUndefSymbol(),

                                     // Next
                                     getUndefSymbol(), getUndefSymbol());
        } else {
            cur_lab += String.format(format,
                                     // Previous
                                     getValue(feature_map, "prev_phrase_num_syls"), getValue(feature_map, "prev_phrase_num_words"),

                                     // Current
                                     getValue(feature_map, "phrase_num_syls"), getValue(feature_map, "phrase_num_words"),
                                     getValue(feature_map, "phrases_from_sentence_start"),
                                     getValue(feature_map, "phrases_from_sentence_end"), getUndefSymbol(),

                                     // Next
                                     getValue(feature_map, "next_phrase_num_syls"), getValue(feature_map, "next_phrase_num_words"));
        }

        // Utterance format
        format = "/J:%s+%s-%s";
        cur_lab += String.format(format, getValue(feature_map, "sentence_num_syls"),
                                 getValue(feature_map, "sentence_num_words"), getValue(feature_map, "sentence_num_phrases"));

        return cur_lab;
    }
}
