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

package de.dfki.lt.mary.sinusoidal;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.filter.ComplementaryFilterBankAnalyser;
import de.dfki.lt.signalproc.filter.FIRBandPassFilterBankAnalyser;
import de.dfki.lt.signalproc.filter.FilterBankAnalyserBase;
import de.dfki.lt.signalproc.filter.Subband;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class MultiresolutionSinusoidalAnalyzer extends BaseSinusoidalAnalyzer {
    public static final int FIR_BANDPASS_FILTERBANK = 1;
    public static final int COMPLEMENTARY_FILTERBANK = 2;
    
    public MultiresolutionSinusoidalAnalyzer()
    {
        
    }
    
    public SinusoidalTracks[] analyzeFixedRate(double[] x, int samplingRate,
                                               int multiresolutionFilterbankType,
                                               int numBands,
                                               double lowestBandWindowSizeInSeconds,
                                               int windowType,
                                               boolean bRefinePeakEstimatesParabola,
                                               boolean bRefinePeakEstimatesBias,
                                               boolean bSpectralReassignment,
                                               boolean bAdjustNeighFreqDependent,
                                               boolean bFreqLimitedAnalysis)
    {
        SinusoidalTracks[] subbandTracks = new SinusoidalTracks[numBands];
        
        Subband[] subbands = null;
        FilterBankAnalyserBase analyser = null;
        
        if (multiresolutionFilterbankType==MultiresolutionSinusoidalAnalyzer.FIR_BANDPASS_FILTERBANK)
        {
            double overlapAround1000Hz = 100.0;
                
            analyser = new FIRBandPassFilterBankAnalyser(numBands, samplingRate, overlapAround1000Hz);            
        }
        else if (multiresolutionFilterbankType==MultiresolutionSinusoidalAnalyzer.COMPLEMENTARY_FILTERBANK)
        {
            if (MathUtils.isPowerOfTwo(numBands))
            {
                int tmpNumBands = 2;
                while (tmpNumBands<numBands)
                    tmpNumBands*=2;
                numBands = tmpNumBands;
                System.out.println("Number of bands should be a power of two for the complementary filterbank");
            }
                
            int baseFilterOrder = SignalProcUtils.getFIRFilterOrder(samplingRate);
            int numLevels = (int)(Math.log(numBands)/Math.log(2));
            analyser = new ComplementaryFilterBankAnalyser(numLevels, baseFilterOrder);
        }
        
        if (analyser!=null)
        {
            subbands = analyser.apply(x, samplingRate);
        
            for (int i=0; i<subbands.length; i++)
            {
                SinusoidalAnalyzer sa = null;
                if (bFreqLimitedAnalysis)
                {
                    sa = new SinusoidalAnalyzer(subbands[i].samplingRate, windowType, 
                                                bRefinePeakEstimatesParabola, 
                                                bRefinePeakEstimatesBias,
                                                bSpectralReassignment,
                                                bAdjustNeighFreqDependent,
                                                subbands[i].lowestFreqInHz, subbands[i].highestFreqInHz);
                }
                else
                {
                    sa = new SinusoidalAnalyzer(subbands[i].samplingRate, windowType, 
                                                bRefinePeakEstimatesParabola, 
                                                bRefinePeakEstimatesBias,
                                                bSpectralReassignment,
                                                bAdjustNeighFreqDependent,
                                                0.0, 0.5*subbands[i].samplingRate);
                }

                float winSizeInSeconds = (float)(lowestBandWindowSizeInSeconds/Math.pow(2.0, i));
                float skipSizeInSeconds = 0.5f*winSizeInSeconds;
                float deltaInHz = 50.0f; //Also make this frequency range dependent??

                //To do: peak detection procedure should be limited by the subband signal´s frequency range
                subbandTracks[i] = sa.analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC);
            
                //Normalize overlapping frequency region gains if an overlapping subband stucture is used
                if (multiresolutionFilterbankType==MultiresolutionSinusoidalAnalyzer.FIR_BANDPASS_FILTERBANK)
                    normalizeSinusoidalAmplitudes(subbandTracks[i], samplingRate, ((FIRBandPassFilterBankAnalyser)analyser).normalizationFilterTransformedIR);
            }
        }
        
        return subbandTracks;
    }
    
    //Normalizes sinusoidal amplitudes when an overlapping subband filterbank structure is used
    public void normalizeSinusoidalAmplitudes(SinusoidalTracks sinTracks, int samplingRate, double[] normalizationFilterTransformedIR)
    {
        int i, j, k;
        int maxFreq = normalizationFilterTransformedIR.length;
        for (i=0; i<sinTracks.tracks.length; i++)
        {
            for (j=0; j<sinTracks.tracks[i].totalSins; j++)
            { 
                k = SignalProcUtils.freq2index(SignalProcUtils.radian2Hz(sinTracks.tracks[i].freqs[j], sinTracks.fs), samplingRate, maxFreq);
                sinTracks.tracks[i].amps[j] *= normalizationFilterTransformedIR[k];
            }
        }
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
        int windowType = Window.HAMMING;
        boolean bRefinePeakEstimatesParabola = true;
        boolean bRefinePeakEstimatesBias = true;
        boolean bSpectralReassignment = true;
        boolean bAdjustNeighFreqDependent = true;
        boolean bFreqLimitedAnalysis = false;

        MultiresolutionSinusoidalAnalyzer msa = new MultiresolutionSinusoidalAnalyzer();

        SinusoidalTracks[] subbandTracks = msa.analyzeFixedRate(x, samplingRate, multiresolutionFilterbankType, numBands, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, bFreqLimitedAnalysis);
    }
}
