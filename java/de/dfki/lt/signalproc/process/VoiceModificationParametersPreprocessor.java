package de.dfki.lt.signalproc.process;

import de.dfki.lt.signalproc.util.InterpolationUtils;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

public class VoiceModificationParametersPreprocessor extends VoiceModificationParameters 
{    
    public double [] pscalesVar;
    public double [] tscalesVar;
    public double [] escalesVar;
    public double [] vscalesVar;
    
    public double tscaleSingle;
    public int numPeriods;

    public VoiceModificationParametersPreprocessor(int samplingRate, int LPOrder,
            double[] pscalesIn, double[] tscalesIn, double[] escalesIn, double[] vscalesIn, 
            int[] pitchMarksIn, 
            double wsFixedIn, double ssFixedIn, 
            int numfrm, int numfrmFixed, int numPeriodsIn, boolean isFixedRate) 
    {
        super(samplingRate, LPOrder, pscalesIn, tscalesIn, escalesIn, vscalesIn);
        
        initialise(pitchMarksIn, wsFixedIn, ssFixedIn, numfrm, numfrmFixed, numPeriodsIn, isFixedRate);
    }
    
    public VoiceModificationParametersPreprocessor(String targetFestivalUttFile, String sourcePitchFile,
                                                   double[] vscalesIn,
                                                   String sourceLabelFile, 
                                                   String sourceEnergyFile,
                                                   String targetLabelFile, 
                                                   String targetEnergyFile,
                                                   boolean isPscaleFromFestivalUttFile, 
                                                   boolean isTscaleFromFestivalUttFile, 
                                                   boolean isEscaleFromTargetWavFile)
    {
        //pscalesVar and tscalesVar from targetFestivalUttFile, sourcePitchFile, sourceLabelFile
        //escalesVar from sourceLabelFile, sourceEnergyFile, targetLabelFile, targetEnergyFile
        //vscalesVar from vscalesIn
    }

    private void initialise(int [] pitchMarksIn, double wsFixedIn, double ssFixedIn, int numfrm, int numfrmFixed, int numPeriodsIn, boolean isFixedRate)
    {
        numPeriods = numPeriodsIn;

        if (pitchMarksIn != null)
        {
            getScalesVar(pitchMarksIn, wsFixedIn, ssFixedIn, numfrm, numfrmFixed, isFixedRate);
        }     
    }

    private void getScalesVar(int [] pitchMarks, double wsFixed, double ssFixed, int numfrm, int numfrmFixed, boolean isFixedRate)
    {   
        if (tscales.length==1)
            tscaleSingle=tscales[0]; 
        else
            tscaleSingle=-1;

        //Find pscale, tscale and escale values corresponding to each fixed skip rate frame
        if (pscales.length != numfrmFixed)
            pscales = InterpolationUtils.modifySize(pscales, numfrmFixed);
        
        if (tscales.length !=numfrmFixed)
            tscales = InterpolationUtils.modifySize(tscales, numfrmFixed);
        
        if (escales.length != numfrmFixed)
            escales = InterpolationUtils.modifySize(escales, numfrmFixed);
        
        if (vscales.length != numfrmFixed)
            vscales = InterpolationUtils.modifySize(vscales, numfrmFixed);
        //

        //Determine the pitch, time, and energy scaling factors corresponding to each pitch synchronous frame
        pscalesVar = MathUtils.ones(numfrm);
        tscalesVar = MathUtils.ones(numfrm);
        escalesVar = MathUtils.ones(numfrm);
        vscalesVar = MathUtils.ones(numfrm);
        
        double tVar;
        int ind;
        for (int i=0; i<numfrm; i++)
        {
            if (!isFixedRate)
                tVar = (0.5*(pitchMarks[i+numPeriods]+pitchMarks[i]))/fs;
            else
                tVar = i*ssFixed+0.5*wsFixed;
            
            ind = (int)(Math.floor((tVar-0.5*wsFixed)/ssFixed+0.5));
            if (ind<0)
                ind=0;
            if (ind>numfrmFixed-1)
                ind=numfrmFixed-1;
            
            pscalesVar[i] = pscales[ind];
            tscalesVar[i] = tscales[ind];
            escalesVar[i] = escales[ind];
            vscalesVar[i] = vscales[ind];
        }
        //
    }
}
