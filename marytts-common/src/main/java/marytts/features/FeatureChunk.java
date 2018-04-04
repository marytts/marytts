package marytts.features;

import marytts.data.item.Item;

import org.tensorflow.Tensor;


/**
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class FeatureChunk extends Item {

    Tensor<Float> data;
    /**
     * Constructor
     *
     */
    public FeatureChunk(Tensor<Float> data) {
        super();
	setData(data);
    }

    public Tensor<Float> getData() {
	return data;
    }

    public void setData(Tensor<Float> data) {
	this.data = data;
    }
}
