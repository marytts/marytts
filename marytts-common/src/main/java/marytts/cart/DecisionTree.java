package marytts.cart;

import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DecisionTree
{
    private Object value;
    private String name;
    private DecisionTree[] sons;

    public DecisionTree()
    {
    }

    public DecisionTree(String name, Object value)
    {
        this.name = name;
        this.value = value;
    }

    public void setLeftRight(DecisionTree left, DecisionTree right)
    {
        sons = new DecisionTree[2];
        sons[0] = left;
        sons[1] = right;
    }

    public boolean isLeaf()
    {
        return sons == null;
    }

    public Object traverse(Map<String, String> features)
    {
        if (isLeaf())
        {
            System.out.println(name);
            return getValue();
        }

        if (features.get(getName()).equals(getValue()))
        {
            System.out.print(name + "=>");
            return sons[1].traverse(features);
        }

        /* TODO */
        System.out.print("!" + name + "=>");
        return sons[0].traverse(features);
    }

    public Object getValue()
    {
        return this.value;
    }

    public String getName()
    {
        return this.name;
    }

}


/* DecisionTree.java ends here */
