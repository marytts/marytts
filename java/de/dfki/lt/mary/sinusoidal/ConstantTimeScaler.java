/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.sinusoidal;

/**
 * @author oytun.turk
 *
 */

import de.dfki.lt.signalproc.analysis.F0Reader;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class ConstantTimeScaler extends SinusoidalSynthesizer {
    
    public ConstantTimeScaler(int samplingRate) {
        super(samplingRate);
    }
    
    public double [] synthesize(SinusoidalTracks st, float timeScale)
    {
        return synthesize(st, timeScale, DEFAULT_ABS_MAX_OUT);
    }
    
    public double [] synthesize(SinusoidalTracks st, float timeScale, double absMaxDesired)
    {
        return synthesize(st, timeScale, absMaxDesired, false);
    }
    
    public double [] synthesize(SinusoidalTracks tr, float timeScale, double absMaxDesired, boolean isSilentSynthesis)
    {
        SinusoidalTracks tr2 = modify(tr, timeScale);
        
        //Call the baseline synthesizer
        return synthesize(tr2, absMaxDesired, isSilentSynthesis);
    }
    
    public SinusoidalTracks modify(SinusoidalTracks tr, float timeScale)
    {
        SinusoidalTracks tr2 = null;
        
        return tr2;
    }
    
    //Anlayze, modify, and synthesize
    public double [] process(double [] x, int [] pitchMarks, float timeScale)
    {    
        return process(x, pitchMarks, timeScale, TrackModifier.DEFAULT_MODIFICATION_SKIP_SIZE);
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
            float skipSizeInSeconds)
    {    
        return process(x, pitchMarks, timeScale,
                       skipSizeInSeconds,
                       PitchSynchronousSinusoidalAnalyzer.DEFAULT_DELTA_IN_HZ);
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
            float skipSizeInSeconds,
            float deltaInHz)
    {    
        return process(x, pitchMarks, timeScale,
                       skipSizeInSeconds,
                       deltaInHz,
                       PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS);
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
            float skipSizeInSeconds,
            float deltaInHz,
            float numPeriods)
    {    
        return process(x, pitchMarks, timeScale,
                       skipSizeInSeconds,
                       deltaInHz,
                       numPeriods,
                       true);
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
            float skipSizeInSeconds,
            float deltaInHz,
            float numPeriods,
            boolean bRefinePeakEstimatesParabola)
    {    
        return process(x, pitchMarks, timeScale,
                       skipSizeInSeconds,
                       deltaInHz,
                       numPeriods,
                       bRefinePeakEstimatesParabola, 
                       true);
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
            float skipSizeInSeconds,
            float deltaInHz,
            float numPeriods,
            boolean bRefinePeakEstimatesParabola, 
            boolean bRefinePeakEstimatesBias)
    {    
        return process(x, pitchMarks, timeScale,
                       skipSizeInSeconds,
                       deltaInHz,
                       numPeriods,
                       bRefinePeakEstimatesParabola, 
                       bRefinePeakEstimatesBias,  
                       false);
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
            float skipSizeInSeconds,
            float deltaInHz,
            float numPeriods,
            boolean bRefinePeakEstimatesParabola, 
            boolean bRefinePeakEstimatesBias,  
            boolean bAdjustNeighFreqDependent)
    {    
        return process(x, pitchMarks, timeScale,
                       skipSizeInSeconds,
                       deltaInHz,
                       numPeriods,
                       bRefinePeakEstimatesParabola, 
                       bRefinePeakEstimatesBias,  
                       bAdjustNeighFreqDependent,
                       false);
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
            float skipSizeInSeconds,
            float deltaInHz,
            float numPeriods,
            boolean bRefinePeakEstimatesParabola, 
            boolean bRefinePeakEstimatesBias,  
            boolean bAdjustNeighFreqDependent,
            boolean isSilentSynthesis)
    {    
        return process(x, pitchMarks, timeScale,
                       skipSizeInSeconds,
                       deltaInHz,
                       numPeriods,
                       bRefinePeakEstimatesParabola, 
                       bRefinePeakEstimatesBias,  
                       bAdjustNeighFreqDependent,
                       isSilentSynthesis,
                       MathUtils.getAbsMax(x));
    }
    
    public double [] process(double [] x, int [] pitchMarks, float timeScale,
                             float skipSizeInSeconds,
                             float deltaInHz,
                             float numPeriods,
                             boolean bRefinePeakEstimatesParabola, 
                             boolean bRefinePeakEstimatesBias,  
                             boolean bAdjustNeighFreqDependent,
                             boolean isSilentSynthesis,
                             double absMaxDesired)
    {    
        //Analysis
        PitchSynchronousSinusoidalAnalyzer pa = new PitchSynchronousSinusoidalAnalyzer(fs, Window.HAMMING, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bAdjustNeighFreqDependent);
        SinusoidalTracks st = pa.analyzePitchSynchronous(x, pitchMarks, numPeriods, skipSizeInSeconds, deltaInHz);
        
        //Modification
        st = TrackModifier.modifyTimeScale(st, timeScale);
         
        //Synthesis
        return synthesize(st, timeScale, absMaxDesired, isSilentSynthesis);
    }
}
