package de.dfki.lt.mary.unitselection.voiceimport;

public class RndDiscretizer implements Discretizer {
    private int step;
    private int max;
    
    public RndDiscretizer(int aStep, int aMax) {
        this.step = aStep;
        this.max = aMax;
    }
    
    public int discretize(int aValue){
        
        if (aValue > this.max)
            aValue = this.max;
        
        return (aValue / this.step) * this.step;
    }

    public int[] getPossibleValues(){
        
        int[] retVals = new int[max/step + 1];
        
        for (int i = 0; i < retVals.length; i++){
            retVals[i] = i * this.step;
        }
        
        return retVals;
    }
}
