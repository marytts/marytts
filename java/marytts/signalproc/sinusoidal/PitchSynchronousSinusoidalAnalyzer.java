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

package marytts.signalproc.sinusoidal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.window.Window;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * @author oytun.turk
 *
 */
public class PitchSynchronousSinusoidalAnalyzer extends SinusoidalAnalyzer {
    public static float DEFAULT_ANALYSIS_PERIODS = 2.5f;
    
    // fs: Sampling rate in Hz
    // windowType: Type of window (See class Window for details)
    // bRefinePeakEstimatesParabola: Refine peak and frequency estimates by fitting parabolas?
    // bRefinePeakEstimatesBias: Further refine peak and frequency estimates by correcting bias? 
    //                           (Only effective when bRefinePeakEstimatesParabola=true)
    public PitchSynchronousSinusoidalAnalyzer(int samplingRate, int windowTypeIn, 
                                              boolean bRefinePeakEstimatesParabolaIn, 
                                              boolean bRefinePeakEstimatesBiasIn, 
                                              boolean bSpectralReassignmentIn,
                                              boolean bAdjustNeighFreqDependentIn,
                                              double startFreqInHz,
                                              double endFreqInHz)
    {
        super(samplingRate, windowTypeIn, 
              bRefinePeakEstimatesParabolaIn, bRefinePeakEstimatesBiasIn, bSpectralReassignmentIn, bAdjustNeighFreqDependentIn,
              startFreqInHz, endFreqInHz);
    }
    //
    
    //Pitch synchronous analysis
    public SinusoidalTracks analyzePitchSynchronous(double[] x, int[] pitchMarks)
    {
        return analyzePitchSynchronous(x, pitchMarks, DEFAULT_ANALYSIS_PERIODS, -1.0f);
    }
    
    //Pitch synchronous analysis
    public SinusoidalTracks analyzePitchSynchronous(double[] x, int[] pitchMarks, float numPeriods)
    {
        return analyzePitchSynchronous(x, pitchMarks, numPeriods, -1.0f);
    }
    
    //Pitch synchronous analysis using a fixed skip size
    public SinusoidalTracks analyzePitchSynchronous(double[] x, int[] pitchMarks, float numPeriods, float skipSizeInSeconds)
    {
        return analyzePitchSynchronous(x, pitchMarks, numPeriods, skipSizeInSeconds, DEFAULT_DELTA_IN_HZ);
    }
    
    public SinusoidalTracks analyzePitchSynchronous(double[] x, int[] pitchMarks, float numPeriods, float skipSizeInSeconds, float deltaInHz)
    {
        return analyzePitchSynchronous(x, pitchMarks, numPeriods, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC); 
    }
    
    /* 
     * Pitch synchronous analysis
     * 
     * x: Speech/Audio signal to be analyzed
     * pitchMarks: Integer array of sample indices for pitch period start instants
     * numPeriods: Number of pitch periods to be used in analysis
     * skipSizeInSeconds: Skip size for fixed skip rate but pitch synchronous analysis 
     *                    (Enter -1.0f for using adaptive skip rates of one complete pitch periods)
     * deltaInHz: Maximum allowed frequency deviation when creating sinusoidal tracks
     * spectralEnvelopeType: Spectral envelope estimation method with possible values
     *                       NO_SPEC (do not compute spectral envelope) 
     *                       LP_SPEC (linear prediction based envelope)
     *                       SEEVOC_SPEC (Spectral Envelope Estimation Vocoder based envelope)
     */
    public SinusoidalTracks analyzePitchSynchronous(double[] x, int[] pitchMarks, float numPeriods, float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType)
    {
        SinusoidalSpeechSignal sinSignal = extracSinusoidsPitchSynchronous(x, pitchMarks, numPeriods, skipSizeInSeconds, deltaInHz, spectralEnvelopeType);
        
        //Extract sinusoidal tracks
        TrackGenerator tg = new TrackGenerator();
        SinusoidalTracks sinTracks = tg.generateTracks(sinSignal, deltaInHz, fs);
        
        if (sinTracks!=null)
        {
            sinTracks.getTrackStatistics();
            getGrossStatistics(sinTracks);
        }
        
        sinTracks.absMaxOriginal = (float)absMax;
        sinTracks.totalEnergy = (float)totalEnergy;
        
        //Add post-processin gfuncitonality to here
        
        return sinTracks;
    }
    
    public SinusoidalSpeechSignal extracSinusoidsPitchSynchronous(double[] x, int[] pitchMarks, float numPeriods, float skipSizeInSeconds, float deltaInHz)
    {
        return extracSinusoidsPitchSynchronous(x, pitchMarks, numPeriods, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC);
    }
    
