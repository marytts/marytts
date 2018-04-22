package marytts.dnn;

import java.util.ArrayList;

import org.tensorflow.Tensor;
import marytts.features.FeatureMap;
import marytts.data.Sequence;

import marytts.MaryException;;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public interface FeatureNormaliser
{
    public Tensor<Float> normalise(Sequence<FeatureMap> list_feature_map) throws MaryException;
    public ArrayList<String> getHeader();
}


/* FeatureNormaliser.java ends here */
