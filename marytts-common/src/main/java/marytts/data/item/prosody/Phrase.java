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
        setBoundary(null);
        setWords(new ArrayList<Word>());
    }

    public Phrase(ArrayList<Word> words)
    {
        setBoundary(null);
        setWords(words);
    }

    public Phrase(Boundary boundary)
    {
        setBoundary(boundary);
        setWords(new ArrayList<Word>());
    }

    public Phrase(Boundary boundary, ArrayList<Word> words)
    {
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
}
