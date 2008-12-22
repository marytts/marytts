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
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * @author Oytun T&uumlrk
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
    public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm)
    {
        return analyzePitchSynchronous(x, pm, DEFAULT_ANALYSIS_PERIODS, -1.0f);
    }
    
    //Pitch synchronous analysis
    public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods)
    {
        return analyzePitchSynchronous(x, pm, numPeriods, -1.0f);
    }
    
    //Pitch synchronous analysis using a fixed skip size
    public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds)
    {
        return analyzePitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, DEFAULT_DELTA_IN_HZ);
    }
    
    public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds, float deltaInHz)
    {
        return analyzePitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC); 
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
     *                       REGULARIZED_CEPS (Regularized cepstrum based envelope)
     */
    public SinusoidalTracks analyzePitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType)
    {
        SinusoidalSpeechSignal sinSignal = extracSinusoidsPitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, deltaInHz, spectralEnvelopeType);
        
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
        
        //Add post-processing functionality to here
        
        return sinTracks;
    }
    
    public SinusoidalSpeechSignal extracSinusoidsPitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds, float deltaInHz)
    {
        return extracSinusoidsPitchSynchronous(x, pm, numPeriods, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC);
    }
    
    public SinusoidalSpeechSignal extracSinusoidsPitchSynchronous(double[] x, PitchMarks pm, float numPeriods, float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType)
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
            totalFrm = (int)Math.floor(pm.pitchMarks.length-numPeriods+0.5);
            if (totalFrm>pm.pitchMarks.length-1)
                totalFrm = pm.pitchMarks.length-1;
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
        boolean isOutputToTextFile = false;
        boolean isVoiced;
        
        for (i=0; i<totalFrm; i++)
        {   
            if (!bFixedSkipRate)
            {
                T0 = pm.pitchMarks[i+1]-pm.pitchMarks[i];
                isVoiced = pm.vuvs[i];
            }
            else
            {
                while (pm.pitchMarks[pmInd]<currentTimeInd)
                {
                    pmInd++;
                    if (pmInd>pm.pitchMarks.length-1)
                    {
                        pmInd = pm.pitchMarks.length-1;
                        break;
                    }
                }
                
                if (pmInd<pm.pitchMarks.length-1)
                {
                    T0 = pm.pitchMarks[pmInd+1]-pm.pitchMarks[pmInd];
                    isVoiced = pm.vuvs[pmInd];
                }
                else
                {
                    T0 = pm.pitchMarks[pmInd]-pm.pitchMarks[pmInd-1];
                    isVoiced = pm.vuvs[pmInd-1];
                }
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
                for (j=pm.pitchMarks[i]; j<Math.min(pm.pitchMarks[i]+ws-1, x.length); j++)
                    frm[j-pm.pitchMarks[i]] = x[j];
            }
            else
            {
                for (j=currentTimeInd; j<Math.min(currentTimeInd+ws-1, x.length); j++)
                    frm[j-currentTimeInd] = x[j];
            }
            
            win = Window.get(windowType, ws);
            win.normalize(1.0f); //Normalize to sum up to unity
            win.applyInline(frm, 0, ws);
            
            if (!bFixedSkipRate)
            {
                //currentTime = (float)(0.5*(pitchMarks[i+1]+pitchMarks[i])/fs);
                currentTime = (float)((pm.pitchMarks[i]+0.5f*ws)/fs);  //Middle of analysis frame
            }
            else
            {
                //currentTime = (currentTimeInd+0.5f*T0)/fs;
                currentTime = (currentTimeInd+0.5f*ws)/fs; //Middle of analysis frame
                currentTimeInd += ss;
            }
            
            /*
            if (currentTime>0.500 && currentTime<0.520)
                isOutputToTextFile = true;
            else
                isOutputToTextFile = false;
            */
            
            sinSignal.framesSins[i] = analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced, f0);
            
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
        int pitchMarkOffset = 0;
        PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length, f0.header.ws, f0.header.ss, true, pitchMarkOffset);
        PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(samplingRate, Window.HAMMING, true, true, true, true, 0.0, 0.5*samplingRate);
        
        SinusoidalTracks st = sa.analyzePitchSynchronous(x, pm);        
    }
}
