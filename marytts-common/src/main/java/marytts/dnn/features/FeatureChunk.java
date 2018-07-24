package marytts.dnn.features;

import marytts.data.item.Item;

import org.tensorflow.Tensor;

import marytts.dnn.FeatureNormaliser;

/**
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class FeatureChunk extends Item {

    private Tensor<Float> data;
    private FeatureNormaliser normaliser;

    /**
     * Constructor
     *
     */
    public FeatureChunk(Tensor<Float> data) {
        super();
	setData(data);
	setNormaliser(null);
    }

    public FeatureChunk(Tensor<Float> data, FeatureNormaliser normaliser) {
        super();
	setData(data);
	setNormaliser(normaliser);
    }

    public Tensor<Float> getData() {
	return data;
    }

    public void setData(Tensor<Float> data) {
	this.data = data;
    }



    public FeatureNormaliser getNormaliser() {
	return normaliser;
    }

    public void setNormaliser(FeatureNormaliser normaliser) {
	this.normaliser = normaliser;
    }
}
