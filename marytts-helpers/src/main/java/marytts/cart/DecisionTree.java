package marytts.cart;

import java.util.Map;
import marytts.cart.MatchingNode;
import de.dfki.lt.tools.tokenizer.regexp.Match;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class DecisionTree {
    private Object value;
    private String name;
    private DecisionTree[] sons;

    public DecisionTree() {
    }

    public DecisionTree(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public void setLeftRight(DecisionTree left, DecisionTree right) {
        sons = new DecisionTree[2];
        sons[0] = left;
        sons[1] = right;
    }

    public boolean isLeaf() {
        return sons == null;
    }

    public Object traverse(Map<String, String> features) {
        if (isLeaf()) {
            System.out.println(name);
            return getValue();
        }

        assert getValue() instanceof MatchingNode;
        MatchingNode the_node = (MatchingNode) getValue();
        Object val = features.get(getName().split("=")[0]);
        if (the_node == null) {
            System.out.println(name);
        }
        int idx = the_node.getIndex(val);

        /* TODO */
        System.out.print(name + " (" + getValue().toString() + ") =>");
        return sons[idx].traverse(features);
    }

    public Object getValue() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

}

