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

package marytts.signalproc.filter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.util.SignalProcUtils;
import marytts.util.MaryUtils;
import marytts.util.audio.AudioDoubleDataSource;
import marytts.util.audio.BufferedDoubleDataSource;
import marytts.util.audio.DDSAudioInputStream;


/**
 * @author oytun.turk
 *
 * This is a simple FIR bandpass filterbank structure with no resampling operations
 * The filters are overlapping and a simple DFT-based frequency response estimation method
 *  is used for reducing reconstruction error due to non-ideal filtering scheme and overlap among filters
 * Given a sampling rate and a set of lower and upper cutoff frequency values in Hz,
 *  a set of bandpass filters that overlap by some amount in frequency
 */
public class FIRBandPassFilterBankAnalyser extends FilterBankAnalyserBase {
    public static final double OVERLAP_AROUND_1000HZ_DEFAULT = 100.0;
    public double overlapAround1000Hz;
    
    public FIRFilter[] filters;
    public double[] normalizationFilterTransformedIR;
    
    public FIRBandPassFilterBankAnalyser(int numBands, int samplingRateInHz)
    {
        this(numBands, samplingRateInHz, OVERLAP_AROUND_1000HZ_DEFAULT);
    }
    
    public FIRBandPassFilterBankAnalyser(int numBands, int samplingRateInHz, double overlapAround1000HzIn)
    {
        double halfSamplingRate = 0.5*samplingRateInHz;
        double[] lowerCutOffsInHz = new double[numBands];
        double[] upperCutOffsInHz = new double[numBands];
        double overlapInHz;
        int i;
        overlapAround1000Hz = overlapAround1000HzIn;
        
        for (i=0; i<numBands; i++)
        {
            if (i<numBands-1)
                upperCutOffsInHz[i] = samplingRateInHz/Math.pow(2, numBands-i);
            else
                upperCutOffsInHz[i] = halfSamplingRate;
            
            if (i==0)
                lowerCutOffsInHz[i] = 0.0;
            else
                lowerCutOffsInHz[i] = upperCutOffsInHz[i-1];
            
            overlapInHz = 0.5*(upperCutOffsInHz[i]+lowerCutOffsInHz[i])/(1000.0/overlapAround1000Hz);
            
            if (i>0)
                lowerCutOffsInHz[i] -= overlapInHz;
            if (i<numBands-1)
                upperCutOffsInHz[i] += overlapInHz;
            
            System.out.println("Subband #" + String.valueOf(i+1) + " - Lower cutoff: " + String.valueOf(lowerCutOffsInHz[i]) + " Upper cutoff: " + String.valueOf(upperCutOffsInHz[i]));
        }
        
        initialise(lowerCutOffsInHz, upperCutOffsInHz, samplingRateInHz, overlapAround1000HzIn);
    }
    
    public FIRBandPassFilterBankAnalyser(double[] lowerCutOffsInHz, double[] upperCutOffsInHz, int samplingRateInHz)
    {
        this(lowerCutOffsInHz, upperCutOffsInHz, samplingRateInHz, OVERLAP_AROUND_1000HZ_DEFAULT);
    }
    
    public FIRBandPassFilterBankAnalyser(double[] lowerCutOffsInHz, double[] upperCutOffsInHz, int samplingRateInHz, double overlapAround1000HzIn)
    {
        initialise(lowerCutOffsInHz, upperCutOffsInHz, samplingRateInHz, overlapAround1000HzIn);
    }
    
