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
import java.util.Iterator;
import java.util.Map;
import java.io.File;

/**
 * Feature serializer to generate TSV format output. There is not import from it
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class TSVSerializer implements Serializer {

	/** The names of the features we are serializing */
	protected List<String> m_feature_names;

	/** The constant for representing the tab separator */
	protected static final String SEP = "\t";

	/**
	 * Constructor
	 *
	 */
	public TSVSerializer() {
		m_feature_names = FeatureComputer.the_feature_computer.listFeatures();
	}

	/**
	 * Generate the TSV output from the utterance. Only the feature sequence is
	 * used !
	 *
	 * @param utt
	 *            the utterance containing the feature sequence
	 * @return the TSV formatted feature sequence
	 * @throws MaryIOException
	 *             if anything is going wrong
	 */
	public String toString(Utterance utt) throws MaryIOException {
		if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
			throw new MaryIOException("Current utterance doesn't have any features. Check the module sequence", null);
		}

		Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES);

		// Header
		String output = "#";
		Iterator<String> it_names = m_feature_names.iterator();
		while (it_names.hasNext()) {
			String feature_name = it_names.next();
			output += feature_name;

			if (it_names.hasNext())
				output += SEP;
			else
				output += "\n";
		}

		// Content
		for (FeatureMap feature_map : seq_features) {
			it_names = m_feature_names.iterator();
			while (it_names.hasNext()) {
				String feature_name = it_names.next();
				output += getValue(feature_map, feature_name);

				if (it_names.hasNext())
					output += SEP;
				else
					output += "\n";
			}
		}

		return output;
	}

	/**
	 * Unsupported operation ! We can't import from a TSV formatted input.
	 *
	 * @param content
	 *            unused
	 * @return nothing
	 * @throws MaryIOException
	 *             never done
	 */
	public Utterance fromString(String content) throws MaryIOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Encapsulation to get the value given the key of the feature map
	 *
	 * @param feature_map
	 *            the feature map
	 * @param feature_name
	 *            the feature name
	 * @return the value of the feature or an empty string if not defined
	 */
	protected final String getValue(FeatureMap feature_map, String feature_name) {
		return feature_map.get(feature_name) == null ? "" : feature_map.get(feature_name).getStringValue();
	}
}

/* HTSLabelSerializer.java ends here */
