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
	private int m_stress_level;
    private Accent m_accent;
    private Phoneme m_tone;

	public Syllable()
	{
        super();
        setStressLevel(0);
        setTone(null);
        setAccent(null);
    }

	public Syllable(Phoneme tone)
    {
        super();
        setStressLevel(0);
        setTone(tone);
    }

	public Syllable(int stress_level)
    {
        super();
        setStressLevel(stress_level);
        setTone(null);
        setAccent(null);
    }

	public Syllable(Phoneme tone, int stress_level)
    {
        super();
        setStressLevel(stress_level);
        setTone(tone);
        setAccent(null);
    }

	public Syllable(Phoneme tone, int stress_level, Accent accent)
    {
        super();
        setStressLevel(stress_level);
        setTone(tone);
        setAccent(accent);
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
