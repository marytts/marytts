package marytts.signalproc.adaptation.gmm.jointgmm;

import marytts.signalproc.adaptation.gmm.GMMMatch;

public class JointGMMMatch extends GMMMatch {
    public double[] mappedSourceLsfs; //Source LSFs mapped onto source acoustic space
    public double[] outputLsfs; //Estimate of target LSFs on the target acoustic space
    
    public JointGMMMatch()
    {
        init(0);
    }
    
    public JointGMMMatch(int lpOrder)
    {
        init(lpOrder);
    }
    
    public void init(int lpOrder)
    {
        if (lpOrder>0)
        {
            mappedSourceLsfs = new double[lpOrder];
            outputLsfs = new double[lpOrder];
        }
        else
        {
            mappedSourceLsfs = null;
            outputLsfs = null;
        }
    }

}
