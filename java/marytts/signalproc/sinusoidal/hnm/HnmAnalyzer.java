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
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.analysis.SeevocAnalyser;
import marytts.signalproc.analysis.SpectrumWithPeakIndices;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
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
    
    
    public HnmAnalyzer()
    {
        
    }
    
    public HnmSpeechSignal analyze(String wavFile, double skipSizeInSeconds, int noisePartRepresentation)
    {
        HnmSpeechSignal hnmSignal = null;
        double[] f0s = null;
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
        double [] x = signal.getAllData();
        float originalDurationInSeconds = SignalProcUtils.sample2time(x.length, fs);
        int lpOrder = SignalProcUtils.getLPOrder(fs);
        double preCoef = 0.97;
        
        //// TO DO
        //Step1. Initial pitch estimation
        
        //Step2: Do for each frame (at 10 ms skip rate):
        //2.a. Voiced/Unvoiced decision
        
        //2.b. If voiced, maximum frequency of voicing estimation
        //     Otherwise, maximum frequency of voicing is set to 0.0
        float maxFreqOfVoicingInHz = 3300.0f; //This should come from the above automatic analysis
        
        //2.c. Refined pitch estimation
        ////
        
        String strPitchFile = StringUtils.modifyExtension(wavFile, ".ptc");
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
        int pitchMarkOffset = 0;
        PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, x.length, f0.header.ws, f0.header.ss, true, pitchMarkOffset);
        
        //Step3. Determine analysis time instants based on refined pitch values.
        //       (Pitch synchronous if voiced, 10 ms skip if unvoiced)
        int windowType = Window.HAMMING;
        double numPeriods = 2.0;
 
        double f0InHz = f0.contour[0];
        int T0;
        double assumedF0ForUnvoicedInHz = 100.0;
        boolean isVoiced;
        if (f0InHz>10.0)
            isVoiced=true;
        else
        {
            isVoiced=false;
            f0InHz=assumedF0ForUnvoicedInHz;
        }
        
        T0 = (int)Math.floor(fs/f0InHz+0.5);
        
        int ws;

        int totalFrm = (int)Math.floor(pm.pitchMarks.length-numPeriods+0.5);
        if (totalFrm>pm.pitchMarks.length-1)
            totalFrm = pm.pitchMarks.length-1;
        
        //Extract frames and analyze them
        double[] frm = null; //Extracted pitch synchronously
        double[] frmNoise = null; //Extracted at fixed window size around analysis time instant since LP analysis requires longer windows (40 ms)
        int i, j, k;
        
        int pmInd = 0;
        
        boolean isOutputToTextFile = false;
        Window win;
        int closestInd;
        
        
        hnmSignal = new HnmSpeechSignal(totalFrm, fs, originalDurationInSeconds);
        boolean isPrevVoiced = false;
        
        int numHarmonics = 0;
        int prevNumHarmonics = 0;
        ComplexNumber[] harmonicAmps = null;
        ComplexNumber[] noiseHarmonicAmps = null;
        
        double[] phases;
        double[] dPhases;
        double[] dPhasesPrev = null;
        int MValue;
        
        int cepsOrderNoise = 10;
        int numNoiseHarmonics = (int)Math.floor((0.5*fs)/NOISE_F0_IN_HZ+0.5);
        double[] freqsInHzNoise = new double [numNoiseHarmonics];
        for (j=0; j<numHarmonics; j++)
            freqsInHzNoise[j] = NOISE_F0_IN_HZ*(j+1);
        
        double[][] M = RegularizedCepstralEnvelopeEstimator.precomputeM(freqsInHzNoise, fs, cepsOrderNoise);
        double[][] MTransW = RegularizedCepstralEnvelopeEstimator.precomputeMTransW(M, null);
        double[][] MTransWM = RegularizedCepstralEnvelopeEstimator.precomputeMTransWM(MTransW, M); 
        double[][] lambdaR = RegularizedCepstralEnvelopeEstimator.precomputeLambdaR(RegularizedCepstralEnvelopeEstimator.DEFAULT_LAMBDA, cepsOrderNoise);
        double[][] inverted = RegularizedCepstralEnvelopeEstimator.precomputeInverted(MTransWM, lambdaR);
        
        for (i=0; i<totalFrm; i++)
        {  
            T0 = pm.pitchMarks[i+1]-pm.pitchMarks[i];
            isVoiced = pm.f0s[i]>10.0 ? true:false;
            
            ws = (int)Math.floor(numPeriods*T0+ 0.5);
            if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                ws++;            
            
            hnmSignal.frames[i].tAnalysisInSeconds = (float)((pm.pitchMarks[i]+0.5f*ws)/fs);  //Middle of analysis frame
            hnmSignal.frames[i].maximumFrequencyOfVoicingInHz = maxFreqOfVoicingInHz;
          
            f0InHz = pm.f0s[i];
            
            if (f0InHz>10.0)
                isVoiced = true;
            else
            {
                isVoiced = false;
                f0InHz = assumedF0ForUnvoicedInHz;
            }

            T0 = (int)Math.floor(fs/f0InHz+0.5);

            ws = (int)Math.floor(numPeriods*T0+ 0.5);
            if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                ws++;
         
            frm = new double[ws];
            Arrays.fill(frm, 0.0);

            for (j=pm.pitchMarks[i]; j<Math.min(pm.pitchMarks[i]+ws-1, x.length); j++)
                frm[j-pm.pitchMarks[i]] = x[j];
            
            win = Window.get(windowType, ws);
            win.normalize(1.0f);
            double[] wgtSquared = win.getCoeffs();
            for (j=0; j<wgtSquared.length; j++)
                wgtSquared[j] = wgtSquared[j]*wgtSquared[j];
                        
            //Step4. Estimate complex amplitudes of harmonics if voiced
            //       The phase of the complex amplitude is the phase and its magnitude is the absolute amplitude of the harmonic
            if (isVoiced)
            {
                numHarmonics = (int)Math.floor(maxFreqOfVoicingInHz/f0InHz+0.5);
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
            if (noisePartRepresentation==LPC)
            {
                //Stylianou does this in a separate, fixed frame rate (10 ms) and window size (40 ms) loop
                //Here, it is pitch synchronous for voiced frames and pitch asynchronous for unvoiced ones
                //Additionally, we have support for preemphasis - this needs to be handled during synthesis of the noisy part with preemphasis removal
                frm = win.apply(frm, 0);
                LpCoeffs lpcs = LpcAnalyser.calcLPC(frm, lpOrder, (float)preCoef);
                hnmSignal.frames[i].n = new FrameNoisePartLpc(lpcs.getA(), lpcs.getGain());
            }
            else if (noisePartRepresentation==PSEUDO_HARMONIC)
            {
                //TO DO: Replace this with the simpler form of the autocorrelation, i.e. harmonics are uncorrelated for noise
                noiseHarmonicAmps = estimateComplexAmplitudes(frm, wgtSquared, NOISE_F0_IN_HZ, numNoiseHarmonics, fs);
                
                double[] linearAmpsNoise = new double[numNoiseHarmonics];
                for (j=0; j<numNoiseHarmonics; j++)
                    linearAmpsNoise[j] = MathUtils.magnitudeComplex(noiseHarmonicAmps[j]);
                
                hnmSignal.frames[i].n = new FrameNoisePartPseudoHarmonic();
                ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmpsNoise, MTransW, inverted);
                
                
                //The following is only for visualization
                //int fftSize = 4096;
                //double[] vocalTractDB = RegularizedCepstralEnvelopeEstimator.cepstrum2logAmpHalfSpectrum(((FrameNoisePartHarmonic)hnmSignal.frames[i].n).ceps, fftSize, fs);
                //MaryUtils.plot(vocalTractDB);
                //
            }
            
            //
            
            //Step6. Estimate amplitude envelopes
            if (isVoiced)
            {
                int cepsOrderHarmonic = 12;
                double[] linearAmps = new double[numHarmonics];
                double[] freqsInHz = new double [numHarmonics];
                for (j=0; j<numHarmonics; j++)
                {
                    linearAmps[j] = MathUtils.magnitudeComplex(harmonicAmps[j]);
                    freqsInHz[j] = f0InHz*(j+1);
                }
                hnmSignal.frames[i].h.ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmps, freqsInHz, fs, cepsOrderHarmonic);
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
    
    //N: local pitch period in samples
    //wgtSquared: window weights squared
    //frm: speech frame to be analysed (its length should be 2*N+1)
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
    
    public static void main(String[] args)
    {
        String wavFile = args[0];
        double skipSizeInSeconds = 0.010;
        
        HnmAnalyzer ha = new HnmAnalyzer();
        
        //int noisePartRepresentation = HnmAnalyzer.LPC;
        int noisePartRepresentation = HnmAnalyzer.PSEUDO_HARMONIC;
        
        HnmSpeechSignal hnmSignal = ha.analyze(wavFile, skipSizeInSeconds, noisePartRepresentation);
    }
}

