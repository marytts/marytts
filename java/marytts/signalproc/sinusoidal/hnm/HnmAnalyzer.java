/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.analysis.SeevocAnalyser;
import marytts.signalproc.analysis.SpectrumWithPeakIndices;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechFrame;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechSignal;
import marytts.signalproc.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.sinusoidal.pitch.VoicingAnalysisOutputData;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.ComplexNumber;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

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
    public static final int LPC = 1; //Noise part model based on LPC
    public static final int PSEUDO_HARMONIC = 2; //Noise part model based on pseude harmonics for f0=NOISE_F0_IN_HZ
    
    public static final double NOISE_F0_IN_HZ = 100.0; //Pseudo-pitch for unvoiced portions (will be used for pseudo harmonic modelling of the noise part)
    public static float FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST = 3500.0f;
    public static float FIXED_MAX_FREQ_OF_NOISE_FOR_QUICK_TEST = 8000.0f;
    public static float HPF_TRANSITION_BANDWIDTH_IN_HZ = 50.0f;
    public static float NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS = 0.040f; //Fixed window size for noise analysis, should be generally large (>=0.040 seconds)
    public static float HARMONICS_FIXED_GAIN = 0.40f;
    public static float NOISE_FIXED_GAIN = 0.02f;
    
    public HnmAnalyzer()
    {
        
    }
    
    public HnmSpeechSignal analyze(String wavFile, float windowSizeInSeconds, float skipSizeInSeconds, int noisePartRepresentation)
    {
        HnmSpeechSignal hnmSignal = null;
        AudioInputStream inputAudio = null;
        try {
            inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int fs = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] x = signal.getAllData();
        x = MathUtils.multiply(x, 32768.0);
        float originalDurationInSeconds = SignalProcUtils.sample2time(x.length, fs);
        int lpOrder = SignalProcUtils.getLPOrder(fs);
        float preCoefNoise = 0.0f;
        
        //// TO DO
        //Step1. Initial pitch estimation: Current version just reads from a file
        String strPitchFile = StringUtils.modifyExtension(wavFile, ".ptc");
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
        int pitchMarkOffset = 0;
        PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, x.length, f0.header.ws, f0.header.ss, true, pitchMarkOffset);
        
        float[] initialF0s = ArrayUtils.subarrayf(f0.contour, 0, f0.header.numfrm);
        //float[] initialF0s = HnmPitchVoicingAnalyzer.estimateInitialPitch(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, f0MinInHz, f0MaxInHz, windowType);
        //
        
        //Step2: Do for each frame (at 10 ms skip rate):
        //2.a. Voiced/Unvoiced decision
        
        //2.b. If voiced, maximum frequency of voicing estimation
        //     Otherwise, maximum frequency of voicing is set to 0.0
        int fftSize = 4096;
        float[] maxFrequencyOfVoicings = HnmPitchVoicingAnalyzer.analyzeVoicings(x, fs, windowSizeInSeconds, skipSizeInSeconds, fftSize, initialF0s);
        float maxFreqOfVoicingInHz;
        //maxFreqOfVoicingInHz = HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST; //This should come from the above automatic analysis
        
        //2.c. Refined pitch estimation
        float[] f0s = ArrayUtils.subarrayf(f0.contour, 0, f0.header.numfrm);
        //float[] f0s = HnmPitchVoicingAnalyzer.estimateRefinedPitch(fftSize, fs, leftNeighInHz, rightNeighInHz, searchStepInHz, initialF0s, maxFrequencyOfVoicings);
        ////
        
       
        //Step3. Determine analysis time instants based on refined pitch values.
        //       (Pitch synchronous if voiced, 10 ms skip if unvoiced)
        int windowType = Window.HAMMING;
        double numPeriods = 2.0;
 
        double f0InHz = f0s[0];
        int T0;
        double assumedF0ForUnvoicedInHz = 100.0;
        boolean isVoiced, isNoised;
        if (f0InHz>10.0)
            isVoiced=true;
        else
        {
            isVoiced=false;
            f0InHz=assumedF0ForUnvoicedInHz;
        }
        
        T0 = (int)Math.floor(fs/f0InHz+0.5);
        
        int i, j, k;
        
        int ws;
        int wsNoise = SignalProcUtils.time2sample(NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, fs);
        if (wsNoise%2==0) //Always use an odd window size to have a zero-phase analysis window
            wsNoise++;  
        
        Window winNoise = Window.get(windowType, wsNoise);
        //winNoise.normalize(1.0f);
        double[] wgtSquaredNoise = winNoise.getCoeffs();
        for (j=0; j<wgtSquaredNoise.length; j++)
            wgtSquaredNoise[j] = wgtSquaredNoise[j]*wgtSquaredNoise[j];

        int fftSizeNoise = SignalProcUtils.getDFTSize(fs);
        
        int totalFrm = (int)Math.floor(pm.pitchMarks.length-numPeriods+0.5);
        if (totalFrm>pm.pitchMarks.length-1)
            totalFrm = pm.pitchMarks.length-1;
        
        //Extract frames and analyze them
        double[] frm = null; //Extracted pitch synchronously
        double[] frmNoise = new double[wsNoise]; //Extracted at fixed window size around analysis time instant since LP analysis requires longer windows (40 ms)
        int noiseFrmStartInd;
        
        int pmInd = 0;
        
        boolean isOutputToTextFile = false;
        Window win;
        int closestInd;
        
        hnmSignal = new HnmSpeechSignal(totalFrm, fs, originalDurationInSeconds, NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, preCoefNoise);
        boolean isPrevVoiced = false;
        
        int numHarmonics = 0;
        int prevNumHarmonics = 0;
        ComplexNumber[] harmonicAmps = null;
        ComplexNumber[] noiseHarmonicAmps = null;
        
        double[] phases;
        double[] dPhases;
        double[] dPhasesPrev = null;
        int MValue;
        
        int cepsOrderHarmonic = 24;
        int cepsOrderNoise = 12;
        int numNoiseHarmonics = (int)Math.floor((0.5*fs)/NOISE_F0_IN_HZ+0.5);
        double[] freqsInHzNoise = new double [numNoiseHarmonics];
        for (j=0; j<numNoiseHarmonics; j++)
            freqsInHzNoise[j] = NOISE_F0_IN_HZ*(j+1);
        
        double[][] M = RegularizedCepstralEnvelopeEstimator.precomputeM(freqsInHzNoise, fs, cepsOrderNoise);
        double[][] MTransW = RegularizedCepstralEnvelopeEstimator.precomputeMTransW(M, null);
        double[][] MTransWM = RegularizedCepstralEnvelopeEstimator.precomputeMTransWM(MTransW, M); 
        double[][] lambdaR = RegularizedCepstralEnvelopeEstimator.precomputeLambdaR(RegularizedCepstralEnvelopeEstimator.DEFAULT_LAMBDA, cepsOrderNoise);
        double[][] inverted = RegularizedCepstralEnvelopeEstimator.precomputeInverted(MTransWM, lambdaR);
        int maxVoicingIndex;
        for (i=0; i<totalFrm; i++)
        {  
            f0InHz = pm.f0s[i];
            T0 = pm.pitchMarks[i+1]-pm.pitchMarks[i];
            ws = (int)Math.floor(numPeriods*T0+ 0.5);
            if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                ws++;            
            
            hnmSignal.frames[i].tAnalysisInSeconds = (float)((pm.pitchMarks[i]+0.5f*ws)/fs);  //Middle of analysis frame
            
            maxVoicingIndex = SignalProcUtils.time2frameIndex(hnmSignal.frames[i].tAnalysisInSeconds, windowSizeInSeconds, skipSizeInSeconds);
            maxVoicingIndex = Math.min(maxVoicingIndex, maxFrequencyOfVoicings.length-1);
            maxFreqOfVoicingInHz = maxFrequencyOfVoicings[maxVoicingIndex];
            //if (hnmSignal.frames[i].tAnalysisInSeconds<0.7 && f0InHz>10.0)
            if (f0InHz>10.0)
                hnmSignal.frames[i].maximumFrequencyOfVoicingInHz = maxFreqOfVoicingInHz; //Normally, this should come from analysis!!!
            else
                hnmSignal.frames[i].maximumFrequencyOfVoicingInHz = 0.0f;
            
            isVoiced = hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0 ? true:false;
            isNoised = hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5*fs ? true:false;

            if (!isVoiced)
                f0InHz = assumedF0ForUnvoicedInHz;

            T0 = SignalProcUtils.time2sample(1.0/f0InHz, fs);

            ws = (int)Math.floor(numPeriods*T0+ 0.5);
            if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                ws++;
         
            frm = new double[ws];
            Arrays.fill(frm, 0.0);

            for (j=pm.pitchMarks[i]; j<Math.min(pm.pitchMarks[i]+ws-1, x.length); j++)
                frm[j-pm.pitchMarks[i]] = HARMONICS_FIXED_GAIN*x[j];
            
            win = Window.get(windowType, ws);
            //win.normalize(1.0f);
            double[] wgtSquared = win.getCoeffs();
            for (j=0; j<wgtSquared.length; j++)
                wgtSquared[j] = wgtSquared[j]*wgtSquared[j];
                        
            //Step4. Estimate complex amplitudes of harmonics if voiced
            //       The phase of the complex amplitude is the phase and its magnitude is the absolute amplitude of the harmonic
            if (isVoiced)
            {
                numHarmonics = (int)Math.floor(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz/f0InHz+0.5);
                harmonicAmps = estimateComplexAmplitudes(frm, wgtSquared, f0InHz, numHarmonics, fs);
                
                //Only for visualization
                //double[] absMags = MathUtils.magnitudeComplex(harmonicAmps);
                //double[] dbMags = MathUtils.amp2db(absMags);
                //MaryUtils.plot(dbMags);
                //
                
                hnmSignal.frames[i].h = new FrameHarmonicPart((float)f0InHz);
            }
            else
                numHarmonics = 0;
            
            //Step5. Perform full-spectrum LPC analysis for generating noise part
            Arrays.fill(frmNoise, 0.0);
            noiseFrmStartInd = Math.max(0, SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds-0.5f*NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, fs));
            for (j=noiseFrmStartInd; j<Math.min(noiseFrmStartInd+wsNoise, x.length); j++)
                frmNoise[j-noiseFrmStartInd] = x[j];
            
            if (isNoised)
            {
                if (noisePartRepresentation==LPC)
                {
                    double origStd = MathUtils.standardDeviation(frmNoise);
                    frmNoise = MathUtils.multiply(frmNoise, NOISE_FIXED_GAIN);
                    
                    //We have support for preemphasis - this needs to be handled during synthesis of the noisy part with preemphasis removal
                    frmNoise = winNoise.apply(frmNoise, 0);
                    
                    //SignalProcUtils.displayDFTSpectrumInDBNoWindowing(frmNoise, fftSizeNoise);  

                    if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f)
                    {
                        //frmNoise = SignalProcUtils.fdFilter(frmNoise, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5f*fs, fs, fftSizeNoise);
                        HighPassFilter hpf = new HighPassFilter(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz/fs, HnmAnalyzer.HPF_TRANSITION_BANDWIDTH_IN_HZ/fs);
                        frmNoise = hpf.apply(frmNoise);
                    }
                    
                    //Only for display purposes...
                    //SignalProcUtils.displayDFTSpectrumInDBNoWindowing(frmNoise, fftSizeNoise); 

                    LpCoeffs lpcs = LpcAnalyser.calcLPC(frmNoise, lpOrder, preCoefNoise);
                    hnmSignal.frames[i].n = new FrameNoisePartLpc(lpcs.getA(), lpcs.getGain());
                    //hnmSignal.frames[i].n = new FrameNoisePartLpc(lpcs.getA(), origStd);
                    
                    //Only for display purposes...
                    //SignalProcUtils.displayLPSpectrumInDB(((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs, ((FrameNoisePartLpc)hnmSignal.frames[i].n).gain, fftSizeNoise);
                }
                else if (noisePartRepresentation==PSEUDO_HARMONIC)
                {
                    //Note that for noise we use the uncorrelated version of the complex amplitude estimator
                    //Correlated version resulted in ill-conditioning
                    //Also, analysis was pretty slow since the number of harmonics is large for pseudo-harmonics of noise, 
                    //i.e. for 16 KHz 5 to 8 KHz bandwidth in steps of 100 Hz produces 50 to 80 pseudo-harmonics

                    //(1) Uncorrelated approach as in Stylianou´s thesis
                    noiseHarmonicAmps = estimateComplexAmplitudesUncorrelated(frmNoise, wgtSquaredNoise, NOISE_F0_IN_HZ, numNoiseHarmonics, fs);
                    //OR... (2)Expensive approach which does not work very well
                    //noiseHarmonicAmps = estimateComplexAmplitudes(frm, wgtSquared, NOISE_F0_IN_HZ, numNoiseHarmonics, fs);
                    //OR... (3) Uncorrelated approach using full autocorrelation matrix (checking if there is a problem in estimateComplexAmplitudesUncorrelated
                    //noiseHarmonicAmps = estimateComplexAmplitudesUncorrelated2(frm, wgtSquared, NOISE_F0_IN_HZ, numNoiseHarmonics, fs);

                    double[] linearAmpsNoise = new double[numNoiseHarmonics];
                    for (j=0; j<numNoiseHarmonics; j++)
                        linearAmpsNoise[j] = MathUtils.magnitudeComplex(noiseHarmonicAmps[j]);

                    double[] vocalTractDB = MathUtils.amp2db(linearAmpsNoise);
                    //MaryUtils.plot(vocalTractDB);

                    hnmSignal.frames[i].n = new FrameNoisePartPseudoHarmonic();
                    //(1) This is how amplitudes are represented in Stylianou´s thesis
                    ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmpsNoise, MTransW, inverted);
                    //OR... (2) The following is the expensive approach in which all matrices are computed again and again
                    //((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmpsNoise, freqsInHzNoise, fs, cepsOrderNoise);
                    //OR... (3) Let´s try to copy linearAmps as they are with no cepstral processing to see if synthesis works OK:
                    //((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps = new double[numNoiseHarmonics];
                    //System.arraycopy(linearAmpsNoise, 0, ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, 0, numNoiseHarmonics);


                    /*
                    //The following is only for visualization
                    //int fftSize = 4096;
                    //double[] vocalTractDB = RegularizedCepstralEnvelopeEstimator.cepstrum2logAmpHalfSpectrum(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, fftSize, fs);
                    double[] vocalTractDB = new double[numNoiseHarmonics];
                    for (j=0; j<numNoiseHarmonics; j++)
                        vocalTractDB[j] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, (j+1)*HnmAnalyzer.NOISE_F0_IN_HZ, fs);
                    vocalTractDB = MathUtils.amp2db(vocalTractDB);
                    MaryUtils.plot(vocalTractDB);
                    //
                     */    
                }
            }
            else
                hnmSignal.frames[i].n = null;
            //
            
            //Step6. Estimate amplitude envelopes
            if (numHarmonics>0)
            {
                if (isVoiced)
                {
                    double[] linearAmps = new double[numHarmonics];
                    double[] freqsInHz = new double [numHarmonics];
                    for (j=0; j<numHarmonics; j++)
                    {
                        linearAmps[j] = MathUtils.magnitudeComplex(harmonicAmps[j]);
                        freqsInHz[j] = f0InHz*(j+1);
                    }
                    
                    hnmSignal.frames[i].h.ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmps, freqsInHz, fs, cepsOrderHarmonic);
                    //Use amplitudes directly:
                    //hnmSignal.frames[i].h.ceps = ArrayUtils.subarray(linearAmps, 0, linearAmps.length);
                    
                    //The following is only for visualization
                    //int fftSize = 4096;
                    //double[] vocalTractDB = RegularizedCepstralEnvelopeEstimator.cepstrum2logAmpHalfSpectrum(hnmFrames[i].ceps , fftSize, fs);
                    //MaryUtils.plot(vocalTractDB);
                    //
                }
                //

                hnmSignal.frames[i].h.phases = new float[numHarmonics];
                for (k=0; k<numHarmonics; k++)
                    hnmSignal.frames[i].h.phases[numHarmonics-k-1] = (float)MathUtils.phaseInRadians(harmonicAmps[numHarmonics-k-1]);
            }

            if (isVoiced)
                isPrevVoiced = true;
            else
            {
                prevNumHarmonics = 0;
                isPrevVoiced = false;
            }

            System.out.println("Analysis complete at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(totalFrm)); 
        }
        
        return hnmSignal;
    }
    
    //Complex amplitude estimation for harmonics in time domain (Full correlation matrix approach, no independence between harmonics assumed)
    //The main advantage is the operation being in time domain.
    //Therefore, we can use window sizes as short as two pitch periods and track rapid changes in amplitudes and phases
    //N: local pitch period in samples
    //wgtSquared: window weights squared
    //frm: speech frame to be analysed (its length should be 2*N+1)
    //Uses Equation 3.25 in Stylianou`s thesis
    public ComplexNumber[] estimateComplexAmplitudes(double[] frm, double[] wgtSquared, double f0InHz, int numHarmonics, int samplingRateInHz)
    {
        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;
        
        ComplexNumber[][] R = new ComplexNumber[2*numHarmonics+1][2*numHarmonics+1];
        ComplexNumber[] b = new ComplexNumber[2*numHarmonics+1];
        ComplexNumber tmp;
        
        int t, i, k;
        double omega;

        for (i=1; i<=2*numHarmonics+1; i++)
        {
            for (k=1; k<=2*numHarmonics+1; k++)
            {
                R[i-1][k-1] = new ComplexNumber(0.0, 0.0);
                for (t=-N; t<=N; t++)
                {
                    omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(k-i);
                    tmp = new ComplexNumber(wgtSquared[t+N]*Math.cos(omega), wgtSquared[t+N]*Math.sin(omega));
                    R[i-1][k-1] = MathUtils.addComplex(R[i-1][k-1], tmp);
                }
            }
        }   
        
        for (k=1; k<=2*numHarmonics+1; k++)
        {
            b[k-1] = new ComplexNumber(0.0, 0.0);
            for (t=-N; t<=N; t++)
            {
                omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(numHarmonics+1-k);
                tmp = new ComplexNumber(wgtSquared[t+N]*frm[t+N]*Math.cos(omega), wgtSquared[t+N]*frm[t+N]*Math.sin(omega));
                b[k-1] = MathUtils.addComplex(b[k-1], tmp);
            }
        }
        
        ComplexNumber[] x = MathUtils.matrixProduct(MathUtils.inverse(R), b);
        ComplexNumber[] xpart = new ComplexNumber[numHarmonics+1];
        for (k=numHarmonics+1; k<2*numHarmonics+1; k++) //The remaning complex amplitudes from L+1 to 2L are complex conjugates of entries from L-1,...,1
            xpart[k-(numHarmonics+1)] = new ComplexNumber(x[k]);
        
        return xpart;
    }
    
    //Complex amplitude estimation for harmonics in time domain (Diagonal correlation matrix approach, harmonics assumed independent)
    //The main advantage is the operation being in time domain.
    //Therefore, we can use window sizes as short as two pitch periods and track rapid changes in amplitudes and phases
    //N: local pitch period in samples
    //wgtSquared: window weights squared
    //frm: speech frame to be analysed (its length should be 2*N+1)
    //Uses Equation 3.32 in Stylianou`s thesis
    //This requires harmonics to be uncorrelated.
    //We use this for estimating pseudo-harmonic amplitudes of the noise part.
    //Note that this function is equivalent to peak-picking in the frequency domain in Quatieri´s sinusoidal framework.
    public ComplexNumber[] estimateComplexAmplitudesUncorrelated(double[] frm, double[] wgtSquared, double f0InHz, int numHarmonics, int samplingRateInHz)
    {
        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;
        
        ComplexNumber tmp;
        
        int t, k;
        double omega;
        
        double denum = 0.0;
        for (t=-N; t<=N; t++)
            denum += wgtSquared[t+N];
        
        ComplexNumber[] Ak = new ComplexNumber[numHarmonics];
        for (k=1; k<=numHarmonics; k++)
        {
            Ak[k-1] = new ComplexNumber(0.0, 0.0);
            for (t=-N; t<=N; t++)
            {
                omega = -1.0*MathUtils.TWOPI*k*f0InHz*((double)t/samplingRateInHz);
                tmp = new ComplexNumber(wgtSquared[t+N]*frm[t+N]*Math.cos(omega), wgtSquared[t+N]*frm[t+N]*Math.sin(omega));
                Ak[k-1] = MathUtils.addComplex(Ak[k-1], tmp);
            }
            Ak[k-1] = MathUtils.divide(Ak[k-1], denum);
        }
        
        return Ak;
    }
    
    /*
    //This is just for testing the full autocorrelation algorithm with diagonal autocorrelation matrix. It produced the same result using estimateComplexAmplitudesUncorrelated2
    public ComplexNumber[] estimateComplexAmplitudesUncorrelated2(double[] frm, double[] wgtSquared, double f0InHz, int numHarmonics, int samplingRateInHz)
    {
        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;
        
        ComplexNumber[][] R = new ComplexNumber[2*numHarmonics+1][2*numHarmonics+1];
        ComplexNumber[] b = new ComplexNumber[2*numHarmonics+1];
        ComplexNumber tmp;
        
        int t, i, k;
        double omega;

        for (i=1; i<=2*numHarmonics+1; i++)
        {
            for (k=1; k<=2*numHarmonics+1; k++)
            {
                R[i-1][k-1] = new ComplexNumber(0.0, 0.0);
                if (i==k)
                {
                    for (t=-N; t<=N; t++)
                    {
                        omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(k-i);
                        tmp = new ComplexNumber(wgtSquared[t+N]*Math.cos(omega), wgtSquared[t+N]*Math.sin(omega));
                        R[i-1][k-1] = MathUtils.addComplex(R[i-1][k-1], tmp);
                    }
                }
            }
        }   
        
        for (k=1; k<=2*numHarmonics+1; k++)
        {
            b[k-1] = new ComplexNumber(0.0, 0.0);
            for (t=-N; t<=N; t++)
            {
                omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(numHarmonics+1-k);
                tmp = new ComplexNumber(wgtSquared[t+N]*frm[t+N]*Math.cos(omega), wgtSquared[t+N]*frm[t+N]*Math.sin(omega));
                b[k-1] = MathUtils.addComplex(b[k-1], tmp);
            }
        }
        
        ComplexNumber[] x = MathUtils.matrixProduct(MathUtils.inverse(R), b);
        ComplexNumber[] xpart = new ComplexNumber[numHarmonics+1];
        for (k=numHarmonics+1; k<2*numHarmonics+1; k++) //The remaning complex amplitudes from L+1 to 2L are complex conjugates of entries from L-1,...,1
            xpart[k-(numHarmonics+1)] = new ComplexNumber(x[k]);
        
        return xpart;
    }
    */
    
    public static void main(String[] args)
    {
        String wavFile = args[0];
        float windowSizeInSeconds = 0.040f;
        float skipSizeInSeconds = 0.010f;
        
        HnmAnalyzer ha = new HnmAnalyzer();
        
        //int noisePartRepresentation = HnmAnalyzer.LPC;
        int noisePartRepresentation = HnmAnalyzer.PSEUDO_HARMONIC;
        
        HnmSpeechSignal hnmSignal = ha.analyze(wavFile, windowSizeInSeconds, skipSizeInSeconds, noisePartRepresentation);
    }
}


