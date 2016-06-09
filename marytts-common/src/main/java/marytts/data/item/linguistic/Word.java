package marytts.data.item.linguistic;

import marytts.data.item.phonology.Syllable;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Accent;

import marytts.data.item.Item;

import java.util.ArrayList;
import java.util.Locale;

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
	private ArrayList<Phoneme> m_phonemes;
	private String m_g2p_method;
    private Locale m_alternative_locale;
    private Accent m_accent;

	public Word(String text)
	{
        super();
		setText(text);
        setAlternativeLocale(null);
        soundsLike(null);
        setG2PMethod(null);
        setAccent(null);
        setSyllables(new ArrayList<Syllable>());
        setPhonemes(new ArrayList<Phoneme>());
    }

	public Word(String text, ArrayList<Syllable> syllables)
    {
        super();
        setText(text);
        setAlternativeLocale(null);
        soundsLike(null);
        setG2PMethod(null);
        setAccent(null);
        setSyllables(syllables);
        setPhonemes(new ArrayList<Phoneme>());
    }

    public Word(String text, Locale alternative_locale)
	{
        super();
		setText(text);
        setAlternativeLocale(alternative_locale);
        soundsLike(null);
        setG2PMethod(null);
        setAccent(null);
        setSyllables(new ArrayList<Syllable>());
        setPhonemes(new ArrayList<Phoneme>());
    }

	public Word(String text, String sounds_like)
	{
        super();
		setText(text);
        setAlternativeLocale(null);
        setG2PMethod(null);
        soundsLike(sounds_like);
        setAccent(null);
        setSyllables(new ArrayList<Syllable>());
        setPhonemes(new ArrayList<Phoneme>());
    }

    public Word(String text, String sounds_like, ArrayList<Syllable> syllables)
    {
        super();
        setText(text);
        setAlternativeLocale(null);
        setG2PMethod(null);
        soundsLike(sounds_like);
        setAccent(null);
        setSyllables(syllables);
        setPhonemes(new ArrayList<Phoneme>());
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

    public void setText(String text)
    {
        m_text = text;
    }

    public Locale getAlternativeLocale()
    {
        return m_alternative_locale;
    }

    public void setAlternativeLocale(Locale alternative_locale)
    {
        m_alternative_locale = alternative_locale;
    }

    public ArrayList<Syllable> getSyllables()
    {
        return m_syllables;
    }

    public void setSyllables(ArrayList<Syllable> syllables)
    {
        m_syllables = syllables;
    }

    public void addSyllable(Syllable syl)
    {
        m_syllables.add(syl);
    }

    public ArrayList<Phoneme> getPhonemes()
    {
        return m_phonemes;
    }

    public void setPhonemes(ArrayList<Phoneme> phonemes)
    {
        m_phonemes = phonemes;
    }

    public String getG2PMethod()
    {
        return m_g2p_method;
    }

    public void setG2PMethod(String g2p_method)
    {
        m_g2p_method = g2p_method;
    }


    public String soundsLike()
    {
        return m_sounds_like;
    }

    public void soundsLike(String sounds_like)
    {
        m_sounds_like = sounds_like;
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
