package marytts.data.item.prosody;

import java.util.ArrayList;

import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phrase extends Item
{
    private Boundary m_boundary;
    private ArrayList<Word> m_words;

    public Phrase()
    {
        super();
        setBoundary(null);
        setWords(new ArrayList<Word>());
    }

    public Phrase(ArrayList<Word> words)
    {
        super();
        setBoundary(null);
        setWords(words);
    }

    public Phrase(Boundary boundary)
    {
        super();
        setBoundary(boundary);
        setWords(new ArrayList<Word>());
    }

    public Phrase(Boundary boundary, ArrayList<Word> words)
    {
        super();
        setBoundary(boundary);
        setWords(words);
    }

    public Boundary getBoundary()
    {
        return m_boundary;
    }

    public void setBoundary(Boundary boundary)
    {
        m_boundary = boundary;
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
}
