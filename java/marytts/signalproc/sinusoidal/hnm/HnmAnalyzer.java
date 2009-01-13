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
    public HnmAnalyzer()
    {
        
    }
    
    public HnmSpeechSignal analyze(String wavFile, double skipSizeInSeconds)
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
        
        double[] phases;
        double[] dPhases;
        double[] dPhasesPrev = null;
        int M;
        
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
            win.normalize(1.0f); //Normalize to sum up to unity
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
            //Stylianou does this in a separate, fixed frame rate (10 ms) and window size (40 ms) loop
            //Here, it is pitch synchronous for voiced frames and pitch asynchronous for unvoiced ones
            //Additionally, we have support for preemphasis - this needs to be handled during synthesis of the noisy part with preemphasis removal
            frm = win.apply(frm, 0);
            LpCoeffs lpcs = LpcAnalyser.calcLPC(frm, lpOrder, (float)preCoef);
            hnmSignal.frames[i].n = new FrameNoisePart(lpcs.getA(), lpcs.getGain());
            //
            
            //Step6. Estimate amplitude envelopes
            if (isVoiced)
            {
                int cepsOrder = 19;
                double[] linearAmps = new double[numHarmonics];
                double[] freqsInHz = new double [numHarmonics];
                for (j=0; j<numHarmonics; j++)
                {
                    linearAmps[j] = MathUtils.magnitudeComplex(harmonicAmps[numHarmonics-j-1]);
                    //linearAmps[j] = MathUtils.magnitudeComplex(harmonicAmps[j]);
                    freqsInHz[j] = f0InHz*(j+1);
                }
                hnmSignal.frames[i].ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmps, freqsInHz, fs, cepsOrder);
                //The following is only for visualization
                //int fftSize = 4096;
                //double[] vocalTractDB = RegularizedCepstralEnvelopeEstimator.cepstrum2logAmpHalfSpectrum(hnmFrames[i].ceps , fftSize, fs);
                //MaryUtils.plot(vocalTractDB);
                //
            }
            //
            
            if (true) //NOT SURE IF STEP7 IS REQUIRED SINCE IN SYNTHESIS WE WILL DO SIMILAR THINGS, SO WE PASS A DIRECT COPY OF PHASES HERE
            {
                hnmSignal.frames[i].h.phases = new float[numHarmonics];
                for (k=0; k<numHarmonics; k++)
                    hnmSignal.frames[i].h.phases[numHarmonics-k-1] = (float)MathUtils.phaseInRadians(harmonicAmps[numHarmonics-k-1]);
            }
            else
            {
                //Step7. Estimate phase envelopes
                if (isVoiced)
                {
                    phases = new double[numHarmonics];
                    dPhases = new double[numHarmonics];

                    for (k=0; k<numHarmonics; k++)
                        phases[k] = MathUtils.phaseInRadians(harmonicAmps[numHarmonics-k-1]);

                    if (!isPrevVoiced) //A new voiced segment starts here
                    {
                        for (k=0; k<numHarmonics; k++)
                        {
                            if (k==0)
                            {
                                if (k<numHarmonics-1)
                                    dPhases[k] = phases[k+1]-phases[k];
                                else
                                    dPhases[k] = 0.0;
                            }
                            else
                            {
                                if (k<numHarmonics-1)
                                {
                                    M = (int)Math.floor(dPhases[k-1]/((phases[k+1]-phases[k])*MathUtils.TWOPI)+0.5);
                                    phases[k] = (phases[k+1]+M*MathUtils.TWOPI) - phases[k]; 
                                    dPhases[k] = phases[k+1]-phases[k];
                                }
                                else
                                    dPhases[k] = 0.0;
                            }
                        }
                    }
                    else //Already inside a voiced segment
                    {
                        for (k=0; k<Math.min(numHarmonics, prevNumHarmonics); k++)
                        {
                            if (k>0 && k<phases.length-1)
                            {
                                M = (int)Math.floor(dPhasesPrev[k-1]/((phases[k+1]-phases[k])*MathUtils.TWOPI)+0.5);
                                phases[k] = (phases[k+1]+M*MathUtils.TWOPI) - phases[k]; 
                                dPhasesPrev[k] = phases[k+1]-phases[k];
                            }
                            else
                                dPhasesPrev[k] = 0.0;
                        }
                        
                        for (k=prevNumHarmonics; k<numHarmonics; k++)
                        {
                            if (k==0)
                            {
                                if (k<numHarmonics-1)
                                    dPhases[k] = phases[k+1]-phases[k];
                                else
                                    dPhases[k] = 0.0;
                            }
                            else
                            {
                                M = (int)Math.floor(dPhases[k-1]/((phases[k+1]-phases[k])*MathUtils.TWOPI)+0.5);
                                phases[k] = (phases[k+1]+M*MathUtils.TWOPI) - phases[k]; 
                                dPhases[k] = phases[k+1]-phases[k];
                            }
                        }
                    }

                    hnmSignal.frames[i].h.phases = new float[numHarmonics];
                    for (k=0; k<numHarmonics; k++)
                        hnmSignal.frames[i].h.phases[numHarmonics-k-1] = (float)phases[k];

                    prevNumHarmonics = numHarmonics;
                    dPhasesPrev = new double[prevNumHarmonics];
                    System.arraycopy(dPhases, 0, dPhasesPrev, 0, prevNumHarmonics);  
                }
                //
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
        for (k=0; k<numHarmonics+1; k++) //The remaning complex amplitudes from L+1 to 2L are complex conjugates of entries from L-1,...,1
            xpart[k] = new ComplexNumber(x[k]);
        
        return xpart;
    }
    
    public static void main(String[] args)
    {
        String wavFile = args[0];
        double skipSizeInSeconds = 0.010;
        
        HnmAnalyzer ha = new HnmAnalyzer();
        
        HnmSpeechSignal hnmSignal = ha.analyze(wavFile, skipSizeInSeconds);
    }
}

