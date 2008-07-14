package marytts.signalproc.adaptation.gmm.jointgmm;

import marytts.signalproc.adaptation.gmm.GMMMatch;

public class JointGMMMatch extends GMMMatch {
    public double[] mappedSourceFeatures; //Source LSFs mapped onto source acoustic space
    public double[] outputFeatures; //Estimate of target LSFs on the target acoustic space
    
    public JointGMMMatch()
    {
        init(0);
    }
    
    public JointGMMMatch(int dimension)
    {
        init(dimension);
    }
    
    public void init(int dimension)
    {
        if (dimension>0)
        {
            mappedSourceFeatures = new double[dimension];
            outputFeatures = new double[dimension];
        }
        else
        {
            mappedSourceFeatures = null;
            outputFeatures = null;
        }
    }

}
