package marytts.data.item.global;

import cern.colt.matrix.DoubleMatrix1D;
import marytts.data.item.Item;

/**
 * A class to represent an double vector item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DoubleVectorItem extends Item {

    /** Values */
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
     *  Set the values
     *
     *  @param values the new values
     */
    public void setValues(DoubleMatrix1D values) {
        m_values = values;
    }

    /**
     *  Get the values
     *
     *  @return the values
     */
    public DoubleMatrix1D getValues() {
        return m_values;
    }

    @Override
    public String toString() {
        return m_values.toString();
    }
}
