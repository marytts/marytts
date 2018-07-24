package marytts.data.item.global;

import cern.colt.matrix.DoubleMatrix1D;
import marytts.data.item.Item;

/**
 * A class to represent an double vector item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DoubleVectorItem extends Item {

    /** F0 Values */
    private DoubleMatrix1D m_values;

    /**
     *  Default constructor
     */
    public DoubleVectorItem() {
        super();
        setValues(null);
    }

    /**
     *  Copy constructor
     */
    public DoubleVectorItem(DoubleMatrix1D values) {
        super();
        setValues(values);
    }

    /**
     *  Set the F0 values
     *
     *  @param values the new F0 values
     */
    public void setValues(DoubleMatrix1D values) {
        m_values = values;
    }

    /**
     *  Get the F0 values
     *
     *  @return the F0 values
     */
    public DoubleMatrix1D getValues() {
        return m_values;
    }

    @Override
    public String toString() {
        return m_values.toString();
    }
}
