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
package marytts.signalproc.sinusoidal;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.filter.FilterBankAnalyserBase;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Prosody modification using sinusoidal model.
 * 
 * Reference: Quatieri, T. F. and R. J. McAula y, 1992, “Shape Invariant Timescale and Pitch Modification of Speech”, IEEE
 * Transactions On Signal Processing, vol. 40, no. 3, pp. 497-510.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class ProsodyModifier {
	public int fs;

	public ProsodyModifier(int samplingRate) {
		fs = samplingRate;
	}

	public double[] process(double[] x, double[] f0s, float f0_ws, float f0_ss, boolean isVoicingAdaptiveTimeScaling,
			float timeScalingVoicingThreshold, boolean isVoicingAdaptivePitchScaling, float timeScale, float pitchScale,
			float skipSizeInSeconds, float deltaInHz, float numPeriods, boolean bRefinePeakEstimatesParabola,
			boolean bRefinePeakEstimatesBias, boolean bSpectralReassignment, boolean bAdjustNeighFreqDependent,
			boolean isSilentSynthesis, double absMaxDesired, int spectralEnvelopeType, int analyzerType, int synthesizerType,
			int pitchMarkOffset, int sysPhaseModMethod, int sysAmpModMethod) throws Exception {
		float[] tScales = new float[1];
		float[] tScalesTimes = new float[1];
		tScales[0] = timeScale;
		tScalesTimes[0] = skipSizeInSeconds;

		float[] pScales = new float[1];
		float[] pScalesTimes = new float[1];
		pScales[0] = pitchScale;
		pScalesTimes[0] = skipSizeInSeconds;

		return process(x, f0s, f0_ws, f0_ss, isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold,
				isVoicingAdaptivePitchScaling, tScales, tScalesTimes, pScales, pScalesTimes, skipSizeInSeconds, deltaInHz,
				numPeriods, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment,
				bAdjustNeighFreqDependent, isSilentSynthesis, absMaxDesired, spectralEnvelopeType, analyzerType, synthesizerType,
				pitchMarkOffset, sysPhaseModMethod, sysAmpModMethod);
	}

	public double[] process(double[] x, double[] f0s, float f0_ws, float f0_ss, boolean isVoicingAdaptiveTimeScaling,
			float timeScalingVoicingThreshold, boolean isVoicingAdaptivePitchScaling, float[] timeScales,
			float[] timeScalesTimes, float[] pitchScales, float[] pitchScalesTimes, float skipSizeInSeconds, float deltaInHz,
			float numPeriods, boolean bRefinePeakEstimatesParabola, boolean bRefinePeakEstimatesBias,
			boolean bSpectralReassignment, boolean bAdjustNeighFreqDependent, boolean isSilentSynthesis, double absMaxDesired,
			int spectralEnvelopeType, int analyzerType, int synthesizerType, int pitchMarkOffset, int sysPhaseModMethod,
			int sysAmpModMethod) throws Exception {
		int windowType = Window.HANNING;
		// Analysis
		PitchMarks pm = SignalProcUtils.pitchContour2pitchMarks(f0s, fs, x.length, f0_ws, f0_ss, false, pitchMarkOffset);

		BaseSinusoidalAnalyzer an = null;
		SinusoidalTracks[] st = null;
		SinusoidalAnalysisParams params = new SinusoidalAnalysisParams(fs, 0.0, 0.5 * fs, windowType,
				bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent);

		if (analyzerType == BaseSinusoidalAnalyzer.FIXEDRATE_FULLBAND_ANALYZER) {
			an = new SinusoidalAnalyzer(params);

			st = new SinusoidalTracks[1];
			st[0] = ((SinusoidalAnalyzer) an).analyzeFixedRate(x, f0_ws, f0_ss, deltaInHz, spectralEnvelopeType, f0s, f0_ws,
					f0_ss);
		} else if (analyzerType == BaseSinusoidalAnalyzer.PITCHSYNCHRONOUS_FULLBAND_ANALYZER) {
			an = new PitchSynchronousSinusoidalAnalyzer(params);

			st = new SinusoidalTracks[1];
			st[0] = ((PitchSynchronousSinusoidalAnalyzer) an).analyzePitchSynchronous(x, pm, numPeriods, skipSizeInSeconds,
					deltaInHz, spectralEnvelopeType);
		} else if (analyzerType == BaseSinusoidalAnalyzer.FIXEDRATE_MULTIRESOLUTION_ANALYZER) {
			// These should be input as well
			int multiresolutionFilterbankType = FilterBankAnalyserBase.FIR_BANDPASS_FILTERBANK;
			int numBands = 4;
			double lowestBandWindowSizeInSeconds = 0.020;
			boolean bFreqLimitedAnalysis = true;
			//

			an = new MultiresolutionSinusoidalAnalyzer(multiresolutionFilterbankType, numBands, fs);

			st = ((MultiresolutionSinusoidalAnalyzer) an).analyze(x, lowestBandWindowSizeInSeconds, windowType,
					bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent,
					bFreqLimitedAnalysis);
		} else if (analyzerType == BaseSinusoidalAnalyzer.PITCHSYNCHRONOUS_MULTIRESOLUTION_ANALYZER) {
			/*
			 * an = new MultiresolutionSinusoidalAnalyzer();
			 * 
			 * //These should be input as well int multiresolutionFilterbankType =
			 * MultiresolutionSinusoidalAnalyzer.FIR_BANDPASS_FILTERBANK; int numBands = 4; double lowestBandWindowSizeInSeconds =
			 * 0.020; //
			 * 
			 * st = ((MultiresolutionSinusoidalAnalyzer)an).analyzePitchSynchronous(x, fs, multiresolutionFilterbankType,
			 * numBands, lowestBandWindowSizeInSeconds, windowType, bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias,
			 * bSpectralReassignment, bAdjustNeighFreqDependent);
			 */
		} else {
			System.out.println("Invalid sinusoidal analyzer...");
			return null;
		}

		// To do: Estimation of voicing probabilities...

		SinusoidalTracks[] stMod = new SinusoidalTracks[st.length];

		for (int i = 0; i < st.length; i++) {
			stMod[i] = TrackModifier.modify(st[i], f0s, f0_ss, f0_ws, pm.pitchMarks, st[i].voicings, numPeriods,
					isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling, timeScales,
					timeScalesTimes, pitchScales, pitchScalesTimes, pitchMarkOffset, sysAmpModMethod, sysPhaseModMethod);
		}

		// Synthesis
		double[] y = null;
		if (synthesizerType == BaseSinusoidalSynthesizer.PEAK_MATCHED_TRACK_SYNTHESIZER) {
			PeakMatchedSinusoidalSynthesizer synthesizer = new PeakMatchedSinusoidalSynthesizer(fs);
			y = synthesizer.synthesize(stMod, isSilentSynthesis);
		} else if (synthesizerType == BaseSinusoidalSynthesizer.PEAK_MATCHED_TRACK_SYNTHESIZER) {
			OverlapAddSinusoidalSynthesizer synthesizer = new OverlapAddSinusoidalSynthesizer(fs);
			y = synthesizer.synthesize(stMod, isSilentSynthesis);
		} else
			throw new Exception("Unknown sinusoidal synthesizer type!");

		if (y != null) {
			// y = OverlapAddEnergyNormalizer.normalize(y, st[0].fs, f0_ws, f0_ss, windowType, 1.0);
			y = MathUtils.normalizeToAbsMax(y, absMaxDesired);
		}

		return y;
	}

	public static void main(String[] args) throws Exception {
		if (false) {
			// File input
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			double[] x = signal.getAllData();

			// Read pitch contour (real speech or create it from pm file
			PitchReaderWriter f0 = null;
			String strPitchFile = args[0].substring(0, args[0].length() - 4) + ".ptc";
			f0 = new PitchReaderWriter(strPitchFile);
			//

			// Analysis
			float deltaInHz = SinusoidalAnalysisParams.DEFAULT_DELTA_IN_HZ;
			float numPeriods = PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS;
			boolean isSilentSynthesis = false;

			boolean bRefinePeakEstimatesParabola = false;
			boolean bRefinePeakEstimatesBias = false;
			boolean bSpectralReassignment = false;
			boolean bAdjustNeighFreqDependent = false;

			double absMaxOriginal = MathUtils.getAbsMax(x);

			float skipSizeInSeconds = TrackModifier.DEFAULT_MODIFICATION_SKIP_SIZE;
			// skipSizeInSeconds = -1.0f;
			// skipSizeInSeconds = 0.002f;

			boolean isVoicingAdaptiveTimeScaling = true;
			float timeScalingVoicingThreshold = 0.5f;
			boolean isVoicingAdaptivePitchScaling = true;

			int spectralEnvelopeType = SinusoidalAnalysisParams.LP_SPEC;

			// int analyzerType = BaseSinusoidalAnalyzer.FIXEDRATE_FULLBAND_ANALYZER;
			// int analyzerType = BaseSinusoidalAnalyzer.PITCHSYNCHRONOUS_FULLBAND_ANALYZER;
			int pitchMarkOffset = 0; // Only used in PITCHSYNCHRONOUS_FULLBAND_ANALYZER
			int analyzerType = BaseSinusoidalAnalyzer.FIXEDRATE_MULTIRESOLUTION_ANALYZER;

			int synthesizerType = BaseSinusoidalSynthesizer.PEAK_MATCHED_TRACK_SYNTHESIZER;
			// int synthesizerType = BaseSinusoidalSynthesizer.OVERLAP_ADD_SYNTHESIZER;

			ProsodyModifier pm = new ProsodyModifier(samplingRate);
			double[] y = null;

			int sysPhaseModMethod, sysAmpModMethod;

			sysPhaseModMethod = TrackModifier.FROM_ORIGINAL;
			// sysPhaseModMethod = TrackModifier.FROM_RESAMPLED;
			// sysPhaseModMethod = TrackModifier.FROM_CEPSTRUM;
			// sysPhaseModMethod = TrackModifier.FROM_INTERPOLATED; //This is only available for phase

			sysAmpModMethod = TrackModifier.FROM_ORIGINAL;
			// sysAmpModMethod = TrackModifier.FROM_RESAMPLED;
			// sysAmpModMethod = TrackModifier.FROM_CEPSTRUM;

			if (true) {
				float timeScale = 1.0f;
				float pitchScale = 1.05f;
				y = pm.process(x, f0.contour, (float) f0.header.windowSizeInSeconds, (float) f0.header.skipSizeInSeconds,
						isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling, timeScale,
						pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, bRefinePeakEstimatesParabola,
						bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis,
						absMaxOriginal, spectralEnvelopeType, analyzerType, synthesizerType, pitchMarkOffset, sysPhaseModMethod,
						sysAmpModMethod);
			} else {
				float[] timeScales = { 0.5f, 0.75f, 1.25f, 1.75f };
				float[] timeScalesTimes = { 0.5f, 1.25f, 2.0f, 2.5f };
				float[] pitchScales = { 2.0f, 1.5f, 0.8f, 0.6f };
				float[] pitchScalesTimes = { 0.5f, 1.25f, 2.0f, 2.5f };

				y = pm.process(x, f0.contour, (float) f0.header.windowSizeInSeconds, (float) f0.header.skipSizeInSeconds,
						isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling, timeScales,
						timeScalesTimes, pitchScales, pitchScalesTimes, skipSizeInSeconds, deltaInHz, numPeriods,
						bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent,
						isSilentSynthesis, absMaxOriginal, spectralEnvelopeType, analyzerType, synthesizerType, pitchMarkOffset,
						sysPhaseModMethod, sysAmpModMethod);
			}

			// File output
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
			String outFileName = args[0].substring(0, args[0].length() - 4) + "_sinProsodyModified.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
			//
		} else
			main2(args);
	}

	public static void main2(String[] args) throws Exception {
		// File input
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		// Read pitch contour (real speech or create it from pm file
		PitchReaderWriter f0 = null;
		String strPitchFile = args[0].substring(0, args[0].length() - 4) + ".ptc";
		f0 = new PitchReaderWriter(strPitchFile);
		//

		// Analysis
		float deltaInHz = SinusoidalAnalysisParams.DEFAULT_DELTA_IN_HZ;
		float numPeriods = PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS;
		boolean isSilentSynthesis = false;

		boolean bRefinePeakEstimatesParabola = false;
		boolean bRefinePeakEstimatesBias = false;
		boolean bSpectralReassignment = false;
		boolean bAdjustNeighFreqDependent = false;

		double absMaxOriginal = MathUtils.getAbsMax(x);

		float skipSizeInSeconds = TrackModifier.DEFAULT_MODIFICATION_SKIP_SIZE;
		// skipSizeInSeconds = -1.0f;
		// skipSizeInSeconds = 0.002f;

		boolean isVoicingAdaptiveTimeScaling = false;
		float timeScalingVoicingThreshold = 0.5f;
		boolean isVoicingAdaptivePitchScaling = false;

		// int spectralEnvelopeType = SinusoidalAnalysisParams.LP_SPEC; String envelopeName="lp";
		int spectralEnvelopeType = SinusoidalAnalysisParams.SEEVOC_SPEC;
		String envelopeName = "sv";
		// int spectralEnvelopeType = SinusoidalAnalysisParams.REGULARIZED_CEPS; String envelopeName="rc";

		// int analyzerType = BaseSinusoidalAnalyzer.FIXEDRATE_FULLBAND_ANALYZER;
		int analyzerType = BaseSinusoidalAnalyzer.PITCHSYNCHRONOUS_FULLBAND_ANALYZER;
		int pitchMarkOffset = 0; // Only used in PITCHSYNCHRONOUS_FULLBAND_ANALYZER
		// int analyzerType = BaseSinusoidalAnalyzer.FIXEDRATE_MULTIRESOLUTION_ANALYZER;

		int synthesizerType = BaseSinusoidalSynthesizer.PEAK_MATCHED_TRACK_SYNTHESIZER;
		// int synthesizerType = BaseSinusoidalSynthesizer.OVERLAP_ADD_SYNTHESIZER;

		ProsodyModifier pm = new ProsodyModifier(samplingRate);
		double[] y = null;

		float pitchScale;
		float timeScale;
		DDSAudioInputStream outputAudio;
		String outFileName;

		// String[] pitchScales = {"0.80", "0.90", "0.95", "0.99", "1.01", "1.05", "1.10", "1.20"};
		// String[] pitchScales = {"2.0"};
		String pitchMarkOffsetStr;

		int sysPhaseModMethod, sysAmpModMethod;

		// sysPhaseModMethod = TrackModifier.FROM_ORIGINAL;
		// sysPhaseModMethod = TrackModifier.FROM_RESAMPLED; //Best so far
		sysPhaseModMethod = TrackModifier.FROM_CEPSTRUM; // Best so far
		// sysPhaseModMethod = TrackModifier.FROM_INTERPOLATED; //This is only available for phase mod, however it does not work
		// properly, need to check phase paper of Quatieri

		// sysAmpModMethod = TrackModifier.FROM_ORIGINAL;
		sysAmpModMethod = TrackModifier.FROM_RESAMPLED; // Best so far
		// sysAmpModMethod = TrackModifier.FROM_CEPSTRUM;

		int n = 0;
		// for (n=0; n<=58; n++)
		{
			pitchMarkOffset = n;

			pitchMarkOffsetStr = String.valueOf(pitchMarkOffset);
			if (n < 10)
				pitchMarkOffsetStr = "0" + pitchMarkOffsetStr;

			timeScale = 1.0f;
			pitchScale = 4.0f;
			// for (int i=0; i<pitchScales.length; i++)
			{
				y = pm.process(x, f0.contour, (float) f0.header.windowSizeInSeconds, (float) f0.header.skipSizeInSeconds,
						isVoicingAdaptiveTimeScaling,
						timeScalingVoicingThreshold,
						isVoicingAdaptivePitchScaling,
						// timeScale, Float.valueOf(pitchScales[i]), skipSizeInSeconds, deltaInHz, numPeriods,
						timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, bRefinePeakEstimatesParabola,
						bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis,
						absMaxOriginal, spectralEnvelopeType, analyzerType, synthesizerType, pitchMarkOffset, sysPhaseModMethod,
						sysAmpModMethod);
				outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
				outFileName = args[0].substring(0, args[0].length() - 4) + "_sin_t" + String.valueOf(timeScale) + "_p"
						+ String.valueOf(pitchScale) + "_phaseMod" + String.valueOf(sysPhaseModMethod) + "_ampMod"
						+ String.valueOf(sysAmpModMethod) + ".wav";
				AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
			}
		}

		System.out.println("Sinusoidal prosody modifications completed...");
	}
}
