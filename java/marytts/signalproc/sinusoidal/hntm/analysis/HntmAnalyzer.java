/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.signalproc.sinusoidal.hntm.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import marytts.machinelearning.ContextualGMMParams;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.analysis.CepstrumSpeechAnalyser;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.analysis.SeevocAnalyser;
import marytts.signalproc.analysis.SpectrumWithPeakIndices;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechSignal;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechFrame;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechSignal;
import marytts.signalproc.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.SinusoidalAnalysisParams;
import marytts.signalproc.sinusoidal.SinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.VoicingAnalysisOutputData;
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

/*
import marytts.util.math.jampack.H;
import marytts.util.math.jampack.Inv;
import marytts.util.math.jampack.JampackException;
import marytts.util.math.jampack.Parameters;
import marytts.util.math.jampack.Times;
import marytts.util.math.jampack.Z;
import marytts.util.math.jampack.Zmat;
 */

/**
 * This class implements a harmonic+noise model for speech as described in
 * Stylianou, Y., 1996, "Harmonic plus Noise Models for Speech, combined with Statistical Methods, 
 *                       for Speech and Speaker Modification", Ph.D. thesis, 
 *                       Ecole Nationale Supérieure des Télécommunications.
 * 
 * @author Oytun T&uumlrk
 *
 */
public class HntmAnalyzer {
    public static final int HARMONICS_PLUS_NOISE = 1;
    public static final int HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE = 2;

    public static final int LPC = 1; //Noise part model based on LPC
    public static final int REGULARIZED_CEPS = 2; //Noise part model based on regularized cepstral coefficients
    public static final int PSEUDO_HARMONIC = 3; //Noise part model based on pseude harmonics for f0=NOISE_F0_IN_HZ
    public static final int HIGHPASS_WAVEFORM = 4; //Noise part model based on frame waveform (i.e. no model, overlap-add noise part generation)

    public static final boolean USE_AMPLITUDES_DIRECTLY = true; //Use amplitudes directly (for testing only, 
        
    public static final double NOISE_F0_IN_HZ = 200.0; //Pseudo-pitch for unvoiced portions (will be used for pseudo harmonic modelling of the noise part)
    public static float FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST = 3500.0f;
    public static float FIXED_MAX_FREQ_OF_NOISE_FOR_QUICK_TEST = 8000.0f;
    public static float HPF_TRANSITION_BANDWIDTH_IN_HZ = 100.0f;
    public static float NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS = 0.050f; //Fixed window size for noise analysis, should be generally large (>=0.040 seconds)
    public static float OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ = 0.0f;
    public static float OVERLAP_BETWEEN_TRANSIENT_AND_NONTRANSIENT_REGIONS_IN_SECONDS = 0.005f;

    public static int TIME_DOMAIN_CORRELATION_HARMONICS_ANALYSIS = 1;
    public static int FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS = 2;

    public static int HARMONICS_ANALYSIS_WINDOW_TYPE = Window.HAMMING;
    //public static int HARMONICS_ANALYSIS_WINDOW_TYPE = Window.HANNING;
    //public static int HARMONICS_ANALYSIS_WINDOW_TYPE = Window.RECT;
    public static int NOISE_ANALYSIS_WINDOW_TYPE = Window.HAMMING;

    //Default search range for voicing detection, i.e. voicing criterion will be computed for frequency range:
    // [DEFAULT_VOICING_START_HARMONIC x f0, DEFAULT_VOICING_START_HARMONIC x f0] where f0 is the fundamental frequency estimate
    public static int NUM_HARMONICS_FOR_VOICING = 4;
    public static float HARMONICS_NEIGH = 0.3f; //Between 0.0 and 1.0: How much the search range for voicing detection will be extended beyond the first and the last harmonic
    //0.3 means the region [0.7xf0, 4.3xf0] will be considered in voicing decision
    //

    public static float NUM_PERIODS_INITIAL_PITCH_ESTIMATION = 3.0f;
    public static float NUM_PERIODS_HARMONICS_EXTRACTION = 2.0f;
    public static double FFT_PEAK_PICKER_PERIODS = 3.0;
    public static int FFT_SIZE_PEAK_MATCHER = 4096;
    public static int FFT_PAK_MATCHER_WINDOW_TYPE = Window.HAMMING;
    public static float MVF_ANALYSIS_WINDOW_SIZE_IN_SECONDS = 0.040f;
    public static float MVF_ANALYSIS_SKIP_SIZE_IN_SECONDS = 0.010f;

    public static float PREEMPHASIS_COEF_NOISE = 0.0f;
    public static boolean HIGHPASS_FILTER_PRIOR_TO_NOISE_ANALYSIS = false;

    ////The following parameters are used by HnmPitchVoicingAnalyzer
    //They are included here so that all fixed analysis parameters are in the same class within the code
    public static double CUMULATIVE_AMP_THRESHOLD = 2.0; //Decreased ==> Voicing increases (Orig: 2.0)
    public static double MAXIMUM_AMP_THRESHOLD_IN_DB = 13.0; //Decreased ==> Voicing increases (Orig: 13.0)
    public static double HARMONIC_DEVIATION_PERCENT = 20.0; //Increased ==> Voicing increases (Orig: 20.0)
    public static double SHARP_PEAK_AMP_DIFF_IN_DB = 12.0; //Decreased ==> Voicing increases
    public static int MINIMUM_TOTAL_HARMONICS = 10; //Minimum number of total harmonics to be included in voiced region (effective only when f0>10.0)
    public static int MAXIMUM_TOTAL_HARMONICS = 10; //Maximum number of total harmonics to be included in voiced region (effective only when f0>10.0)
    public static float MINIMUM_VOICED_FREQUENCY_OF_VOICING = 0.0f; //All voiced sections will have at least this freq. of voicing
    public static float MAXIMUM_VOICED_FREQUENCY_OF_VOICING = 3600.0f; //All voiced sections will have at least this freq. of voicing
    public static float MAXIMUM_FREQUENCY_OF_VOICING_FINAL_SHIFT = 0.0f; //The max freq. of voicing contour is shifted by this amount finally
    public static float RUNNING_MEAN_VOICING_THRESHOLD = 0.5f; //Between 0.0 and 1.0, decrease ==> Max. voicing freq increases

    public static boolean SEARCH_FOR_OPTIMAL_TOTAL_HARMONICS = false;
    public static int SEARCH_LOWER_N_HARMONICS = 10; //Additional lower harmonics to search for finding optimum number of harmonics
    public static int SEARCH_UPPER_N_HARMONICS = 10; //Additional upper harmonics to search for finding optimum number of harmonics

    public static final int LAST_CORRELATED_HARMONIC_NEIGHBOUR = -1; //Assume correlation between at most among this much harmonics (-1 ==> full correlation approach) 
    public static boolean INCLUDE_ZEROTH_HARMONIC = false;
    public static boolean NORMALIZE_HARMONIC_AMPLITUDES = false;
    
    public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_ANALYSIS = true;
    public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_TIME_SCALING = false;
    public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_PITCH_SCALING = false;
    
    public static int NUM_FILTERING_STAGES = 1;
    public static int MEDIAN_FILTER_LENGTH = 12; //12; //Length of median filter for smoothing the max. freq. of voicing contour
    public static int MOVING_AVERAGE_FILTER_LENGTH = 12; //12; //Length of first moving averaging filter for smoothing the max. freq. of voicing contour

    //For voicing detection
    public static double VUV_SEARCH_MIN_HARMONIC_MULTIPLIER = 0.7;
    public static double VUV_SEARCH_MAX_HARMONIC_MULTIPLIER = 4.3;
    //

    public static double NEIGHS_PERCENT = 50.0; //Should be between 0.0 and 100.0. 50.0 means the peak in the band should be greater than 50% of the half of the band samples
    ////

    public HntmAnalyzer()
    {

    }

