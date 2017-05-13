package marytts.data.item.linguistic;

import java.util.ArrayList;

import marytts.data.item.Item;
import marytts.data.item.prosody.Phrase;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Sentence extends Item {
	private String m_text;

	public Sentence(String text) {
		super();
		setText(text);
	}

	public String getText() {
		return m_text;
	}

	protected void setText(String text) {
		m_text = text;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Sentence))
			return false;

		Sentence sent = (Sentence) obj;
		return sent.getText().equals(getText());
	}
}
