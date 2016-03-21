package marytts.data;

import java.util.ArrayList;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;

import marytts.data.item.Paragraph;

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
}
