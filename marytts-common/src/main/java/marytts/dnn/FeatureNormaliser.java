package marytts.dnn;


import org.tensorflow.Tensor;
import marytts.features.FeatureMap;
import marytts.data.Sequence;

import marytts.MaryException;;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public abstract class FeatureNormaliser
{
    public abstract Tensor<Float> normalise(Sequence<FeatureMap> list_feature_map) throws MaryException;
}


/* FeatureNormaliser.java ends here */
