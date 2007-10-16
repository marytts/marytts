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
 * The implementation consists of ideas and algorithms from various papers as described in function headers
 */
public class SinusoidalAnalyzer {
    
    private int fs; //Sampling rate in Hz
    private int windowType; //Type of window (See class Window for details)
    private int fftSize; //FFT size in points
    
    private boolean bRefinePeakEstimatesParabola; //Refine peak and frequency estimates by fitting parabolas?
    private boolean bRefinePeakEstimatesBias; //Further refine peak and frequency estimates by correcting bias? 
                                              //       (Only effective when bRefinePeakEstimatesParabola=true)
    
    private int ws; //Window size in samples
    private int ss; //Skip size in samples
    private Window win; //Windowing applier
    
    public static float DEFAULT_ANALYSIS_WINDOW_SIZE = 0.020f;
    public static float DEFAULT_ANALYSIS_SKIP_SIZE = 0.010f;
    
    // fs: Sampling rate in Hz
    // windowType: Type of window (See class Window for details)
    // bRefinePeakEstimatesParabola: Refine peak and frequency estimates by fitting parabolas?
    // bRefinePeakEstimatesBias: Further refine peak and frequency estimates by correcting bias? 
    //                           (Only effective when bRefinePeakEstimatesParabola=true)
    public SinusoidalAnalyzer(int samplingRate, int windowTypeIn, boolean bRefinePeakEstimatesParabolaIn, boolean bRefinePeakEstimatesBiasIn)
    {
        fs = samplingRate;
        windowType = windowTypeIn;
        fftSize = getSinAnaFFTSize(fs);
        bRefinePeakEstimatesParabola = bRefinePeakEstimatesParabolaIn;
        bRefinePeakEstimatesBias = bRefinePeakEstimatesBiasIn;
    }
    
    public SinusoidalAnalyzer(int samplingRate, int windowTypeIn, boolean bRefinePeakEstimatesParabolaIn)
    {
        this(samplingRate, windowTypeIn, bRefinePeakEstimatesParabolaIn, true);
    }
    
    public SinusoidalAnalyzer(int samplingRate, int windowTypeIn)
    {
        this(samplingRate, windowTypeIn, true);
    }
    
    public SinusoidalAnalyzer(int samplingRate)
    {
        this(samplingRate, Window.HAMMING);
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
    public SinusoidalTracks analyze(double [] x)
    {
        return analyze(x, DEFAULT_ANALYSIS_WINDOW_SIZE);
    }
    
    public SinusoidalTracks analyze(double [] x, float winSizeInSeconds)
    {
        return analyze(x, winSizeInSeconds, DEFAULT_ANALYSIS_SKIP_SIZE);
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
        
        win = Window.get(windowType, ws);
        win.normalize(1.0f); //Normalize to sum up to unity
        
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
            
            win.apply(frm, 0);
            
            framesSins[i] = analyze_frame(frm);
            
            times[i] = (float)((i*ss+0.5*ws)/fs);
            
            System.out.println("Analysis complete for frame " + String.valueOf(i+1) + " of " + String.valueOf(totalFrm));
        }
        //
        
        //Extract sinusoidal tracks
        TrackGenerator tg = new TrackGenerator();
        SinusoidalTracks sinTracks = tg.generateTracksFreqOnly(framesSins, times, 50.0f, fs);
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
        
        //Compute magnitude spectrum in dB as peak frequency estimates tend to be more accurate
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
            
            //Perform parabola fitting around peak estimates to refine frequency estimation (Ref. - PARSHL, see the function for more details)
            float [] freqIndsRefined = new float[numFrameSinusoids];
            float [] ampsRefined = new float[numFrameSinusoids];
            
            //For check only
            float [] amps = new float[numFrameSinusoids];
            for (i=0; i<numFrameSinusoids; i++)
                amps[i] = (float)Ydb[freqInds[i]];
            
            if (bRefinePeakEstimatesParabola)
            {
                refinePeakEstimatesParabola(Ydb, freqInds, freqIndsRefined, ampsRefined);
                
                if (bRefinePeakEstimatesBias)
                    refinePeakEstimatesBias(Ydb, freqInds, freqIndsRefined, ampsRefined);
            }
            else
            {
                System.arraycopy(amps, 0, ampsRefined, 0, numFrameSinusoids);
                
                for (i=0; i<numFrameSinusoids; i++)
                    freqIndsRefined[i] = (float)freqInds[i];
            }
            //
            
            for (i=0; i<numFrameSinusoids; i++)
            {
                frameSins[i] = new Sinusoid();
                
                /*
                // Use simple peak detection results
                frameSins[i].amp = (float)(MathUtils.db2amplitude(Ydb[freqInds[i]]));
                //frameSins[i].freq = (float) ((0.5*fs*freqInds[i])/(maxFreq-1)); //freq in Hz
                frameSins[i].freq = (float) ((0.5*MathUtils.TWOPI*freqInds[i])/(maxFreq-1));  //freq in radians
                */
                
                //Use results of refined peak estimation after simple peak detection
                frameSins[i].amp = (float)(MathUtils.db2amplitude(ampsRefined[i]));
                //frameSins[i].freq = (float)((0.5*fs*freqIndsRefined[i])/(maxFreq-1)); //freq in Hz
                frameSins[i].freq = (float) ((0.5*MathUtils.TWOPI*freqIndsRefined[i])/(maxFreq-1));  //freq in radians
                //
                
                frameSins[i].phase = (float) (Math.atan2(Y.imag[freqInds[i]], Y.real[freqInds[i]]));
                
                //Possible improvement: Refinement of phase values
            }
        }
        //
        
