package marytts.data.item.linguistic;

import marytts.data.item.phonology.Accent;

import marytts.data.item.Item;

import java.util.ArrayList;
import java.util.Locale;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Word extends Item {
	private String m_POS;
	private String m_text;
	private String m_sounds_like;
	private String m_g2p_method;
	private Locale m_alternative_locale;
	private Accent m_accent;

	public Word(String text) {
		super();
		setText(text);
		setAlternativeLocale(null);
		soundsLike(null);
		setPOS(null);
		setG2PMethod(null);
		setAccent(null);
	}

	public Word(String text, Locale alternative_locale) {
		super();
		setText(text);
		setAlternativeLocale(alternative_locale);
		soundsLike(null);
		setPOS(null);
		setG2PMethod(null);
		setAccent(null);
	}

	public Word(String text, String sounds_like) {
		super();
		setText(text);
		setAlternativeLocale(null);
		soundsLike(sounds_like);
		setPOS(null);
		setG2PMethod(null);
		setAccent(null);
	}

	/***************************************************************************************
	 ** Getters / Setters
	 ***************************************************************************************/
	public String getPOS() {
		return m_POS;
	}

	public void setPOS(String POS) {
		m_POS = POS;
	}

	public String getText() {
		return m_text;
	}

	public void setText(String text) {
		m_text = text;
	}

	public Locale getAlternativeLocale() {
		return m_alternative_locale;
	}

	public void setAlternativeLocale(Locale alternative_locale) {
		m_alternative_locale = alternative_locale;
	}

	public String getG2PMethod() {
		return m_g2p_method;
	}

	public void setG2PMethod(String g2p_method) {
		m_g2p_method = g2p_method;
	}

	public String soundsLike() {
		return m_sounds_like;
	}

	public void soundsLike(String sounds_like) {
		m_sounds_like = sounds_like;
	}

	public Accent getAccent() {
		return m_accent;
	}

	public void setAccent(Accent accent) {
		m_accent = accent;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Word))
			return false;

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
}
