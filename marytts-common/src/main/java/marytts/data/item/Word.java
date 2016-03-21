package marytts.data.item;

import java.util.ArrayList;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Word extends Item
{
	private String m_POS;
	private String m_text;
    private String m_sounds_like;
	private ArrayList<Syllable> m_syllables;
	private String m_g2p_method;

	public Word(String text)
	{
		setText(text);
        setSoundsLike(null);
        setSyllables(new ArrayList<Syllable>());
    }

	public Word(String text, String sounds_like)
	{
		setText(text);
        setSoundsLike(sounds_like);
        setSyllables(new ArrayList<Syllable>());
    }

	public Word(String text, String sounds_like, ArrayList<Syllable> syllables)
	{
		setText(text);
        setSoundsLike(sounds_like);
        setSyllables(syllables);
    }

    /***************************************************************************************
     ** Getters / Setters
     ***************************************************************************************/
	public String getPOS()
	{
		return m_POS;
	}

	public void setPOS(String POS)
	{
		m_POS = POS;
	}

	public String getText()
	{
		return m_text;
	}

	protected void setText(String text)
	{
		m_text = text;
	}

	public String getSoundsLike()
	{
		return m_sounds_like;
	}

	protected void setSoundsLike(String sounds_like)
	{
		m_sounds_like = sounds_like;
	}


	public ArrayList<Syllable> getSyllables()
	{
		return m_syllables;
	}

	public void setSyllables(ArrayList<Syllable> syllables)
	{
		m_syllables = syllables;
	}


	public String getG2PMethod()
	{
		return m_g2p_method;
	}

	public void setG2PMethod(String g2p_method)
	{
		m_g2p_method = g2p_method;
	}
}
