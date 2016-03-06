package marytts.data.item;

import java.util.ArrayList;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */

public class Syllable extends Item
{
	private ArrayList<Phoneme> m_phonemes;
	private boolean m_is_stressed;
	
	public Syllable()
	{
		setPhonemes(new ArrayList<Phoneme>());
	}

	public Syllable(ArrayList<Phoneme> phonemes)
	{
		setPhonemes(phonemes);
	}

	public ArrayList<Phoneme> getPhonemes() 
	{
		return m_phonemes;
	}

	public void setPhonemes(ArrayList<Phoneme> phonemes) 
	{
		m_phonemes = phonemes;
	}

	public boolean isStressed() 
	{
		return m_is_stressed;
	}

	public void defineStress(boolean stress_status) 
	{
		m_is_stressed = stress_status;
	}
}
