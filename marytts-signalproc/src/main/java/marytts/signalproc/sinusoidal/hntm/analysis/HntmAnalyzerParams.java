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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.signalproc.window.Window;

/**
 * Analysis parameters for harmonics plus noise model (HNM)
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class HntmAnalyzerParams {

	public HnmPitchVoicingAnalyzerParams hnmPitchVoicingAnalyzerParams; // Parameters of pitch and voicing analyzer

	public boolean useJampackInAnalysis; // Use Jampack library for matrix operations (suggested for increased speed)

	public boolean isSilentAnalysis; // If false, displays a single line of message per frame during analysis
	public boolean readAnalysisResultsFromFile; // If true, analysis results are read from an existing binary file

	public int harmonicModel; // Harmonic model type
	public static final int HARMONICS_PLUS_NOISE = 1;
	public static final int HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE = 2;

	public int noiseModel; // Noise model type
	public static final int WAVEFORM = 1; // Noise part model based on frame waveform (i.e. no model, overlap-add noise part
											// generation)
	public static final int LPC = 2; // Noise part model based on LPC
	public static final int PSEUDO_HARMONIC = 3; // Noise part model based on pseude harmonics for f0=NOISE_F0_IN_HZ
	public static final int VOICEDNOISE_LPC_UNVOICEDNOISE_WAVEFORM = 4; // noise part model based on LPC for voiced parts and
																		// waveform for unvoiced parts
	public static final int UNVOICEDNOISE_LPC_VOICEDNOISE_WAVEFORM = 5; // noise part model based on LPC for unvoiced parts and
																		// waveform for voiced parts

	public int regularizedCepstrumWarpingMethod; // Warping method for regularized cepstral envelope to be fitted to harmonic
													// amplitudes
	public int harmonicSynthesisMethodBeforeNoiseAnalysis; // Synthesize harmonic part before noise analysis for subtraction?

	public boolean useHarmonicAmplitudesDirectly; // If true, regularized cepstral envelope is not used
	public float regularizedCepstrumLambdaHarmonic; // Regularization parameter
	public boolean useWeightingInRegularizedCepstrumEstimationHarmonic; // If true, lower freuqnecies are assigned relatively more
																		// weight in regularized cepstrum estimation
	public int harmonicPartCepstrumOrderPreMel; // Cepstrum order prior to mel scaling
	public int harmonicPartCepstrumOrder; // Cepstrum order in regularized cepstrum estimation

	public boolean computeNoisePartLpOrderFromSamplingRate; // If true, noise part LP order is auto-detected from sampling rate
	public int noisePartLpOrder; // Linear prediction order of noise part if it is not auto-detected from sampling rate
	public float preemphasisCoefNoise; // Pre-emphasis coefficient for the noise part
	public boolean hpfBeforeNoiseAnalysis; // Apply highpass filter before analyzing a noise frame?
	public boolean decimateNoiseWaveform; // Decimate voiced segment noise parts?
	public boolean overlapNoiseWaveformModel; // Perform overlap add processing for waveform based noise model

	// These parameters are effective only when the noise model is pseudo-harmonic
	public boolean useNoiseAmplitudesDirectly; // If true, regularized cepstral envelope is not used
	public float regularizedCepstrumEstimationLambdaNoise; // Regularization parameter
	public boolean useWeightingInRegularizedCesptrumEstimationNoise; // If true, lower freuqnecies are assigned relatively more
																		// weight in regularized cepstrum estimation
	public int noisePartCepstrumOderPre; // Cepstrum order prior to mel scaling
	public int noisePartCepstrumOrder; // Cepstrum order in regularized cepstrum estimation
	public boolean usePosteriorMelWarpingNoise; // Perform posteriro mel-scale warping?

	public float noiseF0InHz; // Fixed f0 for noise part (to determine analysis window size)
	public float hpfTransitionBandwidthInHz; // Transition bandwidth of the highpass filter that separates noise part from
												// harmonic part
	public float noiseAnalysisWindowDurationInSeconds; // Fixed duration of noise analysis windows
	public float overlapBetweenHarmonicAndNoiseRegionsInHz; // Overlap amount in frequency between harmonic and noise regions
	public float overlapBetweenTransientAndNontransientSectionsInSeconds; // Overlap amount in time between transient and
																			// non-transient segments

	public int harmonicAnalysisWindowType; // Window type for harmonic analysis
	public int noiseAnalysisWindowType; // Window type for noise analysis

	public int numHarmonicsForVoicing; // Number of lowest harmonics to use for voicing detection
	public float harmonicsNeigh; // A parameter between 0.0 and 1.0: How much the search range for voicing detection will be
									// extended beyond the first and the last harmonic
									// 0.3 means the region [0.7xf0, 4.3xf0] will be considered in voicing decision

	public float numPeriodsHarmonicsExtraction; // Total periods for hamronic part extraction
	public float fftPeakPickerPeriods; // Total periods for frequency domain peak picking

	public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_ANALYSIS = false; // Apply phase unwrapping along harmonic tracks
																				// after analysis?
	public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_TIME_SCALING = false; // Apply phase unwrapping along harmonic
																					// tracks after time scaling?
	public static boolean UNWRAP_PHASES_ALONG_HARMONICS_AFTER_PITCH_SCALING = false; // Apply phase unwrapping along harmonic
																						// tracks after pitch scaling?

	public HntmAnalyzerParams() {
		hnmPitchVoicingAnalyzerParams = new HnmPitchVoicingAnalyzerParams();

		useJampackInAnalysis = true;

		isSilentAnalysis = false;

		harmonicModel = HARMONICS_PLUS_NOISE;
		noiseModel = WAVEFORM;

		regularizedCepstrumWarpingMethod = RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING;
		harmonicSynthesisMethodBeforeNoiseAnalysis = HntmSynthesizerParams.LINEAR_PHASE_INTERPOLATION;

		useHarmonicAmplitudesDirectly = true; // Use amplitudes directly, the following are only effective if this is false
		regularizedCepstrumLambdaHarmonic = 1.0e-5f; // Reducing this may increase harmonic amplitude estimation accuracy
		useWeightingInRegularizedCepstrumEstimationHarmonic = false;

		harmonicPartCepstrumOrder = 24; // Cepstrum order to represent harmonic amplitudes
		harmonicPartCepstrumOrderPreMel = 40; // Pre-cepstrum order to compute linear cepstral coefficients
												// 0 means auto computation from number of harmonics (See
												// RegularizedPostWarpedCepstrumEstimator.getAutoCepsOrderPre()).

		computeNoisePartLpOrderFromSamplingRate = false; // If true, noise LP order is determined using sampling rate (might be
															// high)
		noisePartLpOrder = 12; // Effective only if the above parameter is false
		preemphasisCoefNoise = 0.97f;
		hpfBeforeNoiseAnalysis = true; // False means the noise part will be full-band
		decimateNoiseWaveform = false; // Apply decimation when noise part is waveform (only in voiced parts)
		overlapNoiseWaveformModel = true; // Keep overlapping chunks of noise waveform for synthesis

		useNoiseAmplitudesDirectly = true; // If noise part is PSEUDE_HARMONICU and if this is true, use amplitudes directly. The
											// following are only effective if this is false
		regularizedCepstrumEstimationLambdaNoise = 2.0e-4f; // Reducing this may increase harmonic amplitude estimation accuracy
		useWeightingInRegularizedCesptrumEstimationNoise = false;
		noisePartCepstrumOderPre = 12; // Effective only for REGULARIZED_CEPS and PSEUDO_HARMONIC noise part types
		noisePartCepstrumOrder = 20; // Effective only for REGULARIZED_CEPS and PSEUDO_HARMONIC noise part types
		usePosteriorMelWarpingNoise = true; // If true, post-warping using Mel-scale is used, otherwise prior warping using
											// Bark-scale is employed

		noiseF0InHz = 100.0f; // Pseudo-pitch for unvoiced portions (will be used for pseudo harmonic modelling of the noise part)
		hpfTransitionBandwidthInHz = 0.0f;
		noiseAnalysisWindowDurationInSeconds = 0.060f; // Fixed window size for noise analysis, should be generally large (>=0.040
														// seconds)
		overlapBetweenHarmonicAndNoiseRegionsInHz = 0.0f;
		overlapBetweenTransientAndNontransientSectionsInSeconds = 0.005f;

		harmonicAnalysisWindowType = Window.HAMMING;
		noiseAnalysisWindowType = Window.HAMMING;

		// Default search range for voicing detection, i.e. voicing criterion will be computed for frequency range:
		// [DEFAULT_VOICING_START_HARMONIC x f0, DEFAULT_VOICING_START_HARMONIC x f0] where f0 is the fundamental frequency
		// estimate
		numHarmonicsForVoicing = 4;
		harmonicsNeigh = 0.3f; // Between 0.0 and 1.0: How much the search range for voicing detection will be extended beyond the
								// first and the last harmonic
								// 0.3 means the region [0.7xf0, 4.3xf0] will be considered in voicing decision

		numPeriodsHarmonicsExtraction = 2.0f;
		fftPeakPickerPeriods = 3.0f;
	}

	public HntmAnalyzerParams(String binaryFile) {
		try {
			read(binaryFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HntmAnalyzerParams(DataInputStream dis) {
		try {
			read(dis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HntmAnalyzerParams(HntmAnalyzerParams existing) {
		hnmPitchVoicingAnalyzerParams = new HnmPitchVoicingAnalyzerParams(existing.hnmPitchVoicingAnalyzerParams);

		useJampackInAnalysis = existing.useJampackInAnalysis;

		isSilentAnalysis = existing.isSilentAnalysis;

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
		decimateNoiseWaveform = existing.decimateNoiseWaveform;
		overlapNoiseWaveformModel = existing.overlapNoiseWaveformModel;

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

	public boolean equals(HntmAnalyzerParams existing) {
		if (!hnmPitchVoicingAnalyzerParams.equals(existing.hnmPitchVoicingAnalyzerParams))
			return false;

		if (useJampackInAnalysis != existing.useJampackInAnalysis)
			return false;

		if (isSilentAnalysis != existing.isSilentAnalysis)
			return false;

		if (harmonicModel != existing.harmonicModel)
			return false;
		if (noiseModel != existing.noiseModel)
			return false;
		if (regularizedCepstrumWarpingMethod != existing.regularizedCepstrumWarpingMethod)
			return false;
		if (harmonicSynthesisMethodBeforeNoiseAnalysis != existing.harmonicSynthesisMethodBeforeNoiseAnalysis)
			return false;

		if (useHarmonicAmplitudesDirectly != existing.useHarmonicAmplitudesDirectly)
			return false;
		if (regularizedCepstrumLambdaHarmonic != existing.regularizedCepstrumLambdaHarmonic)
			return false;
		if (useWeightingInRegularizedCepstrumEstimationHarmonic != existing.useWeightingInRegularizedCepstrumEstimationHarmonic)
			return false;
		if (harmonicPartCepstrumOrderPreMel != existing.harmonicPartCepstrumOrderPreMel)
			return false;
		if (harmonicPartCepstrumOrder != existing.harmonicPartCepstrumOrder)
			return false;

		if (computeNoisePartLpOrderFromSamplingRate != existing.computeNoisePartLpOrderFromSamplingRate)
			return false;
		if (noisePartLpOrder != existing.noisePartLpOrder)
			return false;
		if (preemphasisCoefNoise != existing.preemphasisCoefNoise)
			return false;
		if (hpfBeforeNoiseAnalysis != existing.hpfBeforeNoiseAnalysis)
			return false;
		if (decimateNoiseWaveform != existing.decimateNoiseWaveform)
			return false;
		if (overlapNoiseWaveformModel != existing.overlapNoiseWaveformModel)
			return false;

		if (useNoiseAmplitudesDirectly != existing.useNoiseAmplitudesDirectly)
			return false;
		if (regularizedCepstrumEstimationLambdaNoise != existing.regularizedCepstrumEstimationLambdaNoise)
			return false;
		if (useWeightingInRegularizedCesptrumEstimationNoise != existing.useWeightingInRegularizedCesptrumEstimationNoise)
			return false;
		if (noisePartCepstrumOderPre != existing.noisePartCepstrumOderPre)
			return false;
		if (noisePartCepstrumOrder != existing.noisePartCepstrumOrder)
			return false;
		if (usePosteriorMelWarpingNoise != existing.usePosteriorMelWarpingNoise)
			return false;

		if (noiseF0InHz != existing.noiseF0InHz)
			return false;
		if (hpfTransitionBandwidthInHz != existing.hpfTransitionBandwidthInHz)
			return false;
		if (noiseAnalysisWindowDurationInSeconds != existing.noiseAnalysisWindowDurationInSeconds)
			return false;
		if (overlapBetweenHarmonicAndNoiseRegionsInHz != existing.overlapBetweenHarmonicAndNoiseRegionsInHz)
			return false;
		if (overlapBetweenTransientAndNontransientSectionsInSeconds != existing.overlapBetweenTransientAndNontransientSectionsInSeconds)
			return false;

		if (harmonicAnalysisWindowType != existing.harmonicAnalysisWindowType)
			return false;
		if (noiseAnalysisWindowType != existing.noiseAnalysisWindowType)
			return false;

		if (numHarmonicsForVoicing != existing.numHarmonicsForVoicing)
			return false;
		if (harmonicsNeigh != existing.harmonicsNeigh)
			return false;

		if (numPeriodsHarmonicsExtraction != existing.numPeriodsHarmonicsExtraction)
			return false;
		if (fftPeakPickerPeriods != existing.fftPeakPickerPeriods)
			return false;

		return true;
	}

	public void write(String binaryFile) throws IOException {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(binaryFile)));

		write(dos);
	}

	public void write(DataOutputStream dos) throws IOException {
		hnmPitchVoicingAnalyzerParams.write(dos);

		dos.writeBoolean(useJampackInAnalysis);

		dos.writeBoolean(isSilentAnalysis);

		dos.writeInt(harmonicModel);

		dos.writeInt(noiseModel);

		dos.writeInt(regularizedCepstrumWarpingMethod);
		dos.writeInt(harmonicSynthesisMethodBeforeNoiseAnalysis);

		dos.writeBoolean(useHarmonicAmplitudesDirectly);
		dos.writeFloat(regularizedCepstrumLambdaHarmonic);
		dos.writeBoolean(useWeightingInRegularizedCepstrumEstimationHarmonic);
		dos.writeInt(harmonicPartCepstrumOrderPreMel);
		dos.writeInt(harmonicPartCepstrumOrder);

		dos.writeBoolean(computeNoisePartLpOrderFromSamplingRate);
		dos.writeInt(noisePartLpOrder);
		dos.writeFloat(preemphasisCoefNoise);
		dos.writeBoolean(hpfBeforeNoiseAnalysis);
		dos.writeBoolean(decimateNoiseWaveform);
		dos.writeBoolean(overlapNoiseWaveformModel);

		dos.writeBoolean(useNoiseAmplitudesDirectly);
		dos.writeFloat(regularizedCepstrumEstimationLambdaNoise);
		dos.writeBoolean(useWeightingInRegularizedCesptrumEstimationNoise);
		dos.writeInt(noisePartCepstrumOderPre);
		dos.writeInt(noisePartCepstrumOrder);
		dos.writeBoolean(usePosteriorMelWarpingNoise);

		dos.writeFloat(noiseF0InHz);
		dos.writeFloat(hpfTransitionBandwidthInHz);
		dos.writeFloat(noiseAnalysisWindowDurationInSeconds);
		dos.writeFloat(overlapBetweenHarmonicAndNoiseRegionsInHz);
		dos.writeFloat(overlapBetweenTransientAndNontransientSectionsInSeconds);

		dos.writeInt(harmonicAnalysisWindowType);
		dos.writeInt(noiseAnalysisWindowType);

		dos.writeInt(numHarmonicsForVoicing);
		dos.writeFloat(harmonicsNeigh);

		dos.writeFloat(numPeriodsHarmonicsExtraction);
		dos.writeFloat(fftPeakPickerPeriods);
	}

	public void read(String binaryFile) throws IOException {
		DataInputStream dis = new DataInputStream(new FileInputStream(new File(binaryFile)));

		read(dis);
	}

	public void read(DataInputStream dis) throws IOException {
		hnmPitchVoicingAnalyzerParams = new HnmPitchVoicingAnalyzerParams(dis);

		useJampackInAnalysis = dis.readBoolean();

		isSilentAnalysis = dis.readBoolean();

		harmonicModel = dis.readInt();

		noiseModel = dis.readInt();

		regularizedCepstrumWarpingMethod = dis.readInt();
		harmonicSynthesisMethodBeforeNoiseAnalysis = dis.readInt();

		useHarmonicAmplitudesDirectly = dis.readBoolean();
		regularizedCepstrumLambdaHarmonic = dis.readFloat();
		useWeightingInRegularizedCepstrumEstimationHarmonic = dis.readBoolean();
		harmonicPartCepstrumOrderPreMel = dis.readInt();
		harmonicPartCepstrumOrder = dis.readInt();

		computeNoisePartLpOrderFromSamplingRate = dis.readBoolean();
		noisePartLpOrder = dis.readInt();
		preemphasisCoefNoise = dis.readFloat();
		hpfBeforeNoiseAnalysis = dis.readBoolean();
		decimateNoiseWaveform = dis.readBoolean();
		overlapNoiseWaveformModel = dis.readBoolean();

		useNoiseAmplitudesDirectly = dis.readBoolean();
		regularizedCepstrumEstimationLambdaNoise = dis.readFloat();
		useWeightingInRegularizedCesptrumEstimationNoise = dis.readBoolean();
		noisePartCepstrumOderPre = dis.readInt();
		noisePartCepstrumOrder = dis.readInt();
		usePosteriorMelWarpingNoise = dis.readBoolean();

		noiseF0InHz = dis.readFloat();
		hpfTransitionBandwidthInHz = dis.readFloat();
		noiseAnalysisWindowDurationInSeconds = dis.readFloat();
		overlapBetweenHarmonicAndNoiseRegionsInHz = dis.readFloat();
		overlapBetweenTransientAndNontransientSectionsInSeconds = dis.readFloat();

		harmonicAnalysisWindowType = dis.readInt();
		noiseAnalysisWindowType = dis.readInt();

		numHarmonicsForVoicing = dis.readInt();
		harmonicsNeigh = dis.readFloat();

		numPeriodsHarmonicsExtraction = dis.readFloat();
		fftPeakPickerPeriods = dis.readFloat();
	}
}
