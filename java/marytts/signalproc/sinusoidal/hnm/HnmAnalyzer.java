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
    
    public void analyze(String wavFile, double skipSizeInSeconds)
    {
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
        
        /* TO DO
        //Step1. Initial pitch estimation
        
        //Step2: Do for each frame (at 10 ms skip rate):
        //2.a. Voiced/Unvoiced decision
        
        //2.b. If voiced, maximum frequency of voicing estimation
        //     Otherwise, maximum frequency of voicing is set to 0
        
        //2.c. Refined pitch estimation
        //
        */
        
        String strPitchFile = StringUtils.modifyExtension(wavFile, ".ptc");
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
        f0s = f0.contour;
        
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
        
        
        int ws = (int)Math.floor(numPeriods*T0+ 0.5);
        if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
            ws++;
        int ss = (int)Math.floor(skipSizeInSeconds*fs+0.5);

        int totalFrm = (int)(x.length/ss+0.5);
        
        //Extract frames and analyze them
        double [] frm = null;
        int i, j;
        
        
        int pmInd = 0;
        
        boolean isOutputToTextFile = false;
        Window win;
        int closestInd;
        int currentTimeInd = 0;
        double currentTime;
        
        for (i=0; i<totalFrm; i++)
        {  
            currentTime = (currentTimeInd+0.5f*ws)/fs; //Middle of analysis frame
            
            closestInd = SignalProcUtils.time2frameIndex(currentTime, f0.header.ws, f0.header.ss);
            f0InHz = f0.contour[closestInd];
            
            if (f0InHz>10.0)
                isVoiced=true;
            else
            {
                isVoiced=false;
                f0InHz=assumedF0ForUnvoicedInHz;
            }

            T0 = (int)Math.floor(fs/f0InHz+0.5);

            ws = (int)Math.floor(numPeriods*T0+ 0.5);
            if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                ws++;
         
            frm = new double[ws];
            
            Arrays.fill(frm, 0.0);
            
            for (j=currentTimeInd; j<Math.min(currentTimeInd+ws-1, x.length); j++)
                 frm[j-currentTimeInd] = x[j];
            
            win = Window.get(windowType, ws);
            win.normalize(1.0f); //Normalize to sum up to unity
            double[] wgtSquared = win.getCoeffs();
            for (j=0; j<wgtSquared.length; j++)
                wgtSquared[j] = wgtSquared[j]*wgtSquared[j];
            
            double maxFreqOfVoicingInHz = 700.0;
            
            //Step4. Estimate complex amplitudes of harmonics if voiced
            //       The phase of the complex amplitude is the phase and its magnitude is the absolute amplitude of the harmonic
            if (isVoiced)
            {
                ComplexNumber[] a = estimateComplexAmplitudes(frm, wgtSquared, f0InHz, maxFreqOfVoicingInHz, fs, currentTimeInd);
                double[] absMags = MathUtils.magnitudeComplex(a);
                double[] dbMags = MathUtils.amp2db(absMags);
                MaryUtils.plot(dbMags);
            }
            
            currentTimeInd += ss;

            System.out.println("Analysis complete at " + String.valueOf(currentTime) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(totalFrm)); 
        }
    }
    
    //tia: analysis instant in samples
    //N: local pitch period in samples
    //wgtSquared: window weights squared
    //frm: speech frame to be analysed (its length should be 2*N+1)
    public ComplexNumber[] estimateComplexAmplitudes(double[] frm, double[] wgtSquared, double f0InHz, double maxFreqOfVoicingInHz, int samplingRateInHz, int tia)
    {
        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;
        int L = (int)Math.floor(maxFreqOfVoicingInHz/f0InHz+0.5);
        ComplexNumber[][] R = new ComplexNumber[2*L+1][2*L+1];
        ComplexNumber[] b = new ComplexNumber[2*L+1];
        ComplexNumber tmp;
        
        int t, i, k;
        double omega;

        for (i=1; i<=2*L+1; i++)
        {
            for (k=1; k<=2*L+1; k++)
            {
                R[i-1][k-1] = new ComplexNumber(0.0, 0.0);
                for (t=tia-N; t<=tia+N; t++)
                {
                    omega = MathUtils.TWOPI*f0InHz*(t-tia)/samplingRateInHz*(k-i);
                    tmp = new ComplexNumber(wgtSquared[t-tia+N]*Math.cos(omega), wgtSquared[t-tia+N]*Math.sin(omega));
                    R[i-1][k-1] = MathUtils.addComplex(R[i-1][k-1], tmp);
                }
            }
        }   
        
        for (k=1; k<=2*L+1; k++)
        {
            b[k-1] = new ComplexNumber(0.0, 0.0);
            for (t=tia-N; t<=tia+N; t++)
            {
                omega = MathUtils.TWOPI*f0InHz*(t-tia)/samplingRateInHz*(L+1-k);
                tmp = new ComplexNumber(wgtSquared[t-tia+N]*frm[t-tia+N]*Math.cos(omega), wgtSquared[t-tia+N]*frm[t-tia+N]*Math.sin(omega));
                b[k-1] = MathUtils.addComplex(b[k-1], tmp);
            }
        }
        
        ComplexNumber[] x = MathUtils.matrixProduct(MathUtils.inverse(R), b);
        ComplexNumber[] xpart = new ComplexNumber[L+1];
        for (k=0; k<L+1; k++)
            xpart[k] = new ComplexNumber(x[k]);
        
        return xpart;
    }
    
    public static void main(String[] args)
    {
        String wavFile = args[0];
        double skipSizeInSeconds = 0.010;
        
        HnmAnalyzer ha = new HnmAnalyzer();
        
        ha.analyze(wavFile, skipSizeInSeconds);
    }
}

