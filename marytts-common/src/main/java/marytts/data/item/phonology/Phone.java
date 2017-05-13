package marytts.data.item.phonology;

/**
 * A phone representation. A phone is a phoneme with a position in the signal
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Phone extends Phoneme {
	private double m_start; /* < start position of the phone in second */
	private double m_duration; /* < duration of the phone in second */

	public Phone(String label, double start, double duration) {
		super(label);
		setStart(start);
		setDuration(duration);
	}

	public Phone(Phoneme phoneme, double start, double duration) {
		super(phoneme);
		setStart(start);
		setDuration(duration);
	}
	public Phone(String label, double duration) {
		super(label);
		setStart(-1);
		setDuration(duration);
	}

	public Phone(Phoneme phoneme, double duration) {
		super(phoneme);
		setStart(-1);
		setDuration(duration);
	}

	public double getStart() {
		return m_start;
	}

	public void setStart(double start) {
		m_start = start;
	}

	public double getDuration() {
		return m_duration;
	}

	protected void setDuration(double duration) {
		m_duration = duration;
	}

	@Override
	public String toString() {
		return getLabel() + " : " + getDuration();
	}
}
