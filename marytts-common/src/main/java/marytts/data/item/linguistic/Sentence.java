package marytts.data.item.linguistic;

import java.util.ArrayList;

import marytts.data.item.Item;

/**
 * A class representing a pararaph. A sentence is just composed by its text
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Sentence extends Item {
    /** the text of the sentence */
	private String m_text;

    /**
     * Constructor of a sentence
     *
     * @param text the text of the sentence
     */
	public Sentence(String text) {
		super();
		setText(text);
	}

    /**
     * Accessor to get the text of the sentence
     *
     * @return the text of the sentence
     */
	public String getText() {
		return m_text;
	}

    /**
     * Accessor to set the text of the sentence
     *
     * @param text the text of the sentence
     */
	protected void setText(String text) {
		m_text = text;
	}

	/**
	 * Method to compare an object to the current sentence
	 *
	 * @param obj
	 *            the object to compare
	 * @return true if obj is a sentence and the sentence are equals, false else
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Sentence))
			return false;

		Sentence par = (Sentence) obj;
		return par.getText().equals(getText());
	}
}
