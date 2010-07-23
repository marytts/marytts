package marytts.modules.acoustic;

import java.io.File;

import marytts.cart.io.DirectedGraphReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.TargetFeatureComputer;
import marytts.machinelearning.SoP;
import marytts.unitselection.select.Target;

public class SoPModel extends Model {
    
    private SoP sop;

    public SoPModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName, String modelFeatureName) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, targetElementListName, modelFeatureName);
    }
    
    @Override
    public void setFeatureComputer(TargetFeatureComputer featureComputer) throws MaryConfigurationException {
        // ensure that this SoP's FeatureDefinition is a subset of the one passed in:
        
        // ??? this has to be call after the sop loaded the data, otherwise it does not have any featuredefinition...
        
        FeatureDefinition sopFeatureDefinition = sop.getFeatureDefinition();
        
        FeatureDefinition voiceFeatureDefinition = featureComputer.getFeatureDefinition();
        if (!voiceFeatureDefinition.contains(sopFeatureDefinition)) {
            throw new MaryConfigurationException("SoP file " + dataFile + " contains extra features which are not supported!");
        }
        // TODO should we overwrite featureComputer with one constructed from the cart's FeatureDefinition? if so, how?
        this.featureComputer = featureComputer;
    }

    @Override
    public void loadDataFile() {
        this.sop = new SoP();
        try {
            File sopFile = new File(dataFile);
            String sopFilePath = sopFile.getAbsolutePath();
            // the feature definition of this sop will be set in load()
            sop.load(sopFilePath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Apply the SoP to a Target to get its predicted value
     */
    @Override
    protected float evaluate(Target target) {
        float result = (float) sop.interpret(target);
        float value = result; 
        return value;
    }    
    
    
}
