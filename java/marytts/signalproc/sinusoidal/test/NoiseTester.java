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

package marytts.signalproc.sinusoidal.test;

import java.io.IOException;
import java.util.Arrays;

import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * This class can be used to generate various sinusoid signals and writing them to wav and ptc files
 * to be used in testing the analysis/synthesis algorithms
 *  
 * @author Oytun T&uumlrk
 */
public class NoiseTester extends BaseTester{
    
    public NoiseTester()
    {
        this(0.0f, 0.5f*DEFAULT_FS);
    }
    
    public NoiseTester(float freqStartInHz, float freqEndInHz)
    {
        this(freqStartInHz, freqEndInHz, DEFAULT_AMP);
    }
    
    public NoiseTester(float freqStartInHz, float freqEndInHz, float amp)
    {
        this(freqStartInHz, freqEndInHz, amp, DEFAULT_DUR);
    }
    
    public NoiseTester(float freqStartInHz, float freqEndInHz, float amp, float durationInSeconds)
    {
        this(freqStartInHz, freqEndInHz, amp, durationInSeconds, DEFAULT_FS);
    }
            
    public NoiseTester(float freqStartInHz, float freqEndInHz, float amp, float durationInSeconds, int samplingRateInHz)
    {
        float [][] freqs = new float[1][];
        float [] amps = new float[1];
        freqs[0] = new float[2];
        freqs[0][0] = freqStartInHz;
        freqs[0][1] = freqEndInHz;
        amps[0] = amp;
        
        fs = samplingRateInHz;
        
        checkFreqs(freqs);
        
        init(freqs, amps, durationInSeconds, samplingRateInHz);
    }
    
    public NoiseTester(float[][] freqs)
    {        
        init(freqs);
    }
    
    public NoiseTester(float[][] freqs, float durationInSeconds)
    {
        init(freqs, durationInSeconds);
    }
    
    public NoiseTester(float[][] freqs, float[] amps)
    {
        init(freqs, amps);
    }
    
    public NoiseTester(float[][] freqs, float[] amps, float durationInSeconds)
    {
        init(freqs, amps, durationInSeconds);
    }
    
    public NoiseTester(float[][] freqs, float[] amps, float startTimeInSeconds, float endTimeInSeconds)
    {
        this(freqs, amps, startTimeInSeconds, endTimeInSeconds, DEFAULT_FS);
    }
    
    public NoiseTester(float[][] freqs, float[] amps, float durationInSeconds, int samplingRateInHz)
    {
        init(freqs, amps, durationInSeconds, samplingRateInHz);
    }
    
    public NoiseTester(float[][] freqs, float[] amps, float startTimeInSeconds, float endTimeInSeconds, int samplingRateInHz)
    {
        float[] startTimesInSeconds = new float[freqs.length];
        float[] endTimesInSeconds = new float[freqs.length];
        Arrays.fill(startTimesInSeconds, startTimeInSeconds);
        Arrays.fill(endTimesInSeconds, endTimeInSeconds);
        
        init(freqs, amps, startTimesInSeconds, endTimesInSeconds, samplingRateInHz);
    }
    
    public NoiseTester(float[][] freqs, float[] amps, float[] startTimesInSeconds, float[] endTimesInSeconds, int samplingRateInHz)
    {
        init(freqs, amps, startTimesInSeconds, endTimesInSeconds, samplingRateInHz);
    }
    
    //Check if any of the freqs are greater than half sampling rate
    // or any left freq is greater than the right freq (these are simply swapped)
    // Note that only the first two entries for each frequency pair are considered
    public void checkFreqs(float [][] freqs)
    {
        if (freqs!=null)
        {
            float maxFreq = 0.5f*fs;
            
            for (int i=0; i<freqs.length; i++)
            {
                for (int j=0; j<2; j++)
                {
                    if (freqs[i][j]<0.0f)
                        freqs[i][j]=0.0f;
                    if (freqs[i][j]>maxFreq)
                        freqs[i][j]=maxFreq;
                }
                
                if (freqs[i][0]>freqs[i][1])
                {
                    float tmp = freqs[i][0];
                    freqs[i][0] = freqs[i][1];
                    freqs[i][1] = tmp;
                }
            }
        }
    }
    
    public void init(float [][] freqs)
    {
        float [] amps = new float[freqs.length];
        
        for (int i=0; i<amps.length; i++)
            amps[i] = DEFAULT_AMP;
        
        init(freqs, amps);
    }
    
