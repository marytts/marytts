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

package de.dfki.lt.signalproc.filter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class FIRBandPassFilterBankSynthesiser {
    public FIRBandPassFilterBankSynthesiser()
    {
        
    }

    public double[] apply(FIRBandPassFilterBankAnalyser analyser, Subband[] subbands)
    {
        double[] x = null;

        if (analyser!=null && analyser.filters!=null && subbands!=null)
        {
            int i, j, maxLen;
            
            assert analyser.filters.length == subbands.length;

            int maxFreq = analyser.filters[0].transformedIR.length/2 + 1;

            //Estimate a smooth gain normalization filter
            double[] Hw = new double[maxFreq];
            Arrays.fill(Hw, 0.0);

            for (i=0; i<analyser.filters.length; i++)
            {
                Hw[0] += Math.abs(analyser.filters[i].transformedIR[0]);
                Hw[maxFreq-1] += Math.abs(analyser.filters[i].transformedIR[1]);
                for (j=1; j<maxFreq-1; j++)   
                    Hw[j] += Math.sqrt(analyser.filters[i].transformedIR[2*j]*analyser.filters[i].transformedIR[2*j] + analyser.filters[i].transformedIR[2*j+1]*analyser.filters[i].transformedIR[2*j+1]);    
            }
            
            for (j=0; j<maxFreq; j++)
                Hw[j] = 1.0/Hw[j];

            MaryUtils.plot(Hw, "Normalization filter", false);
            //

            //Add all subbands up and then apply the smooth gain normalization filter
            maxLen = subbands[0].waveform.length;
            for (i=1; i<subbands.length; i++)
                maxLen = Math.max(maxLen, subbands[i].waveform.length);
            
            x = new double[maxLen];
            Arrays.fill(x, 0.0);
            for (i=0; i<subbands.length; i++)
            {
                for (j=0; j<subbands[i].waveform.length; j++)
                    x[j] += subbands[i].waveform[j];
            }
            
            x = SignalProcUtils.filterfd(Hw, x, subbands[0].samplingRate);
        }

        return x;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        int i;
        int numBands = 4;
        double halfSamplingRate = 0.5*samplingRate;
        double[] lowerCutOffsInHz = new double[numBands];
        double[] upperCutOffsInHz = new double[numBands];
        double overlapInHz;
        
        for (i=0; i<numBands; i++)
        {
            if (i<numBands-1)
                upperCutOffsInHz[i] = samplingRate/Math.pow(2, numBands-i);
            else
                upperCutOffsInHz[i] = halfSamplingRate;
            
            if (i==0)
                lowerCutOffsInHz[i] = 0.0;
            else
                lowerCutOffsInHz[i] = upperCutOffsInHz[i-1];
            
            overlapInHz = 0.5*(upperCutOffsInHz[i]+lowerCutOffsInHz[i])/(1000.0/FIRBandPassFilterBankAnalyser.OVERLAP_AROUND_1000HZ);
            
            if (i>0)
                lowerCutOffsInHz[i] -= overlapInHz;
            if (i<numBands-1)
                upperCutOffsInHz[i] += overlapInHz;
            
            System.out.println("Subband #" + String.valueOf(i+1) + " - Lower cutoff: " + String.valueOf(lowerCutOffsInHz[i]) + " Upper cutoff: " + String.valueOf(upperCutOffsInHz[i]));
        }
            
        FIRBandPassFilterBankAnalyser analyser = new FIRBandPassFilterBankAnalyser(lowerCutOffsInHz, upperCutOffsInHz, samplingRate);
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
        
        FIRBandPassFilterBankSynthesiser synthesiser = new FIRBandPassFilterBankSynthesiser();
        double[] y = synthesiser.apply(analyser, subbands);
        
        outputFormat = new AudioFormat(subbands[0].samplingRate, inputAudio.getFormat().getSampleSizeInBits(),  inputAudio.getFormat().getChannels(), true, true);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), outputFormat);
        outFileName = args[0].substring(0, args[0].length()-4) + "_resynthesis" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
    }
}