    public void initialise(double[] lowerCutOffsInHz, double[] upperCutOffsInHz, int samplingRateInHz, double overlapAround1000HzIn)
    {
        normalizationFilterTransformedIR = null;
        
        if (lowerCutOffsInHz!=null && upperCutOffsInHz!=null)
        {
            assert lowerCutOffsInHz.length == upperCutOffsInHz.length;
            int i;
            filters = new FIRFilter[lowerCutOffsInHz.length];
            int filterOrder = SignalProcUtils.getFIRFilterOrder(samplingRateInHz);
            double normalizedLowerCutoff;
            double normalizedUpperCutoff;
            
            overlapAround1000Hz = overlapAround1000HzIn;
            
            for (i=0; i<lowerCutOffsInHz.length; i++)
                assert lowerCutOffsInHz[i]<upperCutOffsInHz[i];
            
            for (i=0; i<lowerCutOffsInHz.length; i++)
            {
                if (lowerCutOffsInHz[i]<=0.0)
                {
                    normalizedUpperCutoff = Math.min(upperCutOffsInHz[i]/samplingRateInHz, 0.5);
                    normalizedUpperCutoff = Math.max(normalizedUpperCutoff, 0.0);
                    filters[i] = new LowPassFilter(normalizedUpperCutoff, filterOrder);
                }
                else if (upperCutOffsInHz[i]>=0.5*samplingRateInHz)
                {
                    normalizedLowerCutoff = Math.max(lowerCutOffsInHz[i]/samplingRateInHz, 0.0);
                    normalizedLowerCutoff = Math.min(normalizedLowerCutoff, 0.5);
                    filters[i] = new HighPassFilter(normalizedLowerCutoff, filterOrder);
                }
                else
                {
                    normalizedLowerCutoff = Math.max(lowerCutOffsInHz[i]/samplingRateInHz, 0.0);
                    normalizedLowerCutoff = Math.min(normalizedLowerCutoff, 0.5);
                    normalizedUpperCutoff = Math.min(upperCutOffsInHz[i]/samplingRateInHz, 0.5);
                    normalizedUpperCutoff = Math.max(normalizedUpperCutoff, 0.0);
                    
                    assert normalizedLowerCutoff<normalizedUpperCutoff;
                    
                    filters[i] = new BandPassFilter(normalizedLowerCutoff, normalizedUpperCutoff, filterOrder);
                }
            } 
            
            int maxFreq = filters[0].transformedIR.length/2 + 1;

            //Estimate a smooth gain normalization filter
            normalizationFilterTransformedIR = new double[maxFreq];
            Arrays.fill(normalizationFilterTransformedIR, 0.0);

            int j;
            for (i=0; i<filters.length; i++)
            {
                normalizationFilterTransformedIR[0] += Math.abs(filters[i].transformedIR[0]);
                normalizationFilterTransformedIR[maxFreq-1] += Math.abs(filters[i].transformedIR[1]);
                for (j=1; j<maxFreq-1; j++)   
                    normalizationFilterTransformedIR[j] += Math.sqrt(filters[i].transformedIR[2*j]*filters[i].transformedIR[2*j] + filters[i].transformedIR[2*j+1]*filters[i].transformedIR[2*j+1]);    
            }
            
            for (j=0; j<maxFreq; j++)
                normalizationFilterTransformedIR[j] = 1.0/normalizationFilterTransformedIR[j];

            //MaryUtils.plot(normalizationFilterTransformedIR, "Normalization filter");
            //
        }
    }
    
    public Subband[] apply(double[] x, int samplingRateInHz)
    {
        Subband[] subbands = null;
        
        if (filters!=null && x!=null)
        {
            int i;
            subbands = new Subband[filters.length];
            for (i=0; i<filters.length; i++)
            {
                if (filters[i] instanceof LowPassFilter)
                    subbands[i] = new Subband(filters[i].apply(x), samplingRateInHz, 0.0, ((LowPassFilter)filters[i]).normalisedCutoffFrequency*samplingRateInHz);
                else if (filters[i] instanceof HighPassFilter)
                    subbands[i] = new Subband(filters[i].apply(x), samplingRateInHz, ((HighPassFilter)filters[i]).normalisedCutoffFrequency*samplingRateInHz, 0.5*samplingRateInHz);
                else if (filters[i] instanceof BandPassFilter)
                    subbands[i] = new Subband(filters[i].apply(x), samplingRateInHz, ((BandPassFilter)filters[i]).lowerNormalisedCutoffFrequency*samplingRateInHz, ((BandPassFilter)filters[i]).upperNormalisedCutoffFrequency*samplingRateInHz);
            }
         }
        //
        
        return subbands;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        int i;
        int numBands = 4;
        double overlapAround1000Hz = 100.0;
            
        FIRBandPassFilterBankAnalyser analyser = new FIRBandPassFilterBankAnalyser(numBands, samplingRate, overlapAround1000Hz);
        Subband[] subbands = analyser.apply(x, samplingRate);
        
        DDSAudioInputStream outputAudio;
        AudioFormat outputFormat;
        String outFileName;
        
        //Write highpass components 0 to numLevels-1
        for (i=0; i<subbands.length; i++)
        {
            outputFormat = new AudioFormat(subbands[i].samplingRate, inputAudio.getFormat().getSampleSizeInBits(),  inputAudio.getFormat().getChannels(), true, true);
            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(subbands[i].waveform), outputFormat);
            outFileName = args[0].substring(0, args[0].length()-4) + "_band" + String.valueOf(i+1) + ".wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }
    }
}
