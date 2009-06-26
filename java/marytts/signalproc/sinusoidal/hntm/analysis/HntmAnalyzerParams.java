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

package marytts.signalproc.sinusoidal.hntm.analysis;

import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class HntmAnalyzerParams {

    public HnmPitchVoicingAnalyzerParams hnmPitchVoicingAnalyzerParams;
    
    public int harmonicModel;
    public static final int HARMONICS_PLUS_NOISE = 1;
    public static final int HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE = 2;
    
    public int noiseModel;
    public static final int WAVEFORM = 1; //Noise part model based on frame waveform (i.e. no model, overlap-add noise part generation)
    public static final int LPC = 2; //Noise part model based on LPC
    public static final int PSEUDO_HARMONIC = 3; //Noise part model based on pseude harmonics for f0=NOISE_F0_IN_HZ
    public static final int VOICEDNOISE_LPC_UNVOICEDNOISE_WAVEFORM = 4; //noise part model based on LPC for voiced parts and waveform for unvoiced parts
    public static final int UNVOICEDNOISE_LPC_VOICEDNOISE_WAVEFORM = 5; //noise part model based on LPC for unvoiced parts and waveform for voiced parts
    
    public int regularizedCepstrumWarpingMethod;
    public int harmonicSynthesisMethodBeforeNoiseAnalysis;
    
    public boolean useHarmonicAmplitudesDirectly; 
    public double regularizedCepstrumLambdaHarmonic;   
    public boolean useWeightingInRegularizedCepstrumEstimationHarmonic;
    public int harmonicPartCepstrumOrderPreMel; 
    public int harmonicPartCepstrumOrder;
    
    public boolean computeNoisePartLpOrderFromSamplingRate;
    public int noisePartLpOrder;
    public float preemphasisCoefNoise;
    public boolean hpfBeforeNoiseAnalysis;
    
    public boolean useNoiseAmplitudesDirectly;
    public double regularizedCepstrumEstimationLambdaNoise;  
    public boolean useWeightingInRegularizedCesptrumEstimationNoise;
    public int noisePartCepstrumOderPre;
    public int noisePartCepstrumOrder;
    public boolean usePosteriorMelWarpingNoise;
    
    public double noiseF0InHz; 
    public float hpfTransitionBandwidthInHz;
    public float noiseAnalysisWindowDurationInSeconds;
    public float overlapBetweenHarmonicAndNoiseRegionsInHz;
    public float overlapBetweenTransientAndNontransientSectionsInSeconds;

    public int harmonicAnalysisWindowType;
    public int noiseAnalysisWindowType;

    public int numHarmonicsForVoicing;
    public float harmonicsNeigh;

    public float numPeriodsHarmonicsExtraction;
    public double fftPeakPickerPeriods;


    //These are not effective, nothing changes if you make any of them true
    public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_ANALYSIS = false;
    public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_TIME_SCALING = false;
    public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_PITCH_SCALING = false;
    //
    
    public HntmAnalyzerParams()
    {
        hnmPitchVoicingAnalyzerParams = new HnmPitchVoicingAnalyzerParams();
        
        harmonicModel = HARMONICS_PLUS_NOISE;
        noiseModel = WAVEFORM;
        
        regularizedCepstrumWarpingMethod = RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING;
        harmonicSynthesisMethodBeforeNoiseAnalysis = HntmSynthesizerParams.LINEAR_PHASE_INTERPOLATION;
        
        useHarmonicAmplitudesDirectly = false; //Use amplitudes directly, the following are only effective if this is false
        regularizedCepstrumLambdaHarmonic = 1.0e-5;  //Reducing this may increase harmonic amplitude estimation accuracy 
        useWeightingInRegularizedCepstrumEstimationHarmonic = false;
        
        harmonicPartCepstrumOrder = 24;  //Cepstrum order to represent harmonic amplitudes
        harmonicPartCepstrumOrderPreMel = 40; //Pre-cepstrum order to compute linear cepstral coefficients
                                              //0 means auto computation from number of harmonics (See RegularizedPostWarpedCepstrumEstimator.getAutoCepsOrderPre()).
        
        computeNoisePartLpOrderFromSamplingRate = false; //If true, noise LP order is determined using sampling rate (might be high)
        noisePartLpOrder = 12; //Effective only if the above parameter is false
        preemphasisCoefNoise = 0.97f;
        hpfBeforeNoiseAnalysis = true; //False means the noise part will be full-band
        
        useNoiseAmplitudesDirectly = true; //If noise part is PSEUDE_HARMONICU and if this is true, use amplitudes directly. The following are only effective if this is false
        regularizedCepstrumEstimationLambdaNoise = 2e-4; //Reducing this may increase harmonic amplitude estimation accuracy    
        useWeightingInRegularizedCesptrumEstimationNoise = false;
        noisePartCepstrumOderPre = 12; //Effective only for REGULARIZED_CEPS and PSEUDO_HARMONIC noise part types
        noisePartCepstrumOrder = 20; //Effective only for REGULARIZED_CEPS and PSEUDO_HARMONIC noise part types
        usePosteriorMelWarpingNoise = true; //If true, post-warping using Mel-scale is used, otherwise prior warping using Bark-scale is employed
        
        noiseF0InHz = 100.0; //Pseudo-pitch for unvoiced portions (will be used for pseudo harmonic modelling of the noise part)
        hpfTransitionBandwidthInHz = 0.0f;
        noiseAnalysisWindowDurationInSeconds = 0.060f; //Fixed window size for noise analysis, should be generally large (>=0.040 seconds)
        overlapBetweenHarmonicAndNoiseRegionsInHz = 0.0f;
        overlapBetweenTransientAndNontransientSectionsInSeconds = 0.005f;

        harmonicAnalysisWindowType = Window.HAMMING;
        noiseAnalysisWindowType = Window.HAMMING;

        //Default search range for voicing detection, i.e. voicing criterion will be computed for frequency range:
        // [DEFAULT_VOICING_START_HARMONIC x f0, DEFAULT_VOICING_START_HARMONIC x f0] where f0 is the fundamental frequency estimate
        numHarmonicsForVoicing = 4;
        harmonicsNeigh = 0.3f; //Between 0.0 and 1.0: How much the search range for voicing detection will be extended beyond the first and the last harmonic
                                                   //0.3 means the region [0.7xf0, 4.3xf0] will be considered in voicing decision

        numPeriodsHarmonicsExtraction = 2.0f;
        fftPeakPickerPeriods = 3.0;
    }
    
    public HntmAnalyzerParams(HntmAnalyzerParams existing)
    {
        hnmPitchVoicingAnalyzerParams = new HnmPitchVoicingAnalyzerParams(existing.hnmPitchVoicingAnalyzerParams);
        
        harmonicModel = existing.harmonicModel;
        noiseModel = existing.noiseModel;
        regularizedCepstrumWarpingMethod = existing.regularizedCepstrumWarpingMethod;
        harmonicSynthesisMethodBeforeNoiseAnalysis = existing.harmonicSynthesisMethodBeforeNoiseAnalysis;
        
        useHarmonicAmplitudesDirectly = existing.useHarmonicAmplitudesDirectly;
        regularizedCepstrumLambdaHarmonic = existing.regularizedCepstrumLambdaHarmonic; 
        useWeightingInRegularizedCepstrumEstimationHarmonic = existing.useWeightingInRegularizedCepstrumEstimationHarmonic;
        harmonicPartCepstrumOrderPreMel = existing.harmonicPartCepstrumOrderPreMel;
        harmonicPartCepstrumOrder = existing.harmonicPartCepstrumOrder;
        
        computeNoisePartLpOrderFromSamplingRate = existing.computeNoisePartLpOrderFromSamplingRate;
        noisePartLpOrder = existing.noisePartLpOrder;
        preemphasisCoefNoise = existing.preemphasisCoefNoise;
        hpfBeforeNoiseAnalysis = existing.hpfBeforeNoiseAnalysis;
        
        useNoiseAmplitudesDirectly = existing.useNoiseAmplitudesDirectly;
        regularizedCepstrumEstimationLambdaNoise = existing.regularizedCepstrumEstimationLambdaNoise;
        useWeightingInRegularizedCesptrumEstimationNoise = existing.useWeightingInRegularizedCesptrumEstimationNoise;
        noisePartCepstrumOderPre = existing.noisePartCepstrumOderPre; 
        noisePartCepstrumOrder = existing.noisePartCepstrumOrder;
        usePosteriorMelWarpingNoise = existing.usePosteriorMelWarpingNoise;
        
        noiseF0InHz = existing.noiseF0InHz; 
        hpfTransitionBandwidthInHz = existing.hpfTransitionBandwidthInHz;
        noiseAnalysisWindowDurationInSeconds = existing.noiseAnalysisWindowDurationInSeconds;
        overlapBetweenHarmonicAndNoiseRegionsInHz = existing.overlapBetweenHarmonicAndNoiseRegionsInHz;
        overlapBetweenTransientAndNontransientSectionsInSeconds = existing.overlapBetweenTransientAndNontransientSectionsInSeconds;

        harmonicAnalysisWindowType = existing.harmonicAnalysisWindowType;
        noiseAnalysisWindowType = existing.noiseAnalysisWindowType;

        numHarmonicsForVoicing = existing.numHarmonicsForVoicing;
        harmonicsNeigh = existing.harmonicsNeigh;

        numPeriodsHarmonicsExtraction = existing.numPeriodsHarmonicsExtraction;
        fftPeakPickerPeriods = existing.fftPeakPickerPeriods;
    }
}
