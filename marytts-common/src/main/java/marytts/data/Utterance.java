package marytts.data;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.ImmutablePair;

import org.apache.log4j.Logger; // FIXME: not really happy with that

import javax.sound.sampled.AudioInputStream;

import marytts.data.item.linguistic.Paragraph;
import marytts.data.item.linguistic.Word;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.prosody.Phrase;

import marytts.data.item.Item;
import marytts.util.MaryUtils;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Utterance {
	/** The voice used to fill the utterance */
	private String m_voice_name;

	/** The original text */
	private String m_text;

	/** The locale used to fill the utterance */
	private Locale m_locale;

	/** Not used for now ! */
	private ArrayList<AudioInputStream> m_list_streams;

	/**
	 * The sequences which contains the data of the utterance. Organized by type
	 * for now
	 */
	private Hashtable<SupportedSequenceType, Sequence<? extends Item>> m_sequences;

	/** The relation graph to link te sequences */
	private RelationGraph m_relation_graph;

	/** The set of "not computed" relation based on types */
	private Set<ImmutablePair<SupportedSequenceType, SupportedSequenceType>> m_available_relation_set;

	/** The logger of the utterance */
	protected Logger logger;

	/**
	 * The constructor of the utterance which forces to define a text and an
	 * associated locale
	 *
	 * @param text
	 *            the original text of the utterance
	 * @param locale
	 *            the locale used for this utterance
	 */
	public Utterance(String text, Locale locale) {
		setVoice(null);
		setText(text);
		setLocale(locale);

		m_sequences = new Hashtable<SupportedSequenceType, Sequence<? extends Item>>();
		m_available_relation_set = new HashSet<ImmutablePair<SupportedSequenceType, SupportedSequenceType>>();
		m_relation_graph = new RelationGraph();

		// FIXME: have to be more consistent
		logger = MaryUtils.getLogger("Utterance");
	}

	/******************************************************************************************************************************************
	 ** Accessors
	 ******************************************************************************************************************************************/
	/**
	 * Accessor to get the original text of the utterance
	 *
	 * @return the original text of the utterance
	 */
	public String getText() {
		return m_text;
	}

	/**
	 * Accessor to set the original text of the utterance
	 *
	 * FIXME: authorized just for the serializer for now... however needs to be
	 * more robust
	 *
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		m_text = text;
	}

	/**
	 * Accessor to get the locale of the utterance
	 *
	 * @return the locale of the utterance
	 */
	public Locale getLocale() {
		return m_locale;
	}

	/**
	 * Accessor to set the locale of the utterance
	 *
	 * FIXME: authorized just for the serializer for now... however needs to be
	 * more robust
	 *
	 * @param locale
	 *            the locale to set
	 */
	protected void setLocale(Locale locale) {
		m_locale = locale;
	}

	/**
	 * Accessor to get the name of the used voice
	 *
	 * FIXME: not really used except serializer for now
	 *
	 * @return the name of the used voice or null if no voice is used
	 */
	public String getVoiceName() {
		return m_voice_name;
	}

	/**
	 * Accessor to set the voice of the utterance
	 *
	 * FIXME: authorized just for the serializer for now... however needs to be
	 * more robust
	 *
	 * @param voice_name
	 *            the voice to set
	 */
	public void setVoice(String voice_name) {
		m_voice_name = voice_name;
	}

	/******************************************************************************************************************************************
	 ** Sequence methods
	 ******************************************************************************************************************************************/

	/**
	 * Adding a sequence of a specified type. If the type is already existing,
	 * the corresponding sequence is replaced.
	 *
	 * @param type
	 *            the type of the sequence
	 * @param sequence
	 *            the sequence
	 */
	public void addSequence(SupportedSequenceType type, Sequence<? extends Item> sequence) {
		m_sequences.put(type, sequence);
	}

	/**
	 * Method to check if a sequence of a certain type is already defined.
	 *
	 * @param type
	 *            the type to check
	 * @return true if a sequence of the given type is already defined, false
	 *         else
	 */
	public boolean hasSequence(SupportedSequenceType type) {
		return m_sequences.containsKey(type);
	}

	/**
	 * Method to get the sequence knowing the type
	 *
	 * @param type
	 *            the type of the sequence
	 * @return the found sequence or an empty sequence
	 */
	public Sequence<? extends Item> getSequence(SupportedSequenceType type) {
		if (m_sequences.containsKey(type))
			return m_sequences.get(type);

		return new Sequence<Item>();
	}

	/**
	 * Get all the available sequence types
	 *
	 * @return the available sequence types
	 */
	public Set<SupportedSequenceType> listAvailableSequences() {
		return m_sequences.keySet();
	}

	/******************************************************************************************************************************************
	 ** Relation methods
	 ******************************************************************************************************************************************/
	/**
	 * Get the relation based on the source type and the target type
	 *
	 * @param source
	 *            the type of the source sequence
	 * @param target
	 *            the type of the target sequence
	 * @return the found relation between the source sequence of type source and
	 *         the target sequence of type target, or null if there is no
	 *         relation
	 */
	public Relation getRelation(SupportedSequenceType source, SupportedSequenceType target) {
		return m_relation_graph.getRelation(getSequence(source), getSequence(target));
	}

	public Relation getRelation(Sequence<? extends Item> source, Sequence<? extends Item> target) {
		return m_relation_graph.getRelation(source, target);
	}

	/**
	 * Get the relation based on the source type and the target type
	 *
	 * @param source
	 *            the type of the source sequence
	 * @param target
	 *            the type of the target sequence
	 */
	public void setRelation(SupportedSequenceType source, SupportedSequenceType target, Relation rel) {
		m_relation_graph.addRelation(rel);
		m_available_relation_set.add(new ImmutablePair<SupportedSequenceType, SupportedSequenceType>(source, target));
	}

	/**
	 * List all the relations which are not computed through the graph. The
	 * results is a Set of couple (source sequence type, target sequence type).
	 *
	 * @return the set of all relations.
	 */
	public Set<ImmutablePair<SupportedSequenceType, SupportedSequenceType>> listAvailableRelations() {
		return m_available_relation_set;
	}

	/******************************************************************************************************************************************
	 ** Object overriding
	 ******************************************************************************************************************************************/
	/**
	 * Method to determine if an object is equal to the current utterance.
	 *
	 * @param obj
	 *            the object to compare
	 * @return true if the object is an utterance and equals the current one,
	 *         false else
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Utterance))
			return false;

		Utterance utt = (Utterance) obj;

		if (!utt.m_sequences.keySet().equals(m_sequences.keySet())) {
			logger.debug("Sequences availables are not the same in both utterances: {" + m_sequences.keySet() + "} vs {"
					+ utt.m_sequences.keySet() + "}");
			return false;
		}

		boolean not_equal = false;
		for (SupportedSequenceType type : m_sequences.keySet()) {

			Sequence<Item> cur_seq = (Sequence<Item>) m_sequences.get(type);
			Sequence<Item> other_seq = (Sequence<Item>) m_sequences.get(type);

			if (cur_seq.size() != other_seq.size()) {
				logger.debug(" => " + type + " is not leading to equal sequences (size difference)");
				break;
			}

			for (int i = 0; i < cur_seq.size(); i++) {
				Item cur_item = cur_seq.get(i);
				Item other_item = other_seq.get(i);
				if (!other_item.equals(cur_item)) {
					not_equal = true;

					logger.debug(" => " + type + " is not leading to equal sequences");
					break;
				}
			}

			if (not_equal)
				break;
		}

		if (not_equal)
			return false;

		if (!m_available_relation_set.equals(utt.m_available_relation_set))
			return false;

		return true;
	}
}
