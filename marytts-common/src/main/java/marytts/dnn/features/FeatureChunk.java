package marytts.dnn.features;

import java.nio.FloatBuffer;

import org.tensorflow.Tensor;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import marytts.data.item.global.DoubleMatrixItem;
import marytts.dnn.FeatureNormaliser;

/**
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class FeatureChunk extends DoubleMatrixItem {

    private FeatureNormaliser normaliser;

    /**
     * Constructor
     *
     */
    public FeatureChunk(DenseDoubleMatrix2D data) {
        super(data);
	setNormaliser(null);
    }

    public FeatureChunk(DenseDoubleMatrix2D data, FeatureNormaliser normaliser) {
        super(data);
	setNormaliser(normaliser);
    }



    public FeatureChunk(Tensor<Float> data, FeatureNormaliser normaliser) {
        super();
        setTensorData(data);
	setNormaliser(normaliser);
    }

    public void setTensorData(Tensor<Float> data) {
        long[] shape = data.shape();
        assert shape.length == 2;

        double[][] vector = new double[(int) shape[0]][(int) shape[1]];
        data.copyTo(vector);
        setValues(new DenseDoubleMatrix2D(vector));
    }

    public Tensor<Float> getTensorData() {
        // Get the shape
        long[] shape = new long[2];
        shape[0] = getValues().rows();
        shape[1] = getValues().columns();

        // Generate a float buffer
        FloatBuffer buf = FloatBuffer.allocate(getValues().size());
        for (int i=0; i<getValues().rows(); i++)
            for (int j=0; j<getValues().columns(); j++)
                buf.put((float) getValues().getQuick(i, j));
        buf.rewind();

        // Fill everything
        Tensor<Float> res = Tensor.create(shape, buf);
        return res;
    }


    public FeatureNormaliser getNormaliser() {
	return normaliser;
    }

    public void setNormaliser(FeatureNormaliser normaliser) {
	this.normaliser = normaliser;
    }
}