        return frameSins;
    }
    
    //Refine peak detection to get more accurate frequency and amplitude values as described in (Smith III and Serra, 1985)(*)
    // 
    // (*) Julius O. Smith III and Xavier Serra, 1985, "PARSHL: An Analysis/Synthesis Program for Non-Harmonic Sounds
    //            Based on a Sinusoidal Representation", Technical Report, Stanford University, CCRMA STAN-M-43.
    //
    // The basic idea is to fit a parabola to each of the peak detected by simple peak picking from the dB spectrum
    // The previous and next frequency bin is used along with each peak to fit the parabola
    // Then, the peak of the parabola is returned as the peak amplitude estimate and 
    //  its location as a floating point frequency index for refined peak location
    // 
    // Parameters:
    // powSpecdB: Power spectrum estimate in dB
    // freqInds: Peak locations (frequency bins) which we want to refine
    // freqIndsRefined: (OUTPUT) - Refined peak locations as floating point frequency bins
    // ampsRefined: (OUTPUT) - Refined peak amplitude estimates corresponding to the peak value of the parabola fit to each amplitude triplet
    public void refinePeakEstimatesParabola(double [] powSpecdB, int [] freqInds, float [] freqIndsRefined, float [] ampsRefined)
    {
        double alpha, beta, gamma, p;
        
        for (int i=0; i<freqInds.length; i++)
        {
            //Make sure the peak is not at the first or last freq bin
            if (freqInds[i]>0 && freqInds[i]<freqInds.length-1)
            {
                alpha = powSpecdB[freqInds[i]-1];
                beta = powSpecdB[freqInds[i]];
                gamma = powSpecdB[freqInds[i]+1];
                
                p = 0.5*(alpha-gamma)/(alpha-2*beta+gamma);
                
                freqIndsRefined[i] = (float) (freqInds[i]+p);
                ampsRefined[i] = (float) (beta-0.25*p*(alpha-gamma));
                //ampsRefined[i] = (float)((p*p*(alpha-beta)+p*2*(alpha-2*beta)-beta)/(2*(alpha-beta)));
            }
            else //otherwise do not refine
            {
                freqIndsRefined[i] = (float) freqInds[i];
                ampsRefined[i] = (float) powSpecdB[freqInds[i]];
            }
        }
    }
    
    //Further refine peak detection to get more accurate frequency and amplitude values as described in (Abe and Smith III, 2004)(**)
    // 
    // (**) Mototsugu Abe and Julius O. Smith III, 2004, "CQIFFT: Correcting Bias in a Sinusoidal Parameter Estimator based
    //          on Quadratic Interpolation of FFT Magnitude Peaks", Technical Report, Center for Computer Research in Music 
    //          and Acoustics, Department of Music, Stanford University, STAN-M-117.
    //
    // The basic idea is to measure and correct the window-dependent bias of the quadratic refinement method in the previous function
    // 
    // Parameters:
    // powSpecdB: Power spectrum estimate in dB
    // freqInds: Peak locations (frequency bins) which we want to refine
    // freqIndsRefined: (OUTPUT) - Refined peak locations as floating point frequency bins
    // ampsRefined: (OUTPUT) - Refined peak amplitude estimates corresponding to the peak value of the parabola fit to each amplitude triplet
    public void refinePeakEstimatesBias(double [] powSpecdB, int [] freqInds, float [] freqIndsRefined, float [] ampsRefined)
    {
        double delHat, Zpf, ZpA;
        double [] c = new double[4];
    
        //The Zp values are for a max bias of 0.01% in frequency and amplitude as given in Table 3 (Abe and Smith III, 2004)
        switch (windowType)
        {
        case Window.HANN:
            Zpf = 1.5;
            ZpA = 1.9;
            c[0] = 0.247560;
            c[1] = 0.084372;
            c[2] = -0.090608;
            c[3] = -0.055781;
            break;
        case Window.HAMMING:
            Zpf = 1.5;
            ZpA = 2.0;
            c[0] = 0.256498;
            c[1] = 0.075977;
            c[2] = -0.116927;
            c[3] = -0.062882;
            break;
        case Window.BLACKMAN:
            Zpf = 1.2;
            ZpA = 1.7;
            c[0] = 0.124188;
            c[1] = 0.013752;
            c[2] = -0.038073;
            c[3] = -0.006195;
            break;
        default: //These are for rectangular window in fact
            Zpf = 2.9; 
            ZpA = 3.5;
            c[0] = 1.279369;
            c[1] = 1.756245;
            c[2] = -1.173273;
            c[3] = -3.241966;      
        }
        
        double EZpf = c[0]*Math.pow(Zpf,-2.0) + c[1]*Math.pow(Zpf,-4.0);
        double nZpA = c[2]*Math.pow(ZpA,-4.0) + c[3]*Math.pow(ZpA,-6.0);
        for (int i=0; i<freqInds.length; i++)
        {
                delHat = freqIndsRefined[i]-freqInds[i];
                freqIndsRefined[i] = freqIndsRefined[i] + (float) (delHat+EZpf*(delHat-0.5)*(delHat+0.5)*delHat);
                ampsRefined[i] = (float) (ampsRefined[i]+nZpA*delHat*delHat);
        }
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        SinusoidalAnalyzer sa = new SinusoidalAnalyzer(samplingRate, Window.HAMMING, true, true);
        SinusoidalTracks st = sa.analyze(x);        
    }
}
