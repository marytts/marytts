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

package marytts.signalproc.sinusoidal.hnm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.CepstrumSpeechAnalyser;
import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.analysis.SeevocAnalyser;
import marytts.signalproc.analysis.SpectrumWithPeakIndices;
import marytts.signalproc.sinusoidal.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.sinusoidal.pitch.VoicingAnalysisOutputData;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * This class implements a harmonic+noise model for speech as described in
 * Stylianou, Y., 1996, "Harmonic plus Noise Models for Speech, combined with Statistical Methods, 
 *                       for Speech and Speaker Modification", Ph.D. thesis, 
 *                       Ecole Nationale Supérieure des Télécommunications.
 * 
 * @author Oytun T&uumlrk
 *
 */
public class HnmAnalyzer {
    public static float DEFAULT_DELTA_IN_HZ = 50.0f;
    public static float DEFAULT_ANALYSIS_WINDOW_SIZE = 0.020f;
    public static float DEFAULT_ANALYSIS_SKIP_SIZE = 0.010f;
    public static double MIN_ENERGY_TH = 1e-50; //Minimum energy threshold to analyze a frame
    public static double MIN_PEAK_IN_DB_LOW = -200.0f;
    public static double MIN_PEAK_IN_DB_HIGH = -200.0f;
    public static double MIN_VOICED_FREQ_IN_HZ = 4000.0f; //Minimum voiced freq allowed (for voiced regions only)
    public static double MAX_VOICED_FREQ_IN_HZ = 5000.0f; //Maximum voiced freq allowed (for voiced regions only)
    
    public static boolean DEFAULT_REFINE_PEAK_ESTIMATES_PARABOLA = true;
    public static boolean DEFAULT_REFINE_PEAK_ESTIMATES_BIAS = true;
    public static boolean DEFAULT_SPECTRAL_REASSIGNMENT = true;
    public static boolean DEFAULT_ADJUST_NEIGH_FREQ_DEPENDENT = false;
    
    public static int NO_SPEC = -1; //No spectral envelope information is extracted
    public static int LP_SPEC = 0; //Linear Prediction (LP) based envelope (Makhoul)
    public static int SEEVOC_SPEC = 1; //Spectral Envelope Estimation Vocoder (SEEVOC) based envelope (Paul, 1981)
    public static int REGULARIZED_CEPS = 2; //Regularized cepstrum based envelope (Cappe, et. al. 1995, Stylianou, et. al. 1995)
    
    protected int fs; //Sampling rate in Hz
    protected int windowType; //Type of window (See class Window for details)
    protected int fftSize; //FFT size in points
    protected int LPOrder; //LP analysis order
    protected int lifterOrder; //Cepstral lifting order
    
    protected double startFreq; //Lowest analysis frequnecy in Hz
    protected double endFreq; //Highest analysis frequency in Hz
    
    protected boolean bRefinePeakEstimatesParabola; //Refine peak and frequency estimates by fitting parabolas?
    protected boolean bRefinePeakEstimatesBias; //Further refine peak and frequency estimates by correcting bias? 
                                                //       (Only effective when bRefinePeakEstimatesParabola=true)
    protected boolean bSpectralReassignment; //Refine spectral peak frequencies considering windowing effect?
    
    protected int ws; //Window size in samples
    protected int ss; //Skip size in samples
    protected Window win; //Windowing applier

    protected int [] freqSampNeighs; //Number of neighbouring samples to search for a peak in the spectrum
    protected boolean bAdjustNeighFreqDependent; //Adjust number of neighbouring samples to search for a peak adaptively depending on frequency?
    public static int DEFAULT_FREQ_SAMP_NEIGHS_LOW = 2; //Default search range for low frequencies for spectral peak detection
    public static int DEFAULT_FREQ_SAMP_NEIGHS_HIGH = 2; //Default search range for high frequencies for spectral peak detection
    
