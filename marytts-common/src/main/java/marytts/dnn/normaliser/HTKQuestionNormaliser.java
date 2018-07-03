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

// File
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

// Mary part
import marytts.data.Sequence;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.dnn.FeatureNormaliser;
import marytts.MaryException;

/**
 *  Default quinphone normaliser based on IPA information extracted from the IPA class
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class HTKQuestionNormaliser implements FeatureNormaliser
{
    protected final String FEAT_QS_SEP = "==";
    protected final String POS_SEP = "#";
    protected HashMap<String, ArrayList<String>> qs_map;
    protected Set<String> cqs_set;
    protected ArrayList<String> list_questions;


    public HTKQuestionNormaliser(String qs_filename) throws IOException, MaryException {
        parseQuestionFile(qs_filename);
    }

    @Override
    public ArrayList<String> getHeader() {
	ArrayList<String> header = new ArrayList<String>();
	for (String qs: list_questions) {
            if (cqs_set.contains(qs)) {
                header.add(qs);
            } else if (qs_map.containsKey(qs)) {
                for (String ans: qs_map.get(qs)) {
                    header.add(qs + POS_SEP + ans);
                }
            }
	}

	return header;
    }

    protected void parseQuestionFile(String filename) throws IOException, MaryException {

        List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.startsWith("QS")) {
                parseQS(line);
            } else if (line.startsWith("CQS")) {
                parseCQS(line);
            } else if (!line.startsWith("#") && !line.isEmpty()) {
                throw new MaryException("the following line is not valid: " + line);
            }
	}
    }

    protected void parseCQS(String line) throws MaryException {
        String pattern = "QS[ \t]*\"([^\"]*)\"[ \t]*.*";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);

        if (m.find()) {
            String qs = m.group(0);
            list_questions.add(qs);
            cqs_set.add(qs);
        }
    }

    protected void parseQS(String line) throws MaryException {
        String pattern = "QS[ \t]*\"([^\"]*)\"[ \t]*\\{(.*)\\}";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);

        if (m.find()) {

            // Get answers
            String ans_str = m.group(1).replaceAll("/[A-Z]:", "/:"); // FIXME: indicate in the doc that "/[A-Z]:" are the only exception !
            String[] ans_arr = ans_str.split(",");

            // Extract answers
            ArrayList<String> ans_list = new ArrayList<String>();
            Pattern p_answer = Pattern.compile("[^a-zA-Z0-9]*([^a-zA-Z0-9]*)[^a-zA-Z0-9].*");
            for (String ans: ans_arr) {
                Matcher m_answer = p_answer.matcher(ans);
                if (m.find()) {
                    ans_list.add(m_answer.group(0));
                } else {
                    throw new MaryException("\"" + ans + "\" is not a valid answer");
                }
            }

            // Adding the questions
            String qs = m.group(0);
            list_questions.add(qs);
            qs_map.put(qs, ans_list);


        } else {
            throw new MaryException("the following is not a valid question file line " + line);
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
            for (String qs: list_questions) {
                if (cqs_set.contains(qs)) {
                    feat_size += 1;
                } else if (qs_map.containsKey(qs)) {
                    feat_size += qs_map.get(qs).size();
                } else {
                    throw new MaryException(qs + " is not available in qs map an cqs set");
                }
            }
	    float[][] normalised_vector = new float[list_feature_map.size()][feat_size];

            // Fill matrix
	    for (int i=0; i<list_feature_map.size(); i++) {
		FeatureMap feature_map = list_feature_map.get(i);
                int shift = 0;
                for (String qs: list_questions) {
                    if (cqs_set.contains(qs)) { // CQS => we are suppose to get a float value
                        normalised_vector[i][shift] = ((Number) feature_map.get(qs).getValue()).floatValue();
                        shift++;
                    } else if (qs_map.containsKey(qs)) { // QS => hot vector !
                        int idx = qs_map.get(qs).indexOf(feature_map.get(qs).getStringValue());
                        if (idx >= 0) {
                            normalised_vector[i][shift+idx] = 1.0f;
                        }
                        shift += qs_map.get(qs).size();
                    }
                }
            }

	    return Tensor.create(normalised_vector, Float.class);

	} catch (Exception ex) {
	    throw new MaryException("Problem with encoding", ex);
	}
    }
}
