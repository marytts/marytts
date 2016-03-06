package marytts.data.item;

import java.util.ArrayList;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phrase extends Item
{

	private ArrayList<Word> m_words;
	private String m_text;
	
	public Phrase(String text)
	{
		setText(text);
	}
	
	public ArrayList<Word> getWords() 
	{
		return m_words;
	}

	public void setWords(ArrayList<Word> words) 
	{
		m_words = words;
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
