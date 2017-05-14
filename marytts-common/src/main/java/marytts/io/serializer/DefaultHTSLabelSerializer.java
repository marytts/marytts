package marytts.io.serializer;

/* Utils part */
import java.util.Map;
import java.util.Hashtable;

/* Mary data part */
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;

/* IO Part */
import marytts.io.MaryIOException;
import java.io.File;

/**
 * This serializer is aimed to generate HTS compatible labels as Festival would
 * do.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class DefaultHTSLabelSerializer implements Serializer {
	/** The phoneme alphabet conversion map */
	protected Hashtable<String, String> alphabet_converter;

	/** the POS tag conversion */
	protected Hashtable<String, String> pos_converter;

	/** The undefined value constant */
	public static final String UNDEF = "x";

	/**
	 * Constructor
	 *
	 */
	public DefaultHTSLabelSerializer() {
		initPhConverter();
		initPOSConverter();
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
	public String toString(Utterance utt) throws MaryIOException {
		if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
			throw new MaryIOException("Current utterance doesn't have any features. Check the module sequence", null);
		}
		Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES);
		String output = "";
		for (FeatureMap map : seq_features) {
			output += format(map);
			output += "\n";
		}

		return output;
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
	public Utterance fromString(String content) throws MaryIOException {
		throw new UnsupportedOperationException();
	}

	/************************************************************************************************
	 * Conversion helpers
	 ************************************************************************************************/
	/**
	 * Method to generate the mapping between phonemes
	 *
	 */
	protected void initPhConverter() {
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

	/**
	 * Method to convert the phoneme label to be compatible with HTS
	 *
	 * @param ph
	 *            the phoneme to convert
	 * @return the converted phoneme
	 */
	protected String convertPh(String ph) {
		String fest_ph = alphabet_converter.get(ph);
		if (fest_ph != null)
			return fest_ph;

		return ph;
	}

	/**
	 * Method to check if a segment is a NonSpeechSound (NSS)
	 *
	 * @param feature_map
	 *            the map of features of the segment
	 * @return true if it is a NSS
	 */
	protected boolean isNSS(FeatureMap feature_map) {
		if (feature_map.get("phone").getStringValue().equals("_"))
			return true;

		return false;
	}

	/**
	 * Method to generate the mapping between Part Of Speech tags
	 *
	 */
	protected void initPOSConverter() {
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

	/**
	 * The POS tag to convert.
	 *
	 * @param pos
	 *            the original POS tag
	 * @return the converted POS tag
	 */
	protected String convertPOS(String pos) {
		String fest_pos = pos_converter.get(pos);
		if (fest_pos != null)
			return fest_pos;

		return "content";
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
		return ((feature_map.containsKey(feature_name)) && (feature_map.get(feature_name) != Feature.UNDEF_FEATURE))
				? feature_map.get(feature_name).getStringValue()
				: UNDEF;
	}

	/**
	 * Method to output the feature map in the HTS label format
	 *
	 * @param feature_map
	 *            the feature map
	 * @return the corresponding HTS label
	 */
	protected String format(FeatureMap feature_map) {
		// Check if current phone is nss ?
		boolean is_nss = isNSS(feature_map);

		// Phoneme format
		String format = "%s^%s-%s+%s=%s@%s_%s";
		String cur_lab = String.format(format,
				// Phoneme
				convertPh(getValue(feature_map, "prev_prev_phone")), convertPh(getValue(feature_map, "prev_phone")),
				convertPh(getValue(feature_map, "phone")), convertPh(getValue(feature_map, "next_phone")),
				convertPh(getValue(feature_map, "next_next_phone")),
				is_nss ? getValue(feature_map, "ph_from_syl_start") : UNDEF,
				is_nss ? getValue(feature_map, "ph_from_syl_end") : UNDEF);

		// Syllable format
		format = "/A:%s_%s_%s/B:%s-%s-%s@%s-%s&%s-%s#%s-%s$%s-%s!%s-%s;%s-%s|%s/C:%s+%s+%s";
		if (is_nss) {
			cur_lab += String.format(format,

					// Previous
					UNDEF, UNDEF, UNDEF,

					// Current
					UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,
					UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

					// Next
					UNDEF, UNDEF, UNDEF);
		} else {

			cur_lab += String.format(format,

					// Previous
					getValue(feature_map, "prev_syl_accent"), UNDEF, getValue(feature_map, "prev_syl_numph"),

					// Current
					getValue(feature_map, "prev_syl_accent"), UNDEF, getValue(feature_map, "prev_syl_numph"),

					getValue(feature_map, "syls_from_word_start"), getValue(feature_map, "syls_from_word_end"),
					getValue(feature_map, "syls_from_phrase_start"), getValue(feature_map, "syls_from_phrase_end"),
					UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

					// Next
					getValue(feature_map, "next_syl_accent"), UNDEF, getValue(feature_map, "next_syl_numph"));
		}

		// Word format
		format = "/D:%s_%s/E:%s+%s@%s+%s&%s+%s#%s+%s/F:%s_%s";
		if (is_nss) {
			cur_lab += String.format(format,
					// Previous
					UNDEF, UNDEF,

					// Current
					UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

					// Next
					UNDEF, UNDEF);
		} else {
			cur_lab += String.format(format,
					// Previous
					getValue(feature_map, "prev_word_pos"), getValue(feature_map, "prev_word_numsyls"),

					// Current
					getValue(feature_map, "word_pos"), getValue(feature_map, "word_numsyls"),
					getValue(feature_map, "words_from_phrase_start"), getValue(feature_map, "words_from_phrase_end"),
					UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

					// Next
					getValue(feature_map, "next_word_pos"), getValue(feature_map, "next_word_numsyls"));
		}

		// Phrase format
		format = "/G:%s_%s/H:%s=%s^%s=%s|%s/I:%s_%s";
		if (is_nss) {
			cur_lab += String.format(format,
					// Previous
					UNDEF, UNDEF,

					// Current
					UNDEF, UNDEF, UNDEF, UNDEF, UNDEF, UNDEF,

					// Next
					UNDEF, UNDEF);
		} else {
			cur_lab += String.format(format,
					// Previous
					getValue(feature_map, "prev_phrase_numsyls"), getValue(feature_map, "prev_phrase_numwords"),

					// Current
					getValue(feature_map, "phrase_numsyls"), getValue(feature_map, "phrase_numwords"),
					getValue(feature_map, "phrases_from_sentence_start"),
					getValue(feature_map, "phrases_from_sentence_end"), UNDEF,

					// Next
					getValue(feature_map, "next_phrase_numsyls"), getValue(feature_map, "next_phrase_numwords"));
		}

		// Utterance format
		format = "/J:%s+%s-%s";
		cur_lab += String.format(format, getValue(feature_map, "sentence_numsyllables"),
				getValue(feature_map, "sentence_numwords"), getValue(feature_map, "sentence_numphrases"));

		return cur_lab;
	}
}

/* DefaultHTSLabelSerializer.java ends here */
