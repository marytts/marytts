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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.PitchMarker;
import marytts.signalproc.filter.ComplementaryFilterBankAnalyser;
import marytts.signalproc.filter.FIRBandPassFilterBankAnalyser;
import marytts.signalproc.filter.FIRWaveletFilterBankAnalyser;
import marytts.signalproc.filter.FilterBankAnalyserBase;
import marytts.signalproc.filter.Subband;
import marytts.signalproc.window.Window;
import marytts.util.data.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * @author oytun.turk
 *
 */
public class MultiresolutionSinusoidalAnalyzer extends BaseSinusoidalAnalyzer {
    public FilterBankAnalyserBase filterbankAnalyser;
    public int multiresolutionFilterbankType;
    public int numBands;
    public int samplingRate;
    
    public MultiresolutionSinusoidalAnalyzer(int multiresolutionFilterbankTypeIn, int numBandsIn, int samplingRateIn)
    {
        multiresolutionFilterbankType = multiresolutionFilterbankTypeIn;
        numBands = numBandsIn;
        samplingRate = samplingRateIn;
        
        filterbankAnalyser = null;
        
        if (multiresolutionFilterbankType==FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK)
        {
            double overlapAround1000Hz = 100.0;
                
            filterbankAnalyser = new FIRBandPassFilterBankAnalyser(numBands, samplingRate, overlapAround1000Hz);            
        }
        else if (multiresolutionFilterbankType==FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK)
        {
            double overlapAround1000Hz = 100.0;
                
            filterbankAnalyser = new FIRWaveletFilterBankAnalyser(numBands, samplingRate);            
        }
        else if (multiresolutionFilterbankType==FilterBankAnalyserBase.COMPLEMENTARY_FILTERBANK)
        {
            if (!MathUtils.isPowerOfTwo(numBands))
            {
                int tmpNumBands = 2;
                while (tmpNumBands<numBands)
                    tmpNumBands*=2;
                numBands = tmpNumBands;
                System.out.println("Number of bands should be a power of two for the complementary filterbank");
            }
                
            int baseFilterOrder = SignalProcUtils.getFIRFilterOrder(samplingRate);
            int numLevels = numBands-1;
            filterbankAnalyser = new ComplementaryFilterBankAnalyser(numLevels, baseFilterOrder);
        }
    }
    
    //Fixed rate version
    public SinusoidalTracks[] analyze(double[] x,
                                      double lowestBandWindowSizeInSeconds,
                                      int windowType,
                                      boolean bRefinePeakEstimatesParabola,
                                      boolean bRefinePeakEstimatesBias,
                                      boolean bSpectralReassignment,
                                      boolean bAdjustNeighFreqDependent,
                                      boolean bFreqLimitedAnalysis)
    {
        return analyze(x,
                       lowestBandWindowSizeInSeconds,
                       windowType,
                       bRefinePeakEstimatesParabola,
                       bRefinePeakEstimatesBias,
                       bSpectralReassignment,
                       bAdjustNeighFreqDependent,
                       bFreqLimitedAnalysis,
                       false, null, 0.0f);
    }
    
