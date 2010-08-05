package marytts.modules.acoustic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.text.Document;

import org.w3c.dom.Element;

import marytts.cart.io.DirectedGraphReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.TargetFeatureComputer;
import marytts.machinelearning.SoP;
import marytts.modules.phonemiser.Allophone;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;

public class SoPModel extends Model {
    
    // If duration this map will contain several sop equations
    // if f0 this map will contain just one sop equation
    private Map<String, SoP> sopModels;
    
    FeatureDefinition sopFeatureDefinition;

    public SoPModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName, String modelFeatureName) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, targetElementListName, modelFeatureName);
    }
    
    @Override
    public void setFeatureComputer(TargetFeatureComputer featureComputer, FeatureProcessorManager featureProcessorManager) 
       throws MaryConfigurationException {
        // ensure that this SoP's FeatureDefinition is a subset of the one passed in:
              
        FeatureDefinition voiceFeatureDefinition = featureComputer.getFeatureDefinition();
        if (!voiceFeatureDefinition.contains(sopFeatureDefinition)) {
            throw new MaryConfigurationException("SoP file " + dataFile + " contains extra features which are not supported!");
        }
        
        // overwrite featureComputer with one constructed from the sop's FeatureDefinition:
        String cartFeatureNames = sopFeatureDefinition.getFeatureNames();
        featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, cartFeatureNames);
        this.featureComputer = featureComputer;

    }

    @Override
    public void loadDataFile() {
       
        sopModels = new HashMap<String, SoP>();
        
        String nextLine, nextType;
        String strContext="";
        Scanner s = null;
        try {
          s = new Scanner(new BufferedReader(new FileReader(dataFile)));
          
          // The first part contains the feature definition
          while (s.hasNext()) {
            nextLine = s.nextLine(); 
            if (nextLine.trim().equals("")) break;
            else
              strContext += nextLine + "\n";
          }
          // the featureDefinition is the same for vowel, consonant and Pause
          sopFeatureDefinition = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

          while (s.hasNext()){
            nextType = s.nextLine();
            nextLine = s.nextLine();
            
            if (nextType.startsWith("f0")){                
                sopModels.put("f0", new SoP(nextLine, sopFeatureDefinition));
            } else {
                sopModels.put(nextType, new SoP(nextLine, sopFeatureDefinition));    
            }
          }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (s != null)
              s.close();
        }   

        
    }

    /**
     * Apply the SoP to a Target to get its predicted value
     */
    @Override
    protected float evaluate(Target target) {
        float result=0;
        
        if(targetAttributeName.contentEquals("f0")){            
          result = (float) sopModels.get("f0").interpret(target);
        } else {          
          if(target.getAllophone().isVowel())
            result = (float) sopModels.get("vowel").interpret(target);
          else if(target.getAllophone().isConsonant())
            result = (float) sopModels.get("consonant").interpret(target);
          else if(target.getAllophone().isPause())
              result = (float) sopModels.get("pause").interpret(target);
          else
            System.out.println("Warning: No SoP model for this target");
        } 
               
        
        return result;
    }    
    
    @Override
    protected void evaluate(List<Element> applicableElements){ }
    
    @Override
    protected void evaluate(org.w3c.dom.Document doc){ }
    
}
