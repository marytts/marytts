package marytts.data.item.phonology;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Segment extends Item
{
	private String m_label;

	public Segment(String label)
	{
        super();
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

    @Override
    public String toString()
    {
        return getLabel();
    }
}
