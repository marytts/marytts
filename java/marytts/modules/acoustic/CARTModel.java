package marytts.modules.acoustic;

import java.io.File;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.TargetFeatureComputer;
import marytts.unitselection.select.Target;

/**
 * Model for applying a CART to a list of Targets
 * 
 * @author steiner
 * 
 */
public class CARTModel extends Model {
    private DirectedGraph cart;

    public CARTModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName, String modelFeatureName) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, targetElementListName, modelFeatureName);
    }

    @Override
    public void setFeatureComputer(TargetFeatureComputer featureComputer) throws MaryConfigurationException {
        // ensure that this CART's FeatureDefinition is a subset of the one passed in:
        FeatureDefinition cartFeatureDefinition = cart.getFeatureDefinition();
        FeatureDefinition voiceFeatureDefinition = featureComputer.getFeatureDefinition();
        if (!voiceFeatureDefinition.contains(cartFeatureDefinition)) {
            throw new MaryConfigurationException("CART file " + dataFile + " contains extra features which are not supported!");
        }
        // TODO should we overwrite featureComputer with one constructed from the cart's FeatureDefinition? if so, how?
        this.featureComputer = featureComputer;
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
