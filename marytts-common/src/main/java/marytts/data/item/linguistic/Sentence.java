package marytts.data.item.linguistic;

import java.util.ArrayList;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Sentence extends Item
{

	private ArrayList<Word> m_words;
	private String m_text;

	public Sentence(String text)
	{
		setText(text);
		setWords(new ArrayList<Word>());
	}

	public Sentence(String text, ArrayList<Word> words) {
		setText(text);
		setWords(words);
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
