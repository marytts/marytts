package marytts.data.item;

import java.util.ArrayList;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Paragraph extends Item
{

	private ArrayList<Word> m_phrases;
	private String m_text;

	public Paragraph(String text)
	{
		setText(text);
	}

	public ArrayList<Word> getPhrases()
	{
		return m_phrases;
	}

	public void setPhrases(ArrayList<Word> phrases)
	{
		m_phrases = phrases;
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
