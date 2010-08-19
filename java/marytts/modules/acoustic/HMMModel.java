package marytts.modules.acoustic;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import marytts.cart.io.DirectedGraphReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;

public class HMMModel extends Model {
    
    private HMMData htsData = null;
    HTSUttModel um = null;
    private CartTreeSet cart;    
    private float fperiodsec;
    protected static Logger logger = MaryUtils.getLogger("HMMModel");    
    protected double diffDuration;
    FeatureDefinition hmmFeatureDefinition;
    
    
    public HMMModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String featureName, String predictFrom, String applyTo) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, featureName, predictFrom, applyTo);
    }
    
    @Override
    public void setFeatureComputer(TargetFeatureComputer featureComputer, FeatureProcessorManager featureProcessorManager) 
       throws MaryConfigurationException {
        // ensure that this HMM's FeatureDefinition is a subset of the one passed in:
              
        FeatureDefinition voiceFeatureDefinition = featureComputer.getFeatureDefinition();
        if (!voiceFeatureDefinition.contains(hmmFeatureDefinition)) {
            throw new MaryConfigurationException("HMM file " + dataFile + " contains extra features which are not supported!");
        }
        
        // overwrite featureComputer with one constructed from the HMM's FeatureDefinition:
        String hmmFeatureNames = hmmFeatureDefinition.getFeatureNames();
        featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, hmmFeatureNames);
        this.featureComputer = featureComputer;

    }
    
    @Override
    public void loadDataFile() {
        if(htsData==null)
          htsData = new HMMData();
        try {
            // the dataFile is the configuration file of the HMM voice whose hmm models will be used
            htsData.initHMMData(dataFile, targetAttributeName);
            cart = htsData.getCartTreeSet();                       
            fperiodsec = ((float)htsData.getFperiod() / (float)htsData.getRate());
            diffDuration = 0.0;
            hmmFeatureDefinition = htsData.getFeatureDefinition();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    public void applyTo(List<Element> elements) {
      logger.debug("predicting duration");  
      if( targetAttributeName.contentEquals("d f0") || targetAttributeName.contentEquals("f0 d"))
          predictAndSet(elements, elements);          
      else { // then it needs to predict and set Duration
             // targetAttributeName must be "d"          
          diffDuration = 0;  
          super.applyFromTo(elements, elements); // so here it will execute evaluate()      
      }  
    }
    
    
    @Override
    public void applyFromTo(List<Element> predictFromElements, List<Element> applyToElements) {
      logger.debug("predicting F0");  
      if( targetAttributeName.contentEquals("d f0") || targetAttributeName.contentEquals("f0 d"))  
        predictAndSet(predictFromElements, applyToElements);
      else { // then it needs to predict and set F0 from the durations already set in predictFromElements
             // targetAttributeName must be "f0" 
          //set um with the duration in elements and call predictAndSetF0 
          setUttModel(predictFromElements, applyToElements);
          predictAndSetF0(applyToElements);
      }
          
        
    }
    
    private void predictAndSet(List<Element> predictFromElements, List<Element> applyToElements) {
        if(um == null) {
            predictAndSetDuration(predictFromElements, applyToElements); 
        } else {
            predictAndSetF0(applyToElements);
        }
    }
    
    
    private void predictAndSetDuration(List<Element> predictFromElements, List<Element> applyToElements){
        int i, k, s, t, mstate, frame, durInFrames, durStateInFrames, numVoicedInModel;
        HTSModel m;
        try {     
        // first time this is called um is null           
          List<Element> predictorElements = predictFromElements;        
          List<Target> predictorTargets = getTargets(predictorElements);                
          FeatureVector fv;       
          um = new HTSUttModel();                
          FeatureDefinition feaDef = htsData.getFeatureDefinition();
          float duration;       
          double diffdurOld = 0.0;
          double diffdurNew = 0.0;
          float f0s[] = null;
          String durAttributeName = "d";
          
          // (1) Predict the values       
          for (i = 0; i < predictorTargets.size(); i++) {            
            fv = predictorTargets.get(i).getFeatureVector();                        
            um.addUttModel(new HTSModel(cart.getNumStates()));            
            m = um.getUttModel(i);
            /* this function also sets the phone name, the phone between - and + */
            m.setPhoneName(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef));
            
            /* increment number of models in utterance model */
            um.setNumModel(um.getNumModel()+1);
            /* update number of states */
            um.setNumState(um.getNumState() + cart.getNumStates());
            
            // Estimate state duration from state duration model (Gaussian)              
            diffdurNew = cart.searchDurInCartTree(m, fv, htsData, diffdurOld);
            diffdurOld = diffdurNew;
            duration  = m.getTotalDur() * fperiodsec; // in seconds
   
            um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());  
            //System.out.format("duration=%.3f sec. durInFrames=%d  durStateInFrames=%d  m.getTotalDur()=%d\n", duration, durInFrames, durStateInFrames, m.getTotalDur());
              
            /* Find pdf for LF0, this function sets the pdf for each state. 
             * and determines, according to the HMM models, whether the states are voiced or unvoiced, (it can be possible that some states are voiced
             * and some unvoiced).*/ 
            //if( targetAttributeName.contentEquals("d f0")  ) {
            // since this is done for both dur and f0...
              cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());
              for(mstate=0; mstate<cart.getNumStates(); mstate++) 
              {  
                for(frame=0; frame<m.getDur(mstate); frame++)    
                  if(m.getVoiced(mstate))                      
                    um.setLf0Frame(um.getLf0Frame() +1);     
              }
            //}
            
            // set the value in elements
            Element element = applyToElements.get(i);
            // "evaluate" pseudo XPath syntax:
            // TODO this needs to be extended to take into account targetAttributeNames like "foo/@bar", which would add the
            // bar attribute to the foo child of this element, creating the child if not already present...
            if (durAttributeName.startsWith("@")) {
                durAttributeName = durAttributeName.replaceFirst("@", "");
            }          
            String formattedTargetValue = String.format(targetAttributeFormat, duration);
            
            //System.out.println("formattedTargetValue = " + formattedTargetValue);
            
            // if the attribute already exists for this element, append targetValue:
            if (element.hasAttribute(durAttributeName)) {
                formattedTargetValue = element.getAttribute(durAttributeName) + " " + formattedTargetValue;
            }

            // set the new attribute value:
            element.setAttribute(durAttributeName, formattedTargetValue);
            
          }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
        
    }
    
    private void predictAndSetF0(List<Element> applyToElements){
      int i, k, s, t, mstate, frame, numVoicedInModel;
      HTSModel m;
      try {
          String f0AttributeName = "f0";  
          HTSParameterGeneration pdf2par = new HTSParameterGeneration();
          /* Once we have all the phone models Process UttModel */
          /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */  
          boolean debug = false;  /* so it does not save the generated parameters. */
          /* this function generates features just for the trees and pdf that are not null in the HMM cart*/
          pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);
              
          // (2) include the predicted values in  applicableElements (as it is done in Model)
          boolean voiced[] = pdf2par.getVoicedArray();  
          
          // make sure that the number of applicable elements is the same as the predicted number of elements
          assert applyToElements.size() == um.getNumModel();        
          float f0;
          String formattedTargetValue;
          t=0;
          for (i = 0; i < applyToElements.size(); i++) {  // this will be the same as the utterance model set
            m = um.getUttModel(i);
            k = 1;
            numVoicedInModel = m.getNumVoiced();
            formattedTargetValue = "";
            //System.out.format("phone = %s dur_in_frames=%d  num_voiced_frames=%d : ", m.getPhoneName(), m.getTotalDur(), numVoicedInModel);
            for(mstate=0; mstate<cart.getNumStates(); mstate++) {
              for(frame=0; frame<m.getDur(mstate); frame++) {
                if( voiced[t++] ){
                  f0 = (float)Math.exp(pdf2par.getlf0Pst().getPar(i,0));
                  formattedTargetValue += "(" + Integer.toString((int)((k*100.0)/numVoicedInModel)) + "," + Integer.toString((int)f0) + ")";
                  k++;
                }
                //else
                //  f0 = 0.0f;                       
              }
            }       
            Element element = applyToElements.get(i);        
            // "evaluate" pseudo XPath syntax:
            // TODO this needs to be extended to take into account targetAttributeNames like "foo/@bar", which would add the
            // bar attribute to the foo child of this element, creating the child if not already present...
            if (f0AttributeName.startsWith("@")) {
                f0AttributeName = f0AttributeName.replaceFirst("@", "");
            }
            // format targetValue according to targetAttributeFormat
            //String formattedTargetValue = String.format(targetAttributeFormat, targetValue);
            // set the new attribute value:
            // if the whole segment is unvoiced then f0 should not be fixed?
            if(formattedTargetValue.length() > 0)
              element.setAttribute(f0AttributeName, formattedTargetValue);
            //System.out.println(formattedTargetValue);                 
          }
          // once finished re-set to null um
          um = null;
              
        } catch (Exception e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
        }     
        
    }
    
    private void setUttModel(List<Element> predictFromElements, List<Element> applyToElements){
        int i, k, s, t, mstate, frame, durInFrames, durStateInFrames, numVoicedInModel;
        HTSModel m;
        try {     
        // first time this is called um is null           
          List<Element> predictorElements = predictFromElements;        
          List<Target> predictorTargets = getTargets(predictorElements);                
          FeatureVector fv;       
          um = new HTSUttModel();                
          FeatureDefinition feaDef = htsData.getFeatureDefinition();
          float duration;       
          double diffdurOld = 0.0;
          double diffdurNew = 0.0;
          float f0s[] = null;
          String durAttributeName = "d";
          
          // (1) Predict the values       
          for (i = 0; i < predictorTargets.size(); i++) {            
            fv = predictorTargets.get(i).getFeatureVector();  
            Element e = predictFromElements.get(i);
            um.addUttModel(new HTSModel(cart.getNumStates()));            
            m = um.getUttModel(i);
            /* this function also sets the phone name, the phone between - and + */
            m.setPhoneName(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef));
            
            /* increment number of models in utterance model */
            um.setNumModel(um.getNumModel()+1);
            /* update number of states */
            um.setNumState(um.getNumState() + cart.getNumStates());
            // get the duration from the element 
            duration = Integer.parseInt(e.getAttribute("d")) * 0.001f;  // in sec.
            // distribute the duration (in frames) among the five states, here it is done the same amount for each state        
            durInFrames = (int)(duration / fperiodsec);
            durStateInFrames = (int)(durInFrames / cart.getNumStates()); 
            m.setTotalDur(0); // reset to set new value according to duration
            for(s=0; s<cart.getNumStates(); s++){
              m.setDur(s, durStateInFrames);  
              m.setTotalDur(m.getTotalDur() + m.getDur(s));
            }
            um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());  
            //System.out.format("duration=%.3f sec. durInFrames=%d  durStateInFrames=%d  m.getTotalDur()=%d\n", duration, durInFrames, durStateInFrames, m.getTotalDur());
              
            /* Find pdf for LF0, this function sets the pdf for each state. 
             * and determines, according to the HMM models, whether the states are voiced or unvoiced, (it can be possible that some states are voiced
             * and some unvoiced).*/ 
            //if( targetAttributeName.contentEquals("d f0")  ) {
            // since this is done for both dur and f0...
            cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());
            for(mstate=0; mstate<cart.getNumStates(); mstate++) 
            {  
              for(frame=0; frame<m.getDur(mstate); frame++)    
                if(m.getVoiced(mstate))                      
                  um.setLf0Frame(um.getLf0Frame() +1);     
            }           
          }
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
        
        FeatureVector fv;                
        double diffdurNew = 0.0;       
        float durSec = 0;
       
        HTSModel m = new HTSModel(cart.getNumStates());
        int i;
        try {
            m.setTotalDur(0);
            fv = target.getFeatureVector();            
            diffdurNew = cart.searchDurInCartTree(m, fv, htsData, diffDuration);           
            durSec = (fperiodsec * m.getTotalDur());  // Duration in seconds
            diffDuration = diffdurNew;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }    
        return durSec;    
            
    }
    

}
