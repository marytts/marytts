package marytts.data;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.ImmutablePair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioInputStream;


import marytts.data.item.Item;
import marytts.data.item.linguistic.Paragraph;
import marytts.data.item.linguistic.Word;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.prosody.Phrase;

/**
 * The Utterance is the entry point to the data used in MaryTTS. It is a
 * container to access to all the information computed during the process.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Utterance {
    /** Not used for now ! */
    private ArrayList<AudioInputStream> m_list_streams;

    /** FIXME: temp feature names, nowhere else to put for now */
    private ArrayList<String> m_feature_names;

    /**
     * The sequences which contains the data of the utterance. Organized by type
     * for now
     */
    private Hashtable<String, Sequence<? extends Item>> m_sequences;

    /** The relation graph to link te sequences */
    private RelationGraph m_relation_graph;

    /** The set of "not computed" relation based on types */
    private Set<ImmutablePair<String, String>> m_available_relation_set;

    /** The logger of the utterance */
    protected static Logger logger = LogManager.getLogger(Utterance.class);

    /**
     * The constructor of the utterance which forces to define a text and an
     * associated locale
     *
     * @param text
     *            the original text of the utterance
     * @param locale
     *            the locale used for this utterance
     */
    public Utterance() {

        m_sequences = new Hashtable<String, Sequence<? extends Item>>();
        m_available_relation_set = new
        HashSet<ImmutablePair<String, String>>();
        m_relation_graph = new RelationGraph();
	m_feature_names = new ArrayList<String>();

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
    public void addSequence(String type, Sequence<? extends Item> sequence) {
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
    public boolean hasSequence(String type) {
        return m_sequences.containsKey(type);
    }

    /**
     * Method to get the sequence knowing the type
     *
     * @param type
     *            the type of the sequence
     * @return the found sequence or an empty sequence
     */
    public Sequence<? extends Item> getSequence(String type) {
        if (m_sequences.containsKey(type)) {
            return m_sequences.get(type);
        }

        return new Sequence<Item>();
    }

    /**
     * Get all the available sequence types
     *
     * @return the available sequence types
     */
    public Set<String> listAvailableSequences() {
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
    public Relation getRelation(String source, String target) {
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
    public void setRelation(String source, String target, Relation rel) {
        m_relation_graph.addRelation(rel);
        m_available_relation_set.add(new ImmutablePair<String, String>(source,
                                     target));
    }

    /**
     * List all the relations which are not computed through the graph. The
     * results is a Set of couple (source sequence type, target sequence type).
     *
     * @return the set of all relations.
     */
    public Set<ImmutablePair<String, String>> listAvailableRelations() {
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
        if (!(obj instanceof Utterance)) {
            return false;
        }

        Utterance utt = (Utterance) obj;

        if (!utt.m_sequences.keySet().equals(m_sequences.keySet())) {
            logger.debug("Sequences availables are not the same in both utterances: {" + m_sequences.keySet() +
                         "} vs {"
                         + utt.m_sequences.keySet() + "}");
            return false;
        }

        boolean not_equal = false;
        for (String type : m_sequences.keySet()) {

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

            if (not_equal) {
                break;
            }
        }

        if (not_equal) {
            return false;
        }

        if (!m_available_relation_set.equals(utt.m_available_relation_set)) {
            return false;
        }

        return true;
    }



    /******************************************************************************************************************************************
     ** Temporary
     ******************************************************************************************************************************************/
    public ArrayList<String> getFeatureNames() {
	return m_feature_names;
    }

    public void setFeatureNames(ArrayList<String> feature_names) {
	m_feature_names = feature_names;
    }
}
