package marytts.data.item.linguistic;

import java.util.ArrayList;

import marytts.data.item.Item;
import marytts.data.item.prosody.Phrase;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Sentence extends Item
{

	private ArrayList<Phrase> m_phrases;
	private ArrayList<Word> m_words;
	private String m_text;

	public Sentence(String text)
	{
        super();
		setText(text);
		setWords(new ArrayList<Word>());
		setPhrases(new ArrayList<Phrase>());
	}

	public Sentence(String text, ArrayList<Word> words)
    {
        super();
		setText(text);
		setWords(words);
		setPhrases(new ArrayList<Phrase>());
	}

	public String getText()
	{
		return m_text;
	}

	protected void setText(String text)
	{
		m_text = text;
	}

    public ArrayList<Word> getWords()
	{
		return m_words;
	}

	public void setWords(ArrayList<Word> words)
	{
		m_words = words;
	}

    public void addWord(Word word)
    {
        m_words.add(word);
    }

    public ArrayList<Phrase> getPhrases()
	{
		return m_phrases;
	}

	public void setPhrases(ArrayList<Phrase> phrases)
	{
		m_phrases = phrases;
	}

    public void addPhrase(Phrase phrase)
    {
        m_phrases.add(phrase);
    }

}
