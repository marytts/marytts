package marytts.io.serializer;


/* Regexp */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Reflections part */
import java.lang.reflect.Constructor;

/* Introspection part */
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* MaryTTS data part */
import marytts.MaryException;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

/* IO part */
import marytts.io.MaryIOException;
import java.io.File;

/* Utils part */
import java.util.Locale;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import org.apache.commons.lang3.tuple.ImmutablePair;

/* SparseMatrix */
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

/* JSON part */
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * ROOTS JSON serializer. This serializer is the most accurate considering the
 * topology of the
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class ROOTSJSONSerializer implements Serializer {
    /** The string builder initial capacity */
    private final static int SB_INIT_CAP = 1000;

    /** The set of wrappers for primitive types */
    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    /**
     * Method to check if the given class is a wrapper one
     *
     * @param clazz
     *            the class to check
     * @return true if clazz is a wrapper for primitive types
     */
    public static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    /**
     * List wrapper types
     *
     * @return the set of wrapper classes
     */
    private static Set<Class<?>> getWrapperTypes() {
        Set<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        ret.add(String.class);
        return ret;
    }

    /**
     * Constructor
     *
     */
    public ROOTSJSONSerializer() {
    }


    /************************************************************************************************
     ** Exporting
     ************************************************************************************************/

    /**
     * Generate the JSON formatted string of the ROOTS utterance
     *
     * @param utt
     *            the utterance to export
     * @return the JSON formatted string
     * @throws MaryIOException
     *             if anything is going wrong
     */
    public Object export(Utterance utt) throws MaryIOException {
        try {

	    JSONObject obj = new JSONObject();
	    obj.put("sequences", exportSequences(utt));
	    obj.put("relations", exportRelations(utt));

            return obj.toJSONString();
        } catch (Exception ex) {

            throw new MaryIOException("Cannot serialize utt", ex);
        }
    }

    /**
     * Export the sequences of the given utterance to the string builder in a
     * JSON format
     *
     * @param utt
     *            the given utterance
     * @throws Exception
     *             any kind of exception
     */
    protected Object exportSequences(Utterance utt) throws Exception {
        String cur_type = null;
        Object[] types = utt.listAvailableSequences().toArray();

	JSONObject hash_seq = new JSONObject();
        for (int t = 0; t < types.length; t++) {

	    // Get the type of the sequence
            cur_type = ((String) types[t]);

	    // Get the "root class" of the sequence
	    JSONObject seq = new JSONObject();

	    // Generate json array from the sequence
	    JSONArray seq_arr = new JSONArray();
	    for (Item i: (Sequence<Item>) utt.getSequence(cur_type))
		seq_arr.add(exportItem(i));
	    seq.put("items", seq_arr);

	    // // FIXME: get sequence main class type
	    // seq.put("type", utt.getSequence(cur_type).getPersistentClass().getName());

	    // Add the sequence to the hash
	    hash_seq.put(cur_type, seq);
	}

	return hash_seq;
    }

    /**
     * Export the given item to the string builder in a JSON format
     *
     * @param it
     *            the given item
     * @throws Exception
     *             any kind of exception
     */
    protected Object exportItem(Item it) throws Exception {

	JSONObject json_item = new JSONObject();
	json_item.put("class", it.getClass().getName());

	// Serialize item using gson
	Gson gson = new Gson();
	String json = gson.toJson(it);

	// Add the json item to the serialized object
	json_item.put("item", new JSONParser().parse(json));

	return json_item;
    }

    // public JSONObject exportObject()

    /**
     * Export the relations of the given utterance to the string builder in a
     * JSON format
     *
     * @param utt
     *            the given utterance
     * @param sb
     *            the string builder
     */
    protected Object exportRelations(Utterance utt) throws MaryException {
        SparseDoubleMatrix2D cur_rel_mat;

	JSONArray relations = new JSONArray();
	for (ImmutablePair<String, String> cur_rel_id: utt.listAvailableRelations()) {
	    JSONObject cur_rel = new JSONObject();
	    cur_rel.put("source", cur_rel_id.left);
	    cur_rel.put("target", cur_rel_id.right);

            cur_rel_mat = utt.getRelation(cur_rel_id.left, cur_rel_id.right).getRelations();
	    JSONArray mat = new JSONArray();
            for (int j = 0; j < cur_rel_mat.rows(); j++) {

                for (int k = 0; k < cur_rel_mat.columns(); k++) {
		    double elt = cur_rel_mat.getQuick(j, k);

		    if (elt > 0) {
			JSONArray cur_row = new JSONArray();
			cur_row.add(j);
			cur_row.add(k);
			cur_row.add(elt);

			mat.add(cur_row);
		    }
                }
            }

	    cur_rel.put("matrix", mat);
	    relations.add(cur_rel);
        }

	return relations;
    }


    /************************************************************************************************
     ** Importing
     ************************************************************************************************/

    /**
     * Generate an utterance from the ROOTS json information stored in the
     * string format. For now, it is not supported.
     *
     * @param content
     *            the json roots content
     * @return the created utterance
     * @throws MaryIOException
     *             if anything is going wrong
     */
    public Utterance load(String content) throws MaryIOException {
	try {
	    // Initialisation
	    Utterance utt = new Utterance();
	    JSONObject root_utt = (JSONObject) new JSONParser().parse(content);

	    // First load the sequences
	    loadSequences((JSONObject) root_utt.get("sequences"), utt);

	    // Then load the relations
	    loadRelations((JSONArray) root_utt.get("relations"), utt);


	    return utt;
	} catch (Exception ex) {
	    throw new MaryIOException("Cannot load utterance", ex);
	}
    }

    /**
     * Load the relations into the given utterance
     *
     * @param relations the relations in JSON format
     * @param utt the given utterance
     * @throws Exception if anything is going wrong
     */
    public void loadRelations(JSONArray relations, Utterance utt) throws Exception {
	for (Object rel_ob: relations) {
	    JSONObject rel_json = (JSONObject) rel_ob;

	    Sequence<Item> source = (Sequence<Item>) utt.getSequence((String) rel_json.get("source"));
	    Sequence<Item> target = (Sequence<Item>) utt.getSequence((String) rel_json.get("target"));

	    SparseDoubleMatrix2D matrix = new SparseDoubleMatrix2D(source.size(), target.size());
	    for (Object rel_elts: (JSONArray) rel_json.get("matrix")) {
		long x = (long) ((JSONArray) rel_elts).get(0);
		long y = (long) ((JSONArray) rel_elts).get(1);
		double z = (double) ((JSONArray) rel_elts).get(2);
		matrix.setQuick((int) x, (int) y, z);
	    }

	    Relation rel = new Relation(source, target, matrix);

	    utt.setRelation((String) rel_json.get("source"),
			    (String) rel_json.get("target"),
			    rel);
	}
    }

    /**
     * Load the sequences into the given utterance
     *
     * @param sequences the sequences in JSON format
     * @param utt the given utterance
     * @throws Exception if anything is going wrong
     */
    public void loadSequences(JSONObject sequences, Utterance utt) throws Exception {
	for (Object k: sequences.keySet()) {
	    JSONObject the_seq = (JSONObject) sequences.get(k);
	    // FIXME: find a way to force the type !
	    Sequence<Item> seq_item = new Sequence<Item>();
	    for (Object item: (JSONArray) the_seq.get("items")) {
		seq_item.add(loadItem((JSONObject) item));
	    }
	    utt.addSequence((String) k, seq_item);
	}
    }

    /**
     * Load the item
     *
     * @param item the item in JSON format
     * @return the generated item
     * @throws Exception if anything is going wrong
     */
    public Item loadItem(JSONObject item) throws Exception {

	Gson gson = new Gson();
	return (Item) gson.fromJson(((JSONObject) item.get("item")).toJSONString(),
				    (Class) Class.forName((String) item.get("class")));
    }

    /**
     *  Internal method to transform a property name to its setter name
     *
     *  By convention the first charatecter is always capitalized as all characters prefixed by
     *  underscores. For example, prop_name is going to be transformed in setPropName.
     *
     *  @param property_name the name of the property for which we want the setter method
     *  @return the setter method name
     */
    protected String adaptPropertyName(String property_name) {
	// Upper case the first letter
	property_name = property_name.substring(0,1).toUpperCase() + property_name.substring(1).toLowerCase();

	// Upper case the character prefixed by underscores
	StringBuffer result = new StringBuffer();
	Matcher m = Pattern.compile("_(\\w)").matcher(property_name);
	while (m.find()) {
	    m.appendReplacement(result,
				m.group(1).toUpperCase());
	}
	m.appendTail(result);

	return result.toString();
    }
}
