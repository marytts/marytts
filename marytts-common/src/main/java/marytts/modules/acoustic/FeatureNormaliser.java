package marytts.modules.acoustic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import marytts.features.FeatureMap;

import marytts.MaryException;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class FeatureNormaliser
{
    /** Everything in qs which is not key in qs_label_name is a continuous part => Just take the value! */
    protected ArrayList<String> qs_list;
    protected HashMap<String,String> qs_label_map;
    protected HashMap<String, Set<String>> qs_answers_map;

    // public FeatureNormaliser()
    // {
    // }

    public ArrayList<Float> normalise(FeatureMap feature_map) throws MaryException{
	ArrayList<Float> normalised_vector = new ArrayList<Float>();

	for (String qs: qs_list) {
	    // Label assessment
	    String label = qs_label_map.get(qs);
	    if (!feature_map.containsKey(label))
		throw new MaryException("Can't normalise: " + label + " is not par of the feature map!");

	    // Encode the "value"
	    if (qs_answers_map.containsKey(qs)) {
		Set<String> answers = qs_answers_map.get(qs);


		if (answers.contains(feature_map.get(label))) {
		    normalised_vector.add(1.0f);
		} else {
		    normalised_vector.add(0.0f);
		}
	    } else {
		normalised_vector.add(Float.parseFloat(feature_map.get(label).getStringValue())); // FIXME: string value
	    }
	}

	return normalised_vector;
    }
}


/* FeatureNormaliser.java ends here */
