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
    private List<Integer> m_values;
    private List<Integer> m_positions;

    public F0List() {
        super();
        setValues(new ArrayList<Integer>());
        setPositions(new ArrayList<Integer>());
    }

    public F0List(List<Integer> values) {
        super();
        setValues(values);
        setPositions(new ArrayList<Integer>());
    }

    public F0List(List<Integer> positions, List<Integer> values) {
        super();
        setValues(values);
        setPositions(positions);
    }

    public void setPositions(List<Integer> positions) {
        m_positions = positions;
    }

    public void setValues(List<Integer> values) {
        m_values = values;
    }

    public List<Integer> getValues() {
        return m_values;
    }

    public List<Integer> getPositions() {
        return m_positions;
    }

    @Override
    public String toString() {
        String output = "";
        if (m_positions.size() == 0) {
            output = "[";
            if (m_values.size() > 0) {
                for (int i = 0; i < m_values.size() - 1; i++) {
                    output += m_values.get(i).toString() + ", ";
                }

                output += m_values.get(m_values.size() - 1).toString();
            }
            output += "]";
        } else {
            assert m_positions.size() == m_values.size();
            for (int i = 0; i < m_values.size(); i++) {
                output += "(" + m_positions.get(i).toString() + "," + m_values.get(i).toString() + ")";
            }
        }

        return output;
    }
}
