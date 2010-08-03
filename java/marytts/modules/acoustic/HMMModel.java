package marytts.modules.acoustic;

import java.io.File;

import marytts.cart.io.DirectedGraphReader;
import marytts.features.FeatureVector;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HTSModel;
import marytts.unitselection.select.Target;

public class HMMModel extends Model {
    
    private HMMData htsData;
    private CartTreeSet cart;
    private HTSModel m;
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
            m = new HTSModel(cart.getNumStates());
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
        boolean firstPh = true; 
        boolean lastPh = false;        
        double diffdurNew = 0.0;       
        float totalDurSec = 0;
      
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

}
