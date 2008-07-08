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
 */
public class FIRBandPassFilterBankSynthesiser {
    public FIRBandPassFilterBankSynthesiser()
    {
        
    }
    
    public double[] apply(FIRBandPassFilterBankAnalyser analyser, Subband[] subbands)
    {
        return apply(analyser,  subbands, true);
    }

    public double[] apply(FIRBandPassFilterBankAnalyser analyser, Subband[] subbands, boolean bNormalizeInOverlappingRegions)
    {
        double[] x = null;

        if (analyser!=null && analyser.filters!=null && subbands!=null)
        {
            int i, j, maxLen;
            
            assert analyser.filters.length == subbands.length;
            
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
            
            x = SignalProcUtils.filterfd(analyser.normalizationFilterTransformedIR, x, subbands[0].samplingRate);
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
        double overlapAround1000Hz = 100.0;
            
        FIRBandPassFilterBankAnalyser analyser = new FIRBandPassFilterBankAnalyser(numBands, samplingRate, overlapAround1000Hz);
        Subband[] subbands = analyser.apply(x);
        
        DDSAudioInputStream outputAudio;
        AudioFormat outputFormat;
        String outFileName;
        
        //Write highpass components 0 to numLevels-1
        for (i=0; i<subbands.length; i++)
        {
            outputFormat = new AudioFormat((int)(subbands[i].samplingRate), inputAudio.getFormat().getSampleSizeInBits(),  inputAudio.getFormat().getChannels(), true, true);
            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(subbands[i].waveform), outputFormat);
            outFileName = args[0].substring(0, args[0].length()-4) + "_band" + String.valueOf(i+1) + ".wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }
        
        FIRBandPassFilterBankSynthesiser synthesiser = new FIRBandPassFilterBankSynthesiser();
        double[] y = synthesiser.apply(analyser, subbands);
        
        outputFormat = new AudioFormat((int)(subbands[0].samplingRate), inputAudio.getFormat().getSampleSizeInBits(),  inputAudio.getFormat().getChannels(), true, true);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), outputFormat);
        outFileName = args[0].substring(0, args[0].length()-4) + "_resynthesis" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
    }
}