    public static float MIN_WINDOW_SIZE = 0.020f; 
    protected int minWindowSize; //Minimum window size allowed to satisfy 100 Hz criterion for unvoiced sounds computed from MIN_WINDOW_SIZE and sampling rate

    public double absMax; //Keep absolute max of the input signal for normalization after resynthesis
    public double totalEnergy; //Keep total energy for normalization after resynthesis
    
    // fs: Sampling rate in Hz
    // windowType: Type of window (See class Window for details)
    // bRefinePeakEstimatesParabola: Refine peak and frequency estimates by fitting parabolas?
    // bRefinePeakEstimatesBias: Further refine peak and frequency estimates by correcting bias? 
    //                           (Only effective when bRefinePeakEstimatesParabola=true)
    public HnmAnalyzer(int samplingRate, int windowTypeIn, 
                       boolean bRefinePeakEstimatesParabolaIn, 
                       boolean bRefinePeakEstimatesBiasIn, 
                       boolean bSpectralReassignmentIn,
                       boolean bAdjustNeighFreqDependentIn,
                       double startFreqInHz, double endFreqInHz)
    {
        fs = samplingRate;
        startFreq = startFreqInHz;
        if (startFreq<0.0)
            startFreq=0.0;
        
        endFreq = endFreqInHz;
        if (endFreq<0.0)
            endFreq=0.5*fs;
        
        windowType = windowTypeIn;
        setSinAnaFFTSize(getDefaultFFTSize(fs));
        
        bRefinePeakEstimatesParabola = bRefinePeakEstimatesParabolaIn;
        bRefinePeakEstimatesBias = bRefinePeakEstimatesBiasIn;
        bSpectralReassignment = bSpectralReassignmentIn;
        bAdjustNeighFreqDependent = bAdjustNeighFreqDependentIn;
        
        minWindowSize = (int)(Math.floor(fs*MIN_WINDOW_SIZE+0.5));
        if (minWindowSize%2==0) //Always use an odd window size to have a zero-phase analysis window
            minWindowSize++;

        absMax = -1.0;
        totalEnergy = 0.0;
        LPOrder = SignalProcUtils.getLPOrder(fs);
        lifterOrder = SignalProcUtils.getLifterOrder(fs);
    }
    //
    
    public static int getDefaultFFTSize(int samplingRate)
    { 
        if (samplingRate<10000)
            return 1024;
        else if (samplingRate<20000)
            return 2048;
        else
            return 4096;
    }
    
    public void setSinAnaFFTSize(int fftSizeIn)
    { 
        fftSize = fftSizeIn;
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
                freqSampNeighs[i] = DEFAULT_FREQ_SAMP_NEIGHS_LOW;
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
     */
    public HnmSpeechSignal analyzeFixedRate(double [] x)
    {
        return analyzeFixedRate(x, DEFAULT_ANALYSIS_WINDOW_SIZE);
    }
    
    /* 
     * Fixed rate analysis
     * 
     * x: Speech/Audio signal to be analyzed
     * winSizeInSeconds: Integer array of sample indices for pitch period start instants
     */
    public HnmSpeechSignal analyzeFixedRate(double [] x, float winSizeInSeconds)
    {
        return analyzeFixedRate(x, winSizeInSeconds, DEFAULT_ANALYSIS_SKIP_SIZE);
    }
    
    /* 
     * Fixed rate analysis
     * 
     * x: Speech/Audio signal to be analyzed
     * winSizeInSeconds: Integer array of sample indices for pitch period start instants
     * skipSizeInSeconds: Number of pitch periods to be used in analysis
     */
    public HnmSpeechSignal analyzeFixedRate(double [] x, float winSizeInSeconds, float skipSizeInSeconds)
    {
        return analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, DEFAULT_DELTA_IN_HZ);
    }
    
