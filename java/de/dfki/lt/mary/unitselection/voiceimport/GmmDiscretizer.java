package de.dfki.lt.mary.unitselection.voiceimport;

/**
 * 
 * This discretizes values according to a gaussian mixture model (gmm).
 * The result of discretization is the mean of the class that contributed most
 * probability to a point.
 * 
 * @author benjaminroth
 *
 */
public class GmmDiscretizer implements Discretizer {
    
    private int[] means; 
    private double[] stdDevs; 
    private double[] probs;
    private boolean extraZero;
    
    public GmmDiscretizer(int[] means, double[] stdDevs, double[] probs, boolean extraZeroClass ) {
        this.means = means;
        this.probs = probs;
        this.stdDevs = stdDevs;
        this.extraZero = extraZeroClass;
    }
    
    private double normPdf(double x, double m, double s){
        // TODO: store constants
        return Math.exp(-Math.pow(x-m,2)/(2 * Math.pow(s, 2))) / (s * Math.sqrt(2 * Math.PI));
    }

    public int discretize(int value) {
        
        if (this.extraZero && value == 0)
            return 0;
        
        int maxClass = 0;
        double maxP = 0f;
        
        for (int i = 0; i < means.length; i++ ){
            double contrib = probs[i] * normPdf(value, means[i], stdDevs[i]);
            if ( contrib > maxP ){
                maxClass = i;
                maxP =  contrib;
                }
        }

        return means[maxClass];
    }

    public int[] getPossibleValues() {
        
        if (this.extraZero){
            // TODO: space for optimization
            int[] retArr = new int[this.means.length + 1];
            retArr[0] = 0;
            
            for ( int i = 0; i < this.means.length ; i++){
                retArr[i+1] = this.means[i];
            }
            
            
            return retArr;
        }
        
        return means;
    }
    
    

}
