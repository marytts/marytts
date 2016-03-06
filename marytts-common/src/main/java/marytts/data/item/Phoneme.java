package marytts.data.item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phoneme extends Item
{

	private String m_label;

	public Phoneme(String label)
	{
		setLabel(label);
	}

	public String getLabel() 
	{
		return m_label;
	}

	protected void setLabel(String label) 
	{
		m_label = label;
	}
}
