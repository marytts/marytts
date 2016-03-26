package marytts.data;

import java.util.ArrayList;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;

import marytts.data.item.linguistic.Paragraph;
import marytts.data.item.linguistic.Word;
import marytts.data.item.linguistic.Sentence;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Utterance
{
	private String m_text;
    private Locale m_locale;
    private ArrayList<Paragraph> m_list_paragraphs;
    private ArrayList<AudioInputStream> m_list_streams;

    public Utterance(String text, Locale locale)
    {
        setText(text);
        setLocale(locale);
        setParagraphs(new ArrayList<Paragraph>());
    }
    public Utterance(String text, Locale locale, ArrayList<Paragraph> list_paragraphs)
    {
        setText(text);
        setLocale(locale);
        setParagraphs(list_paragraphs);
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

    public ArrayList<Paragraph> getParagraphs()
	{
		return m_list_paragraphs;
	}

	public void setParagraphs(ArrayList<Paragraph> list_paragraphs)
	{
		m_list_paragraphs = list_paragraphs;
	}

    public void addParagraph(Paragraph p)
    {
        m_list_paragraphs.add(p);
    }

    public ArrayList<Sentence> getAllSentences()
    {
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        for (Paragraph p: getParagraphs())
        {
            sentences.addAll(p.getSentences());
        }
        return sentences;
    }

    public ArrayList<Word> getAllWords()
    {
        ArrayList<Word> words = new ArrayList<Word>();
        for (Sentence s: getAllSentences())
        {
            words.addAll(s.getWords());
        }
        return words;
    }
}
