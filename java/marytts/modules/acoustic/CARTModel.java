package marytts.modules.acoustic;

import java.io.File;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.unitselection.select.Target;

/**
 * Model subclass for applying a CART to a list of Targets
 * 
 * @author steiner
 * 
 */
public class CARTModel extends Model {
    private DirectedGraph cart;
    private FeatureProcessorManager featureProcessorManager;

    public CARTModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName, FeatureProcessorManager featureProcessorManager) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, targetElementListName);
        this.featureProcessorManager = featureProcessorManager;
    }

    @Override
    public void loadDataFile() {
        this.cart = null;
        try {
            File cartFile = new File(dataFile);
            String cartFilePath = cartFile.getAbsolutePath();
            cart = new DirectedGraphReader().load(cartFilePath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // unless we already have a featureComputer, load the CART's:
        if (featureComputer == null) {
            featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, cart.getFeatureDefinition()
                    .getFeatureNames());
        }
    }

    /**
     * Apply the CART to a Target to get its predicted value
     */
    @Override
    protected float evaluate(Target target) {
        float[] result = (float[]) cart.interpret(target);
        float value = result[1]; // assuming result is [stdev, val]
        return value;
    }
}
