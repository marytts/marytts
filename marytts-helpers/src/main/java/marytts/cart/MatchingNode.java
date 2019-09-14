package marytts.cart;

import java.util.Set;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class MatchingNode implements IndexedNode {
    private Set<String> m_matching_values;

    public MatchingNode(Set<String> matching_values) {
        m_matching_values = matching_values;
    }

    public int getIndex(Object value) {
        if (value == null) {
            return 1;
        }
        String the_real_value = value.toString();

        if (m_matching_values.contains(the_real_value)) {
            return 0;
        }

        return 1;
    }
}

