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
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Utterance
{
    private String m_voice_name;
	private String m_text;
    private Locale m_locale;
    private ArrayList<AudioInputStream> m_list_streams;
    private Hashtable<SupportedSequenceType, Sequence<? extends Item>> m_sequences;
    private RelationGraph m_relation_graph;
    private Set<ImmutablePair<SupportedSequenceType, SupportedSequenceType>> m_available_relation_set;

	protected Logger logger;

    public Utterance(String text, Locale locale)
    {
        setVoice(null);
        setText(text);
        setLocale(locale);

        m_sequences = new Hashtable<SupportedSequenceType, Sequence<? extends Item>>();
        m_available_relation_set = new HashSet<ImmutablePair<SupportedSequenceType, SupportedSequenceType>>();
        m_relation_graph = new RelationGraph();

        // FIXME: have to be more consistent
		logger = MaryUtils.getLogger("Utterance");
    }

    public String getText()
    {
        return m_text;
    }


    /**
     *  FIXME: authorized just for the serializer for now... however needs to be more robust
     *
     */
    public void setText(String text)
    {
        m_text = text;
    }

    public Locale getLocale()
    {
        return m_locale;
    }

    protected void setLocale(Locale locale)
    {
        m_locale = locale;
    }

    public String getVoiceName()
    {
        return m_voice_name;
    }

    public void setVoice(String voice_name)
    {
        m_voice_name = voice_name;
    }

    public Sequence<Phrase> getAllPhrases()
    {
        return (Sequence<Phrase>) getSequence(SupportedSequenceType.PHRASE);
    }

    /**
     * Adding a sequence. If the label is already existing, the corresponding sequence is replaced
     *
     *  @param type the type of the sequence
     *  @param sequence the sequence
     */
    public void addSequence(SupportedSequenceType type, Sequence<? extends Item> sequence)
    {
        m_sequences.put(type, sequence);
    }


    public boolean hasSequence(SupportedSequenceType type)
    {
        return m_sequences.containsKey(type);
    }

    public Sequence<? extends Item> getSequence(SupportedSequenceType type)
    {
        if (m_sequences.containsKey(type))
            return m_sequences.get(type);

        return new Sequence<Item>();
    }

    public Set<SupportedSequenceType> listAvailableSequences()
    {
        return m_sequences.keySet();
    }

    /**
     * FIXME: not really efficient right now
     *
     */
    public Relation getRelation(SupportedSequenceType source, SupportedSequenceType target)
    {
        return m_relation_graph.getRelation(getSequence(source), getSequence(target));
    }

    public Relation getRelation(Sequence<? extends Item> source, Sequence<? extends Item> target)
    {
        return m_relation_graph.getRelation(source, target);
    }

    /**
     * FIXME: have to check !
     */
    public void setRelation(SupportedSequenceType source, SupportedSequenceType target, Relation rel)
    {
        m_relation_graph.addRelation(rel);
        m_available_relation_set.add(new ImmutablePair<SupportedSequenceType, SupportedSequenceType>(source, target));
    }


    public Set<ImmutablePair<SupportedSequenceType, SupportedSequenceType>> listAvailableRelations()
    {
        return m_available_relation_set;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof Utterance))
            return false;

        Utterance utt = (Utterance) obj;

        if (!utt.m_sequences.keySet().equals(m_sequences.keySet()))
        {
            logger.debug("Sequences availables are not the same in both utterances: {" + m_sequences.keySet() + "} vs {" + utt.m_sequences.keySet() + "}");
            return false;
        }

        boolean not_equal = false;
        for (SupportedSequenceType type: m_sequences.keySet())
        {

            Sequence<Item> cur_seq = (Sequence<Item>) m_sequences.get(type);
            Sequence<Item> other_seq = (Sequence<Item>) m_sequences.get(type);

            if (cur_seq.size() != other_seq.size())
            {
                logger.debug(" => " + type + " is not leading to equal sequences (size difference)");
                break;
            }

            for (int i=0; i<cur_seq.size(); i++)
            {
                Item cur_item = cur_seq.get(i);
                Item other_item = other_seq.get(i);
                if (!other_item.equals(cur_item))
                {
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

        if (! m_available_relation_set.equals(utt.m_available_relation_set))
            return false;

        return true;
    }
}
