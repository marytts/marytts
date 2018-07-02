package marytts.dnn.normaliser;

// Numeric
import org.tensorflow.Tensor;

// Collections
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;


// File
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

// Mary part
import marytts.phonetic.IPA;
import marytts.data.Sequence;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.dnn.FeatureNormaliser;
import marytts.MaryException;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class QuinphoneWithDictNormaliser implements FeatureNormaliser
{
    protected String[] feature_names = {"prev_prev_phone", "prev_phone", "phone", "next_phone", "next_next_phone"};  // FIXME: hardcode
    protected ArrayList<String> feat_code = new ArrayList<String>();

    public QuinphoneWithDictNormaliser(String dict_filename) throws Exception {
	try (Stream<String> stream = Files.lines(Paths.get(dict_filename))) {
	    stream.forEach(feat_code::add);
	}
    }

    public ArrayList<String> getHeader() {
	ArrayList<String> header = new ArrayList<String>();
	for (String name: feature_names) {
	    for (String code: feat_code) {
		header.add(name + "_" + code);
	    }
	}
	return header;
    }

    public Tensor<Float> normalise(Sequence<FeatureMap> list_feature_map) throws MaryException {

	try {

	    // Get sizes
	    int feat_size = feature_names.length * feat_code.size();
	    float[][] normalised_vector = new float[list_feature_map.size()][feat_size];

	    // Adapt everything
	    for (int i=0; i<list_feature_map.size(); i++) {
		FeatureMap feature_map = list_feature_map.get(i);

		int j=0;
		for (String feature_name: feature_names) {
		    Feature cur = feature_map.get(feature_name);

		    if (cur != Feature.UNDEF_FEATURE) {
			int idx = feat_code.indexOf(cur.getStringValue());
			if (idx >= 0)
			    normalised_vector[i][j*feat_code.size()+idx] = 1.0f;
			else
			    throw new MaryException(cur.getStringValue() + " is not in the given dictionnary");
		    }

		    j++;
		}
	    }

	    return Tensor.create(normalised_vector, Float.class);

	} catch (Exception ex) {
	    throw new MaryException("Problem with encoding", ex);
	}
    }
}


/* QuinphoneNormaliser.java ends here */
