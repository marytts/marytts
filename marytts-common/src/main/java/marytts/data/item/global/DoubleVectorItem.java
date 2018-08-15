package marytts.data.item.global;

import java.util.List;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import marytts.data.item.Item;

/**
 * A class to represent an double vector item
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DoubleVectorItem extends Item {

    /** Values */
    private DenseDoubleMatrix1D m_values;

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
    public DoubleVectorItem(List<Double> values) {
        super();
        double[] d_val = new double[values.size()];
        for (int i=0; i<d_val.length; i++)
            d_val[i] = values.get(i).doubleValue();

        setValues(new DenseDoubleMatrix1D(d_val));
    }

    /**
     *  Copy constructor
     */
    public DoubleVectorItem(Double[] values) {
        super();
        double[] d_val = new double[values.length];
        for (int i=0; i<d_val.length; i++)
            d_val[i] = values[i].doubleValue();

        setValues(new DenseDoubleMatrix1D(d_val));
    }

    /**
     *  Copy constructor
     */
    public DoubleVectorItem(double[] values) {
        super();
        setValues(new DenseDoubleMatrix1D(values));
    }

    /**
     *  Copy constructor
     */
    public DoubleVectorItem(DenseDoubleMatrix1D values) {
        super();
        setValues(values);
    }

    /**
     *  Set the values
     *
     *  @param values the new values
     */
    public void setValues(DenseDoubleMatrix1D values) {
        m_values = values;
    }

    /**
     *  Get the values
     *
     *  @return the values
     */
    public DenseDoubleMatrix1D getValues() {
        return m_values;
    }

    /**
     *  Get the values
     *
     *  @return the values
     */
    public double[] getArrayValues() {
        return m_values.toArray();
    }

    @Override
    public String toString() {
        return m_values.toString();
    }
}
