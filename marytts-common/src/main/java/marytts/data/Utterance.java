package marytts.data;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;

import marytts.data.item.linguistic.Paragraph;
import marytts.data.item.linguistic.Word;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.prosody.Phrase;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Utterance
{
    public enum SupportedSequenceType {
        PARAGRAPH
    };
    private String m_voice_name;
	private String m_text;
    private Locale m_locale;
    private ArrayList<AudioInputStream> m_list_streams;
    private Hashtable<SupportedSequenceType, Sequence<? extends Item>> m_sequences;
    private ArrayList<Relation> m_relations;

    public Utterance(String text, Locale locale)
    {
        setVoice(null);
        setText(text);
        setLocale(locale);

        m_sequences = new Hashtable<SupportedSequenceType, Sequence<? extends Item>>();
        m_relations = new ArrayList<Relation>();
    }

    public String getText()
    {
        return m_text;
    }

    protected void setText(String text)
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

    public Sequence<Sentence> getAllSentences()
    {
        Sequence<Sentence> sentences = new Sequence<Sentence>();
        for (Paragraph p:  (Sequence<Paragraph>) getSequence(SupportedSequenceType.PARAGRAPH))
        {
            sentences.addAll(p.getSentences());
        }
        return sentences;
    }

    public Sequence<Phrase> getAllPhrases()
    {
        Sequence<Sentence> sentences = getAllSentences();
        Sequence<Phrase> phrases = new Sequence<Phrase>();

        for (Sentence s: sentences)
        {
            phrases.addAll(s.getPhrases());
        }

        return phrases;
    }

    public Sequence<Word> getAllWords()
    {
        Sequence<Word> words = new Sequence<Word>();
        for (Sentence s: getAllSentences())
        {
            words.addAll(s.getWords());
        }

        for (Phrase p: getAllPhrases())
        {
            words.addAll(p.getWords());
        }
        return words;
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

    public Sequence<? extends Item> getSequence(SupportedSequenceType type)
    {
        if (m_sequences.containsKey(type))
            return m_sequences.get(type);

        return new Sequence<Item>();
    }
}
