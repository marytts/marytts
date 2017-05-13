package marytts.data.item.phonology;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Phoneme extends Segment {
	private String m_stress;

	public Phoneme(String label) {
		super(label);
		setStress(null);
	}

	public Phoneme(Phoneme phoneme) {
		super(phoneme.getLabel());
		setStress(phoneme.getStress());
	}

	public String getStress() {
		return m_stress;
	}

	protected void setStress(String stress) {
		m_stress = stress;
	}
}
