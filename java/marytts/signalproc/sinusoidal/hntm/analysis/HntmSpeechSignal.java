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

package marytts.signalproc.sinusoidal.hntm.analysis;

import marytts.util.math.ArrayUtils;


/**
 * @author oytun.turk
 *
 */
public class HntmSpeechSignal {
    public HntmSpeechFrame[] frames;
    public float originalDurationInSeconds;
    public int samplingRateInHz;
    public float windowDurationInSecondsNoise;
    public float preCoefNoise;
    public float f0WindowDurationInSeconds;
    public float f0SkipSizeInSeconds;
    
    public HntmSpeechSignal(int totalFrm, int samplingRateInHz, float originalDurationInSeconds, 
                           float f0WindowDurationInSeconds, float f0SkipSizeInSeconds,
                           float windowDurationInSecondsNoise, float preCoefNoise)
    {
        if (totalFrm>0)
        {
            frames =  new HntmSpeechFrame[totalFrm];
            for (int i=00; i<totalFrm; i++)
                frames[i] = new HntmSpeechFrame();
        }
        else
            frames = null;
        
        this.samplingRateInHz = samplingRateInHz;
        this.originalDurationInSeconds = originalDurationInSeconds;
        this.windowDurationInSecondsNoise = windowDurationInSecondsNoise;
        this.preCoefNoise = preCoefNoise;
        this.f0WindowDurationInSeconds = f0WindowDurationInSeconds;
        this.f0SkipSizeInSeconds = f0SkipSizeInSeconds;
    }
    
    public float[] getAnalysisTimes()
    {
        float[] times = null;
        
        if (frames!=null)
        {
            times = new float[frames.length];
            for (int i=0; i<frames.length; i++)
                times[i] = frames[i].tAnalysisInSeconds;
        }
        
        return times;
    }
    
    public float[] getOriginalAverageSampleEnergyContour()
    {
        float[] originalAverageSampleEnergyContour = null;
        
        if (frames!=null)
        {
            originalAverageSampleEnergyContour = new float[frames.length];
            for (int i=0; i<frames.length; i++)
                originalAverageSampleEnergyContour[i] = frames[i].origAverageSampleEnergy;
        }
        
        return originalAverageSampleEnergyContour;
    }
    
    //Returns track segments for a given harmonic. Each segment corresponds to a voiced segment
    public double[][] getPhasesInRadians()
    {
        double[][] phases = null;
        
        if (frames!=null && frames.length>0)
        {
            phases = new double[frames.length][];
            
            for (int i=0; i<frames.length; i++)
            {
                if (frames[i].h!=null) 
                    phases[i] = frames[i].h.getPhasesInRadians();
            }
        }
        
        return phases;
    }
    
    public double[][] getLpcsAll()
    {
        double[][] lpcsAll = null;
        
        if (frames!=null && frames.length>0)
        {
            int lpOrder = frames[0].lpCoeffs.length;
            lpcsAll = new double[frames.length][];
            for (int i=0; i<frames.length; i++)
                lpcsAll[i] = ArrayUtils.copy(frames[i].lpCoeffs);
        }
        
        return lpcsAll;
    }
    
    public double[] getLpcGainsAll()
    {
        double[] gainsAll = null;
        
        if (frames!=null && frames.length>0)
        {
            gainsAll = new double[frames.length];
            for (int i=0; i<frames.length; i++)
                gainsAll[i] = frames[i].lpGain;
        }
        
        return gainsAll;
    }
    
    public float[] getOrigNoiseStds()
    {
        float[] origNoiseStdsAll = null;
        
        if (frames!=null && frames.length>0)
        {
            origNoiseStdsAll = new float[frames.length];
            for (int i=0; i<frames.length; i++)
                origNoiseStdsAll[i] = frames[i].origNoiseStd;
        }
        
        return origNoiseStdsAll;
    }
    
    public double[] getMaximumFrequencyOfVoicings()
    {
        double[] maximumFrequencyOfVoicings = null;
        
        if (frames!=null && frames.length>0)
        {
            maximumFrequencyOfVoicings = new double[frames.length];
            
            for (int i=0; i<frames.length; i++)
                maximumFrequencyOfVoicings[i] = frames[i].maximumFrequencyOfVoicingInHz;
        }
        
        return maximumFrequencyOfVoicings;
    }
}
