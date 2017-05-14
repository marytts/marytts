package marytts.io.serializer;

import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Phone;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.data.Utterance;
import marytts.io.MaryIOException;
import marytts.data.SupportedSequenceType;

// FIXME: for now we use the static feature computer....
import marytts.features.FeatureComputer;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.io.File;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class HTSLabelSerializer implements Serializer {
	public static final String UNDEF = "x";
	public static final String LEFT_SEP = "+S";
	public static final String RIGHT_SEP = "E_";
	protected static final String PHONE_FEATURE_NAME = "phone";
	protected List<String> m_feature_names;

	public HTSLabelSerializer() {
		m_feature_names = FeatureComputer.the_feature_computer.listFeatures();
		m_feature_names.remove(PHONE_FEATURE_NAME);
	}


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

	public Utterance fromString(String content) throws MaryIOException {
		throw new UnsupportedOperationException();
	}

	protected final String getValue(FeatureMap feature_map, String key) {
		return ((feature_map.containsKey(key)) && (feature_map.get(key) != Feature.UNDEF_FEATURE))
				? feature_map.get(key).getStringValue()
				: UNDEF;
	}

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

/* HTSLabelSerializer.java ends here */
