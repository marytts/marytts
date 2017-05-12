package marytts.data.item.linguistic;

import java.util.ArrayList;

import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Paragraph extends Item
{
	private String m_text;

	public Paragraph(String text)
	{
        super();
        setText(text);
    }

    public String getText()
	{
		return m_text;
	}

	protected void setText(String text)
	{
		m_text = text;
	}

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Paragraph))
            return false;

        Paragraph par = (Paragraph) obj;
        return par.getText().equals(getText());
    }
}
