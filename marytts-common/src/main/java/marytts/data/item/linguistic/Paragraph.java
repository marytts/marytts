package marytts.data.item.linguistic;

import java.util.ArrayList;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Paragraph extends Item
{

	private ArrayList<Sentence> m_sentences;
	private String m_text;

	public Paragraph(String text)
	{
		setText(text);
        setSentences(new ArrayList<Sentence>());
    }

	public Paragraph(String text, ArrayList<Sentence> sentences)
	{
		setText(text);
		setSentences(sentences);
	}

	public ArrayList<Sentence> getSentences()
	{
		return m_sentences;
	}

	public void setSentences(ArrayList<Sentence> sentences)
	{
		m_sentences = sentences;
	}

    public void addSentence(Sentence sentence)
    {
        m_sentences.add(sentence);
    }

    public String getText()
	{
		return m_text;
	}

	protected void setText(String text)
	{
		m_text = text;
	}
}
