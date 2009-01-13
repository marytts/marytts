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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Synthesis using harmonics plus noise model
 * 
 * @author Oytun T&uumlrk
 *
 */
public class HnmSynthesizer {
    
    public HnmSynthesizer()
    {
        
    }
    
    public double[] syntesize(HnmSpeechSignal hnmSignal, 
                              float[] tScales,
                              float[] tScalesTimes,
                              float[] pScales,
                              float[] pScalesTimes)
    {
        double[] h = synthesizeHarmonicPart(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        double[] n = synthesizeNoisePart(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        
        return SignalProcUtils.addSignals(h, n);
    }
    
    public double[] synthesizeHarmonicPart(HnmSpeechSignal hnmSignal, 
                                           float[] tScales,
                                           float[] tScalesTimes,
                                           float[] pScales,
                                           float[] pScalesTimes)
    {
        double[] h = null;
        
        int i, k, n;
        float t, tsi, tsiPlusOne; //Time in seconds
        int startIndex, endIndex;
        double akt;
        int numHarmonicsCurrentFrame;
        int maxNumHarmonics = 0;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f)
            {
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }
        
        double[] aksis = null;
        double[] aksiPlusOnes = null;
        float[] phasekis = null;
        float[] phasekiPlusOnes = null;
        if (maxNumHarmonics>0)
        {
            aksis = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            aksiPlusOnes = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            phasekis = new float[maxNumHarmonics];
            Arrays.fill(phasekis, 0.0f);
            phasekiPlusOnes = new float[maxNumHarmonics];
            Arrays.fill(phasekiPlusOnes, 0.0f);
        }
        
        float f0InHz, f0InHzNext, f0av;
        int lastPeriodInSamples = 0;
        double ht;
        float phasekt = 0.0f;
        
        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isVoiced, isNextVoiced;
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        h = new double[origLen+2000]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(h, 0.0);
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isVoiced = (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f) ? true : false;
            if (i<hnmSignal.frames.length-1)
                isNextVoiced = (hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz>0.0f) ? true : false;
            else
                isNextVoiced = false;
            
            tsi = hnmSignal.frames[i].tAnalysisInSeconds;
            startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);
            if (isNextVoiced)
            {
                tsiPlusOne = hnmSignal.frames[i+1].tAnalysisInSeconds;
                endIndex = SignalProcUtils.time2sample(tsiPlusOne, hnmSignal.samplingRateInHz);
            }
            else
            {
                endIndex = startIndex + lastPeriodInSamples;
                tsiPlusOne = SignalProcUtils.sample2time(endIndex, hnmSignal.samplingRateInHz);
            }

            numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
            for (n=startIndex; n<endIndex; n++)
            {
                t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                h[n] = 0.0;
                for (k=0; k<Math.min(numHarmonicsCurrentFrame,maxNumHarmonics); k++)
                {
                    f0InHz = hnmSignal.frames[i].h.f0InHz;
                    if (isNextVoiced && k<hnmSignal.frames[i+1].h.phases.length)
                        f0InHzNext = hnmSignal.frames[i+1].h.f0InHz;
                    else
                        f0InHzNext = f0InHz;

                    f0av = 0.5f*(f0InHz+f0InHzNext);

                    //Estimate amplitude
                    if (i==0)
                        aksis[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i].ceps, (k+1)*f0InHz, hnmSignal.samplingRateInHz);
                    else
                    {
                        if (hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz>0.0f)
                            aksis[k] = aksiPlusOnes[k]; //from previous
                        else
                            aksis[k] = 0.0;
                    }

                    if (isNextVoiced)
                        aksiPlusOnes[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i+1].ceps , (k+1)*f0InHzNext, hnmSignal.samplingRateInHz);
                    else
                        aksiPlusOnes[k] = 0.0;

                    akt = aksis[k] + (aksiPlusOnes[k]-aksis[k])*(t-tsi)/(tsiPlusOne-tsi);
                    
                    /*
                    f0InHzNext = 400.0f;
                    f0InHz = 400.0f;
                    f0av = 400.0f;
                    akt = 1.0;
                    */
                    //

                    //Estimate phase
                    if (isVoiced)
                        phasekis[k] = hnmSignal.frames[i].h.phases[k];
                    
                    if (isNextVoiced && k<hnmSignal.frames[i+1].h.phases.length)
                        phasekiPlusOnes[k] = hnmSignal.frames[i+1].h.phases[k];
                        
                    if (!isVoiced)
                        phasekis[k] = (float)( phasekiPlusOnes[k] - (k+1)*MathUtils.TWOPI*f0InHzNext*(tsiPlusOne-tsi) ); //Equation (3.54)
                     
                    if (!(isNextVoiced && k<hnmSignal.frames[i+1].h.phases.length))
                        phasekiPlusOnes[k] = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0InHz*(tsiPlusOne-tsi) ); //Equation (3.55)
                        
                    phasekiPlusOneEstimate = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0av*(tsiPlusOne-tsi));
                    Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOnes[k])/MathUtils.TWOPI + 0.5);
                    phasekt = (float)( phasekis[k] + (phasekiPlusOnes[k]+MathUtils.TWOPI*Mk-phasekis[k])*(t-tsi)/(tsiPlusOne-tsi) );
                    //

                    h[n] += akt*Math.cos(phasekt);
                }
            }

            lastPeriodInSamples = SignalProcUtils.time2sample(tsiPlusOne - tsi, hnmSignal.samplingRateInHz);
        }
        
        return h;
    }
    
    public double[] synthesizeNoisePart(HnmSpeechSignal hnmSignal, 
                                        float[] tScales,
                                        float[] tScalesTimes,
                                        float[] pScales,
                                        float[] pScalesTimes)
    {
        double[] n = null;

        return n;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        //File input
        String wavFile = args[0];
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] x = signal.getAllData();
        double maxOrig = MathUtils.getAbsMax(x);
        //
        
        //Analysis
        double skipSizeInSeconds = 0.010;

        HnmAnalyzer ha = new HnmAnalyzer();
        
        HnmSpeechSignal hnmSignal = ha.analyze(wavFile, skipSizeInSeconds);
        //
        
        //Synthesis
        float[] tScales = {1.0f};
        float[] tScalesTimes = null;
        float[] pScales = {1.0f};
        float[] pScalesTimes = null;
        
        HnmSynthesizer hs = new HnmSynthesizer();
        double[] y = hs.syntesize(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        //This scaling is only for comparison among different parameter sets, different synthesizer outputs etc
        double maxNew = MathUtils.getAbsMax(y);
        for (int i=0; i<y.length; i++)
            y[i] = y[i]*(maxOrig/maxNew);
        //
        
        //File output
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        String outFileName = args[0].substring(0, args[0].length()-4) + "_hnmResynth.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        //
    }
}