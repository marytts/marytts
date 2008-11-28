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
 * Implements a pitch tracker based on Quatieri´s book
 * 
 * @author Oytun T&uumlrk
 */
public class CombFilterPitchTracker extends BaseSinusoidalPitchTracker {

    public CombFilterPitchTracker()
    {
        
    }
    
    
    public double performanceCriterion(SinusoidalSpeechFrame sinFrame, float f0Candidate, int samplingRate)
    {
        double Q = 0.0;
        
        int endIndex = 0;
        float f0CandidateInRadians = SignalProcUtils.hz2radian(f0Candidate, samplingRate);
        
        while (sinFrame.sinusoids[endIndex].freq<f0CandidateInRadians)
        {
            if (endIndex>=sinFrame.sinusoids.length-1)
                break;
            
            endIndex++;
        }
        
        for (int k=0; k<=endIndex; k++)
            Q += sinFrame.sinusoids[k].amp*sinFrame.sinusoids[k].amp*Math.cos(MathUtils.TWOPI*SignalProcUtils.radian2Hz(sinFrame.sinusoids[k].freq, samplingRate)/f0Candidate);
        
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
        
        boolean bRefinePeakEstimatesParabola = true;
        boolean bRefinePeakEstimatesBias = true;
        boolean bSpectralReassignment = true;
        boolean bAdjustNeighFreqDependent = true;
        
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
        
        //sa.setSinAnaFFTSize(4096);
        SinusoidalSpeechSignal ss = sa.extractSinusoidsFixedRate(x, windowSizeInSeconds, skipSizeInSeconds, deltaInHz);
        
        CombFilterPitchTracker p = new CombFilterPitchTracker();
        float [] f0s = p.pitchTrack(ss, samplingRate, searchStepInHz, minFreqInHz, maxFreqInHz);    
        
        String strPitchFileOut = args[0].substring(0, args[0].length()-4) + ".ptcSin";
        
        F0ReaderWriter.write_pitch_file(strPitchFileOut, f0s, windowSizeInSeconds, skipSizeInSeconds, samplingRate);
        
        for (int i=0; i<f0s.length; i++)
            System.out.println(String.valueOf(i*skipSizeInSeconds+0.5f*windowSizeInSeconds) + " sec. = " + String.valueOf(f0s[i]));
    }
}
