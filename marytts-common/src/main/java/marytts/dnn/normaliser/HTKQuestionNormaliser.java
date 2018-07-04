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
 *  Normaliser based on an HTK Question file.
 *
 *  Some constraint are supposed to be encoded in the file
 *    - the question name is formated as &lt;feature name&gt;==&lt;something&gt;
 *    - values should be only alphanumerical values
 *    - separator should not contains any alphanumerical information except if it is validating the following regexp /[A-Z]:
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class HTKQuestionNormaliser implements FeatureNormaliser
{
    /** Separator between feature id and anything else from the question filename */
    protected final String FEAT_QS_SEP = "==";

    /** Separator for identifying the value position in the debug header */
    protected final String POS_SEP = "#";

    /** Discrete question/answers map */
    protected HashMap<String, ArrayList<String>> qs_map;

    /** Continuous question set */
    protected Set<String> cqs_set;

    /** Vector of questions */
    protected ArrayList<String> list_questions;

    /**
     *  File constructor
     *
     *  @param qs_filename the filename
     *  @throws IOException if there is a problem with the loading of the file
     *  @throws MaryException if there is a problem with the parsing
     */
    public HTKQuestionNormaliser(String qs_filename) throws IOException, MaryException {
        List<String> lines = Files.readAllLines(Paths.get(qs_filename), StandardCharsets.UTF_8);
        parseQuestionLines(lines);
    }


    /**
     *  Stream constructor
     *
     *  @param qs_stream the input stream
     *  @throws IOException if there is a problem with the loading of the stream
     *  @throws MaryException if there is a problem with the parsing
     */
    public HTKQuestionNormaliser(InputStream qs_stream) throws IOException, MaryException {
        List<String> lines =
            new BufferedReader(new InputStreamReader(qs_stream,
                                                     StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
        parseQuestionLines(lines);
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

    /**
     *  Parsing question lines and fill the content of the normaliser
     *
     *  @param lines the lines to parse
     *  @throws MaryException if there is a problem with the parsing
     */
    protected void parseQuestionLines(List<String> lines) throws MaryException {

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

    /**
     *  Parsing continuous question line
     *
     *  @param line the line to parse
     *  @throws MaryException if there is a problem with the parsing
     */
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

    /**
     *  Parsing discrete question line
     *
     *  The format of the question is QS "&lt;name&gt;" {&lt;val&gt;[,&lt;val&gt;]*} with the following constraints
     *    - the question name is formated as &lt;feature name&gt;==&lt;something&gt;
     *    - values should be only alphanumerical values
     *    - static eparator should not contains any alphanumerical information except if it is validating the following regexp /[A-Z]:
     *
     *  @param line the line to parse
     *  @throws MaryException if there is a problem with the parsing
     */
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
     *  vector is a mix of discrete hot vector concatenated with continous values. Each part depends
     *  on the type and the answers if associated to the discrete question.
     *
     *  For discrete value, as
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
