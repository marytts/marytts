package marytts.data.item.prosody;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Boundary
{
    private int m_breakindex;
    private String m_tone;

    public Boundary(int breakindex, String tone)
    {
        setBreakindex(breakindex);
        setTone(tone);
    }

    public int getBreakindex()
    {
        return m_breakindex;
    }

    public void setBreakindex(int breakindex)
    {
        m_breakindex = breakindex;
    }

    public String getTone()
    {
        return m_tone;
    }

    public void setTone(String tone)
    {
        m_tone = tone;
    }
}
