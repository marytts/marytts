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
import de.dfki.lt.signalproc.util.SignalProcUtils;
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
    public static float DEFAULT_DELTA_IN_HZ = 50.0f;
    public static float DEFAULT_ANALYSIS_WINDOW_SIZE = 0.020f;
    public static float DEFAULT_ANALYSIS_SKIP_SIZE = 0.010f;
    public static double MIN_ENERGY_TH = 1e-10; //Minimum energy threshold to analyze a frame
    
    protected int fs; //Sampling rate in Hz
    protected int windowType; //Type of window (See class Window for details)
    protected int fftSize; //FFT size in points

    protected boolean bRefinePeakEstimatesParabola; //Refine peak and frequency estimates by fitting parabolas?
    protected boolean bRefinePeakEstimatesBias; //Further refine peak and frequency estimates by correcting bias? 
                                                //       (Only effective when bRefinePeakEstimatesParabola=true)
    
    protected int ws; //Window size in samples
    protected int ss; //Skip size in samples
    protected Window win; //Windowing applier
    
    protected int [] freqSampNeighs; //Number of neighbouring samples to search for a peak in the spectrum
    protected boolean bAdjustNeighFreqDependent; //Adjust number of neighbouring samples to search for a peak adaptively depending on frequency?
    public static int DEFAULT_FREQ_SAMP_NEIGHS = 1; //Default search range for all frequencies for spectral peak detection
    
    public static float MIN_WINDOW_SIZE = 0.020f; 
    protected int minWindowSize; //Minimum window size allowed to satisfy 100 Hz criterion for unvoiced sounds computed from MIN_WINDOW_SIZE and sampling rate
    
    protected double absMax; //Keep absolute max of the input signal for normalization after resynthesis
    
    // fs: Sampling rate in Hz
    // windowType: Type of window (See class Window for details)
    // bRefinePeakEstimatesParabola: Refine peak and frequency estimates by fitting parabolas?
    // bRefinePeakEstimatesBias: Further refine peak and frequency estimates by correcting bias? 
    //                           (Only effective when bRefinePeakEstimatesParabola=true)
    public SinusoidalAnalyzer(int samplingRate, int windowTypeIn, boolean bRefinePeakEstimatesParabolaIn, boolean bRefinePeakEstimatesBiasIn, boolean bAdjustNeighFreqDependentIn)
    {
        fs = samplingRate;
        windowType = windowTypeIn;
        fftSize = getSinAnaFFTSize(fs);
        bRefinePeakEstimatesParabola = bRefinePeakEstimatesParabolaIn;
        bRefinePeakEstimatesBias = bRefinePeakEstimatesBiasIn;
        minWindowSize = (int)(Math.floor(fs*MIN_WINDOW_SIZE+0.5));
        bAdjustNeighFreqDependent = bAdjustNeighFreqDependentIn;
        absMax = -1.0f;
    }
    
    public SinusoidalAnalyzer(int samplingRate, int windowTypeIn, boolean bRefinePeakEstimatesParabolaIn, boolean bRefinePeakEstimatesBiasIn)
    {
        this(samplingRate, windowTypeIn, bRefinePeakEstimatesParabolaIn, bRefinePeakEstimatesBiasIn, false);
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
    //
    
    // Fixed window size and skip rate analysis
    public SinusoidalTracks analyzeFixedRate(double [] x)
    {
        return analyzeFixedRate(x, DEFAULT_ANALYSIS_WINDOW_SIZE);
    }
    
    public SinusoidalTracks analyzeFixedRate(double [] x, float winSizeInSeconds)
    {
        return analyzeFixedRate(x, winSizeInSeconds, DEFAULT_ANALYSIS_SKIP_SIZE);
    }
    
    public SinusoidalTracks analyzeFixedRate(double [] x, float winSizeInSeconds, float skipSizeInSeconds)
    {
        return analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, DEFAULT_DELTA_IN_HZ);
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
    
    //Set default search range for peak detection for different frequency intervals
    //The aim is to eliminate some of the peaks, especially in the high frequency region to reduce phase mismatches at frame boundaries
    //However, we should not set the range to broad as it is required to estimate a peak per 100 Hz on the average theoretically for an accurate representation
    //We simply divide the spectrum into non-overlapping bands using Bark scale
    //Groups of 5-equal Bark ranges are assigned the same peak search range
    //The search ranges are increased as frequency increases, i.e. a higher freq peak candidate needs to be greater than a larger number of neighbours 
    // to be selected as a peak
    public void setNeighFreq()
    {
        int maxFreq = (int) (Math.floor(0.5*fftSize+0.5)+1);
        freqSampNeighs = new int[maxFreq];
        
        int i;
        if (!bAdjustNeighFreqDependent)
        {
            for (i=0; i<maxFreq; i++)
                freqSampNeighs[i] = DEFAULT_FREQ_SAMP_NEIGHS;
        }
        else
        {
            float freq;
            int [] vals = new int[6];
            float maxSeparationInHz = 100.0f; //Maximum average separation allowed for peaks of noise
            for (i=0; i<vals.length; i++)
                vals[i] = Math.min(i+1, (int)Math.floor(0.5*maxSeparationInHz/fs*fftSize+0.5));
            
            for (i=0; i<maxFreq; i++)
            {
                freq = ((float)i)/(maxFreq-1.0f)*0.5f*fs;
                if (freq<500.0f)
                    freqSampNeighs[i] = vals[0];
                else if (freq<1270.0f)
                    freqSampNeighs[i] = vals[1];
                else if (freq<2700.0f)
                    freqSampNeighs[i] = vals[2];
                else if (freq<6400.0f)
                    freqSampNeighs[i] = vals[3];
                else if (freq<15500.0f)
                    freqSampNeighs[i] = vals[4];
                else
                    freqSampNeighs[i] = vals[5];
            }
        }
    }
    //
    
    /* 
     * Fixed rate analysis
     * 
     * x: Speech/Audio signal to be analyzed
     * winSizeInSeconds: Integer array of sample indices for pitch period start instants
     * skipSizeInSeconds: Number of pitch periods to be used in analysis
     */
    public SinusoidalTracks analyzeFixedRate(double [] x, float winSizeInSeconds, float skipSizeInSeconds, float deltaInHz)
    {
        absMax = MathUtils.getAbsMax(x);
        
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
        boolean [] isSinusoidNulls = new boolean[totalFrm]; 
        Arrays.fill(isSinusoidNulls, false);
        int totalNonNull = 0;
        
        for (i=0; i<totalFrm; i++)
        {
            Arrays.fill(frm, 0.0);
            for (j=i*ss; j<Math.min(i*ss+ws, x.length); j++)
                frm[j-i*ss] = x[j];
            
            win.applyInline(frm, 0, ws);
            
            framesSins[i] = analyze_frame(frm);
            
            if (framesSins[i]==null)
                isSinusoidNulls[i] = true;
            else
                totalNonNull++;
            
            times[i] = (float)((i*ss+0.5*ws)/fs);
            
            System.out.println("Analysis complete at " + String.valueOf(times[i]) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(totalFrm) + "(found " + String.valueOf(totalNonNull) + " peaks)");
        }
        //
        
        Sinusoid [][] framesSins2 = null;
        float [] times2 = null;
        if (totalNonNull>0)
        {
            //Collect non-null sinusoids only
            framesSins2 =  new Sinusoid[totalNonNull][];
            times2 = new float[totalNonNull];
            int ind = 0;
            for (i=0; i<totalFrm; i++)
            {
                if (!isSinusoidNulls[i])
                {
                    framesSins2[ind] = new Sinusoid[framesSins[i].length];
                    for (j=0; j<framesSins[i].length; j++)
                        framesSins2[ind][j] = new Sinusoid(framesSins[i][j]);

                    times2[ind] = times[i];

                    ind++;
                    if (ind>totalNonNull-1)
                        break;
                }
            }
            //
        }
        
        //Extract sinusoidal tracks
        TrackGenerator tg = new TrackGenerator();
        SinusoidalTracks sinTracks = tg.generateTracksFreqOnly(framesSins2, times2, deltaInHz, fs);
        
        if (sinTracks!=null)
        {
            sinTracks.getTrackStatistics(winSizeInSeconds, skipSizeInSeconds);
            getGrossStatistics(sinTracks);
        }
        //
        
        return sinTracks;
    }
    
    public void getGrossStatistics(SinusoidalTracks sinTracks)
    {
        int totalSins = 0;
        int i, j;
        for (i=0; i<sinTracks.totalTracks; i++)
        {
            for (j=0; j<sinTracks.tracks[i].totalSins; j++)
            {
                if (sinTracks.tracks[i].states[j]==SinusoidalTrack.ACTIVE)
                    totalSins++;
            }
        }
        
        System.out.println("Total sinusoids to model this file = " + String.valueOf(totalSins));
    }

    public Sinusoid [] analyze_frame(double [] frm)
    {   
        Sinusoid [] frameSins = null;
        
        double frmEn = SignalProcUtils.getEnergy(frm);
        if (frmEn>MIN_ENERGY_TH)
        {
            if (fftSize<frm.length)
                fftSize = frm.length;
            
            setNeighFreq();

            int maxFreq = (int) (Math.floor(0.5*fftSize+0.5)+1);
            Complex Y = new Complex(fftSize);
            int i;

            //Perform circular buffering as described in (Quatieri, 2001) to provide correct phase estimates
            int midPoint = (int) Math.floor(0.5*frm.length+0.5);
            System.arraycopy(frm, midPoint, Y.real, 0, frm.length-midPoint);
            System.arraycopy(frm, 0, Y.real, fftSize-midPoint, midPoint);
            //

            //Take FFT
            if (MathUtils.isPowerOfTwo(fftSize))
                FFT.transform(Y.real, Y.imag, false);
            else
                Y = FFTMixedRadix.fftComplex(Y);

            //Compute magnitude spectrum in dB as peak frequency estimates tend to be more accurate
            double [] Ydb = new double[maxFreq]; 
            for (i=0; i<maxFreq; i++)
                Ydb[i] = 10*Math.log10(Y.real[i]*Y.real[i]+Y.imag[i]*Y.imag[i]+1e-20);
            //
            
            //Determine peak amplitude indices and the corresponding amplitudes, frequencies, and phases 
            int [] freqInds = MathUtils.getExtrema(Ydb, freqSampNeighs, freqSampNeighs, true);

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

                    frameSins[i].phase = (float) (Math.atan2(Y.imag[freqInds[i]], Y.real[freqInds[i]])); //phase in radians

                    /*
                    if (Y.real[freqInds[i]]<0)
                        frameSins[i].phase += 0.5*MathUtils.TWOPI;
                    */

                    //Possible improvement: Refinement of phase values
                }
            }
            //
        }
        
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
    
    public double getAbsMaxOriginal()
    {
        return absMax;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        SinusoidalAnalyzer sa = new SinusoidalAnalyzer(samplingRate, Window.HAMMING, true, true);
        
        SinusoidalTracks st = sa.analyzeFixedRate(x);        
    }
}
