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
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class TSVSerializer implements Serializer {
	protected List<String> m_feature_names;
	protected static final String SEP = "\t";

	public TSVSerializer() {
		m_feature_names = FeatureComputer.the_feature_computer.listFeatures();
	}


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

	public Utterance fromString(String content) throws MaryIOException {
		throw new UnsupportedOperationException();
	}

	protected final String getValue(FeatureMap feature_map, String key) {
		return feature_map.get(key) == null ? "" : feature_map.get(key).getStringValue();
	}
}

/* HTSLabelSerializer.java ends here */
