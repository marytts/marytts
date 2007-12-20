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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 * 
 * Sinusoidal Modeling Synthesis Module
 * Given tracks of sinusoids estimated during analysis and after possible modifications,
 * synthesis of output speech is performed
 * 
 */
public class SinusoidalSynthesizer {
    public int fs; //Sampling rate in Hz
    public static double DEFAULT_ABS_MAX_OUT = 0.90;
    
    public SinusoidalSynthesizer(int samplingRate)
    {
        fs = samplingRate;
    }
    
    public double [] synthesize(SinusoidalTracks st)
    {
        return synthesize(st, DEFAULT_ABS_MAX_OUT, false);
    }
    
    public double [] synthesize(SinusoidalTracks st, double absMaxDesired)
    {
        return synthesize(st, absMaxDesired, false);
    }
    
    //st: Sinusoidal tracks
    //absMaxDesired: Desired absolute maximum of the output
    public double [] synthesize(SinusoidalTracks st, double absMaxDesired, boolean isSilentSynthesis)
    {
        if (absMaxDesired<0.0f)
            absMaxDesired = DEFAULT_ABS_MAX_OUT;
        
        int n; //discrete time index
        int i, j;
        int nStart, nEnd, pStart, pEnd;
        float t; //continuous time
        float t2; //continuous time squared
        float t3; //continuous time cubed
        
        float tFinal = st.getOriginalDuration();
        int nFinal = (int)(Math.floor(tFinal*st.fs + 0.5));
        double [] y = new double[nFinal+1];
        Arrays.fill(y, 0.0);
        float currentAmp;
        float currentTheta;
        double alpha, beta;
        int M;
        float T; //Number of samples between consecutive frames (equals to pitch period in pitch synchronous analysis/synthesis)
        float T2; //T squared
        float T3; //T cubed
        double oneOverTwoPi = 1.0/MathUtils.TWOPI;
        double term1, term2;
        
        st = perceptualProcessing(st);
        float currentTime; //For debugging purposes
        
        for (i=0; i<st.totalTracks; i++) 
        {
            for (j=0; j<st.tracks[i].totalSins-1; j++)
            {
                if (st.tracks[i].states[j]!=SinusoidalTrack.TURNED_OFF)
                {
                    pStart = (int)Math.floor(st.tracks[i].times[j]*st.fs+0.5);
                    pEnd = (int)Math.floor(st.tracks[i].times[j+1]*st.fs+0.5);

                    nStart = Math.max(0, pStart);
                    nEnd = Math.max(0, pEnd);
                    nStart = Math.min(y.length-1, nStart);
                    nEnd = Math.min(y.length-1, nEnd);
                    
                    //currentTime = 0.5f*(nEnd+nStart)/st.fs;
                    //System.out.println("currentTime=" + String.valueOf(currentTime));

                    for (n=nStart; n<nEnd; n++)
                    {   
                        if (false) //Direct synthesis
                        {
                            currentAmp = st.tracks[i].amps[j];
                            currentTheta = (n-nStart)*st.tracks[i].freqs[j] + st.tracks[i].phases[j];
                            y[n] += currentAmp*Math.cos(currentTheta);
                        }
                        else //Synthesis with interpolation
                        {
                            //Amplitude interpolation
                            currentAmp = st.tracks[i].amps[j] + (st.tracks[i].amps[j+1]-st.tracks[i].amps[j])*((float)n-pStart)/(pEnd-pStart+1);

                            T = (pEnd-pStart);

                            if (n==nStart && st.tracks[i].states[j]==SinusoidalTrack.TURNED_ON) //Turning on a track
                            {
                                //Quatieri
                                currentTheta = st.tracks[i].phases[j+1] - T*st.tracks[i].freqs[j+1]; 
                                currentAmp = 0.0f;
                            }
                            else if (n==nStart && st.tracks[i].states[j]==SinusoidalTrack.TURNED_OFF && j>0) //Turning off a track
                            {
                                //Quatieri
                                currentTheta = st.tracks[i].phases[j-1] + T*st.tracks[i].freqs[j-1];
                                currentAmp = 0.0f;
                            }
                            else //Cubic phase interpolation
                            {
                                //Quatieri
                                M = (int)(Math.floor(oneOverTwoPi*((st.tracks[i].phases[j] + T*st.tracks[i].freqs[j] - st.tracks[i].phases[j+1])+(st.tracks[i].freqs[j+1]-st.tracks[i].freqs[j])*0.5*T)+0.5));
                                term1 = st.tracks[i].phases[j+1]-st.tracks[i].phases[j]-T*st.tracks[i].freqs[j]+M*MathUtils.TWOPI;
                                term2 = st.tracks[i].freqs[j+1]-st.tracks[i].freqs[j];

                                T2 = T*T;
                                T3 = T*T2;
                                alpha = 3.0*term1/T2-term2/T;
                                beta = -2*term1/T3+term2/T2;

                                t = ((float)n-nStart);
                                t2 = t*t;
                                t3 = t*t2;

                                //Quatieri
                                currentTheta=(float)(st.tracks[i].phases[j] + st.tracks[i].freqs[j]*t + alpha*t2 + beta*t3);
                            }

                            //Synthesis
                            y[n] += currentAmp*Math.cos(currentTheta);
                        }

                        //System.out.println(String.valueOf(currentTheta)); 
                    }
                }
            }

            if (!isSilentSynthesis)
                System.out.println("Synthesized track " + String.valueOf(i+1) + " of " + String.valueOf(st.totalTracks));
        }   
        
        double maxy = MathUtils.getAbsMax(y);
        for (i=0; i<y.length; i++)
            y[i] = absMaxDesired*y[i]/maxy;
        
        return y;
    }
    
