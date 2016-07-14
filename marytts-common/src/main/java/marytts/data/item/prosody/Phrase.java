package marytts.data.item.prosody;

import java.util.ArrayList;

import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phrase extends Item
{
    private Boundary m_boundary;

    public Phrase()
    {
        super();
        setBoundary(null);
    }

    public Phrase(Boundary boundary)
    {
        super();
        setBoundary(boundary);
    }

    public Boundary getBoundary()
    {
        return m_boundary;
    }

    public void setBoundary(Boundary boundary)
    {
        m_boundary = boundary;
    }
}
