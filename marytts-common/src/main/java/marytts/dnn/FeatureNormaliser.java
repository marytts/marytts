package marytts.dnn;

// Java utils
import java.util.ArrayList;

// Tensorflow
import org.tensorflow.Tensor;

// Marytts
import marytts.features.FeatureMap;
import marytts.data.Sequence;
import marytts.MaryException;;

/**
 * The feature normaliser interface.
 *
 * The normaliser goal is to provide a float tensor (for tensorflow) using a sequence of feature maps.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public interface FeatureNormaliser
{
    /**
     *  The normalising method which provides the logic to generate the binary matrix from the feature maps.
     *
     *  @param list_feature_map the feature maps
     *  @return the binary matrix
     *  @throw MaryException if anything is going wrong
     */
    public Tensor<Float> normalise(Sequence<FeatureMap> list_feature_map) throws MaryException;

    /**
     *  Method to get the head which describes each elements of the vector. This method is more for
     *  debugging purpose
     *
     *  @return the header in a form of a list of ids.
     */
    public ArrayList<String> getHeader();
}


