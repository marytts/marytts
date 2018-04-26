package marytts.data.item.acoustic;

import java.util.List;
import java.util.ArrayList;
import marytts.data.item.Item;

/**
 * A phone representation. A phone is a phoneme with a position in the signal
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class F0List extends Item {
    private List<Float> m_values;

    public F0List() {
        super();
        setValues(new ArrayList<Float>());
    }

    public F0List(List<Float> values) {
        super();
        setValues(values);
    }
    public void setValues(List<Float> values) {
        m_values = values;
    }

    public List<Float> getValues() {
        return m_values;
    }

    @Override
    public String toString() {
        String output = "";
	output = "[";
	if (m_values.size() > 0) {
	    for (int i = 0; i < m_values.size() - 1; i++) {
		output += m_values.get(i).toString() + ", ";
	    }

	    output += m_values.get(m_values.size() - 1).toString();
	}
	output += "]";

        return output;
    }
}
