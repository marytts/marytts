package marytts.io.serializer;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.Utterance;
import marytts.io.MaryIOException;
import marytts.data.SupportedSequenceType;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.File;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class ROOTSJSONSerializer implements Serializer {
	private final static int SB_INIT_CAP = 1000;

	public ROOTSJSONSerializer() {
	}


	public String toString(Utterance utt) throws MaryIOException {
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
			throw new MaryIOException(null, ex);
		}
	}

	public void appendSequences(Utterance utt, StringBuilder sb) throws Exception {
		SupportedSequenceType cur_type = null;
		Object[] types = utt.listAvailableSequences().toArray();
		for (int t = 0; t < types.length; t++) {
			cur_type = ((SupportedSequenceType) types[t]);
			sb.append("\t\t\"" + cur_type + "\": [\n");
			Sequence<Item> seq = (Sequence<Item>) utt.getSequence(cur_type);
			for (int i = 0; i < (seq.size() - 1); i++) {
				sb.append("\t\t\t");
				appendItem(seq.get(i), sb);
				sb.append(",\n");
			}
			sb.append("\t\t\t");
			appendItem(seq.get(seq.size() - 1), sb);
			sb.append("\n");

			if (t < (types.length - 1))
				sb.append("\t\t],\n");
			else
				sb.append("\t\t]\n");
		}
	}

	public void appendItem(Item it, StringBuilder sb) throws IntrospectionException {
		sb.append("{");
		for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(it.getClass()).getPropertyDescriptors()) {

			Method method = propertyDescriptor.getReadMethod();
			String method_name = propertyDescriptor.getReadMethod().toString();

			try {
				if ((method_name.indexOf("java.lang.Object") < 0)
						&& (method_name.indexOf("marytts.data.item.Item") < 0)) {
					Object value = propertyDescriptor.getReadMethod().invoke(it, (Object[]) null);

					if ((value != null) && (isWrapperType(value.getClass()))) {
						sb.append("\"" + propertyDescriptor.getReadMethod().toString() + "\": ");
						sb.append("\"" + value.toString() + "\",");
					}
				}
			} catch (IllegalAccessException iae) {
				// TODO
			} catch (IllegalArgumentException iaee) {
				// TODO
			} catch (InvocationTargetException ite) {
				// TODO
			}
		}

		sb.append("}");
	}

	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

	public static boolean isWrapperType(Class<?> clazz) {
		return WRAPPER_TYPES.contains(clazz);
	}

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

	public void appendRelations(Utterance utt, StringBuilder sb) {
		Object[] relations = utt.listAvailableRelations().toArray();
		ImmutablePair<SupportedSequenceType, SupportedSequenceType> cur_rel_id;
		SparseDoubleMatrix2D cur_rel;

		for (int r = 0; r < relations.length; r++) {
			sb.append("\t\t{\n");
			cur_rel_id = (ImmutablePair<SupportedSequenceType, SupportedSequenceType>) relations[r];
			sb.append("\t\t\t\"source\" : \"" + cur_rel_id.left + "\",\n");
			sb.append("\t\t\t\"target\" : \"" + cur_rel_id.right + "\",\n");

			cur_rel = utt.getRelation(cur_rel_id.left, cur_rel_id.right).getRelations();
			sb.append("\t\t\t \"matrix\" : [\n");
			for (int j = 0; j < cur_rel.rows(); j++) {
				sb.append("\t\t\t\t");
				for (int k = 0; k < cur_rel.columns(); k++)
					sb.append(cur_rel.get(j, k) + " ");
				sb.append(" ");
				sb.append("\n");
			}
			sb.append("\t\t\t]\n");

			if (r < (relations.length - 1))
				sb.append("\t\t},\n");
			else
				sb.append("\t\t}\n");
		}
	}

	public Utterance fromString(String content) throws MaryIOException {
		throw new UnsupportedOperationException();
	}

}

/* ROOTSJSONSerializer.java ends here */
