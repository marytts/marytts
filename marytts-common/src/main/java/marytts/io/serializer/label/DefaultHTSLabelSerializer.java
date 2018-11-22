package marytts.io.serializer.label;

/* Regexp */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Utils part */
import java.util.Map;
import java.util.Hashtable;
import java.util.ArrayList;

/* Mary data part */
import marytts.data.Sequence;
import marytts.data.utils.IntegerPair;
import marytts.data.item.Item;
import marytts.data.item.global.StringItem;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.acoustic.Segment;
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
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
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
        if (!utt.hasSequence(SupportedSequenceType.FEATURES) && !utt.hasSequence(SupportedSequenceType.LABEL)) {
            throw new MaryIOException("Current utterance doesn't have any features or labels. Check the module sequence");
        }

	try {
            Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES);
            Relation rel_feat_ph = utt.getRelation(SupportedSequenceType.FEATURES, SupportedSequenceType.PHONE);

            Sequence<Segment> seq_seg = null;
            Relation rel_ph_seg = null;
            if (utt.hasSequence(SupportedSequenceType.SEGMENT)) {
                seq_seg = (Sequence<Segment>) utt.getSequence(SupportedSequenceType.SEGMENT);
                rel_ph_seg = utt.getRelation(SupportedSequenceType.PHONE, SupportedSequenceType.SEGMENT);
            }

	    String output = "";
	    for (int i=0; i<seq_features.size(); i++) {
		FeatureMap map = seq_features.get(i);
		ArrayList<Phoneme> phs = (ArrayList<Phoneme>) rel_feat_ph.getRelatedItems(i);
		if (phs.size() <= 0) {
		    continue;
		}

		if (seq_seg != null)  {
                    ArrayList<Segment> segments = (ArrayList<Segment>) rel_ph_seg.getRelatedItems(i);
                    double dur = 0;
                    for (Segment s: segments)
                        dur += s.getDuration();
		    long start = (long) (segments.get(i).getStart() * HTK_STEP);
		    long end = start + (long) (dur * HTK_STEP);
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
     * Generate an utterance from the HTS labels. It is producing a sequence of phone and a sequence
     * of string items identified as LABELS in the utterance
     *
     * @param content the HTS labels
     * @return the utterance generated
     * @throws MaryIOException
     *             when something is going wrong
     */
    public Utterance load(String content) throws MaryIOException {
        Sequence<Phoneme> seq_ph = new Sequence<Phoneme>();
        Sequence<Segment> seq_seg = new Sequence<Segment>();
        Sequence<StringItem> seq_labels = new Sequence<StringItem>();

        try {
            double start = 0;
            String[] lines = content.split("\n");
            for (String l: lines) {
                Segment cur_seg = null;

                // Ignore empty lines
                if (l.length() == 0)
                    continue;

                // Get elements
                String[] elts = l.split("[ \t]");
                if ((elts.length != 1) && (elts.length != 3))
                    throw new MaryIOException(String.format("\"%s\" doesn't respect the HTK label standard format",
                                                            l));

                if (elts.length == 3) { // Label with duration
                    // Fill label sequence
                    String label = elts[2];
                    seq_labels.add(new StringItem(label));

                    // Extract timestamps
                    start = Long.parseLong(elts[0]) / 10000.0;
                    double end = Long.parseLong(elts[1]) / 10000.0;

                    // Adapt label
                    Matcher m = Pattern.compile("-([a-zA-Z0-9]+)\\+").matcher(label);
                    if (m.find()) {
                        label = m.group(1).toUpperCase();
                    } else {
                        throw new MaryIOException(String.format("\"%s\" doesn't follow the HTK label format convention, couldn't find the monophone label"));
                    }
                    label = ipa2arp.getCorrespondingIPA(label);

                    // Generate phone and fill sequence
                    Phoneme ph = new Phoneme(label);
                    seq_ph.add(ph);

                    //
                    cur_seg = new Segment(start, end-start);
                } else {
                    // Fill label sequence
                    String label = elts[0];
                    seq_labels.add(new StringItem(label));

                    // Adapt label
                    Matcher m = Pattern.compile("-([a-zA-Z0-9]+)\\+").matcher(label);
                    if (m.find()) {
                        label = m.group(1).toUpperCase();
                    } else {
                        throw new MaryIOException(String.format("\"%s\" doesn't follow the HTK label format convention, couldn't find the monophone label"));
                    }
                    label = ipa2arp.getCorrespondingIPA(label);

                    // Generate phone and fill sequence
                    Phoneme ph = new Phoneme(label);
                    seq_ph.add(ph);

                    // Add dummy segment
                    cur_seg = new Segment(start, Segment.DEFAULT_DURATION);
                    start += Segment.DEFAULT_DURATION;
                }

                seq_seg.add(cur_seg);
            }


            // Generate utterance
            Utterance utt = new Utterance();

            // Add the sequence
            utt.addSequence(SupportedSequenceType.SEGMENT, seq_seg);
            utt.addSequence(SupportedSequenceType.PHONE, seq_ph);
            utt.addSequence(SupportedSequenceType.LABEL, seq_labels);

            // Add relation segment -> phone
            ArrayList<IntegerPair> relation_pairs = new ArrayList<IntegerPair>();
            for (int i=0; i<seq_seg.size(); i++)
                relation_pairs.add(new IntegerPair(i, i));
            utt.setRelation(SupportedSequenceType.SEGMENT, SupportedSequenceType.PHONE,
                            new Relation(seq_seg, seq_ph, relation_pairs));

            // Add the relation segment -> feature
            relation_pairs = new ArrayList<IntegerPair>();
            for (int i=0; i<seq_seg.size(); i++)
                relation_pairs.add(new IntegerPair(i, i));
            utt.setRelation(SupportedSequenceType.SEGMENT, SupportedSequenceType.LABEL,
                            new Relation(seq_seg, seq_labels, relation_pairs));

            return utt;

        } catch (MaryException ex) {
            throw new MaryIOException("couldn't deserialize labels", ex);
        }
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