    //Turn-off segments of tracks that are masked by more strong components in other tracks
    //This is a basic implementation that performs some amplitude comparison in freq-time bins of sinusoidal tracks
    //Segments that contain relatively weak sinusoids are turned off
    //In fact, this module can be extended to use perceptual masking phenomena from psycho-acoustics
    //Current version checks only for simple spectral and temporal masking as follows:
    //
    //It would be nice to add: 
    // - Support for spectral and temporal masking as in MPEG
    // - Pre-processing with an absolute hearing threshold curve
    // - Grouping of spectrum in perceptually relevant segments, i.e. like in Bark-scale
    public SinusoidalTracks perceptualProcessing(SinusoidalTracks st)
    {
        //TO-DO
        
        return st;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        //File input
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        SinusoidalAnalyzer sa = null;
        SinusoidalTracks st = null;
        PitchSynchronousSinusoidalAnalyzer pa = null;
        //
        
        //Analysis
        float deltaInHz = SinusoidalAnalyzer.DEFAULT_DELTA_IN_HZ;
        float numPeriods = PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS;
        
        boolean isSilentSynthesis = false;
        
        boolean bRefinePeakEstimatesParabola = false;
        boolean bRefinePeakEstimatesBias = false;
        boolean bAdjustNeighFreqDependent = false;
        double absMaxOriginal;
        
        int spectralEnvelopeType = SinusoidalAnalyzer.SEEVOC_SPEC;
        
        boolean isFixedRateAnalysis = true;
        boolean isRealSpeech = true;
        
        if (isFixedRateAnalysis)
        {
            //Fixed window size and skip rate analysis
            double [] f0s = null;
            float ws_f0 = -1.0f;
            float ss_f0 = -1.0f;
            sa = new SinusoidalAnalyzer(samplingRate, Window.HANN, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bAdjustNeighFreqDependent);
            
            if (spectralEnvelopeType == SinusoidalAnalyzer.SEEVOC_SPEC) //Pitch info needed
            {
                String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
                F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
                f0s = f0.getContour();
                ws_f0 = (float) f0.ws;
                ss_f0 = (float) f0.ss;
            }
                
            st = sa.analyzeFixedRate(x, 0.020f, 0.010f, deltaInHz, spectralEnvelopeType, f0s, ws_f0, ss_f0);
            absMaxOriginal = sa.getAbsMaxOriginal();
            //
        }
        else
        {
            //Pitch synchronous analysis
            String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
            F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
            PitchMarker pm = SignalProcUtils.pitchContour2pitchMarks(f0.getContour(), samplingRate, x.length, f0.ws, f0.ss, true);
            pa = new PitchSynchronousSinusoidalAnalyzer(samplingRate, Window.HANN, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bAdjustNeighFreqDependent);
            st = pa.analyzePitchSynchronous(x, pm.pitchMarks, numPeriods, -1.0f, deltaInHz);
            isSilentSynthesis = false;

            absMaxOriginal = pa.getAbsMaxOriginal();
        }
        //
        
        //Resynthesis
        SinusoidalSynthesizer ss = new SinusoidalSynthesizer(samplingRate);
        x = ss.synthesize(st, absMaxOriginal, isSilentSynthesis);
        //
        
        //File output
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), inputAudio.getFormat());
        String outFileName = args[0].substring(0, args[0].length()-4) + "_sinResynth.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        //
    }
}
