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

import marytts.util.data.AudioDoubleDataSource;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class FIRWaveletFilterBankSynthesiser {
    public FIRWaveletFilterBankSynthesiser()
    {

    }

    public double[] apply(FIRWaveletFilterBankAnalyser analyser, Subband[] subbands, boolean bNormalizeInOverlappingRegions)
    {
        double[] x = null;
        double[] lowBandIntFilt = null;
        double[] highBandIntFilt = null;
        
        if (analyser!=null && analyser.filters!=null && subbands!=null)
        {
            int i, j;
            for (i=subbands.length-2; i>=0; i--)
            {
                if (i==subbands.length-2)
                    lowBandIntFilt = SignalProcUtils.interpolate(subbands[i+1].waveform, 2.0);
                else
                    lowBandIntFilt = SignalProcUtils.interpolate(x, 2.0);

                lowBandIntFilt = analyser.filters[i][0].apply(lowBandIntFilt);
                
                highBandIntFilt = SignalProcUtils.interpolate(subbands[i].waveform, 2.0);
                highBandIntFilt = analyser.filters[i][1].apply(highBandIntFilt);
                
                x = new double[Math.max(lowBandIntFilt.length, highBandIntFilt.length)];
                Arrays.fill(x, 0.0);
                System.arraycopy(lowBandIntFilt, 0, x, 0, lowBandIntFilt.length);
                for (j=0; j<highBandIntFilt.length; j++)
                    x[j] += highBandIntFilt[j];
                
                if (bNormalizeInOverlappingRegions)
                    x = SignalProcUtils.filterfd(analyser.normalizationFilterTransformedIRs[i], x, 2.0*analyser.samplingRates[i]);
            }
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
        int numLevels = 3;
        boolean bNormalizeInOverlappingRegions = false;
            
        FIRWaveletFilterBankAnalyser analyser = new FIRWaveletFilterBankAnalyser(numLevels, samplingRate);
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
        
        FIRWaveletFilterBankSynthesiser synthesiser = new FIRWaveletFilterBankSynthesiser();
        double[] y = synthesiser.apply(analyser, subbands, bNormalizeInOverlappingRegions);
        
        outputFormat = new AudioFormat(samplingRate, inputAudio.getFormat().getSampleSizeInBits(),  inputAudio.getFormat().getChannels(), true, true);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), outputFormat);
        outFileName = args[0].substring(0, args[0].length()-4) + "_resynthesis" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
    }
}
