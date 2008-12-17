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

package marytts.signalproc.sinusoidal;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.filter.FIRWaveletFilterBankAnalyser;
import marytts.signalproc.filter.FIRWaveletFilterBankSynthesiser;
import marytts.signalproc.filter.FilterBankAnalyserBase;
import marytts.signalproc.filter.Subband;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * @author Oytun T&uumlrk
 *
 */
public class MultiresolutionSinusoidalSynthesizer {
    public MultiresolutionSinusoidalAnalyzer analyser;
    
    public MultiresolutionSinusoidalSynthesizer(MultiresolutionSinusoidalAnalyzer analyserIn)
    {
        analyser = analyserIn;
    }
    
    public double[] synthesize(SinusoidalTracks[] subbandTracks, boolean isSilentSynthesis)
    {
        double[] y = null;
        double[] tmpy = null;
        if (subbandTracks!=null)
        {
            int i, j;
            
            //Sinusoidal resynthesis
            if (analyser.multiresolutionFilterbankType == FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK)
            {
                Subband[] subbands = new Subband[subbandTracks.length];
                for (i=0; i<subbandTracks.length; i++)
                {
                    PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(subbandTracks[i].fs);
                    tmpy = ss.synthesize(subbandTracks[i], isSilentSynthesis);
                    subbands[i] = new Subband(tmpy, subbandTracks[i].fs);
                }
                
                FIRWaveletFilterBankSynthesiser filterbankSynthesiser = new FIRWaveletFilterBankSynthesiser();
                y = filterbankSynthesiser.apply((FIRWaveletFilterBankAnalyser)(analyser.filterbankAnalyser), subbands, false);
            }
            else
            {
                for (i=0; i<subbandTracks.length; i++)
                {
                    PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(subbandTracks[i].fs);
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
        }
        
        return y;
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();

        int multiresolutionFilterbankType;
        //multiresolutionFilterbankType = FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK;
        multiresolutionFilterbankType = FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK;
        //multiresolutionFilterbankType = FilterBankAnalyserBase.COMPLEMENTARY_FILTERBANK;
        int numBands = 2;
        double lowestBandWindowSizeInSeconds = 0.020;
        int windowType = Window.HANNING;
        boolean bRefinePeakEstimatesParabola = false;
        boolean bRefinePeakEstimatesBias = false;
        boolean bSpectralReassignment = false;
        boolean bAdjustNeighFreqDependent = false;
        boolean isSilentSynthesis = false;
        boolean bFreqLimitedAnalysis = false; //Only used for FIR_BANDPASS_FILTERBANK
        boolean bPitchSynchronous = false;
        float numPeriods = 2.5f;

        MultiresolutionSinusoidalAnalyzer msa = new MultiresolutionSinusoidalAnalyzer(multiresolutionFilterbankType, numBands, samplingRate);

        SinusoidalTracks[] subbandTracks = null;
        
        if (!bPitchSynchronous)
            subbandTracks = msa.analyze(x, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, bFreqLimitedAnalysis);
        else
        {
            String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
            F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
            int pitchMarkOffset = 0;
            PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length, f0.header.ws, f0.header.ss, true, pitchMarkOffset);
            PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(samplingRate, Window.HAMMING, true, true, true, true, 0.0, 0.5*samplingRate);
       
            subbandTracks = msa.analyze(x, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, bFreqLimitedAnalysis, true, pm, numPeriods);
        }
                
        //Resynthesis
        MultiresolutionSinusoidalSynthesizer mss = new MultiresolutionSinusoidalSynthesizer(msa);
        x = mss.synthesize(subbandTracks, isSilentSynthesis);
        //
        
        //This scaling is only for comparison among different parameter sets, different synthesizer outputs etc
        double maxx = MathUtils.getAbsMax(x);
        for (int i=0; i<x.length; i++)
            x[i] = x[i]/maxx*0.9;
        //
        
        //File output
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), inputAudio.getFormat());
        String outFileName = args[0].substring(0, args[0].length()-4) + "_multiResWaveletFixedRate.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        //
    }
}
