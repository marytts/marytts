package marytts.data.item.phonology;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Accent
{
    private String m_label;

    public Accent(String label)
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

    public boolean equals(Accent obj)
    {
        if (!(obj instanceof Accent))
            return false;

        return getLabel().equals(((Accent) obj).getLabel());
    }
}