    public SinusoidalSpeechSignal extracSinusoidsPitchSynchronous(double[] x, int[] pitchMarks, float numPeriods, float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType)
    {
        absMax = MathUtils.getAbsMax(x);
        totalEnergy = SignalProcUtils.energy(x);
 
        boolean bFixedSkipRate = false;
        if (skipSizeInSeconds>0.0f) //Perform fixed skip rate but pitch synchronous analysis. This is useful for time/pitch scale modification
        {
            ss = (int)Math.floor(skipSizeInSeconds*fs + 0.5);
            bFixedSkipRate = true;
        }
        
        int totalFrm;
        
        if (!bFixedSkipRate)
        {
            totalFrm = (int)Math.floor(pitchMarks.length-numPeriods+0.5);
            if (totalFrm>pitchMarks.length-1)
                totalFrm = pitchMarks.length-1;
        }
        else
            totalFrm = (int)(x.length/ss+0.5);
        
        //Extract frames and analyze them
        double [] frm = null;
        int i, j;
        int T0;
        
        SinusoidalSpeechSignal sinSignal =  new SinusoidalSpeechSignal(totalFrm);
        boolean [] isSinusoidNulls = new boolean[totalFrm]; 
        Arrays.fill(isSinusoidNulls, false);
        int totalNonNull = 0;
        
        int pmInd = 0;
        int currentTimeInd = 0;
        float f0;
        float currentTime;
        
        for (i=0; i<totalFrm; i++)
        {   
            if (!bFixedSkipRate)
                T0 = pitchMarks[i+1]-pitchMarks[i];
            else
            {
                while (pitchMarks[pmInd]<currentTimeInd)
                {
                    pmInd++;
                    if (pmInd>pitchMarks.length-1)
                    {
                        pmInd = pitchMarks.length-1;
                        break;
                    }
                }
                
                if (pmInd<pitchMarks.length-1)
                    T0 = pitchMarks[pmInd+1]-pitchMarks[pmInd];
                else
                    T0 = pitchMarks[pmInd]-pitchMarks[pmInd-1];
            }
            
            f0 = ((float)fs)/T0;
            
            ws = (int)Math.floor(numPeriods*T0+ 0.5);
            if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                ws++;
            
            //System.out.println("ws=" + String.valueOf(ws) + " minWindowSize=" + String.valueOf(minWindowSize));
            ws = Math.max(ws, minWindowSize);
         
            frm = new double[ws];
            
            Arrays.fill(frm, 0.0);
            
            if (!bFixedSkipRate)
            {
                for (j=pitchMarks[i]; j<Math.min(pitchMarks[i]+ws-1, x.length); j++)
                    frm[j-pitchMarks[i]] = x[j];
            }
            else
            {
                for (j=currentTimeInd; j<Math.min(currentTimeInd+ws-1, x.length); j++)
                    frm[j-currentTimeInd] = x[j];
            }
            
            win = Window.get(windowType, ws);
            win.normalize(1.0f); //Normalize to sum up to unity
            win.applyInline(frm, 0, ws);
            
            sinSignal.framesSins[i] = analyze_frame(frm, spectralEnvelopeType, f0);
            
            if (sinSignal.framesSins[i]!=null)
            {
                for (j=0; j<sinSignal.framesSins[i].sinusoids.length; j++)
                    sinSignal.framesSins[i].sinusoids[j].frameIndex = i;
            }
            
            int peakCount = 0;
            if (sinSignal.framesSins[i]==null)
                isSinusoidNulls[i] = true;
            else
            {
                isSinusoidNulls[i] = false;
                totalNonNull++;
                peakCount = sinSignal.framesSins[i].sinusoids.length;
            }


            if (!bFixedSkipRate)
            {
                //currentTime = (float)(0.5*(pitchMarks[i+1]+pitchMarks[i])/fs);
                currentTime = (float)((pitchMarks[i]+0.5f*ws)/fs);
            }
            else
            {
                //currentTime = (currentTimeInd+0.5f*T0)/fs;
                currentTime = (currentTimeInd+0.5f*ws)/fs;
                currentTimeInd += ss;
            }

            if (sinSignal.framesSins[i]!=null)
                sinSignal.framesSins[i].time = currentTime;

            System.out.println("Analysis complete at " + String.valueOf(currentTime) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(totalFrm) + "(found " + String.valueOf(peakCount) + " peaks)"); 
        }
        //
        
        SinusoidalSpeechSignal sinSignal2 = null;
        float [] voicings2 = null;
        if (totalNonNull>0)
        {
            //Collect non-null sinusoids only
            sinSignal2 =  new SinusoidalSpeechSignal(totalNonNull);
            int ind = 0;
            for (i=0; i<totalFrm; i++)
            {
                if (!isSinusoidNulls[i])
                {
                    sinSignal2.framesSins[ind] = new SinusoidalSpeechFrame(sinSignal.framesSins[i]);

                    ind++;
                    if (ind>totalNonNull-1)
                        break;
                }
            }
            //
            
            sinSignal2.originalDurationInSeconds = ((float)x.length)/fs;
        }
        
        return sinSignal2;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
        PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length, f0.header.ws, f0.header.ss, true);
        PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(samplingRate, Window.HAMMING, true, true, true, true, 0.0, 0.5*samplingRate);
        
        SinusoidalTracks st = sa.analyzePitchSynchronous(x, pm.pitchMarks);        
    }
}
