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
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class TriphoneNormaliser implements FeatureNormaliser
{
    protected ArrayList<String> dict;
    protected Alphabet ipa2arpa;
    protected String[] feature_names = {"prev_phone", "phone", "next_phone"};  // FIXME: hardcode

    public TriphoneNormaliser() throws Exception {

	// FIXME: initalize info to encode the phonem sequence ()
	dict = new ArrayList<String>();
	try (Stream<String> stream = Files.lines(Paths.get("/home/slemaguer/work/maintained_tools/src/dnn/dict"))) {

	    stream.forEach(dict::add);

	} catch (IOException e) {
	    throw e;
	}

	ipa2arpa = AlphabetFactory.getAlphabet("arpabet");

	// FIXME: hard coded

    }

    private int encodePhoneme(String ph) throws Exception {
	String arpa = "pau";
	if (! ph.equals("_"))
	    arpa = ipa2arpa.getLabelFromIPA(ph);
	return dict.indexOf(arpa.toLowerCase());
    }


    public ArrayList<String> getHeader() {
	ArrayList<String> header = new ArrayList<String>();
	for (String name: feature_names) {
	    for (String code: dict) {
		header.add(name + "_" + code);
	    }
	}
	return header;
    }

    public Tensor<Float> normalise(Sequence<FeatureMap> list_feature_map) throws MaryException {

	try {

	    // Get sizes
	    int feat_size = feature_names.length * dict.size();
	    float[][] normalised_vector = new float[list_feature_map.size()][feat_size];

	    // Adapt everything
	    for (int i=0; i<list_feature_map.size(); i++) {
		FeatureMap feature_map = list_feature_map.get(i);

		int j=0;
		for (String feature_name: feature_names) {
		    Feature cur = feature_map.get(feature_name);

		    if (cur != Feature.UNDEF_FEATURE) {
			int idx = encodePhoneme(cur.getStringValue());
			if (idx == -1)
			    throw new MaryException(String.format("%s leads to -1 (means not present in the dictionnary)", ipa2arpa.getLabelFromIPA(cur.getStringValue())));
			normalised_vector[i][j*dict.size()+idx] = 1;
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