    public void init(float [][] freqs, float durationInSeconds)
    {
        float [] amps = new float[freqs.length];
        
        for (int i=0; i<amps.length; i++)
            amps[i] = DEFAULT_AMP;
        
        init(freqs, amps, durationInSeconds);
    }
    
    public void init(float [][] freqs, float [] amps)
    {
        init(freqs, amps, DEFAULT_DUR);
    }
    
    public void init(float [][] freqs, float [] amps, float durationInSeconds)
    {
        init(freqs, amps, durationInSeconds, DEFAULT_FS);
    }
   
    public void init(float[][] freqs, float [] amps, float durationInSeconds, int samplingRateInHz)
    {
        if (freqs!=null && freqs.length>0)
        {
            float[] startTimesInSeconds = new float[freqs.length];
            float[] endTimesInSeconds = new float[freqs.length];
            
            for (int i=0; i<freqs.length; i++)
            {
                startTimesInSeconds[i] = 0.0f;
                endTimesInSeconds[i] = durationInSeconds;
            }

            init(freqs, amps, startTimesInSeconds, endTimesInSeconds, samplingRateInHz);
        }
    }

    public void init(float [][] freqs, float [] amps, float [] startTimesInSeconds, float [] endTimesInSeconds, int samplingRateInHz)
    {
        fs = samplingRateInHz;
        signal = null;
        pitchMarks = null;

        int i, j;
        
        if (freqs!=null && freqs.length>0)
        {
            assert amps.length==freqs.length;
            assert startTimesInSeconds.length==freqs.length;
            assert startTimesInSeconds.length==endTimesInSeconds.length;

            for (i=0; i<freqs.length; i++)
                assert freqs[i].length>=2;
            
            int [] startSampleIndices = new int[freqs.length];
            int [] endSampleIndices = new int[freqs.length];
            
            for (i=0; i<startTimesInSeconds.length; i++)
            {
                if (startTimesInSeconds[i]<0.0f)
                    startTimesInSeconds[i]=0.0f;
                if (endTimesInSeconds[i]<0.0f)
                    endTimesInSeconds[i]=0.0f;
                if (startTimesInSeconds[i]>endTimesInSeconds[i])
                    startTimesInSeconds[i] = endTimesInSeconds[i];
                
                startSampleIndices[i] = (int)(Math.floor(startTimesInSeconds[i]*fs+0.5));
                endSampleIndices[i] = (int)(Math.floor(endTimesInSeconds[i]*fs+0.5))-1;
            }
            
            //int minStartSampleIndex = MathUtils.getMin(startSampleIndices);
            int minStartSampleIndex = 0; //To ensure pitch marks being generated starting from 0th sample
            int maxEndSampleIndex = MathUtils.getMax(endSampleIndices);
            
            //Create pitch marks by finding the longest period
            int maxT0 = (int)Math.floor(0.010f*fs+0.5);
            int numPitchMarks = (int)(Math.floor(((double)(maxEndSampleIndex-minStartSampleIndex+1))/maxT0+0.5)) + 1; 
            pitchMarks = new int[numPitchMarks];
            for (i=0; i<numPitchMarks; i++)
                pitchMarks[i] = Math.min(i*maxT0+minStartSampleIndex, maxEndSampleIndex);
            //
            
            f0s = SignalProcUtils.pitchMarks2PitchContour(pitchMarks, ws, ss, fs);
            
            if (maxEndSampleIndex>0)
            {
                signal = new double[maxEndSampleIndex+1];
                Arrays.fill(signal, 0.0);

                //Synthesize noise
                for (i=0; i<freqs.length; i++)
                {
                    double[] noise = SignalProcUtils.getNoise(freqs[i][0], freqs[i][1], fs, endSampleIndices[i]-startSampleIndices[i]+1); 
                    
                    for (j=startSampleIndices[i]; j<endSampleIndices[i]; j++)
                        signal[j] += 2.0f*amps[i]*noise[j-startSampleIndices[i]];
                }  
            }
        }
    }
    
    public static void main(String[] args) throws IOException
    {
        int i, numTracks;
        float [] tStarts, tEnds;
        NoiseTester t = null;

        numTracks = 1;
        float [][] freqs = new float[numTracks][];
        for (i=0; i<numTracks; i++)
            freqs[i] = new float[2];
        
        freqs[0][0] = 0;
        freqs[0][1] = 4000;
        float durationInSeconds = 1.0f;

        t = new NoiseTester(freqs, durationInSeconds);
        
        if (args.length>1)
            t.write(args[0], args[1]);
        else
            t.write(args[0]);
    }
}
