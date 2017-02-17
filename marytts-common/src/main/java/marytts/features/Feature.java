package marytts.features;

import marytts.data.item.Item;


/**
 * A feature is a specific item with a specific value.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Feature extends Item
{
    Object m_value;
    public Feature(Object value)
    {
        m_value = value;
    }

    public Object getValue()
    {
        return m_value;
    }

    public String getStringValue()
    {
        if (m_value == null)
            return "";
        return m_value.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Feature))
            return false;

        return (getValue() == ((Feature) o).getValue());
    }

    public static final Feature UNDEF_FEATURE = new Feature(null);
}
