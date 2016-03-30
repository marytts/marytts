package marytts.data.item.phonology;

import java.util.ArrayList;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */

public class Syllable extends Item
{
	private ArrayList<Phoneme> m_phonemes;
	private int m_stress_level;
    private Accent m_accent;
    private Phoneme m_tone;

	public Syllable()
	{
		setPhonemes(new ArrayList<Phoneme>());
        setStressLevel(0);
        setTone(null);
        setAccent(null);
    }

	public Syllable(ArrayList<Phoneme> phonemes)
	{
		setPhonemes(phonemes);
        setStressLevel(0);
        setTone(null);
        setAccent(null);
    }

	public Syllable(ArrayList<Phoneme> phonemes, Phoneme tone)
    {
        setPhonemes(phonemes);
        setStressLevel(0);
        setTone(tone);
    }

	public Syllable(ArrayList<Phoneme> phonemes, int stress_level)
    {
        setPhonemes(phonemes);
        setStressLevel(stress_level);
        setTone(null);
        setAccent(null);
    }

	public Syllable(ArrayList<Phoneme> phonemes, Phoneme tone, int stress_level)
    {
        setPhonemes(phonemes);
        setStressLevel(stress_level);
        setTone(tone);
        setAccent(null);
    }

	public Syllable(ArrayList<Phoneme> phonemes, Phoneme tone, int stress_level, Accent accent)
    {
        setPhonemes(phonemes);
        setStressLevel(stress_level);
        setTone(tone);
        setAccent(accent);
    }

    public ArrayList<Phoneme> getPhonemes()
	{
		return m_phonemes;
	}

	public void setPhonemes(ArrayList<Phoneme> phonemes)
	{
		m_phonemes = phonemes;
	}

    public int getStressLevel()
    {
        return m_stress_level;
    }

    public void setStressLevel(int stress_level)
    {
        m_stress_level = stress_level;
    }

    public Phoneme getTone()
    {
        return m_tone;
    }

    public void setTone(Phoneme tone)
    {
        m_tone = tone;
    }

    public Accent getAccent()
    {
        return m_accent;
    }

    public void setAccent(Accent accent)
    {
        m_accent = accent;
    }
}
