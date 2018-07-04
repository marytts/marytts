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
 *  Default quinphone normaliser based on IPA information extracted from the IPA class
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class QuinphoneNormaliser implements FeatureNormaliser
{
    /** The names of the features used in the feature maps */
    protected String[] feature_names = {"prev_prev_phone", "prev_phone", "phone", "next_phone", "next_next_phone"};  // FIXME: hardcoded

    /** The code of the characteristic features used */
    protected ArrayList<String> feat_code = new ArrayList<String>();

    /**
     *  Default constructor
     *
     *  it just adds the default code into the code list.
     */
    public QuinphoneNormaliser() {
	feat_code.add("consonant");
	feat_code.add("plosive");
	feat_code.add("nasal");
	feat_code.add("trill");
	feat_code.add("tap");
	feat_code.add("flap");
	feat_code.add("fricative");
	feat_code.add("lateral");
	feat_code.add("approximant");
	feat_code.add("voiced");
	feat_code.add("vowel");
	feat_code.add("front");
	feat_code.add("central");
	feat_code.add("back");
	feat_code.add("close");
	feat_code.add("close-mid");
	feat_code.add("open-mid");
	feat_code.add("mid");
	feat_code.add("rounded");
    }

    @Override
    public ArrayList<String> getHeader() {
	ArrayList<String> header = new ArrayList<String>();
	for (String name: feature_names) {
	    for (String code: feat_code) {
		header.add(name + "_" + code);
	    }
	}
	return header;
    }

    /**
     *  Method to check if an ipa label is validating the property given by the code
     *
     *  @param ipa_label the ipa label to check
     *  @param code the property code
     *  @return true if the label has the property identified by the code, false else
     *  @throws MaryException if the given label is not a valid ipa label.
     */
    protected boolean validateCode(String ipa_label, String code) throws MaryException {

	Set<Character> ipa = IPA.cat_ipa_map.get(code);
	if (ipa == null) {
	    throw new MaryException(code + " is unknown!");
	}

	boolean found = false;
	int i = 0;
	while ((!found) && (i<ipa_label.length())) {
	    if (ipa.contains(ipa_label.charAt(i)))
		found = true;

	    i++;
	}

	return found;
    }


    /**
     *  The normalising method.

     *  This consists of generating a binary matrix with each vector corresponding to a frame. The
     *  vector is a hot vector of size nb_features*nb_code.
     *
     *  For a specific context (feature), a cell at 1.0f indicates that the corresponding phone
     *  label validate the code identified by the index of this cell.
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
			int idx = 0;
			for (String code: feat_code) {

			    // Check if IPA validate the code
			    if (validateCode(cur.getStringValue(), code))
				normalised_vector[i][j*feat_code.size()+idx] = 1;

			    idx++;
			}
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
