package marytts.dnn.normaliser;

import marytts.dnn.FeatureNormaliser;

import org.tensorflow.Tensor;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import marytts.data.Sequence;


import marytts.phonetic.converter.Alphabet;
import marytts.phonetic.AlphabetFactory;

// File
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import marytts.features.FeatureMap;
import marytts.features.Feature;

import marytts.MaryException;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class TriphoneNormaliser extends FeatureNormaliser
{
    private ArrayList<String> dict;
    private Alphabet ipa2arpa;

    public TriphoneNormaliser() throws Exception {

	// FIXME: initalize info to encode the phonem sequence ()
	dict = new ArrayList<String>();
	try (Stream<String> stream = Files.lines(Paths.get("/home/slemaguer/work/maintained_tools/src/dnn/dict"))) {

	    stream.forEach(dict::add);

	} catch (IOException e) {
	    throw e;
	}

	ipa2arpa = AlphabetFactory.getAlphabet("arpabet");
    }


    private float encodePhoneme(String ph) throws Exception {
	String arpa = "pau";
	if (! ph.equals("_"))
	    arpa = ipa2arpa.getLabelFromIPA(ph);
	return dict.indexOf(arpa);
    }


    public Tensor<Float> normalise(Sequence<FeatureMap> list_feature_map) throws MaryException {

	try {
	    // Get sizes
	    int feat_size = 3;
	    float[][] normalised_vector = new float[list_feature_map.size()][feat_size];

	    // FIXME: hard coded
	    String[] feature_names = {"phone", "prev_phone", "next_phone"};

	    // Adapt everything
	    for (int i=0; i<list_feature_map.size(); i++) {
		FeatureMap feature_map = list_feature_map.get(i);

		for (String feature_name: feature_names) {
		    Feature cur = feature_map.get(feature_name);
		    if (cur == Feature.UNDEF_FEATURE) {
			normalised_vector[i][0] = 0f;
		    } else {
			normalised_vector[i][0] = encodePhoneme(cur.getStringValue());
		    }
		}

	    }
	    return Tensor.create(normalised_vector, Float.class);

	} catch (Exception ex) {
	    throw new MaryException("Problem with encoding", ex);
	}
    }
}


/* FeatureNormaliser.java ends here */
