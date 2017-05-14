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

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.io.File;

/**
 * Serializer to export a JSON formatted map of features
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class JSONFeatureSerializer implements Serializer {

	/**
	 * Constructor
	 *
	 */
	public JSONFeatureSerializer() {
		;
	}

	/**
	 * Generate the JSON formatted map of features from the given utterance
	 *
	 * @param utt
	 *            the utterance
	 * @return the JSON formatted string of the map of the features
	 * @throws MaryIOException
	 *             when something goes wrong
	 */
	public String toString(Utterance utt) throws MaryIOException {

		// Check that the sequence of features exists
		if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
			throw new MaryIOException("Current utterance doesn't have any features. Check the module sequence", null);
		}

		// Initialize
		Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES);
		String output = "[";
		int i_seg = 0;
		int nb_segs = seq_features.size();

		for (FeatureMap map : seq_features) {
			Map<String, Feature> real_map = map.getMap();
			Set<String> key_set = real_map.keySet();
			int nb_features = key_set.size();
			int i_feat = 0;

			// Generate a JSON map for eache feature map
			output += "{";
			for (String k : key_set) {
				output += "\"" + k + "\":\"" + real_map.get(k).getStringValue() + "\"";

				if ((i_feat + 1) < nb_features)
					output += ",";

				i_feat++;
			}

			if ((i_seg + 1) == nb_segs)
				output += "}\n";
			else
				output += "},\n";

			i_seg++;
		}
		output += "]\n";
		return output;
	}

	public Utterance fromString(String content) throws MaryIOException {
		throw new UnsupportedOperationException();
	}
}

/* JSONFeatureSerializer.java ends here */
