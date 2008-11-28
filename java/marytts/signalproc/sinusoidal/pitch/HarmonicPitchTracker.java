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

package marytts.signalproc.sinusoidal.pitch;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.SinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.SinusoidalSpeechFrame;
import marytts.signalproc.sinusoidal.SinusoidalSpeechSignal;
import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * This pitch tracker is based on Quatieri´s book
 * 
 * @author Oytun T&uumlrk
 */
public class HarmonicPitchTracker extends BaseSinusoidalPitchTracker {
    
    public HarmonicPitchTracker()
    {
        
    }
    
    public double performanceCriterion(SinusoidalSpeechFrame sinFrame, float f0Candidate, int samplingRate)
    {   
        int l, k, kw0Ind, Kw0, K;
        double kw0;
        
        double Q = 0.0;
        double tempSum, tempSum2;
        double freqHz;
        
        double maxFreqInHz = Math.max(1000.0, f0Candidate+50.0);
        double maxFreqInRadians = SignalProcUtils.hz2radian(maxFreqInHz, samplingRate);
        
        Kw0 = Math.max(0, (int)Math.floor(maxFreqInHz/f0Candidate+0.5+1));
        
        K = 0;
        while (sinFrame.sinusoids[K].freq<maxFreqInRadians)
        {
            K++;
            if (K>=sinFrame.sinusoids.length-1)
            {
                K = sinFrame.sinusoids.length-1;
                break;
            }
        }

        tempSum2 = 0.0;
        
   
        if (K<1)
            Q = -1e+50;
        else
        {
            for (l=1; l<=K; l++)
            {
                tempSum = 0.0;

                freqHz = SignalProcUtils.radian2Hz(sinFrame.sinusoids[l-1].freq, samplingRate);
                
                for (k=1; k<=K; k++)
                {
                    kw0 = k*f0Candidate;
                    kw0Ind = SignalProcUtils.freq2index(kw0, samplingRate, sinFrame.systemAmps.length-1);
                    tempSum += sinFrame.systemAmps[kw0Ind]*Math.abs(MathUtils.sinc(freqHz-kw0, 10*sinFrame.systemAmps.length));   
                }

                Q += sinFrame.sinusoids[l-1].amp*tempSum;
            }

            tempSum = 0.0;
            for (k=1; k<=Kw0; k++)
            {
                kw0 = k*f0Candidate;
                kw0Ind = SignalProcUtils.freq2index(kw0, samplingRate, sinFrame.systemAmps.length-1);
                tempSum += sinFrame.systemAmps[kw0Ind]*sinFrame.systemAmps[kw0Ind];
            }
            
            Q = Q - 0.5*tempSum;
        }
        
        return Q;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        float searchStepInHz = 0.5f;
        float minFreqInHz = 40.0f;
        float maxFreqInHz = 400.0f;
        
        float windowSizeInSeconds = SinusoidalAnalyzer.DEFAULT_ANALYSIS_WINDOW_SIZE;
        float skipSizeInSeconds = SinusoidalAnalyzer.DEFAULT_ANALYSIS_SKIP_SIZE;
        float deltaInHz  = SinusoidalAnalyzer.DEFAULT_DELTA_IN_HZ;
        int spectralEnvelopeType = SinusoidalAnalyzer.SEEVOC_SPEC;
        //int spectralEnvelopeType = SinusoidalAnalyzer.LP_SPEC;
        
        boolean bRefinePeakEstimatesParabola = false;
        boolean bRefinePeakEstimatesBias = false;
        boolean bSpectralReassignment = false;
        boolean bAdjustNeighFreqDependent = false;
        
        double startFreq = 0.0;
        double endFreq = 0.5*samplingRate;
        
        String strPitchFileIn = args[0].substring(0, args[0].length()-4) + ".ptc";
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFileIn);
        PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length, f0.header.ws, f0.header.ss, true);
        PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(samplingRate, Window.HAMMING, 
                                                                                       bRefinePeakEstimatesParabola, 
                                                                                       bRefinePeakEstimatesBias, 
                                                                                       bSpectralReassignment,
                                                                                       bAdjustNeighFreqDependent,
                                                                                       startFreq, endFreq);
        
        SinusoidalSpeechSignal ss = sa.extractSinusoidsFixedRate(x, windowSizeInSeconds, skipSizeInSeconds, deltaInHz, spectralEnvelopeType, f0.contour, (float)f0.header.ws, (float)f0.header.ss);
        
        HarmonicPitchTracker p = new HarmonicPitchTracker();
        float [] f0s = p.pitchTrack(ss, samplingRate, searchStepInHz, minFreqInHz, maxFreqInHz);    
        
        String strPitchFileOut = args[0].substring(0, args[0].length()-4) + ".ptcSin";
        
        F0ReaderWriter.write_pitch_file(strPitchFileOut, f0s, windowSizeInSeconds, skipSizeInSeconds, samplingRate);
        
        for (int i=0; i<f0s.length; i++)
            System.out.println(String.valueOf(i*skipSizeInSeconds+0.5f*windowSizeInSeconds) + " sec. = " + String.valueOf(f0s[i]));
    }
}
