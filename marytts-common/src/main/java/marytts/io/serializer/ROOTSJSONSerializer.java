package marytts.io.serializer;

/* Introspection part */
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* MaryTTS data part */
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
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import org.apache.commons.lang3.tuple.ImmutablePair;

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
        StringBuilder sb = new StringBuilder(SB_INIT_CAP);
        try {
            sb.append("{\n");
            sb.append("\t\"sequences\": {\n");
            appendSequences(utt, sb);
            sb.append("\t},\n");

            // Dump relation
            sb.append("\t\"relations\": [\n");
            appendRelations(utt, sb);
            sb.append("\t]\n");
            sb.append("}\n");

            return sb.toString();
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
     * @param sb
     *            the string builder
     * @throws Exception
     *             any kind of exception
     */
    protected void appendSequences(Utterance utt, StringBuilder sb) throws Exception {
        String cur_type = null;
        Object[] types = utt.listAvailableSequences().toArray();
        for (int t = 0; t < types.length; t++) {
            cur_type = ((String) types[t]);
            sb.append("\t\t\"" + cur_type + "\": [\n");
            Sequence<Item> seq = (Sequence<Item>) utt.getSequence(cur_type);
            int s = 0;
            while (s < (seq.size() - 1)) {
                sb.append("\t\t\t");
                appendItem(seq.get(s), sb);
                sb.append(",\n");
                s++;
            }

            if (s < seq.size()) {
                sb.append("\t\t\t");
                appendItem(seq.get(seq.size() - 1), sb);
            }
            sb.append("\n");

            if (t < (types.length - 1)) {
                sb.append("\t\t],\n");
            } else {
                sb.append("\t\t]\n");
            }
        }
    }

    /**
     * Export the given item to the string builder in a JSON format
     *
     * @param it
     *            the given item
     * @param sb
     *            the string builder
     * @throws Exception
     *             any kind of exception
     */
    protected void appendItem(Item it, StringBuilder sb) throws Exception {
        Hashtable<String, String> methods = new Hashtable<String, String>();
        sb.append("{ ");
        for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(it.getClass()).getPropertyDescriptors()) {
            Method method = propertyDescriptor.getReadMethod();
            if (method != null) {
                String method_name = method.toString();

                if ((method_name.indexOf("java.lang.Object") < 0) &&
                        (method_name.indexOf("marytts.data.item.Item") < 0)) {
                    Object value = method.invoke(it, (Object[]) null);

                    if ((value != null) && (isWrapperType(value.getClass()))) {
                        methods.put(method.getName().replaceFirst("get", "").toLowerCase(),
                                    value.toString());
                    }
                }
            }
        }

        ArrayList<String> keys = new ArrayList<String>(methods.keySet());
        int i = 0;
        while (i < (keys.size() - 1)) {
            // Append method informations
            sb.append("\"" + keys.get(i) + "\": ");
            sb.append("\"" + methods.get(keys.get(i))  + "\", ");
            i++;
        }

        if (i < keys.size()) {
            sb.append("\"" + keys.get(i) + "\": ");
            sb.append("\"" + methods.get(keys.get(i)) + "\"");
        }

        sb.append(" }");
    }

    /**
     * Export the relations of the given utterance to the string builder in a
     * JSON format
     *
     * @param utt
     *            the given utterance
     * @param sb
     *            the string builder
     */
    protected void appendRelations(Utterance utt, StringBuilder sb) {
        Object[] relations = utt.listAvailableRelations().toArray();
        ImmutablePair<String, String> cur_rel_id;
        SparseDoubleMatrix2D cur_rel;

        for (int r = 0; r < relations.length; r++) {
            sb.append("\t\t{\n");
            cur_rel_id = (ImmutablePair<String, String>) relations[r];
            sb.append("\t\t\t\"source\" : \"" + cur_rel_id.left + "\",\n");
            sb.append("\t\t\t\"target\" : \"" + cur_rel_id.right + "\",\n");

            cur_rel = utt.getRelation(cur_rel_id.left, cur_rel_id.right).getRelations();
            sb.append("\t\t\t \"matrix\" : [\n");
            for (int j = 0; j < cur_rel.rows(); j++) {
                sb.append("\t\t\t\t");
                for (int k = 0; k < cur_rel.columns(); k++) {
                    sb.append(cur_rel.get(j, k));
                    if ((j < (cur_rel.rows() - 1)) ||
                            (k < (cur_rel.columns() - 1))) {
                        sb.append(", ");
                    }
                }
                sb.append("\n");
            }
            sb.append("\t\t\t]\n");

            if (r < (relations.length - 1)) {
                sb.append("\t\t},\n");
            } else {
                sb.append("\t\t}\n");
            }
        }
    }

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
        throw new UnsupportedOperationException();
    }
}

/* ROOTSJSONSerializer.java ends here */
