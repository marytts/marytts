package marytts.data.item.linguistic;

import marytts.data.item.phonology.Accent;
import marytts.data.item.Item;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A class to represent a word
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Word extends Item {
    /** The text of the word */
    private String m_text;

    /** The part of speech of the word */
    private String m_POS;

    /** */
    private String m_sounds_like;

    /** the G2P method to use to phonemize this word */
    private String m_g2p_method;

    /** The alternative locale */
    private Locale m_alternative_locale;

    /** The accent of the word */
    private Accent m_accent;

    /**
     * Minimal constructor, the text is mandatory
     *
     * @param text
     *            the text of the word
     */
    public Word(String text) {
        super();
        setText(text);
        setAlternativeLocale((Locale) null);
        soundsLike(null);
        setPOS(null);
        setG2PMethod(null);
        setAccent(null);
    }

    /**
     * Alternative locale constructor, the text is mandatory and the alternative
     * locale is given
     *
     * @param text
     *            the text of the word
     * @param alternative_locale
     *            the alternative locale
     */
    public Word(String text, Locale alternative_locale) {
        super();
        setText(text);
        setAlternativeLocale(alternative_locale);
        soundsLike(null);
        setPOS(null);
        setG2PMethod(null);
        setAccent(null);
    }

    /**
     * Sounds like constructor, the text is mandatory and the alternative
     * pronunciation word is given
     *
     * @param text
     *            the text of the word
     * @param sounds_like
     *            alternative pronunciation word
     */
    public Word(String text, String sounds_like) {
        super();
        setText(text);
        setAlternativeLocale((Locale) null);
        soundsLike(sounds_like);
        setPOS(null);
        setG2PMethod(null);
        setAccent(null);
    }

    /*************************************************************************************
     * Getters / Setters
     *************************************************************************************/

    /**
     * Gets the part of speech of the word
     *
     * @return the part of speech
     */
    public String getPOS() {
        return m_POS;
    }

    /**
     * Sets the part of speech.
     *
     * @param POS
     *            the new part of speech
     */
    public void setPOS(String POS) {
        m_POS = POS;
    }

    /**
     * Gets the text.
     *
     * @return the text
     */
    public String getText() {
        return m_text;
    }

    /**
     * Sets the text.
     *
     * @param text
     *            the new text
     */
    public void setText(String text) {
        m_text = text;
    }

    /**
     * Gets the alternative locale.
     *
     * @return the alternative locale
     */
    public Locale getAlternativeLocale() {
        return m_alternative_locale;
    }

    /**
     * Sets the alternative locale.
     *
     * @param alternative_locale
     *            the new alternative locale
     */
    public void setAlternativeLocale(String alternative_locale) {
        m_alternative_locale = new Locale(alternative_locale);
    }

    /**
     * Sets the alternative locale.
     *
     * @param alternative_locale
     *            the new alternative locale
     */
    public void setAlternativeLocale(Locale alternative_locale) {
        m_alternative_locale = alternative_locale;
    }

    /**
     * Gets the grapheme to phoneme method.
     *
     * @return the grapheme to phoneme method
     */
    public String getG2PMethod() {
        return m_g2p_method;
    }

    /**
     * Sets the grapheme to phoneme method.
     *
     * @param g2p_method
     *            the new grapheme to phoneme method
     */
    public void setG2PMethod(String g2p_method) {
        m_g2p_method = g2p_method;
    }

    /**
     * Gets the sounds like reference
     *
     * @return the sounds like word
     */
    public String soundsLike() {
        return m_sounds_like;
    }

    /**
     * Sets the sounds like word
     *
     * @param sounds_like
     *            the sounds like to use
     */
    public void soundsLike(String sounds_like) {
        m_sounds_like = sounds_like;
    }

    /**
     * Gets the accent.
     *
     * @return the accent
     */
    public Accent getAccent() {
        return m_accent;
    }

    /**
     * Sets the accent.
     *
     * @param accent
     *            the new accent
     */
    public void setAccent(Accent accent) {
        m_accent = accent;
    }

    /***************************************************************************************
     ** Object overriding
     ***************************************************************************************/
    /**
     * Method to compare an object to the current word
     *
     * @param obj
     *            the object to compare
     * @return true if obj is a word and the word are equals, false else
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Word)) {
            return false;
        }

        Word wrd = (Word) obj;

        if (!(((wrd.soundsLike() == null) && (soundsLike() == null))
                || ((wrd.soundsLike() != null) && (wrd.soundsLike().equals(soundsLike()))))) {
            return false;
        }

        if (!(((wrd.getG2PMethod() == null) && (getG2PMethod() == null))
                || ((wrd.getG2PMethod() != null) && (wrd.getG2PMethod().equals(getG2PMethod()))))) {
            return false;
        }

        if (!(((wrd.getPOS() == null) && (getPOS() == null))
                || ((wrd.getPOS() != null) && (wrd.getPOS().equals(getPOS()))))) {
            return false;
        }

        if (!(((wrd.getAccent() == null) && (getAccent() == null))
                || ((wrd.getAccent() != null) && (wrd.getAccent().equals(getAccent()))))) {
            return false;
        }

        if (!(((wrd.getAlternativeLocale() == null) && (getAlternativeLocale() == null))
                || ((wrd.getAlternativeLocale() != null)
                    && (wrd.getAlternativeLocale().equals(getAlternativeLocale()))))) {
            return false;
        }

        return getText().equals(wrd.getText());
    }

    /**
     * Returns a string representation of the word.
     *
     * @return a string representation of the word
     */
    @Override
    public String toString() {
        return getText();
    }

}
