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

package de.dfki.lt.mary.sinusoidal.multiresolution;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import de.dfki.lt.mary.sinusoidal.SinusoidalAnalyzer;
import de.dfki.lt.mary.sinusoidal.SinusoidalSynthesizer;
import de.dfki.lt.mary.sinusoidal.SinusoidalTracks;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class MultiresolutionSinusoidalSynthesizer {
    
    public MultiresolutionSinusoidalSynthesizer()
    {
        
    }
    
    public double[] synthesize(SinusoidalTracks[] subbandTracks, boolean isSilentSynthesis)
    {
        double[] y = null;
        double[] tmpy = null;
        if (subbandTracks!=null)
        {
            int i, j;
            
            //Sinusoidal resynthesis
            for (i=0; i<subbandTracks.length; i++)
            {
                SinusoidalSynthesizer ss = new SinusoidalSynthesizer(subbandTracks[i].fs);
                tmpy = ss.synthesize(subbandTracks[i], isSilentSynthesis);
                
                if (i==0)
                {
                    y = new double[tmpy.length];
                    System.arraycopy(tmpy, 0, y, 0, tmpy.length);
                }
                else
                {
                    for (j=0; j<Math.min(y.length, tmpy.length); j++)
                        y[j] += tmpy[j];
                }
            }
        }
        
        return y;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();

        int multiresolutionFilterbankType = MultiresolutionSinusoidalAnalyzer.FIR_BANDPASS_FILTERBANK;
        //int multiresolutionFilterbankType = MultiresolutionSinusoidalAnalyzer.COMPLEMENTARY_FILTERBANK;
        int numBands = 4;
        double lowestBandWindowSizeInSeconds = 0.020;
        int windowType = Window.HANN;
        boolean bRefinePeakEstimatesParabola = false;
        boolean bRefinePeakEstimatesBias = false;
        boolean bSpectralReassignment = false;
        boolean bAdjustNeighFreqDependent = false;
        boolean isSilentSynthesis = false;

        MultiresolutionSinusoidalAnalyzer msa = new MultiresolutionSinusoidalAnalyzer();

        SinusoidalTracks[] subbandTracks = msa.analyzeFixedRate(x, samplingRate, multiresolutionFilterbankType, numBands, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);
       

        //Resynthesis
        MultiresolutionSinusoidalSynthesizer mss = new MultiresolutionSinusoidalSynthesizer();
        x = mss.synthesize(subbandTracks, isSilentSynthesis);
        //
        
        //This scaling is only for comparison among different parameter sets, different synthesizer outputs etc
        double maxx = MathUtils.getAbsMax(x);
        for (int i=0; i<x.length; i++)
            x[i] = x[i]/maxx*0.9;
        //
        

        //File output
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), inputAudio.getFormat());
        String outFileName = args[0].substring(0, args[0].length()-4) + "_multiResSinResynth.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        //
    }
}
