package marytts.modules.acoustic;

import java.io.File;
import java.util.List;

import org.w3c.dom.Element;

import marytts.cart.io.DirectedGraphReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.unitselection.select.Target;

public class HMMModel extends Model {
    
    private HMMData htsData;
    private CartTreeSet cart;    
    private float fperiodsec;
    
    public HMMModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName, String modelFeatureName) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, targetElementListName, modelFeatureName);
    }
    
    @Override
    public void loadDataFile() {
        htsData = new HMMData();
        try {
            // the dataFile is the configuration file of the HMM voice whose hmm models will be used
            htsData.initHMMData(dataFile, targetAttributeName);
            cart = htsData.getCartTreeSet();           
            
            fperiodsec = ((float)htsData.getFperiod() / (float)htsData.getRate());
            diffDuration = 0.0;
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
        float result=0;
        if(targetAttributeName.contentEquals("d")) {   
           result = evaluateDur(target);  
        } else {
           //result = evaluateF0(target); 
        }
        return result;
    }
    
    // CHECK: which module is setting the duration for _ at the ini and end
    protected float evaluateDur(Target target) 
    {
        FeatureVector fv;
        boolean firstPh = true; // not used here
        boolean lastPh = false; // not used here       
        double diffdurNew = 0.0;       
        float totalDurSec = 0;
        HTSModel m = new HTSModel(cart.getNumStates());
        
        fv = target.getFeatureVector();
        m.setPhoneName(fv.getFeatureAsString(htsData.getFeatureDefinition().getFeatureIndex("phone"), htsData.getFeatureDefinition()));
        m.setTotalDur(0);
        try{   
          
          diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, lastPh, diffDuration); 
          totalDurSec = (fperiodsec * m.getTotalDur()); 
          
          System.out.println("phone=" + m.getPhoneName() + " dur=" +  m.getTotalDur() + "(" + totalDurSec + ")  diffdurNew = " + diffdurNew + "  diffdurOld = " + diffDuration);
          diffDuration = diffdurNew;  // first time it will be 0, how to reset it when finish with a sentence???? 
          
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return totalDurSec;
    }
    
    // For predicting f0 it is needed the whole sequence at once
    protected void evaluate(List<Element> applicableElements, List<Target> predictorTargets) {
      
     FeatureVector fv;
     HTSUttModel um = new HTSUttModel();
     HTSParameterGeneration pdf2par = new HTSParameterGeneration();
     HTSModel m;
     FeatureDefinition feaDef = htsData.getFeatureDefinition();
     float duration;
     int i, k, s, t, mstate, frame, durInFrames, durStateInFrames, numVoicedInModel;
     double diffdurOld = 0.0;
     double diffdurNew = 0.0;
     boolean firstPh = true;  // not used here
     boolean lastPh = false;  // not used here 
     float f0s[] = null;
     
     // (1) Predict the values
     try {
     for (i = 0; i < predictorTargets.size(); i++) {            
       fv = predictorTargets.get(i).getFeatureVector();            
       
       byte[] byteValues = fv.byteValuedDiscreteFeatures;
       for(k=0; k<byteValues.length; k++)
         System.out.print(byteValues[k] + " ");
       System.out.println();       
 
       um.addUttModel(new HTSModel(cart.getNumStates()));            
       m = um.getUttModel(i);
       /* this function also sets the phone name, the phone between - and + */
       m.setName(fv.toString(), fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef));
       
       /* increment number of models in utterance model */
       um.setNumModel(um.getNumModel()+1);
       /* update number of states */
       um.setNumState(um.getNumState() + cart.getNumStates());
       
       // we need to set the duration of this model per state, here it willbe set equally the amount of frames 
       // among the five states
       // get the duration in frames
       // I can use the duration that is already in the fv, but it needs to be fixed 
       // (I) this if using continuous features....
       //duration = fv.getContinuousFeature(feaDef.getFeatureIndex("unit_duration"));
         
       // (II) this if using HMM predicted duration
       // Estimate state duration from state duration model (Gaussian)                 
       diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, lastPh, diffdurOld);
       diffdurOld = diffdurNew;
       duration  = m.getTotalDur() * fperiodsec; // in seconds
               
       durInFrames = (int)(duration / fperiodsec);
       durStateInFrames = (int)(durInFrames / cart.getNumStates()); 
       m.setTotalDur(0); // reset to set new value according to duration
       for(s=0; s<cart.getNumStates(); s++){
         m.setDur(s, durStateInFrames);  
         m.setTotalDur(m.getTotalDur() + m.getDur(s));
       }
       um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());  
       System.out.format("duration=%.3f sec. durInFrames=%d  durStateInFrames=%d  m.getTotalDur()=%d\n", duration, durInFrames, durStateInFrames, m.getTotalDur());
       
         
       /* Find pdf for LF0, this function sets the pdf for each state. */ 
       cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());                       
     } 
          
     // Here we need to know how many frames do we have per state, since here we do not calculate duration
     // with HMMs then we do not know (unless we put it for example on the fv continuous features)
     // what we can do here is to get the total num of frames based on the total duration and split them
     // equally among the five states
     // voiced or unvoiced state is determined when searchLf0CartTree 
     for(i=0; i<um.getNumUttModel(); i++){
       m = um.getUttModel(i);                  
       System.out.format("phone=%s  model(%d) dur in frames=%d\n", m.getPhoneName(), i, m.getTotalDur());
       for(mstate=0; mstate<cart.getNumStates(); mstate++) 
       {  
         for(frame=0; frame<m.getDur(mstate); frame++)    
           if(m.getVoiced(mstate))                      
             um.setLf0Frame(um.getLf0Frame() +1);     
         if(m.getVoiced(mstate))
           System.out.format("  state(%d) : num frames(%d) : voiced\n", mstate, m.getDur(mstate));
         else
           System.out.format("  state(%d) : num frames(%d) : unvoiced\n", mstate, m.getDur(mstate));   
       }                                                       
     }                                                        

     /* Once we have all the phone models Process UttModel */
     /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */  
     boolean debug = false;  /* so it does not save the generated parameters. */
     /* this function generates features just for the trees and pdf that are not null in the HMM cart*/
     pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData,"", debug);
     
     } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
     }
    
     // (2) include the predicted values in  applicableElements (as it is done in Model
     boolean voiced[] = pdf2par.getVoicedArray();    
     assert applicableElements.size() == um.getNumModel();     
     float f0;
     String formattedTargetValue;
     t=0;
     for (i = 0; i < applicableElements.size(); i++) {  // this will be the same as the utterace modeles set
       m = um.getUttModel(i);
       k = 1;
       numVoicedInModel = m.getNumVoiced();
       formattedTargetValue = "";
       System.out.format("phone = %s dur in frames=%d num voiced frames=%d\n", m.getPhoneName(), m.getTotalDur(), numVoicedInModel);
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
       Element element = applicableElements.get(i);         
       // "evaulate" pseudo XPath syntax:
       // TODO this needs to be extended to take into account targetAttributeNames like "foo/@bar", which would add the
       // bar attribute to the foo child of this element, creating the child if not already present...
       if (targetAttributeName.startsWith("@")) {
         targetAttributeName = targetAttributeName.replaceFirst("@", "");
       }

       // format targetValue according to targetAttributeFormat
       //String formattedTargetValue = String.format(targetAttributeFormat, targetValue);
       // if the attribute already exists for this element, append targetValue:
       if (element.hasAttribute(targetAttributeName)) {
         formattedTargetValue = element.getAttribute(targetAttributeName) + " " + formattedTargetValue;
       }

       // set the new attribute value:
       // if the whole segment is unvoiced then f0 should not be fixed?
       if(formattedTargetValue.length() > 0)
         element.setAttribute(targetAttributeName, formattedTargetValue);
       System.out.println(formattedTargetValue);                 
     }
    }

}
