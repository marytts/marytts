package marytts.data.item.acoustic;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import marytts.data.item.global.DoubleVectorItem;

/**
 * A class to represent the F0 values into an item.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class F0List extends DoubleVectorItem {

    public F0List() {
        super();
    }

    public F0List(DenseDoubleMatrix1D val) {
        super(val);
    }
}
