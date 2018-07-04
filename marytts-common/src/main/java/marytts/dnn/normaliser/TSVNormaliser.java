package marytts.dnn.normaliser;

// Regexp
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Numeric
import org.tensorflow.Tensor;

// Collections
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

// File / Streams
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

// Mary part
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
public class TSVNormaliser implements FeatureNormaliser
{
    protected final String SEP = "\t";
    protected HashMap<String, ArrayList<String>> map_id_answers;
    protected Set<String> cts_ids;
    protected HashMap<String, String> map_id_feature;
    protected ArrayList<String> list_ids;


    public TSVNormaliser(String tsv_filename) throws IOException, MaryException {
        List<String> lines = Files.readAllLines(Paths.get(tsv_filename), StandardCharsets.UTF_8);
        loadInformations(lines);
    }

    public TSVNormaliser(InputStream tsv_stream) throws IOException, MaryException {
        List<String> lines =
            new BufferedReader(new InputStreamReader(tsv_stream,
                                                     StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
        loadInformations(lines);
    }

    @Override
    public ArrayList<String> getHeader() {
        return list_ids;
    }

    protected void loadInformations(List<String> lines) throws MaryException {
        for (String line: lines) {
            String[] elts = line.split(SEP);

            // Id
            String id = elts[0];
            list_ids.add(id);

            // Feature
            map_id_feature.put(id, elts[1]);

            // Parse answer
            if (elts[2].equals("CONT")) { // Continuous
                cts_ids.add(id);
            } else if (elts[2].equals("DISC")) { // Answers (if discrete)
                ArrayList<String> ans = new ArrayList<String>();
                for (int i=3; i<elts.length; i++) {
                    ans.add(elts[i]);
                }
                map_id_answers.put(id, ans);
            } else {
                throw new MaryException(elts[2] + " is an unknown type. It should be (CONT or DISC)");
            }
	}
    }

    /**
     *  The normalising method.
     *
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

	    // Initialize size and array
            int feat_size = 0;
            for (String id: list_ids) {
                if (cts_ids.contains(id)) {
                    feat_size += 1;
                } else {
                    feat_size += map_id_answers.get(id).size();
                }
            }
	    float[][] normalised_vector = new float[list_feature_map.size()][feat_size];

            // Fill matrix
	    for (int i=0; i<list_feature_map.size(); i++) {
		FeatureMap feature_map = list_feature_map.get(i);
                int shift = 0;
                for (String id: list_ids) {
                    if (cts_ids.contains(id)) { // continuous => we are suppose to get a float value
                        normalised_vector[i][shift] = ((Number) feature_map.get(map_id_feature.get(id)).getValue()).floatValue();
                        shift++;
                    } else { // discrete => hot vector !
                        int idx = map_id_answers.get(id).indexOf(feature_map.get(map_id_feature.get(id)).getStringValue());
                        if (idx >= 0) {
                            normalised_vector[i][shift+idx] = 1.0f;
                        }
                        shift += map_id_answers.get(id).size();
                    }
                }
            }

	    return Tensor.create(normalised_vector, Float.class);

	} catch (Exception ex) {
	    throw new MaryException("Problem with encoding", ex);
	}
    }
}
