package marytts.data;

import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;

import marytts.data.item.Phrase;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Utterance
{
	private String m_text;
    private ArrayList<Phrase> m_list_phrases;
    private ArrayList<AudioInputStream> m_list_streams;
    
    public Utterance(String text) 
    {
    	m_text = text;
    }
    
    public String getText()
    {
    	return m_text;
    }

	public ArrayList<Phrase> getPhrases() 
	{
		return m_list_phrases;
	}

	public void setPhrases(ArrayList<Phrase> list_phrases) 
	{
		m_list_phrases = list_phrases;
	}
}