    public HntmSpeechSignal analyze(double[] x, int fs, PitchReaderWriter f0, Labels labels, int fftSize,
            int model, int noisePartRepresentation,
            int harmonicPartAnalysisMethod)
    {
        HntmSpeechSignal hnmSignal = null;

        float originalDurationInSeconds = SignalProcUtils.sample2time(x.length, fs);
        int lpOrder = SignalProcUtils.getLPOrder(fs);

        //Only effective for FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS 
        SinusoidalAnalysisParams sinAnaParams = null;
        boolean bRefinePeakEstimatesParabola = false;
        boolean bRefinePeakEstimatesBias = false;
        boolean bSpectralReassignment = false;
        boolean bAdjustNeighFreqDependent = false;
        //

        int i, j, k;

        double[] xPreemphasized = null;
        if (PREEMPHASIS_COEF_NOISE>0.0)
            xPreemphasized = SignalProcUtils.applyPreemphasis(x, PREEMPHASIS_COEF_NOISE);
        else
            xPreemphasized = ArrayUtils.copy(x);

        //// TO DO
        //Step1. Initial pitch estimation: Current version just reads from a file
        if (f0!=null)
        {
            int pitchMarkOffset = 0;
            PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, x.length, f0.header.ws, f0.header.ss, true, pitchMarkOffset);

            float[] initialF0s = ArrayUtils.subarrayf(f0.contour, 0, f0.header.numfrm);
            //float[] initialF0s = HnmPitchVoicingAnalyzer.estimateInitialPitch(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, f0MinInHz, f0MaxInHz, windowType);
            //

            //Step2: Do for each frame (at 10 ms skip rate):
            //2.a. Voiced/Unvoiced decision

            //2.b. If voiced, maximum frequency of voicing estimation
            //     Otherwise, maximum frequency of voicing is set to 0.0
            float[] maxFrequencyOfVoicings = HnmPitchVoicingAnalyzer.analyzeVoicings(x, fs, MVF_ANALYSIS_WINDOW_SIZE_IN_SECONDS, MVF_ANALYSIS_SKIP_SIZE_IN_SECONDS, fftSize, initialF0s, (float)f0.header.ws, (float)f0.header.ss);
            float maxFreqOfVoicingInHz;
            //maxFreqOfVoicingInHz = HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST; //This should come from the above automatic analysis

            //2.c. Refined pitch estimation
            float[] f0s = ArrayUtils.subarrayf(f0.contour, 0, f0.header.numfrm);
            //float[] f0s = HnmPitchVoicingAnalyzer.estimateRefinedPitch(fftSize, fs, leftNeighInHz, rightNeighInHz, searchStepInHz, initialF0s, maxFrequencyOfVoicings);
            ////


            //Step3. Determine analysis time instants based on refined pitch values.
            //       (Pitch synchronous if voiced, 10 ms skip if unvoiced)
            double numPeriods = NUM_PERIODS_HARMONICS_EXTRACTION;
            if (harmonicPartAnalysisMethod==TIME_DOMAIN_CORRELATION_HARMONICS_ANALYSIS)
                numPeriods = NUM_PERIODS_HARMONICS_EXTRACTION;
            else if (harmonicPartAnalysisMethod==FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS)
                numPeriods = PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS;

            double f0InHz = f0s[0];
            double T0Double;
            double assumedF0ForUnvoicedInHz = 100.0;
            boolean isVoiced, isNoised;
            if (f0InHz>10.0)
                isVoiced=true;
            else
            {
                isVoiced=false;
                f0InHz=assumedF0ForUnvoicedInHz;
            }

            int ws, wsPeakMatcher;
            int wsNoise = SignalProcUtils.time2sample(NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, fs);
            if (wsNoise%2==0) //Always use an odd window size to have a zero-phase analysis window
                wsNoise++;  

            Window winNoise = Window.get(NOISE_ANALYSIS_WINDOW_TYPE, wsNoise);
            winNoise.normalizePeakValue(1.0f);
            double[] wgtSquaredNoise = winNoise.getCoeffs();
            for (j=0; j<wgtSquaredNoise.length; j++)
                wgtSquaredNoise[j] = wgtSquaredNoise[j]*wgtSquaredNoise[j];

            int fftSizeNoise = SignalProcUtils.getDFTSize(fs);

            int totalFrm = (int)Math.floor(pm.pitchMarks.length-numPeriods+0.5);
            if (totalFrm>pm.pitchMarks.length-1)
                totalFrm = pm.pitchMarks.length-1;

            //Extract frames and analyze them
            double[] frm = null; //Extracted pitch synchronously
            double[] frmPeakMatcher = null; //Extracted pitch synchronously but typically using larger number of preiods to enhance spectral resolution
            double[] frmNoise = new double[wsNoise]; //Extracted at fixed window size around analysis time instant since LP analysis requires longer windows (40 ms)
            double[] frmHarmonic = null;
            int noiseFrmStartInd;

            int pmInd = 0;

            boolean isOutputToTextFile = false;
            Window win, winPeakMatcher;
            int closestInd;

            String[] transientPhonemesList = {"p", "t", "k", "pf", "ts", "tS"};

            if (model == HntmAnalyzer.HARMONICS_PLUS_NOISE)
                hnmSignal = new HntmSpeechSignal(totalFrm, fs, originalDurationInSeconds, (float)f0.header.ws, (float)f0.header.ss, NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, PREEMPHASIS_COEF_NOISE);
            else if (model == HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE && labels!=null)
                hnmSignal = new HntmPlusTransientsSpeechSignal(totalFrm, fs, originalDurationInSeconds, (float)f0.header.ws, (float)f0.header.ss, NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, PREEMPHASIS_COEF_NOISE, labels.items.length);

            boolean isPrevVoiced = false;

            int numHarmonics = 0;
            int prevNumHarmonics = 0;
            ComplexNumber[] harmonics = null;
            ComplexNumber[] noiseHarmonics = null;

            double[] phases;
            double[] dPhases;
            double[] dPhasesPrev = null;
            int MValue;

            int cepsOrderHarmonic = 16;
            int cepsOrderNoiseReg = 24;
            int cepsOrderNoise = 16;
            int numNoiseHarmonics = (int)Math.floor((0.5*fs)/NOISE_F0_IN_HZ+0.5);
            double[] freqsInHzNoise = new double [numNoiseHarmonics];
            for (j=0; j<numNoiseHarmonics; j++)
                freqsInHzNoise[j] = NOISE_F0_IN_HZ*(j+1);

            double[][] M = null;
            double[][] MTransW = null;
            double[][] MTransWM = null; 
            double[][] lambdaR = null;
            double[][] inverted = null;
            if (noisePartRepresentation==PSEUDO_HARMONIC)
            {
                M = RegularizedCepstralEnvelopeEstimator.precomputeM(freqsInHzNoise, fs, cepsOrderNoise);
                MTransW = RegularizedCepstralEnvelopeEstimator.precomputeMTransW(M, null);
                MTransWM = RegularizedCepstralEnvelopeEstimator.precomputeMTransWM(MTransW, M); 
                lambdaR = RegularizedCepstralEnvelopeEstimator.precomputeLambdaR(RegularizedCepstralEnvelopeEstimator.DEFAULT_LAMBDA, cepsOrderNoise);
                inverted = RegularizedCepstralEnvelopeEstimator.precomputeInverted(MTransWM, lambdaR);
            }

            int maxVoicingIndex;
            int currentLabInd = 0;
            boolean isInTransientSegment = false;
            int transientSegmentInd = 0;
            float totalHarmonicEnergy, totalNoiseEnergy, noiseToTotalEnergyRatio;

            for (i=0; i<totalFrm; i++)
            {  
                f0InHz = pm.f0s[i];
                //T0 = pm.pitchMarks[i+1]-pm.pitchMarks[i];
                if (f0InHz>10.0)
                    T0Double = SignalProcUtils.time2sampleDouble(1.0/f0InHz, fs);
                else 
                    T0Double = SignalProcUtils.time2sampleDouble(1.0/assumedF0ForUnvoicedInHz, fs);

                ws = (int)Math.floor(numPeriods*T0Double + 0.5);
                if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                    ws++;            

                hnmSignal.frames[i].tAnalysisInSeconds = (float)((pm.pitchMarks[i]+0.5f*ws)/fs);  //Middle of analysis frame

                totalHarmonicEnergy = 0.0f;
                totalNoiseEnergy = 0.0f;

                if (model == HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE && labels!=null)
                {
                    while(labels.items[currentLabInd].time<hnmSignal.frames[i].tAnalysisInSeconds)
                    {
                        currentLabInd++;
                        if (currentLabInd>labels.items.length-1)
                        {
                            currentLabInd = labels.items.length-1;
                            break;
                        }
                    }

                    if (!isInTransientSegment) //Perhaps start of a new transient segment
                    {
                        for (j=0; j<transientPhonemesList.length; j++)
                        {
                            if (labels.items[currentLabInd].phn.compareTo(transientPhonemesList[j])==0)
                            {
                                isInTransientSegment = true;
                                ((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[transientSegmentInd] = new TransientSegment();
                                ((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[transientSegmentInd].startTime = Math.max(0.0f, (((float)pm.pitchMarks[i])/fs)-HntmAnalyzer.OVERLAP_BETWEEN_TRANSIENT_AND_NONTRANSIENT_REGIONS_IN_SECONDS);
                                break;
                            }
                        }
                    }
                    else //Perhaps end of an existing transient segment
                    {
                        boolean isTransientPhoneme = false;
                        for (j=0; j<transientPhonemesList.length; j++)
                        {
                            if (labels.items[currentLabInd].phn.compareTo(transientPhonemesList[j])==0)
                            {
                                isTransientPhoneme = true;
                                break;
                            }
                        }

                        if (!isTransientPhoneme) //End of transient segment, put it in transient part
                        {
                            float endTime = Math.min((((float)pm.pitchMarks[i]+0.5f*ws)/fs)+HntmAnalyzer.OVERLAP_BETWEEN_TRANSIENT_AND_NONTRANSIENT_REGIONS_IN_SECONDS, hnmSignal.originalDurationInSeconds);
                            int waveformStartInd = Math.max(0, SignalProcUtils.time2sample(((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[transientSegmentInd].startTime, fs));
                            int waveformEndInd = Math.min(x.length-1, SignalProcUtils.time2sample(endTime, fs));
                            if (waveformEndInd-waveformStartInd+1>0)
                            {
                                ((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[transientSegmentInd].waveform = new int[waveformEndInd-waveformStartInd+1];
                                for (j=waveformStartInd; j<=waveformEndInd; j++)
                                    ((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[transientSegmentInd].waveform[j-waveformStartInd] = (int)x[j];
                            }

                            transientSegmentInd++;
                            isInTransientSegment = false;
                        }
                    }
                }

                maxVoicingIndex = SignalProcUtils.time2frameIndex(hnmSignal.frames[i].tAnalysisInSeconds, MVF_ANALYSIS_WINDOW_SIZE_IN_SECONDS, MVF_ANALYSIS_SKIP_SIZE_IN_SECONDS);
                maxVoicingIndex = Math.min(maxVoicingIndex, maxFrequencyOfVoicings.length-1);
                maxFreqOfVoicingInHz = maxFrequencyOfVoicings[maxVoicingIndex];
                //if (hnmSignal.frames[i].tAnalysisInSeconds<0.7 && f0InHz>10.0)
                if (f0InHz>10.0)
                    hnmSignal.frames[i].maximumFrequencyOfVoicingInHz = maxFreqOfVoicingInHz;
                else
                    hnmSignal.frames[i].maximumFrequencyOfVoicingInHz = 0.0f;

                numHarmonics = (int)Math.floor(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz/f0InHz+0.5);
                isVoiced = numHarmonics>0 ? true:false;
                isNoised = hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5*fs ? true:false;

                if (isInTransientSegment)
                {
                    hnmSignal.frames[i].h = null;
                    hnmSignal.frames[i].n = null;
                }
                else
                {
                    if (!isVoiced)
                        f0InHz = assumedF0ForUnvoicedInHz;

                    T0Double = SignalProcUtils.time2sampleDouble(1.0/f0InHz, fs);

                    ws = (int)Math.floor(numPeriods*T0Double+ 0.5);
                    if (ws%2==0) //Always use an odd window size to have a zero-phase analysis window
                        ws++;

                    frm = new double[ws];
                    int frmStartIndex = SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds-0.5*numPeriods/f0InHz, fs);
                    int frmEndIndex = frmStartIndex+ws-1;
                    int count = 0;
                    for (j=frmStartIndex; j<0; j++)
                        frm[count++] = 0.0;
                    for (j=Math.max(0, frmStartIndex); j<Math.min(frmEndIndex, x.length-1); j++)
                        frm[count++] = x[j];
                    for (j=x.length; j<=frmEndIndex; j++)
                        frm[count++] = 0.0;

                    /*
                    for (j=pm.pitchMarks[i]; j<Math.min(pm.pitchMarks[i]+ws-1, x.length); j++)
                        frm[j-pm.pitchMarks[i]] = x[j];
                     */

                    //FFT peak macther
                    wsPeakMatcher = (int)Math.floor(FFT_PEAK_PICKER_PERIODS*T0Double+ 0.5);
                    if (wsPeakMatcher%2==0) //Always use an odd window size to have a zero-phase analysis window
                        wsPeakMatcher++;

                    frmPeakMatcher = new double[wsPeakMatcher];
                    Arrays.fill(frmPeakMatcher, 0.0);
                    int fftSizePeakMatcher, maxFreqIndPeakMatcher;

                    int frmPeakMatcherStartIndex = Math.max(0, SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds-0.5*FFT_PEAK_PICKER_PERIODS/f0InHz, fs));
                    int frmPeakMatcherEndIndex = frmPeakMatcherStartIndex+wsPeakMatcher-1;
                    for (j=frmPeakMatcherStartIndex; j<Math.min(frmPeakMatcherEndIndex, x.length); j++)
                        frmPeakMatcher[j-frmPeakMatcherStartIndex] = x[j];
                    //

                    win = Window.get(HARMONICS_ANALYSIS_WINDOW_TYPE, ws);
                    win.normalizePeakValue(1.0f);
                    double[] wgt = win.getCoeffs();
                    //double[] wgtSquared = new double[wgt.length];
                    //for (j=0; j<wgt.length; j++)
                    //    wgtSquared[j] = wgt[j]*wgt[j];

                    winPeakMatcher = Window.get(FFT_PAK_MATCHER_WINDOW_TYPE, wsPeakMatcher);
                    double[] wgtPeakMatcher = winPeakMatcher.getCoeffs();
                    
                    //Step4. Estimate complex amplitudes of harmonics if voiced
                    //       The phase of the complex amplitude is the phase and its magnitude is the absolute amplitude of the harmonic
                    if (isVoiced)
                    {
                        if (harmonicPartAnalysisMethod==TIME_DOMAIN_CORRELATION_HARMONICS_ANALYSIS)
                        {
                            //Method 1: Time-domain full cross-correlation, i.e. harmonics are correlated  
                            fftSizePeakMatcher = FFT_SIZE_PEAK_MATCHER;
                            while (fftSizePeakMatcher<frm.length)
                                fftSizePeakMatcher*=2;
                            maxFreqIndPeakMatcher = fftSizePeakMatcher/2;
                            
                            double origEn = SignalProcUtils.energy(frmPeakMatcher);
                            double[] frmPeakMatcherDft = SignalProcUtils.getFrameMagnitudeSpectrum(frmPeakMatcher, fftSizePeakMatcher, wgtPeakMatcher);
                            double windowedEn = SignalProcUtils.energy(MathUtils.multiply(frmPeakMatcher, wgtPeakMatcher));
                            frmPeakMatcherDft = MathUtils.multiply(frmPeakMatcherDft, Math.sqrt(origEn)/(1e-20+Math.sqrt(windowedEn)));
                            harmonics = estimateComplexAmplitudes(frm, frmPeakMatcherDft, wgt, f0InHz, numHarmonics, fs, hnmSignal.frames[i].tAnalysisInSeconds);
                            //

                            /*
                            double[] harmonicAmps = SignalProcUtils.getPeakAmplitudes(frmPeakMatcherDft, f0InHz, numHarmonics, fftSizePeakMatcher, fs, INCLUDE_ZEROTH_HARMONIC);
                            totalHarmonicEnergy = (float)MathUtils.sumSquared(harmonicAmps);
                            */
                            
                            int noiseStartHarmonicIndex = (int)Math.floor((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ)/f0InHz+0.5);
                            int noiseStartFreqIndex = SignalProcUtils.freq2index(noiseStartHarmonicIndex*f0InHz, fs, maxFreqIndPeakMatcher);
                            totalHarmonicEnergy = (float)MathUtils.sumSquared(frmPeakMatcherDft, 0, noiseStartFreqIndex-1);
                            totalNoiseEnergy = (float)MathUtils.sumSquared(frmPeakMatcherDft, noiseStartFreqIndex, maxFreqIndPeakMatcher);

                            /*
                            //Method 2: Jampack versions for matrix operations
                            try {
                                harmonicAmps = estimateComplexAmplitudesJampack(frm, wgt, f0InHz, numHarmonics, fs, hnmSignal.frames[i].tAnalysisInSeconds);
                                //harmonicAmps = estimateComplexAmplitudesJampack2(frm, wgt, f0InHz, numHarmonics, fs, hnmSignal.frames[i].tAnalysisInSeconds);
                            } catch (JampackException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            //
                             */

                            //Method 3: Assumes uncorrelated harmonics
                            //harmonicAmps = estimateComplexAmplitudesUncorrelated(frm, wgtSquared, numHarmonics, f0InHz, fs);

                            numHarmonics = harmonics.length;
                        }
                        else if (harmonicPartAnalysisMethod==FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS)
                        {
                            sinAnaParams = new SinusoidalAnalysisParams(fs, 0.0, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, HARMONICS_ANALYSIS_WINDOW_TYPE,
                                    bRefinePeakEstimatesParabola, 
                                    bRefinePeakEstimatesBias,
                                    bSpectralReassignment,
                                    bAdjustNeighFreqDependent);
                            
                            sinAnaParams.fftSize = fftSize;

                            double[] windowedFrm = win.apply(frm, 0);
                            
                            harmonics = estimateComplexAmplitudesPeakPicking(windowedFrm, SinusoidalAnalysisParams.NO_SPEC, isVoiced, (float)f0InHz,
                                                                             hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, false, sinAnaParams);

                            numHarmonics = harmonics.length;
                        }

                        //Only for visualization
                        //double[] absMags = MathUtils.magnitudeComplex(harmonicAmps);
                        //double[] dbMags = MathUtils.amp2db(absMags);
                        //MaryUtils.plot(dbMags);
                        //

                        hnmSignal.frames[i].f0InHz = (float)f0InHz;
                        hnmSignal.frames[i].h = new FrameHarmonicPart();
                    }
                    else
                    {
                        hnmSignal.frames[i].f0InHz = 0.0f;
                        numHarmonics = 0;
                    }

                    //Step5. Perform full-spectrum LPC analysis for generating noise part
                    Arrays.fill(frmNoise, 0.0);
                    noiseFrmStartInd = Math.max(0, SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds-0.5f*NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, fs));

                    for (j=noiseFrmStartInd; j<Math.min(noiseFrmStartInd+wsNoise, x.length); j++)
                        frmNoise[j-noiseFrmStartInd] = x[j];

                    hnmSignal.frames[i].origAverageSampleEnergy = (float)SignalProcUtils.getAverageSampleEnergy(frmNoise);

                    for (j=noiseFrmStartInd; j<Math.min(noiseFrmStartInd+wsNoise, x.length); j++)
                        frmNoise[j-noiseFrmStartInd] = xPreemphasized[j];

                    //hnmSignal.frames[i].harmonicTotalEnergyRatio = 1.0f;

                    if (isNoised)
                    {
                        double[] y = null;

                        if (noisePartRepresentation==LPC)
                        {                            
                            //SignalProcUtils.displayDFTSpectrumInDBNoWindowing(frmNoise, fftSizeNoise);  

                            //We have support for preemphasis - this needs to be handled during synthesis of the noisy part with preemphasis removal
                            //frmNoise = winNoise.apply(frmNoise, 0);

                            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ>0.0f)
                                y = SignalProcUtils.fdFilter(frmNoise, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ, 0.5f*fs, fs, fftSizeNoise);

                            if (y!=null)
                            {
                                if (HIGHPASS_FILTER_PRIOR_TO_NOISE_ANALYSIS)
                                    frmNoise = ArrayUtils.copy(y); //Use fdfo only for computing energy ratio between noise and speech (if we get this working, we can remove filtering from above and include only gain ratio computation)
                            }
                            else
                            {
                                y = ArrayUtils.copy(frmNoise);
                                totalNoiseEnergy = 1.0f;
                                totalHarmonicEnergy = 0.0f;
                            }

                            //SignalProcUtils.displayDFTSpectrumInDBNoWindowing(frmNoise, fftSizeNoise);

                            frmNoise = winNoise.apply(frmNoise, 0);

                            LpCoeffs lpcs = LpcAnalyser.calcLPC(frmNoise, lpOrder, 0.0f);
                            hnmSignal.frames[i].n = new FrameNoisePartLpc(lpcs.getA()); //Lp coefficients, AR filter required for synthesis!
                            //hnmSignal.frames[i].n = new FrameNoisePartLpc(lpcs.getLPRefc()); //Reflection coefficients (Lattice filter required for synthesis!

                            //Only for display purposes...
                            //SignalProcUtils.displayLPSpectrumInDB(((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs, ((FrameNoisePartLpc)hnmSignal.frames[i].n).gain, fftSizeNoise);
                        }
                        else if (noisePartRepresentation==REGULARIZED_CEPS)
                        {
                            frmNoise = winNoise.apply(frmNoise, 0);

                            ComplexArray noiseDft = SignalProcUtils.getFrameDft(frmNoise, fftSizeNoise);
                            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ>0.0f)
                                y = SignalProcUtils.fdFilter(noiseDft, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ, 0.5f*fs, fs, frmNoise.length);

                            if (y!=null)
                            {
                                if (HIGHPASS_FILTER_PRIOR_TO_NOISE_ANALYSIS)
                                    frmNoise = ArrayUtils.copy(y); //Use fdfo only for computing energy ratio between noise and speech (if we get this working, we can remove filtering from above and include only gain ratio computation)
                            }
                            else
                            {
                                y = ArrayUtils.copy(frmNoise);
                                totalNoiseEnergy = 1.0f;
                                totalHarmonicEnergy = 0.0f;
                            }

                            int numNoiseHarmonicsReg = (int)Math.floor(0.5*fs/NOISE_F0_IN_HZ+0.5);
                            int startHarmonicNo = (int)Math.floor((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ)/NOISE_F0_IN_HZ+0.5);
                            double[] freqsInHzNoiseReg = new double [numNoiseHarmonicsReg];
                            double[] linearAmpsNoiseReg = new double[numNoiseHarmonicsReg];
                            int ind;
                            int maxFreqIndex = noiseDft.real.length/2;
                            double[] magnitudes = MathUtils.magnitudeComplex(noiseDft);
                            int startInd, endInd;
                            for (j=0; j<startHarmonicNo; j++)
                            {
                                freqsInHzNoiseReg[j] = NOISE_F0_IN_HZ*(j+1);
                                startInd = SignalProcUtils.freq2index(freqsInHzNoiseReg[j]-0.5*NOISE_F0_IN_HZ, fs, maxFreqIndex);
                                endInd = SignalProcUtils.freq2index(freqsInHzNoiseReg[j]+0.5*NOISE_F0_IN_HZ, fs, maxFreqIndex);
                                ind = MathUtils.getMinIndex(magnitudes, startInd, endInd);
                                linearAmpsNoiseReg[j] = magnitudes[ind];
                            }
                            for (j=startHarmonicNo; j<numNoiseHarmonicsReg; j++)
                            {
                                freqsInHzNoiseReg[j] = NOISE_F0_IN_HZ*(j+1);
                                ind = SignalProcUtils.freq2index(freqsInHzNoiseReg[j], fs, maxFreqIndex);
                                linearAmpsNoiseReg[j] = magnitudes[ind];
                            }

                            //MaryUtils.plot(MathUtils.amp2db(linearAmpsNoiseReg));

                            hnmSignal.frames[i].n = new FrameNoisePartRegularizedCeps();
                            ((FrameNoisePartRegularizedCeps)hnmSignal.frames[i].n).ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmpsNoiseReg, freqsInHzNoiseReg, fs, cepsOrderNoiseReg);
                        }
                        else if (noisePartRepresentation==PSEUDO_HARMONIC)
                        {
                            //Note that for noise we use the uncorrelated version of the complex amplitude estimator
                            //Correlated version resulted in ill-conditioning
                            //Also, analysis was pretty slow since the number of harmonics is large for pseudo-harmonics of noise, 
                            //i.e. for 16 KHz 5 to 8 KHz bandwidth in steps of 100 Hz produces 50 to 80 pseudo-harmonics

                            //(1) Uncorrelated approach as in Stylianou´s thesis
                            noiseHarmonics= estimateComplexAmplitudesUncorrelated(frmNoise, wgtSquaredNoise, numNoiseHarmonics, NOISE_F0_IN_HZ, fs);
                            //OR... (2)Expensive approach which does not work very well
                            //noiseHarmonicAmps = estimateComplexAmplitudes(frm, wgt, numNoiseHarmonics, NOISE_F0_IN_HZ, fs);
                            //OR... (3) Uncorrelated approach using full autocorrelation matrix (checking if there is a problem in estimateComplexAmplitudesUncorrelated
                            //noiseHarmonicAmps = estimateComplexAmplitudesUncorrelated2(frm, wgtSquared, numNoiseHarmonics, NOISE_F0_IN_HZ, fs);

                            double[] linearAmpsNoise = new double[numNoiseHarmonics];
                            for (j=0; j<numNoiseHarmonics; j++)
                                linearAmpsNoise[j] = MathUtils.magnitudeComplex(noiseHarmonics[j]);

                            double[] vocalTractDB = MathUtils.amp2db(linearAmpsNoise);
                            //MaryUtils.plot(vocalTractDB);

                            hnmSignal.frames[i].n = new FrameNoisePartPseudoHarmonic();
                            //(1) This is how amplitudes are represented in Stylianou´s thesis
                            ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmpsNoise, MTransW, inverted);
                            //OR... (2) The following is the expensive approach in which all matrices are computed again and again
                            //((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmpsNoise, freqsInHzNoise, fs, cepsOrderNoise);
                            //OR... (3) Let´s try to copy linearAmps as they are with no cepstral processing to see if synthesis works OK:
                            //((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps = new double[numNoiseHarmonics];
                            //System.arraycopy(linearAmpsNoise, 0, ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, 0, numNoiseHarmonics);


                            /*
                            //The following is only for visualization
                            //int fftSize = 4096;
                            //double[] vocalTractDB = RegularizedCepstralEnvelopeEstimator.cepstrum2logAmpHalfSpectrum(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, fftSize, fs);
                            double[] vocalTractDB = new double[numNoiseHarmonics];
                            for (j=0; j<numNoiseHarmonics; j++)
                                vocalTractDB[j] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, (j+1)*HnmAnalyzer.NOISE_F0_IN_HZ, fs);
                            vocalTractDB = MathUtils.amp2db(vocalTractDB);
                            MaryUtils.plot(vocalTractDB);
                            //
                             */    
                        }
                        else if (noisePartRepresentation==HIGHPASS_WAVEFORM)
                        {  
                            //SignalProcUtils.displayDFTSpectrumInDBNoWindowing(frmNoise, fftSizeNoise);  

                            //We have support for preemphasis - this needs to be handled during synthesis of the noisy part with preemphasis removal
                            //frmNoise = winNoise.apply(frmNoise, 0);


                            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ>0.0f)
                                y = SignalProcUtils.fdFilter(frmNoise, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz-OVERLAP_BETWEEN_HARMONIC_AND_NOISE_REGIONS_IN_HZ, 0.5f*fs, fs, fftSizeNoise);
                            else
                            {
                                y = ArrayUtils.copy(frmNoise);
                                totalNoiseEnergy = 1.0f;
                                totalHarmonicEnergy = 0.0f;
                            }

                            //SignalProcUtils.displayDFTSpectrumInDBNoWindowing(frmNoise, fftSizeNoise);

                            hnmSignal.frames[i].n = new FrameNoisePartWaveform(y); //Lp coefficients, AR filter required for synthesis!
                            //hnmSignal.frames[i].n = new FrameNoisePartLpc(lpcs.getLPRefc()); //Reflection coefficients (Lattice filter required for synthesis!
                            //hnmSignal.frames[i].noiseTotalEnergyRatio = fdfo.passBandToTotalEnergyRatio;
                        }
                    }
                    else
                        hnmSignal.frames[i].n = null;
                    //

                    //Step6. Estimate amplitude envelopes
                    if (numHarmonics>0)
                    {
                        if (isVoiced)
                        {
                            /*
                            //estimateComplexAmplitudesJampack2 versions
                            double[] linearAmps = new double[numHarmonics];
                            double[] freqsInHz = new double [numHarmonics];
                            for (j=1; j<numHarmonics; j++)
                            {
                                linearAmps[j-1] = MathUtils.magnitudeComplex(harmonicAmps[j]);
                                freqsInHz[j-1] = f0InHz*j;
                            }
                            //
                             */

                            //estimateComplexAmplitudes and estimateComplexAmplitudesJampack versions
                            double[] linearAmps = new double[numHarmonics];
                            double[] freqsInHz = new double [numHarmonics];
                            if (INCLUDE_ZEROTH_HARMONIC)
                            {
                                for (j=0; j<numHarmonics; j++)
                                {
                                    linearAmps[j] = MathUtils.magnitudeComplex(harmonics[j]);
                                    freqsInHz[j] = j*f0InHz;
                                }
                            }
                            else
                            {
                                for (j=0; j<numHarmonics; j++)
                                {
                                    linearAmps[j] = MathUtils.magnitudeComplex(harmonics[j]);
                                    freqsInHz[j] = (j+1)*f0InHz;
                                }
                            }
                            //

                            if (!HntmAnalyzer.USE_AMPLITUDES_DIRECTLY)
                                hnmSignal.frames[i].h.ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmps, freqsInHz, fs, cepsOrderHarmonic);
                            else
                                hnmSignal.frames[i].h.ceps = ArrayUtils.subarray(linearAmps, 0, linearAmps.length); //Use amplitudes directly

                            /*
                            //The following is only for visualization
                            double[] frameDftDB = MathUtils.amp2db(SignalProcUtils.getFrameMagnitudeSpectrum(frm, fftSize));
                            MaryUtils.plot(frameDftDB, 0, fftSize/2);
                            double[] vocalTractDB = RegularizedCepstralEnvelopeEstimator.cepstrum2dbSpectrumValues(hnmSignal.frames[i].h.ceps , fftSize, fs);
                            MaryUtils.plot(vocalTractDB);
                            //
                             */
                        }
                        //

                        /*
                        //estimateComplexAmplitudesJampack2 version
                        hnmSignal.frames[i].h.phases = new float[numHarmonics];
                        for (k=1; k<=numHarmonics; k++)
                            hnmSignal.frames[i].h.phases[k-1] = (float)MathUtils.phaseInRadians(harmonicAmps[k]);
                        //
                         */

                        //estimateComplexAmplitudes and estimateComplexAmplitudesJampack versions
                        hnmSignal.frames[i].h.phases = new float[numHarmonics];
                        for (k=0; k<numHarmonics; k++)
                            hnmSignal.frames[i].h.phases[k] = (float)MathUtils.phaseInRadians(harmonics[k]); 
                        //
                    }
                }

                if (isVoiced && !isInTransientSegment)
                    isPrevVoiced = true;
                else
                {
                    prevNumHarmonics = 0;
                    isPrevVoiced = false;
                }

                System.out.println("Analysis complete at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(totalFrm)); 
            }
        }

        if (hnmSignal instanceof HntmPlusTransientsSpeechSignal)
        {
            int numTransientSegments = 0;
            for (i=0; i<((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments.length; i++)
            {
                if (((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[i]!=null)
                    numTransientSegments++;
            }

            if (numTransientSegments>0)
            {
                TransientPart tempPart = new TransientPart(numTransientSegments);
                int count = 0;
                for (i=0; i<((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments.length; i++)
                {
                    if (((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[i]!=null)
                    {
                        tempPart.segments[count++] = new TransientSegment(((HntmPlusTransientsSpeechSignal)hnmSignal).transients.segments[i]);
                        if (count>=numTransientSegments)
                            break;
                    }
                }

                ((HntmPlusTransientsSpeechSignal)hnmSignal).transients = new TransientPart(tempPart);
            }
            else
                ((HntmPlusTransientsSpeechSignal)hnmSignal).transients = null;
        }

        //Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
        if (UNWRAP_PHASES_ALONG_HARMONICS_AFTER_ANALYSIS)
            unwrapPhasesAlongHarmonics(hnmSignal);
        //

        return hnmSignal;
    }

    //Complex amplitude estimation for harmonics in time domain 
    //(Full correlation matrix approach, no independence between harmonics assumed)
    //  This function implements Equation 3.25 in Stylianou`s PhD thesis
    //  The main advantage is the operation being in time domain.
    //  Therefore, we can use window sizes as short as two pitch periods and track rapid changes in amplitudes and phases
    //  frm: speech frame to be analysed (its length should be 2*N+1)
    //  wgtSquared: window weights squared
    //  f0InHz: f0 value for the current frame in Hz
    //  L: number of harmonics
    //  samplingRateInHz: sampling rate in Hz
    /*
    public ComplexNumber[] estimateComplexAmplitudes(double[] frm, double[] wgtSquared, double f0InHz, int L, double samplingRateInHz)
    {
        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;

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
                for (t=-N; t<=N; t++)
                {
                    omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(k-i);
                    tmp = new ComplexNumber(wgtSquared[t+N]*Math.cos(omega), wgtSquared[t+N]*Math.sin(omega));
                    R[i-1][k-1] = MathUtils.addComplex(R[i-1][k-1], tmp);
                }
            }
        }   

        for (k=1; k<=2*L+1; k++)
        {
            b[k-1] = new ComplexNumber(0.0, 0.0);
            for (t=-N; t<=N; t++)
            {
                omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(L+1-k);
                tmp = new ComplexNumber(wgtSquared[t+N]*frm[t+N]*Math.cos(omega), wgtSquared[t+N]*frm[t+N]*Math.sin(omega));
                b[k-1] = MathUtils.addComplex(b[k-1], tmp);
            }
        }

        ComplexNumber[][] invR = MathUtils.inverse(R);


        //Check matrix inversion operation
        //ComplexNumber[][] RinvR = MathUtils.matrixProduct(R, invR);
        //for (i=0; i<RinvR.length; i++)
        //{
        //    for (k=0; k<RinvR[i].length; k++)
        //    {
        //        if (i!=k && MathUtils.magnitudeComplex(RinvR[i][k])>1e-10)
        //            System.out.println("Check here! Non-zero non-diagonal element detected!");
        //        if (i==k && Math.abs(MathUtils.magnitudeComplex(RinvR[i][k])-1.0)>1e-10)
        //            System.out.println("Check here! Non-unity diagonal element detected!");
        //    }
        //}
        //

        //
        ComplexNumber[] x = MathUtils.matrixProduct(invR, b);
        ComplexNumber[] xpart = new ComplexNumber[L+1];
        for (k=L+1; k<2*L+1; k++) //The remaning complex amplitudes from L+1 to 2L are complex conjugates of entries from L-1,...,1
            xpart[k-(L+1)] = new ComplexNumber(x[k]);

        return xpart;
    }
     */

    public ComplexNumber[] estimateComplexAmplitudes(double[] s, double[] sDft, double[] wgt, double f0InHz, int LIn, double samplingRateInHz, float tAnalysis)
    {
        int t, i, k;
        int fftSize = sDft.length;
        int maxFreqIndex = (int)Math.floor(0.5*fftSize+0.5);

        //LIn = (int)(Math.floor(0.5*samplingRateInHz/f0InHz+0.5));

        int L;
        ComplexNumber[] xpart = null;
        ComplexNumber[] xpartOptimal = null;
        int numHarmonicsMin;
        int numHarmonicsMax;
        if (SEARCH_FOR_OPTIMAL_TOTAL_HARMONICS)
        {
            numHarmonicsMin = Math.max(MINIMUM_TOTAL_HARMONICS, LIn-SEARCH_LOWER_N_HARMONICS);
            numHarmonicsMax = Math.min(LIn+SEARCH_UPPER_N_HARMONICS, (int)Math.floor(0.5*samplingRateInHz/f0InHz+0.5));
        }
        else
        {
            numHarmonicsMin = LIn;
            numHarmonicsMax = LIn;
        }

        int[] freqInds= new int[numHarmonicsMax-numHarmonicsMin+1+1];
        for (L=numHarmonicsMin; L<=numHarmonicsMax+1; L++)
            freqInds[L-numHarmonicsMin] = SignalProcUtils.freq2index(L*f0InHz, (int)samplingRateInHz, maxFreqIndex);

        //L = LIn;
        double minDist = Double.MAX_VALUE;
        double currentDist;

        double harmonicSample;
        double noiseSample;
        
        int M = s.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;

        int LOptimal = 0;
        for (L=numHarmonicsMin; L<=numHarmonicsMax; L++)
        {
            ComplexNumber[][] B = new ComplexNumber[2*N+1][2*L+1];
            ComplexNumber tmp;

            double omega;

            ComplexNumber[][] W = MathUtils.diagonalComplexMatrix(wgt);
            /*
            for (t=-N; t<=N; t++)
            {
                for (k=-L; k<=L; k++)
                {
                    //omega = MathUtils.TWOPI*k*f0InHz*(tAnalysis+t/samplingRateInHz);
                    omega = MathUtils.TWOPI*k*f0InHz*(t/samplingRateInHz);
                    //omega = MathUtils.TWOPI*k*f0InHz*(tAnalysis*samplingRateInHz+t);
                    B[t+N][k+L] = new ComplexNumber(Math.cos(omega), Math.sin(omega));
                }
            }
            */
            
            for (k=-L; k<=L; k++)
            {
                for (t=-N; t<=N; t++)
                {
                    omega = MathUtils.TWOPI*k*f0InHz*(t/samplingRateInHz);
                    B[t+N][k+L] = new ComplexNumber(Math.cos(omega), Math.sin(omega)); 
                }
            }

            ComplexNumber[][] BTWTW = MathUtils.matrixProduct(MathUtils.hermitianTranspoze(B), MathUtils.transpoze(W));
            BTWTW = MathUtils.matrixProduct(BTWTW, W);

            ComplexNumber[] b = MathUtils.matrixProduct(BTWTW, s); //MathUtils.multiply(s,2.0));

            ComplexNumber[][] R = MathUtils.matrixProduct(BTWTW, B);
            
            //Set some R entries equal to zero to neglect interaction between far harmonics
            if (LAST_CORRELATED_HARMONIC_NEIGHBOUR>-1 && LAST_CORRELATED_HARMONIC_NEIGHBOUR<L)
            {
                for (i=0; i<2*L+1; i++)
                {
                    for (k=0; k<2*L+1; k++)
                    {
                        if (i>k+LAST_CORRELATED_HARMONIC_NEIGHBOUR || k>i+LAST_CORRELATED_HARMONIC_NEIGHBOUR)
                            R[i][k] = new ComplexNumber(0.0, 0.0);
                    }
                }
            }
            //

            /*
            ComplexNumber[][] R = new ComplexNumber[2*L+1][2*L+1];
            for (i=1; i<=2*L+1; i++)
            {
                for (k=1; k<=2*L+1; k++)
                {
                    R[i-1][k-1] = new ComplexNumber(0.0, 0.0);
                    for (t=-N; t<=N; t++)
                    {
                        //omega = MathUtils.TWOPI*f0InHz*(tAnalysis+t/samplingRateInHz)*(i-k);
                        omega = MathUtils.TWOPI*f0InHz*(t/samplingRateInHz)*(i-k);
                        tmp = new ComplexNumber(wgt[t+N]*wgt[t+N]*Math.cos(omega), wgt[t+N]*wgt[t+N]*Math.sin(omega));
                        R[i-1][k-1] = MathUtils.addComplex(R[i-1][k-1], tmp);
                    }
                }
            } 
             */

            /*
            //Use matrix inversion
            ComplexNumber[][] invR = MathUtils.inverse(R);
            ComplexNumber[] x = MathUtils.matrixProduct(invR,MathUtils.multiplyComplex(b, 1.0));
            */

            //Use generalized Levinson
            ComplexNumber[] r = new ComplexNumber[R.length];
            for (i=0; i<R.length; i++)
                r[i] = new ComplexNumber(R[i][0]);
            ComplexNumber[] x = MathUtils.levinson(r, MathUtils.multiplyComplex(b, 1.0));
            //

            /*
            //Check matrix inversion operation
            ComplexNumber[][] RinvR = MathUtils.matrixProduct(R, invR);
            for (i=0; i<RinvR.length; i++)
            {
                for (k=0; k<RinvR[i].length; k++)
                {
                    if (i!=k && MathUtils.magnitudeComplex(RinvR[i][k])>1e-4)
                        System.out.println("Check here! Non-zero non-diagonal element detected!");
                    if (i==k && Math.abs(MathUtils.magnitudeComplex(RinvR[i][k])-1.0)>1e-4)
                        System.out.println("Check here! Non-unity diagonal element detected!");
                }
            }
            //
             */

            //

            /*
            //Do not include 0 Hz harmonic (Synthesis and analysis need to change if this part is activated; k-->(k+1), number of harmonics etc)
            ComplexNumber[] xpart = new ComplexNumber[L];
            for (k=L+1; k<=2*L; k++)
                xpart[k-L-1] = new ComplexNumber(x[k].real, x[k].imag);
                //
             */

            //Include 0 Hz harmonic (dc-value estimate) as well
            if (INCLUDE_ZEROTH_HARMONIC)
            {
                xpart = new ComplexNumber[L+1];

                for (k=L; k<=2*L; k++)
                    xpart[k-L] = new ComplexNumber(x[k].real, x[k].imag);
            }
            else
            {
                xpart = new ComplexNumber[L];
                
                for (k=L+1; k<=2*L; k++)
                    xpart[k-(L+1)] = new ComplexNumber(x[k].real, x[k].imag);
            }
            //

            /*
            double gain = MathUtils.absMax(s)/MathUtils.sum(MathUtils.abs(xpart));

            for (k=0; k<xpart.length; k++)
                xpart[k] = MathUtils.multiply(gain, xpart[k]);
             */


            //Normalize gains by using FFT based peak amplitudes
            if (NORMALIZE_HARMONIC_AMPLITUDES)
            {
                int freqStartInd, freqEndInd;
                double num = 0.0;
                double denum = 0.0;
                int currentHarmonicNo;
                for (i=0; i<L; i++)
                {
                    if (INCLUDE_ZEROTH_HARMONIC)
                        currentHarmonicNo = i;
                    else
                        currentHarmonicNo = i+1;
                    
                    freqStartInd = SignalProcUtils.freq2index(currentHarmonicNo*f0InHz-0.1*f0InHz, (int)samplingRateInHz, maxFreqIndex);
                    freqEndInd = SignalProcUtils.freq2index(currentHarmonicNo*f0InHz+0.1*f0InHz, (int)samplingRateInHz, maxFreqIndex);
                    k = MathUtils.getMaxIndex(sDft, freqStartInd, freqEndInd);
                    num += sDft[k];
                    denum += MathUtils.magnitudeComplex(xpart[i]);
                    xpart[i] = MathUtils.multiply(sDft[k]/MathUtils.magnitudeComplex(xpart[i]), xpart[i]);
                }
            }

            /*
            double gain = num/denum;

            for (i=0; i<L; i++)
                xpart[i] = MathUtils.multiply(gain, xpart[i]);
             */

            //

            currentDist = 0.0;
            for (i=numHarmonicsMin; i<=L; i++)
                currentDist += (MathUtils.magnitudeComplex(xpart[i-numHarmonicsMin])-sDft[freqInds[i-numHarmonicsMin]])*(MathUtils.magnitudeComplex(xpart[i-numHarmonicsMin])-sDft[freqInds[i-numHarmonicsMin]]);
            currentDist /= (L-numHarmonicsMin+1);
            if (currentDist<minDist)
            {
                minDist = currentDist;
                LOptimal = L;
                xpartOptimal = new ComplexNumber[xpart.length];
                for (i=0; i<xpart.length; i++)
                    xpartOptimal[i] = new ComplexNumber(xpart[i]);
            }


            /*
            noiseSample = 0.0;
            for (i=freqInds[L-numHarmonicsMin]; i<=Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1]); i++)
                noiseSample += Math.abs(sDft[i]);
            noiseSample /= (Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1])-freqInds[L-numHarmonicsMin]+1);

            double distNoise = 0.0;
            for (i=freqInds[L-numHarmonicsMin]; i<=Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1]); i++)
                distNoise += (sDft[i]-noiseSample)*(sDft[i]-noiseSample);

            double distHarmonic = 0.0;
            int midPoint = (int)Math.floor(0.5*(freqInds[L-numHarmonicsMin]+Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1]))+0.5);
            int minInd = MathUtils.getMinIndex(sDft, freqInds[L-numHarmonicsMin], Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1]));
            int maxInd = MathUtils.getMaxIndex(sDft, freqInds[L-numHarmonicsMin], Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1]));

            for (i=freqInds[L-numHarmonicsMin]; i<midPoint; i++)
            {
                harmonicSample = MathUtils.linearMap(i, freqInds[L-numHarmonicsMin], midPoint-1, sDft[minInd], sDft[maxInd]);
                distHarmonic += (sDft[i]-harmonicSample)*(sDft[i]-harmonicSample);
            }
            for (i=midPoint; i<=Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1]); i++)
            {
                harmonicSample = MathUtils.linearMap(i, midPoint, Math.min(sDft.length-1,freqInds[L-numHarmonicsMin+1]), sDft[maxInd], sDft[minInd]);
                distHarmonic += (sDft[i]-harmonicSample)*(sDft[i]-harmonicSample);
            }

            if (distHarmonic>distNoise)
                break;
            else
                LOptimal = L;
             */
        }

        //MaryUtils.plot(MathUtils.amp2db(sDft));
        //MaryUtils.plot(MathUtils.amp2db(MathUtils.magnitudeComplex(xpart)));

        //Display
        //MaryUtils.plot(MathUtils.amp2db(sDft), 0, maxFreqIndex);
        //MaryUtils.plot(MathUtils.amp2db(MathUtils.magnitudeComplex(x)));
        //MaryUtils.plot(MathUtils.amp2db(MathUtils.magnitudeComplex(xpart)));
        //

        System.out.println("Input num. harmonics=" + String.valueOf(LIn) + " optimal=" + String.valueOf(LOptimal) + " - " + String.valueOf(MathUtils.magnitudeComplex(xpart[0])));
        
        //StringUtils.toTextFile(MathUtils.phaseInRadians(xpart), "d:\\hamming.txt");
        
        return xpart;
    }

    /*
    public ComplexNumber[] estimateComplexAmplitudesJampack(double[] s, double[] wgt, double f0InHz, int L, double samplingRateInHz, float tAnalysis) throws JampackException
    {
        if (Parameters.getBaseIndex()!=0)
            Parameters.setBaseIndex(0);

        int M = s.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;

        //ComplexNumber[][] B = new ComplexNumber[2*N+1][2*L+1];
        Zmat B = new Zmat(2*N+1, 2*L+1);
        //ComplexNumber tmp;
        Z tmp;

        int t, i, k;
        double omega;

        //ComplexNumber[][] W = MathUtils.diagonalComplexMatrix(wgt);
        Zmat W = new Zmat(wgt.length, wgt.length);
        for (t=0; t<wgt.length; t++)
            W.put(t,t,new Z(wgt[t], 0.0));

        for (t=-N; t<=N; t++)
        {
            for (k=-L; k<=L; k++)
            {
                //omega = MathUtils.TWOPI*k*f0InHz*(tAnalysis+t/samplingRateInHz);
                omega = MathUtils.TWOPI*k*f0InHz*(t/samplingRateInHz);
                //B[t+N][k+L] = new ComplexNumber(Math.cos(omega), Math.sin(omega));
                B.put(t+N, k+L, new Z(Math.cos(omega), Math.sin(omega)));
            }
        }
        //ComplexNumber[][] BTWTW = MathUtils.matrixProduct(MathUtils.transpoze(B), MathUtils.transpoze(W));
        //BTWTW = MathUtils.matrixProduct(BTWTW, W);
        Zmat BTWTW = Times.o(H.o(B), H.o(W));
        BTWTW = Times.o(BTWTW, W);

        //ComplexNumber[] b = MathUtils.matrixProduct(BTWTW, s);
        Zmat S = new Zmat(s.length, 1);
        for (t=0; t<s.length; t++)
            S.put(t, 0, new Z(s[t], 0.0));
        Zmat b = Times.o(BTWTW, S);

        //ComplexNumber[][] R = MathUtils.matrixProduct(BTWTW, B);

        //ComplexNumber[][] R = new ComplexNumber[2*L+1][2*L+1];
        Zmat R = new Zmat(2*L+1, 2*L+1);
        for (i=1; i<=2*L+1; i++)
        {
            for (k=1; k<=2*L+1; k++)
            {
                //R[i-1][k-1] = new ComplexNumber(0.0, 0.0);
                R.put(i-1, k-1, new Z(0.0, 0.0));
                for (t=-N; t<=N; t++)
                {
                    //omega = MathUtils.TWOPI*f0InHz*(tAnalysis+t/samplingRateInHz)*(i-k);
                    omega = MathUtils.TWOPI*f0InHz*(t/samplingRateInHz)*(i-k);
                    //tmp = new ComplexNumber(wgt[t+N]*wgt[t+N]*Math.cos(omega), wgt[t+N]*wgt[t+N]*Math.sin(omega));
                    tmp = new Z(wgt[t+N]*wgt[t+N]*Math.cos(omega), wgt[t+N]*wgt[t+N]*Math.sin(omega));
                    //R[i-1][k-1] = MathUtils.addComplex(R[i-1][k-1], tmp);
                    R.put(i-1, k-1, new Z(R.get(i-1, k-1).re+tmp.re, R.get(i-1, k-1).im+tmp.im));
                }
            }
        } 

        //ComplexNumber[][] invR = MathUtils.inverse(R);
        Zmat invR = Inv.o(R);

        //Check matrix inversion operation
        //ComplexNumber[][] RinvR = MathUtils.matrixProduct(R, invR);
        Zmat RinvR = Times.o(R, invR);
        //for (i=0; i<RinvR.length; i++)
        for (i=0; i<RinvR.nr; i++)
        {
            //for (k=0; k<RinvR[i].length; k++)
            for (k=0; k<RinvR.nc; k++)
            {
                //if (i!=k && MathUtils.magnitudeComplex(RinvR[i][k])>1e-10)
                if (i!=k && MathUtils.magnitudeComplex(RinvR.get(i, k).re, RinvR.get(i, k).im)>1e-4)
                    System.out.println("Check here! Non-zero non-diagonal element detected!");
                //if (i==k && Math.abs(MathUtils.magnitudeComplex(RinvR[i][k])-1.0)>1e-10)
                if (i==k && Math.abs(MathUtils.magnitudeComplex(RinvR.get(i, k).re, RinvR.get(i, k).im)-1.0)>1e-4)
                    System.out.println("Check here! Non-unity diagonal element detected!");
            }
        }
        //

        //
        //ComplexNumber[] x = MathUtils.matrixProduct(invR, b);
        Zmat x = Times.o(invR, b);
        ComplexNumber[] xpart = new ComplexNumber[L];

        for (k=L-1; k>=0; k--) //The remaning complex amplitudes from L+1 to 2L are complex conjugates of entries from L-1,...,0
            xpart[L-1-k] = new ComplexNumber(x.get(k,0).re, -1.0*x.get(k,0).im);

        //double gain = MathUtils.absMax(s)/MathUtils.sum(MathUtils.abs(xpart));
        //for (k=0; k<xpart.length; k++)
            //xpart[k] = MathUtils.multiply(gain, xpart[k]);

        return xpart;
    }

    public ComplexNumber[] estimateComplexAmplitudesJampack2(double[] frm, double[] wgt, double f0InHz, int L, double samplingRateInHz, float tAnalysis) throws JampackException
    {
        if (Parameters.getBaseIndex()!=0)
            Parameters.setBaseIndex(0);

        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;
        Zmat B = new Zmat(M, 2*L+1);
        int i, j;
        double bcij, bsij;
        for (i=1; i<=2*N; i++)
        {
            for (j=1; j<=L; j++)
            {
                bcij = Math.cos(i*MathUtils.TWOPI*f0InHz*((-N+j-1)/samplingRateInHz));
                bsij = Math.sin(i*MathUtils.TWOPI*f0InHz*((-N+j-1)/samplingRateInHz));
                B.put(i-1, 2*(j-1), new Z(bcij,0.0));
                B.put(i-1, 2*(j-1)+1, new Z(bsij,0.0));
            }
        }

        for (i=1; i<=2*N; i++)
            B.put(i-1, 2*L, new Z(1.0, 0.0));

        Zmat s = new Zmat(M, 1);
        for (i=-N; i<=N; i++)
            s.put(i+N, 0, new Z(frm[i+N], 0.0));

        Zmat W = new Zmat(M, M);
        for (i=-N; i<=N; i++)
        {
            for (j=-N; j<=N; j++)
            {
                if (i==j)
                    W.put(i+N,j+N, new Z(wgt[i+N], 0.0));
                else
                    W.put(i+N,j+N, new Z(0.0, 0.0));
            }
        }

        Zmat BTWTW = H.o(B);
        BTWTW = Times.o(BTWTW, H.o(W));
        BTWTW = Times.o(BTWTW, W);

        Zmat BTWTWs = Times.o(BTWTW, s);

        Zmat BTWTWB = Times.o(BTWTW, B);

        Zmat x = Times.o(Inv.o(BTWTWB), BTWTWs);

        ComplexNumber[] xpart = new ComplexNumber[L+1];

        for (i=1; i<=L; i++)
            xpart[i-1] = new ComplexNumber(x.get(2*(i-1), 0).re, x.get(2*(i-1)+1, 0).re);
        xpart[L] = new ComplexNumber(x.get(2*L, 0).re, 0.0);

        return xpart;
    }
     */

    //Complex amplitude estimation for harmonics in time domain (Diagonal correlation matrix approach, harmonics assumed independent)
    //The main advantage is the operation being in time domain.
    //Therefore, we can use window sizes as short as two pitch periods and track rapid changes in amplitudes and phases
    //N: local pitch period in samples
    //wgtSquared: window weights squared
    //frm: speech frame to be analysed (its length should be 2*N+1)
    //Uses Equation 3.32 in Stylianou`s thesis
    //This requires harmonics to be uncorrelated.
    //We use this for estimating pseudo-harmonic amplitudes of the noise part.
    //Note that this function is equivalent to peak-picking in the frequency domain in Quatieri´s sinusoidal framework.
    public ComplexNumber[] estimateComplexAmplitudesUncorrelated(double[] frm, double[] wgtSquared, int L, double f0InHz, double samplingRateInHz)
    {
        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;

        ComplexNumber tmp;

        int t, k;
        double omega;

        double denum = 0.0;
        for (t=-N; t<=N; t++)
            denum += wgtSquared[t+N];

        ComplexNumber[] Ak = new ComplexNumber[L];
        for (k=1; k<=L; k++)
        {
            Ak[k-1] = new ComplexNumber(0.0, 0.0);
            for (t=-N; t<=N; t++)
            {
                omega = -1.0*MathUtils.TWOPI*k*f0InHz*((double)t/samplingRateInHz);
                tmp = new ComplexNumber(wgtSquared[t+N]*frm[t+N]*Math.cos(omega), wgtSquared[t+N]*frm[t+N]*Math.sin(omega));
                Ak[k-1] = MathUtils.addComplex(Ak[k-1], tmp);
            }
            Ak[k-1] = MathUtils.divide(Ak[k-1], denum);
        }

        return Ak;
    }

    //This is just for testing the full autocorrelation algorithm with diagonal autocorrelation matrix. It produced the same result using estimateComplexAmplitudesUncorrelated2
    public ComplexNumber[] estimateComplexAmplitudesUncorrelated2(double[] frm, double[] wgtSquared, int L, double f0InHz, double samplingRateInHz)
    {
        int M = frm.length;
        assert M % 2==1; //Frame length should be odd
        int N = (M-1)/2;

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
                if (i==k)
                {
                    for (t=-N; t<=N; t++)
                    {
                        omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(k-i);
                        tmp = new ComplexNumber(wgtSquared[t+N]*Math.cos(omega), wgtSquared[t+N]*Math.sin(omega));
                        R[i-1][k-1] = MathUtils.addComplex(R[i-1][k-1], tmp);
                    }
                }
            }
        }   

        for (k=1; k<=2*L+1; k++)
        {
            b[k-1] = new ComplexNumber(0.0, 0.0);
            for (t=-N; t<=N; t++)
            {
                omega = MathUtils.TWOPI*f0InHz*t/samplingRateInHz*(L+1-k);
                tmp = new ComplexNumber(wgtSquared[t+N]*frm[t+N]*Math.cos(omega), wgtSquared[t+N]*frm[t+N]*Math.sin(omega));
                b[k-1] = MathUtils.addComplex(b[k-1], tmp);
            }
        }

        ComplexNumber[] x = MathUtils.matrixProduct(MathUtils.inverse(R), b);
        ComplexNumber[] xpart = new ComplexNumber[L+1];
        for (k=L+1; k<2*L+1; k++) //The remaning complex amplitudes from L+1 to 2L are complex conjugates of entries from L-1,...,1
            xpart[k-(L+1)] = new ComplexNumber(x[k]);

        return xpart;
    }

    public ComplexNumber[] estimateComplexAmplitudesPeakPicking(double[] windowedFrm, int spectralEnvelopeType, boolean isVoiced, float f0, float maximumFreqOfVoicingInHz, boolean bEstimateHNMVoicing, SinusoidalAnalysisParams params)
    {
        int k;
        ComplexNumber[] x = null;
        int numHarmonics = (int)Math.floor(maximumFreqOfVoicingInHz/f0+0.5);
        if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
            numHarmonics++;
        
        if (numHarmonics>0)
        {
            /*
            float[] initialPeakLocationsInHz = new float[numHarmonics+1];
            initialPeakLocationsInHz[0] = 0.0f;
            for (int i=1; i<numHarmonics+1; i++)
                initialPeakLocationsInHz[i] = i*f0;
             */

            float[] initialPeakLocationsInHz = new float[numHarmonics];
            for (int i=0; i<numHarmonics; i++)
            {
                if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
                    initialPeakLocationsInHz[i] = i*f0;
                else
                    initialPeakLocationsInHz[i] = (i+1)*f0;
            }

            NonharmonicSinusoidalSpeechFrame nhs = SinusoidalAnalyzer.analyze_frame(windowedFrm, false, spectralEnvelopeType, isVoiced, f0, maximumFreqOfVoicingInHz, bEstimateHNMVoicing, params, initialPeakLocationsInHz);

            x = new ComplexNumber[nhs.sinusoids.length];
            for (int i=0; i<nhs.sinusoids.length; i++)
                x[i] = MathUtils.ampPhase2ComplexNumber(nhs.sinusoids[i].amp, nhs.sinusoids[i].phase);
        }

        return x;
    }
    
    //Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
    public static void unwrapPhasesAlongHarmonics(HntmSpeechSignal hnmSignal)
    {
        int i, k;
        int maxNumHarmonics = 0;
        int numHarmonicsCurrentFrame;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null)
            {
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }

        float[] dphaseks = new float[maxNumHarmonics];
        Arrays.fill(dphaseks, 0.0f);

        float[] origPhases = new float[maxNumHarmonics];
        float[] newPhases = new float[maxNumHarmonics];

        boolean isPrevTrackVoiced;
        int Mk;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null)
            { 
                Arrays.fill(origPhases, 0.0f);
                Arrays.fill(newPhases, 0.0f);
                System.arraycopy(hnmSignal.frames[i].h.phases, 0, origPhases, 0, hnmSignal.frames[i].h.phases.length);
                System.arraycopy(hnmSignal.frames[i].h.phases, 0, newPhases, 0, hnmSignal.frames[i].h.phases.length);

                for (k=1; k<hnmSignal.frames[i].h.phases.length-1; k++)
                {     
                    isPrevTrackVoiced = false;

                    if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].h.phases!=null && hnmSignal.frames[i-1].h.phases.length>k)
                        isPrevTrackVoiced = true;

                    if (!isPrevTrackVoiced) //First voiced frame of a voiced segment
                        dphaseks[k-1] = hnmSignal.frames[i].h.phases[k]-hnmSignal.frames[i].h.phases[k-1];

                    Mk = (int)(Math.floor((dphaseks[k-1] + hnmSignal.frames[i].h.phases[k] - hnmSignal.frames[i].h.phases[k+1])/(MathUtils.TWOPI)+0.5));
                    hnmSignal.frames[i].h.phases[k+1] += Mk*MathUtils.TWOPI;

                    dphaseks[k] = hnmSignal.frames[i].h.phases[k+1]-hnmSignal.frames[i].h.phases[k];

                    newPhases[k+1] = hnmSignal.frames[i].h.phases[k+1];
                }

                if (hnmSignal.frames[i].tAnalysisInSeconds>=0.060 && hnmSignal.frames[i].tAnalysisInSeconds<=0.070)
                {
                    MaryUtils.plot(origPhases);
                    MaryUtils.plot(newPhases);
                }
            }
            //else
            //    Arrays.fill(dphaseks, 0.0f);
        }
    }
}