    /* 
     * Fixed rate analysis
     * 
     * x: Speech/Audio signal to be analyzed
     * winSizeInSeconds: Integer array of sample indices for pitch period start instants
     * skipSizeInSeconds: Number of pitch periods to be used in analysis
     * deltaInHz: Maximum allowed frequency deviance when creating sinusoidal tracks
     */
    public HnmSpeechSignal analyzeFixedRate(double [] x, float winSizeInSeconds, float skipSizeInSeconds, float deltaInHz)
    {
        return analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, LP_SPEC);
    }
    
    /* 
     * Fixed rate analysis
     * 
     * x: Speech/Audio signal to be analyzed
     * winSizeInSeconds: Integer array of sample indices for pitch period start instants
     * skipSizeInSeconds: Number of pitch periods to be used in analysis
     * deltaInHz: Maximum allowed frequency deviance when creating sinusoidal tracks
     * spectralEnvelopeType: Spectral envelope estimation method with possible values
     *                       NO_SPEC (do not compute spectral envelope) 
     *                       LP_SPEC (linear prediction based envelope)
     *                       SEEVOC_SPEC (Spectral Envelope Estimation Vocoder based envelope)
     *                       REGULARIZED_CEPS (Regularized cepstrum based envelope)
     *                       See below for details...
     */
    public HnmSpeechSignal analyzeFixedRate(double [] x, float winSizeInSeconds, float skipSizeInSeconds, float deltaInHz, int spectralEnvelopeType)
    {
        return analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, spectralEnvelopeType, null, -1.0f, -1.0f);
    }
    
    /* 
     * Fixed rate analysis
     * 
     * x: Speech/Audio signal to be analyzed
     * winSizeInSeconds: Integer array of sample indices for pitch period start instants
     * skipSizeInSeconds: Number of pitch periods to be used in analysis
     * deltaInHz: Maximum allowed frequency deviation when creating sinusoidal tracks
     * spectralEnvelopeType: Spectral envelope estimation method with possible values
     *                       NO_SPEC (do not compute spectral envelope) 
     *                       LP_SPEC (linear prediction based envelope)
     *                       SEEVOC_SPEC (Spectral Envelope Estimation Vocoder based envelope)
     *                       REGULARIZED_CEPS (Regularized cepstrum based envelope)
     * f0s: f0 values in Hz (optional, required for SEEVOC based spectral envelope estimation. 
     *      If not specified, SEEVOC based estimation will be performed at a fixed f0 value of 100.0 Hz    
     * ws_f0s: Window size in seconds used for f0 extraction (Functional only for SEEVOC based envelope estimation and when f0s are not null)
     * ss_f0s: Skip size in seconds used for f0 extraction (Functional only for SEEVOC based envelope estimation and when f0s are not null)                      
     */
    public HnmSpeechSignal analyzeFixedRate(double [] x, float winSizeInSeconds, float skipSizeInSeconds, float deltaInHz,
                                            int spectralEnvelopeType, double [] f0s, float ws_f0, float ss_f0)
    {
        return null;
    }

    public HnmSpeechFrame analyze_frame(double[] frm, boolean isOutputToTextFile, boolean isVoiced)
    { 
        return analyze_frame(frm, isOutputToTextFile, LP_SPEC, isVoiced, -1.0);
    }
    
    public HnmSpeechFrame analyze_frame(double[] frm, boolean isOutputToTextFile, int spectralEnvelopeType, boolean isVoiced)
    { 
        if (spectralEnvelopeType==SEEVOC_SPEC)
            return analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced, 100.0);
        else
            return analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced, -1.0);
    }
    
    public HnmSpeechFrame analyze_frame(double[] frm, boolean isOutputToTextFile, int spectralEnvelopeType, boolean isVoiced, double f0)
    {
        return analyze_frame(frm, isOutputToTextFile, spectralEnvelopeType, isVoiced, f0, false);
    }
    
    //Extract sinusoidal model parameter from a windowed speech frame using the DFT peak-picking algorithm
    // frm: Windowed speech frame
    // spectralEnvelopeType: Desired spectral envelope (See above, i.e LP_SPEC, SEEVOC_SPEC, REGULARIZED_CEPS)
    // 
    public HnmSpeechFrame analyze_frame(double[] frm, boolean isOutputToTextFile, int spectralEnvelopeType, boolean isVoiced, double f0, boolean bEstimateHNMVoicing)
    {   
        return null;
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
        case Window.HANNING:
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
    
    //References: Auger, F. and Flandrin, P., 1995, "Improving the readability of time-frequency and time-scale representations by the reassignment method",
    //               in IEEE Trans. on Signal Proc., Vol. 43, Issue 5, May 1995, pp. 1068-1089.
    //            Borum, S. and Jensen, K., 1999, "Additive analysis/synthesis using analytically derived windows", 
    //               in Proc. of the 2nd COST G-6 Workshop on Digital Audio Effects (DAFx-99), NTNU, Trondheim,  
    public float[] refineBySpectralReassignment(ComplexArray windowedFrameFFT, ComplexArray windowedFrameDerivativeFFT, float[] freqInds)
    {
        float[] freqIndsRefined = null;

        if (freqInds!=null)
        {
            freqIndsRefined = new float[freqInds.length];
            
            if (windowedFrameFFT!=null && windowedFrameDerivativeFFT!=null)
            {
                int km;
                double f0InRadians, f0RefinedInRadians, f0RefinedInHz, f0RefinedInd;
                int maxFreqInd = (int)(fftSize/2+1);
                for (int i=0; i<freqInds.length; i++)
                {
                    km = (int)Math.floor(freqInds[i]+0.5);
                    f0InRadians = SignalProcUtils.hz2radian(SignalProcUtils.index2freq(km, fs, maxFreqInd), fs);
                    f0RefinedInRadians = f0InRadians - (windowedFrameFFT.real[km]*windowedFrameDerivativeFFT.imag[km]-windowedFrameFFT.imag[km]*windowedFrameDerivativeFFT.real[km])/(windowedFrameFFT.real[km]*windowedFrameFFT.real[km]+windowedFrameFFT.imag[km]*windowedFrameFFT.imag[km])/MathUtils.TWOPI;
                    f0RefinedInHz = SignalProcUtils.radian2Hz(f0RefinedInRadians, fs);
                    freqIndsRefined[i] = (float)SignalProcUtils.freq2indexDouble(f0RefinedInHz, fs, maxFreqInd);
                }
            }
            else
            {
                freqIndsRefined = new float[freqInds.length];
                System.arraycopy(freqInds, 0, freqIndsRefined, 0, freqInds.length);
            }
        }
            
        return freqIndsRefined;
    }

    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        int windowType = Window.HAMMING;
        boolean bRefinePeakEstimatesParabola = false;
        boolean bRefinePeakEstimatesBias = false;
        boolean bSpectralReassignment = true;
        boolean bAdjustNeighFreqDependent = false;
        
        double startFreq = 0.0;
        double endFreq = 0.5*samplingRate;
        
        HnmAnalyzer ha = new HnmAnalyzer(samplingRate, windowType, 
                                         bRefinePeakEstimatesParabola, 
                                         bRefinePeakEstimatesBias,
                                         bSpectralReassignment,
                                         bAdjustNeighFreqDependent,
                                         startFreq,
                                         endFreq);
        
        float winSizeInSeconds = 0.020f;
        float skipSizeInSeconds = 0.010f;
        float deltaInHz = 50.0f;
        int spectralEnvelopeType = HnmAnalyzer.SEEVOC_SPEC;
        
        String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
        float ws_f0 = (float)f0.header.ws;
        float ss_f0 = (float)f0.header.ss;
        
        HnmSpeechSignal sp = ha.analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz,
                                                  spectralEnvelopeType, f0.contour , ws_f0, ss_f0);        
    }
}

