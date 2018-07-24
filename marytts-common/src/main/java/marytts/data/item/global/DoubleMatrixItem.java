package marytts.data.item.global;

import cern.colt.matrix.DoubleMatrix2D;
import marytts.data.item.Item;

/**
 * A class to represent an double vector item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DoubleMatrixItem extends Item {

    /** F0 Values */
    private DoubleMatrix2D m_values;

    /**
     *  Default constructor
     */
    public DoubleMatrixItem() {
        super();
        setValues(null);
    }

    /**
     *  Copy constructor
     */
    public DoubleMatrixItem(DoubleMatrix2D values) {
        super();
        setValues(values);
    }

    /**
     *  Set the F0 values
     *
     *  @param values the new F0 values
     */
    public void setValues(DoubleMatrix2D values) {
        m_values = values;
    }

    /**
     *  Get the F0 values
     *
     *  @return the F0 values
     */
    public DoubleMatrix2D getValues() {
        return m_values;
    }

    @Override
    public String toString() {
        return m_values.toString();
    }
}
