package marytts.data.item.prosody;

/**
 * Class to represent a boundary
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Boundary {
	/** The break index */
	private int m_breakindex;

	/** The tone */
	private String m_tone;

	/** The duration */
	private int m_duration;

	/**
	 * Instantiates a new boundary.
	 *
	 * @param breakindex
	 *            the breakindex
	 * @param tone
	 *            the tone
	 */
	public Boundary(int breakindex, String tone) {
		setBreakindex(breakindex);
		setTone(tone);
		setDuration(-1);
	}

	/**
	 * Instantiates a new boundary.
	 *
	 * @param breakindex
	 *            the breakindex
	 * @param duration
	 *            the duration
	 */
	public Boundary(int breakindex, int duration) {
		setBreakindex(breakindex);
		setTone(null);
		setDuration(duration);
	}

	/**
	 * Gets the breakindex.
	 *
	 * @return the breakindex
	 */
	public int getBreakindex() {
		return m_breakindex;
	}

	/**
	 * Sets the breakindex.
	 *
	 * @param breakindex
	 *            the new breakindex
	 */
	public void setBreakindex(int breakindex) {
		m_breakindex = breakindex;
	}

	/**
	 * Gets the tone.
	 *
	 * @return the tone
	 */
	public String getTone() {
		return m_tone;
	}

	/**
	 * Sets the tone.
	 *
	 * @param tone
	 *            the new tone
	 */
	public void setTone(String tone) {
		m_tone = tone;
	}

	/**
	 * Gets the duration.
	 *
	 * @return the duration
	 */
	public int getDuration() {
		return m_duration;
	}

	/**
	 * Sets the duration.
	 *
	 * @param duration
	 *            the new duration
	 */
	public void setDuration(int duration) {
		m_duration = duration;
	}
}
