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

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.FFTMixedRadix;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.FrameProvider;
import de.dfki.lt.signalproc.process.LPCCrossSynthesis;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils.Complex;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 * Sinusoidal Modeling Analysis Module
 * Given a speech/audio signal, a set of amplitudes, frequencies and phases are estimated on a frame-by-frame basis
 * Then, sinusoids that are close in frequency are grouped together to form sinusoidal tracks
 * Optional amplitude and phase continuity constraints can be employed during track generation
 */
public class SinusoidalAnalyzer {
    private int fftSize; //FFT size in points
    private int fs; //Sampling rate in Hz
    private int ws; //Window size in samples
    private int ss; //Skip size in samples
    
    public static float DEFAULT_ANALYSIS_WINDOW_SIZE = 0.020f;
    public static float DEFAULT_ANALYSIS_SKIP_SIZE = 0.010f;
    
    public SinusoidalAnalyzer(int samplingRate, int FFTSize)
    {
        fs = samplingRate;
        fftSize = FFTSize;
    }
    
    public SinusoidalAnalyzer(int samplingRate)
    {
        fs = samplingRate;
        fftSize = getSinAnaFFTSize(fs);
    }
    
    public static int getSinAnaFFTSize(int samplingRate)
    {
        if (samplingRate<=10000)
            return 1024;
        else if (samplingRate<=20000)
            return 2048;
        else
            return 4096;
    }
    
    // Fixed window size and skip rate analysis
    public void analyze(double [] x)
    {
        analyze(x, DEFAULT_ANALYSIS_WINDOW_SIZE);
    }
    
    public void analyze(double [] x, float winSizeInSeconds)
    {
        analyze(x, winSizeInSeconds, DEFAULT_ANALYSIS_SKIP_SIZE);
    }
    
    /* 
     * x: Speech/Audio signal to be analyzed
     * winSizeInSeconds: Integer array of sample indices for pitch period start instants
     * skipSizeInSeconds: Number of pitch periods to be used in analysis
     */
    public SinusoidalTracks analyze(double [] x, float winSizeInSeconds, float skipSizeInSeconds)
    {
        ws = (int)Math.floor(winSizeInSeconds*fs + 0.5);
        ss = (int)Math.floor(skipSizeInSeconds*fs + 0.5);
        
        int totalFrm = (int)((x.length-0.5*ws)/ss);
        
        //Extract frames and analyze them
        double [] frm = new double[ws];
        int i, j;

        Sinusoid [][] framesSins =  new Sinusoid[totalFrm][];
        float [] times = new float[totalFrm];
        
        for (i=0; i<totalFrm; i++)
        {
            Arrays.fill(frm, 0.0);
            for (j=i*ss; j<Math.min(i*ss+ws, x.length); j++)
                frm[j-i*ss] = x[j];
            
            framesSins[i] = analyze_frame(frm);
            
            times[i] = (float)((i*ss+0.5*ws)/fs);
        }
        //
        
        //Extract sinusoidal tracks
        TrackGenerator tg = new TrackGenerator();
        SinusoidalTracks sinTracks = tg.generateTracksFreqOnly(framesSins, times, 50.0f);
        sinTracks.getTrackStatistics(winSizeInSeconds, skipSizeInSeconds);
        
        return sinTracks;
    }

    public Sinusoid [] analyze_frame(double [] frm)
    {
        if (fftSize<frm.length)
            fftSize = frm.length;
        
        int maxFreq = (int) (Math.floor(0.5*fftSize+0.5)+1);
        Complex Y = new Complex(fftSize);
        int i;
        
        //Perform circular buffering as described in (Quatieri, 2001) to provide correct phase estimates
        int midPoint = (int) Math.floor(0.5*frm.length+0.5);
        System.arraycopy(frm, midPoint, Y.real, 0, frm.length-midPoint);
        System.arraycopy(frm, 0, Y.real, frm.length-midPoint, midPoint);
        //
        
        //Take FFT
        if (MathUtils.isPowerOfTwo(fftSize))
            FFT.transform(Y.real, Y.imag, false);
        else
            FFTMixedRadix.fftComplex(Y);
        
        //Compute magnitude spectrum in dB
        double [] Ydb = new double[maxFreq]; 
        for (i=0; i<maxFreq; i++)
            Ydb[i] = 10*Math.log10(Y.real[i]*Y.real[i]+Y.imag[i]*Y.imag[i]);
        //
        
        //Determine peak amplitude indices and the corresponding amplitudes, frequencies, and phases 
        int [] freqInds = MathUtils.getExtrema(Ydb, 1, 1, true);
        Sinusoid [] frameSins = null;
        if (freqInds != null)
        {
            int numFrameSinusoids = freqInds.length;
            frameSins = new Sinusoid[numFrameSinusoids];
            
            for (i=0; i<numFrameSinusoids; i++)
            {
                frameSins[i] = new Sinusoid();
                frameSins[i].amp = (float) Ydb[freqInds[i]];
                frameSins[i].freq = (float) ((0.5*fs*freqInds[i])/(maxFreq-1)); //freq in Hz
                //frameSins[i].freq = (float) ((0.5*MathUtils.TWOPI*freqInds[i])/(maxFreq-1));  //freq in radians
                frameSins[i].phase = (float) (Math.atan2(Y.imag[freqInds[i]], Y.real[freqInds[i]]));
            }
        }
        //
        
        return frameSins;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        SinusoidalAnalyzer sa = new SinusoidalAnalyzer(samplingRate);
        sa.analyze(x);
    }
}