    //Fixed rate and pitch synchronous version.
    //Set bPitchSynchronousAnalysis=false to get fixed rate version. In this case pitchMarks can be anything (i.e. null) 
    // and numPeriods can be a dummy value since they are only used for ptich synchronous processing
    public SinusoidalTracks[] analyze(double[] x,
                                      double lowestBandWindowSizeInSeconds,
                                      int windowType,
                                      boolean bRefinePeakEstimatesParabola,
                                      boolean bRefinePeakEstimatesBias,
                                      boolean bSpectralReassignment,
                                      boolean bAdjustNeighFreqDependent,
                                      boolean bFreqLimitedAnalysis,
                                      boolean bPitchSynchronousAnalysis,
                                      int[] pitchMarks, //Only used when bPitchSynchronousAnalysis=true
                                      float numPeriods) //Only used when bPitchSynchronousAnalysis=true
    {
        SinusoidalTracks[] subbandTracks = new SinusoidalTracks[numBands];
        Subband[] subbands = null;
        
        //When there is downsampling, no need for frequency limited analysis
        if (multiresolutionFilterbankType!=FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK)
            bFreqLimitedAnalysis = false; 
        
        if (filterbankAnalyser!=null)
        {
            subbands = filterbankAnalyser.apply(x);
        
            for (int i=0; i<subbands.length; i++)
            {
                if (!bPitchSynchronousAnalysis || i>0) //Pitch synchrounous subband analysis is only performed at the lowest frequency subband
                {
                    SinusoidalAnalyzer sa = null;
                    if (bFreqLimitedAnalysis)
                    {
                        sa = new SinusoidalAnalyzer((int)(subbands[i].samplingRate), windowType, 
                                                    bRefinePeakEstimatesParabola, 
                                                    bRefinePeakEstimatesBias,
                                                    bSpectralReassignment,
                                                    bAdjustNeighFreqDependent,
                                                    subbands[i].lowestFreqInHz, subbands[i].highestFreqInHz);
                    }
                    else
                    {
                        sa = new SinusoidalAnalyzer((int)(subbands[i].samplingRate), windowType, 
                                                    bRefinePeakEstimatesParabola, 
                                                    bRefinePeakEstimatesBias,
                                                    bSpectralReassignment,
                                                    bAdjustNeighFreqDependent,
                                                    0.0, 0.5*subbands[i].samplingRate);
                    }

                    float winSizeInSeconds = (float)(lowestBandWindowSizeInSeconds/Math.pow(2.0,i));
                    float skipSizeInSeconds = 0.5f*winSizeInSeconds;
                    float deltaInHz = 50.0f; //Also make this frequency range dependent??

                    if (multiresolutionFilterbankType==FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK)
                        subbandTracks[i] =  sa.analyzeFixedRate(subbands[i].waveform, winSizeInSeconds, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC);
                    else
                        subbandTracks[i] =  sa.analyzeFixedRate(x, winSizeInSeconds, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC);

                    //Normalize overlapping frequency region gains if an overlapping subband stucture is used
                    if (multiresolutionFilterbankType==FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK)
                        normalizeSinusoidalAmplitudes(subbandTracks[i], samplingRate, ((FIRBandPassFilterBankAnalyser)filterbankAnalyser).normalizationFilterTransformedIR);
                }
                else
                {
                    PitchSynchronousSinusoidalAnalyzer sa = null;
                    if (bFreqLimitedAnalysis)
                    {
                        sa = new PitchSynchronousSinusoidalAnalyzer((int)(subbands[i].samplingRate), windowType, 
                                                                    bRefinePeakEstimatesParabola, 
                                                                    bRefinePeakEstimatesBias,
                                                                    bSpectralReassignment,
                                                                    bAdjustNeighFreqDependent,
                                                                    subbands[i].lowestFreqInHz, subbands[i].highestFreqInHz);
                    }
                    else
                    {
                        sa = new PitchSynchronousSinusoidalAnalyzer((int)(subbands[i].samplingRate), windowType, 
                                                                    bRefinePeakEstimatesParabola, 
                                                                    bRefinePeakEstimatesBias,
                                                                    bSpectralReassignment,
                                                                    bAdjustNeighFreqDependent,
                                                                    0.0, 0.5*subbands[i].samplingRate);
                    }

                    float winSizeInSeconds = (float)(lowestBandWindowSizeInSeconds/Math.pow(2.0,i)); //This is computed only for determining skip rate
                    float skipSizeInSeconds = 0.5f*winSizeInSeconds;
                    float deltaInHz = 50.0f; //Also make this frequency range dependent??
                    float numPeriodsCurrent = (float)(numPeriods/Math.pow(2.0,i)); //This iteratively halves the effective window size for higher frequency subbands

                    subbandTracks[i] = sa.analyzePitchSynchronous(x, pitchMarks, numPeriodsCurrent, skipSizeInSeconds, deltaInHz, SinusoidalAnalyzer.LP_SPEC);
                }
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

        int multiresolutionFilterbankType;
        //multiresolutionFilterbankType = FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK;
        multiresolutionFilterbankType = FilterBankAnalyserBase.FIR_WAVELET_FILTERBANK;
        //multiresolutionFilterbankType = FilterBankAnalyserBase.COMPLEMENTARY_FILTERBANK;
        
        int numBands = 4;
        double lowestBandWindowSizeInSeconds = 0.020;
        int windowType = Window.HAMMING;
        boolean bRefinePeakEstimatesParabola = true;
        boolean bRefinePeakEstimatesBias = true;
        boolean bSpectralReassignment = true;
        boolean bAdjustNeighFreqDependent = true;
        boolean bFreqLimitedAnalysis = false;
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
            PitchMarker pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, samplingRate, x.length, f0.header.ws, f0.header.ss, true);
            PitchSynchronousSinusoidalAnalyzer sa = new PitchSynchronousSinusoidalAnalyzer(samplingRate, Window.HAMMING, true, true, true, true, 0.0, 0.5*samplingRate);
       
            subbandTracks = msa.analyze(x, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, bFreqLimitedAnalysis, true, pm.pitchMarks, numPeriods);
        }   
    }
}
