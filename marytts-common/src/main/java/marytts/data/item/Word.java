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
	private ArrayList<Syllable> m_syllables;
	private String m_g2p_method;
	
	public Word(String text)
	{
		setText(text);
	}
	
	
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
