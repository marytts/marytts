package marytts.data.item.prosody;

import java.util.ArrayList;

import marytts.data.item.Item;

// TODO: Auto-generated Javadoc
/**
 * Class to represent a phrase.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Phrase extends Item {

    /** The m boundary. */
    private Boundary m_boundary;

    /**
     * Instantiates a new phrase.
     */
    public Phrase() {
        super();
        setBoundary(null);
    }

    /**
     * Instantiates a new phrase.
     *
     * @param boundary
     *            the boundary
     */
    public Phrase(Boundary boundary) {
        super();
        setBoundary(boundary);
    }

    /**
     * Gets the boundary.
     *
     * @return the boundary
     */
    public Boundary getBoundary() {
        return m_boundary;
    }

    /**
     * Sets the boundary.
     *
     * @param boundary
     *            the new boundary
     */
    public void setBoundary(Boundary boundary) {
        m_boundary = boundary;
    }

    @Override
    public String toString() {
        return "Phrase";
    }
}
