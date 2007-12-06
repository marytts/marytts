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

package de.dfki.lt.mary.sinusoidal.pitch;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import de.dfki.lt.mary.sinusoidal.SinusoidalAnalyzer;
import de.dfki.lt.mary.sinusoidal.SinusoidalSpeechFrame;
import de.dfki.lt.mary.sinusoidal.SinusoidalSpeechSignal;
import de.dfki.lt.mary.sinusoidal.SinusoidalTracks;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class HarmonicPitchTracker extends BaseSinusoidalPitchTracker {
    
    public HarmonicPitchTracker()
    {
        
    }
    
    public double performanceCriterion(SinusoidalSpeechFrame sinFrame, int startIndex, int endIndex, float f0Candidate, int samplingRate)
    {   
        if (sinFrame==null || sinFrame.sinusoids.length<1)
            return -1e+50;
        
        return -1e+50;
        
        /*
        int l, k, kw0Ind, Kw0, K;
        double kw0;
        
        double Q = 0.0;
        double tempSum;
        
        double maxFreqInRadians = SignalProcUtils.hz2radian(1000.0, samplingRate);
        
        Kw0 = Math.max(0, (int)Math.floor(800.0f/f0Candidate+0.5+1));
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

        if (K<1)
            Q = -1e+50;
        else
        {
            for (l=1; l<=K; l++)
            {
                tempSum = 0.0;

                for (k=1; k<=K; k++)
                {
                    kw0 = k*f0Candidate;
                    kw0Ind = SignalProcUtils.freq2index(kw0, samplingRate, sinFrame.systemAmps.length-1);

                    tempSum += sinFrame.systemAmps[kw0Ind]*MathUtils.sinc(sinFrame.sinusoids[k-1].freq-kw0);
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
        */
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        float searchStepInHz = 0.2f;
        float minFreqInHz = 40.0f;
        float maxFreqInHz = 800.0f;
        
        float windowSizeInSeconds = SinusoidalAnalyzer.DEFAULT_ANALYSIS_WINDOW_SIZE;
        float skipSizeInSeconds = SinusoidalAnalyzer.DEFAULT_ANALYSIS_SKIP_SIZE;
        float deltaInHz  = SinusoidalAnalyzer.DEFAULT_DELTA_IN_HZ;
        int spectralEnvelopeType = SinusoidalAnalyzer.SEEVOC_SPEC;
        
        String strPitchFileIn = args[0].substring(0, args[0].length()-4) + ".ptc";
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFileIn);
        PitchMarker pm = SignalProcUtils.pitchContour2pitchMarks(f0.getContour(), samplingRate, x.length, f0.ws, f0.ss, true);
        PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(samplingRate, Window.HAMMING, true, true);
        
        SinusoidalSpeechSignal ss = sa.extractSinusoidsFixedRate(x, windowSizeInSeconds, skipSizeInSeconds, deltaInHz, spectralEnvelopeType);
        
        HarmonicPitchTracker p = new HarmonicPitchTracker();
        float [] f0s = p.pitchTrack(ss, samplingRate, searchStepInHz, minFreqInHz, maxFreqInHz);    
        
        String strPitchFileOut = args[0].substring(0, args[0].length()-4) + ".ptcSin";
        
        F0ReaderWriter.write_pitch_file(strPitchFileOut, f0s, windowSizeInSeconds, skipSizeInSeconds, samplingRate);
        
        for (int i=0; i<f0s.length; i++)
            System.out.println(String.valueOf(i*skipSizeInSeconds+0.5f*windowSizeInSeconds) + " sec. = " + String.valueOf(f0s[i]));
    }
}
