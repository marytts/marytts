package marytts.io.serializer.label;

/* Utils */
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/* Data */
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Phone;
import marytts.data.SupportedSequenceType;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.Utterance;

/* Features */
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.features.FeatureComputer; // FIXME: for now we use the static feature computer....

/* I/O */
import java.io.File;
import marytts.io.MaryIOException;
import marytts.io.serializer.Serializer;


/**
 * Serializer to generate easily an HTS label file. The format is different from
 * HTS standard format. Indeed, the first feature is the phone separated by
 * -phone+ as it is mandatory for HTS. Then, each feature i is surrounded by
 * +Si-1E_feat+SiE_.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class HTSLabelSerializer implements Serializer {
    /** Constant to define the undefined value */
    public static final String UNDEF = "x";

    /** Left part of the separator */
    public static final String LEFT_SEP = "+S";

    /** Right part of the separator */
    public static final String RIGHT_SEP = "E_";

    /** Phone feature name in the map (this is forced in featurecomputer !) */
    protected static final String PHONE_FEATURE_NAME = "phone";

    /** List of the available feature names */
    protected List<String> m_feature_names;

    /**
     * Constructor
     *
     */
    public HTSLabelSerializer() {
    }

    /**
     * Generate HTS compatibale labels from the utterance
     *
     * @param utt
     *            the utterance
     * @return the HTS compatible labels
     * @throws MaryIOException
     *             when something wrong is happening
     */
    public Object export(Utterance utt) throws MaryIOException {

        if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
            throw new MaryIOException("Current utterance doesn't have any features. Check the module sequence",
                                      null);
        }

        m_feature_names = (List<String>) utt.getFeatureNames().clone();
        m_feature_names.remove(PHONE_FEATURE_NAME);

        Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(
                                                SupportedSequenceType.FEATURES);
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
    public Utterance load(String content) throws MaryIOException {
        throw new UnsupportedOperationException();
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
        return ((feature_map.containsKey(feature_name)) &&
                (feature_map.get(feature_name) != Feature.UNDEF_FEATURE))
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
        // Phoneme format
        String cur_lab = "-" + getValue(feature_map, PHONE_FEATURE_NAME) + LEFT_SEP;

        int i = 0;
        for (String feat : m_feature_names) {
            cur_lab += i + RIGHT_SEP + getValue(feature_map, feat) + LEFT_SEP;
            i++;
        }

        cur_lab += i + RIGHT_SEP;

        return cur_lab;
    }
}
