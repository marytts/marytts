package marytts.data.item.global;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import marytts.data.item.Item;

/**
 * A class to represent an double vector item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DoubleMatrixItem extends Item {

    /** Values */
    private DenseDoubleMatrix2D m_values;

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
    public DoubleMatrixItem(DenseDoubleMatrix2D values) {
        super();
        setValues(values);
    }

    /**
     *  Set the values
     *
     *  @param values the new values
     */
    public void setValues(DenseDoubleMatrix2D values) {
        m_values = values;
    }

    /**
     *  Get the values
     *
     *  @return the values
     */
    public DenseDoubleMatrix2D getValues() {
        return m_values;
    }

    @Override
    public String toString() {
        return m_values.toString();
    }
}
