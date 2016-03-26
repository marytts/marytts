package marytts.data.item.phonology;

import marytts.data.item.Item;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phoneme extends Item
{
    private String m_stress;
	private String m_label;

	public Phoneme(String label)
	{
		setLabel(label);
        setStress(null);
    }

	public Phoneme(String label, String stress)
	{
		setLabel(label);
        setStress(stress);
    }

	public String getLabel()
	{
		return m_label;
	}

	protected void setLabel(String label)
	{
		m_label = label;
	}

	public String getStress()
	{
		return m_stress;
	}

	protected void setStress(String stress)
	{
		m_stress = stress;
	}
}
