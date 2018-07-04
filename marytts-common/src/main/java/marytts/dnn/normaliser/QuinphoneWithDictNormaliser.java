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
import marytts.data.Sequence;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.dnn.FeatureNormaliser;
import marytts.MaryException;

/**
 *  A quinphone normaliser based on a dictionary of labels.
 *
 *  This implies that all the possible labels should be in the dictionary!
 *
 *  @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class QuinphoneWithDictNormaliser extends QuinphoneNormaliser
{
    /**
     *  Default constructor is deactivated !
     *
     */
    public QuinphoneWithDictNormaliser() {
        throw new UnsupportedOperationException();
    }

    /**
     *  Proper constructor which needs a dictionary file name. The dictionary is loaded as the code.
     *
     *  @param dict_filename the filename of the dictionary
     *  @throws IOException if the dictionary can not be open
     */
    public QuinphoneWithDictNormaliser(String dict_filename) throws IOException {
	try (Stream<String> stream = Files.lines(Paths.get(dict_filename))) {
	    stream.forEach(feat_code::add);
	}
    }


    /**
     *  The normalising method.
     *
     *  This consists of generating a binary matrix with each vector * corresponding to a frame. The
     *  vector is a hot vector of size nb_features*nb_code.
     *
     *  For a specific context (feature), a cell of this vector at 1.0f indicates that the label
     *  associated corresponds to the label of the dictionary of the same index.
     *
     *  @param list_feature_map the feature maps
     *  @return the binary matrix
     *  @throw MaryException if anything is going wrong
     */
    @Override
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
			    throw new MaryException(cur.getStringValue() + " is not in the given dictionary");
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
