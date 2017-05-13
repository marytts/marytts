package marytts.data.item.phonology;

import java.util.ArrayList;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */

public class Syllable extends Item {

	/** The stress level. */
	private int m_stress_level;

	/** The accent. */
	private Accent m_accent;

	/** The tone. */
	private Phoneme m_tone;

	/**
	 * Instantiates a new syllable.
	 */
	public Syllable() {
		super();
		setStressLevel(0);
		setTone(null);
		setAccent(null);
	}

	/**
	 * Instantiates a new syllable.
	 *
	 * @param tone
	 *            the tone
	 */
	public Syllable(Phoneme tone) {
		super();
		setStressLevel(0);
		setTone(tone);
	}

	/**
	 * Instantiates a new syllable.
	 *
	 * @param stress_level
	 *            the stress level
	 */
	public Syllable(int stress_level) {
		super();
		setStressLevel(stress_level);
		setTone(null);
		setAccent(null);
	}

	/**
	 * Instantiates a new syllable.
	 *
	 * @param tone
	 *            the tone
	 * @param stress_level
	 *            the stress level
	 */
	public Syllable(Phoneme tone, int stress_level) {
		super();
		setStressLevel(stress_level);
		setTone(tone);
		setAccent(null);
	}

	/**
	 * Instantiates a new syllable.
	 *
	 * @param tone
	 *            the tone
	 * @param stress_level
	 *            the stress level
	 * @param accent
	 *            the accent
	 */
	public Syllable(Phoneme tone, int stress_level, Accent accent) {
		super();
		setStressLevel(stress_level);
		setTone(tone);
		setAccent(accent);
	}

	/**
	 * Gets the stress level.
	 *
	 * @return the stress level
	 */
	public int getStressLevel() {
		return m_stress_level;
	}

	/**
	 * Sets the stress level.
	 *
	 * @param stress_level
	 *            the new stress level
	 */
	public void setStressLevel(int stress_level) {
		m_stress_level = stress_level;
	}

	/**
	 * Gets the tone.
	 *
	 * @return the tone
	 */
	public Phoneme getTone() {
		return m_tone;
	}

	/**
	 * Sets the tone.
	 *
	 * @param tone
	 *            the new tone
	 */
	public void setTone(Phoneme tone) {
		m_tone = tone;
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
}
