package marytts.cart;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DecisionTree
{
    public enum TypeValue { STRING, PDF};
    private TypeValue type_value;
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
        this.type_value = type;
    }

    public void setLeftRight(DecisionTree left, DecisionTree right)
    {
        sons = new DecisionTree[2];
        sons[0] = left;
        sons[1] = right;
    }

    public booleans isLeaf()
    {
        return sons == null;
    }

    public DecisionTree interpret(Object value)
    {
        /* TODO */
        return null;
    }

    public Object getValue()
    {
        return this.value;
    }

    public String getName()
    {
        return this.name;
    }

    public TypeValue getTypeValue()
    {
        return type_value;
    }

}


/* DecisionTree.java ends here */
