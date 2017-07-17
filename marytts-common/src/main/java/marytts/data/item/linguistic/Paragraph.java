package marytts.data.item.linguistic;

import java.util.ArrayList;

import marytts.data.item.Item;

/**
 * A class representing a pararaph. A paragraph is just composed by its text
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Paragraph extends Item {
    /** the text of the paragraph */
    private String m_text;

    /**
     * Constructor of a paragraph
     *
     * @param text
     *            the text of the paragraph
     */
    public Paragraph(String text) {
        super();
        setText(text);
    }

    /**
     * Accessor to get the text of the paragraph
     *
     * @return the text of the paragraph
     */
    public String getText() {
        return m_text;
    }

    /**
     * Accessor to set the text of the paragraph
     *
     * @param text
     *            the text of the paragraph
     */
    protected void setText(String text) {
        m_text = text;
    }

    /**
     * Method to compare an object to the current paragraph
     *
     * @param obj
     *            the object to compare
     * @return true if obj is a paragraph and the paragraph are equals, false
     *         else
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Paragraph)) {
            return false;
        }

        Paragraph par = (Paragraph) obj;
        return par.getText().equals(getText());
    }


    /**
     * Returns a string representation of the paragraph.
     *
     * @return a string representation of the paragraph
     */
    @Override
    public String toString() {
        return getText();
    }
}
