/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.vocalizations;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.unitselection.data.TimelineReader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.DatagramDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;

/**
 * HNM Synthesis technology to synthesize vocalizations
 * 
 * @author Sathish Pammi
 */

public class HNMSynthesisTechnology extends VocalizationSynthesisTechnology {

	protected HNMFeatureFileReader vHNMFeaturesReader;
	protected VocalizationIntonationReader vIntonationReader;
	protected HntmAnalyzerParams analysisParams;
	protected HntmSynthesizerParams synthesisParams;
	protected TimelineReader audioTimeline;
	protected VocalizationUnitFileReader unitFileReader;
	protected boolean f0ContourImposeSupport;

	public HNMSynthesisTechnology(String waveTimeLineFile, String unitFile, String hnmFeatureFile, String intonationFeatureFile,
			boolean imposeF0Support) throws MaryConfigurationException {

		try {
			this.audioTimeline = new TimelineReader(waveTimeLineFile);
			this.unitFileReader = new VocalizationUnitFileReader(unitFile);
			this.f0ContourImposeSupport = imposeF0Support;
			this.vHNMFeaturesReader = new HNMFeatureFileReader(hnmFeatureFile);

			if (f0ContourImposeSupport) {
				this.vIntonationReader = new VocalizationIntonationReader(intonationFeatureFile);
			} else {
				this.vIntonationReader = null;
			}
		} catch (IOException e) {
			throw new MaryConfigurationException("Can not read data from files " + e);
		}

		initializeParameters();
	}

	public HNMSynthesisTechnology(TimelineReader audioTimeline, VocalizationUnitFileReader unitFileReader,
			HNMFeatureFileReader vHNMFeaturesReader, VocalizationIntonationReader vIntonationReader, boolean imposeF0Support) {

		this.audioTimeline = audioTimeline;
		this.unitFileReader = unitFileReader;
		this.vHNMFeaturesReader = vHNMFeaturesReader;
		this.vIntonationReader = vIntonationReader;
		this.f0ContourImposeSupport = imposeF0Support;

		initializeParameters();
	}

	/**
	 * intialize hnm parameters
	 */
	private void initializeParameters() {
		// Analysis parameters
		analysisParams = new HntmAnalyzerParams();
		analysisParams.harmonicModel = HntmAnalyzerParams.HARMONICS_PLUS_NOISE;
		analysisParams.noiseModel = HntmAnalyzerParams.WAVEFORM;
		analysisParams.useHarmonicAmplitudesDirectly = true;
		analysisParams.harmonicSynthesisMethodBeforeNoiseAnalysis = HntmSynthesizerParams.LINEAR_PHASE_INTERPOLATION;
		analysisParams.regularizedCepstrumWarpingMethod = RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING;

		// Synthesis parameters
		synthesisParams = new HntmSynthesizerParams();
		synthesisParams.harmonicPartSynthesisMethod = HntmSynthesizerParams.LINEAR_PHASE_INTERPOLATION;
		// synthesisParams.harmonicPartSynthesisMethod = HntmSynthesizerParams.QUADRATIC_PHASE_INTERPOLATION;
		synthesisParams.overlappingHarmonicPartSynthesis = false;
		synthesisParams.harmonicSynthesisOverlapInSeconds = 0.010f;
		/* to output just one file */
		synthesisParams.writeHarmonicPartToSeparateFile = false;
		synthesisParams.writeNoisePartToSeparateFile = false;
		synthesisParams.writeTransientPartToSeparateFile = false;
		synthesisParams.writeOriginalMinusHarmonicPartToSeparateFile = false;
	}

	/**
	 * Synthesize given vocalization (i.e. unit-selection)
	 * 
	 * @param backchannelNumber
	 *            unit index
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	@Override
	public AudioInputStream synthesize(int backchannelNumber, AudioFileFormat aft) throws SynthesisException {

		int numberOfBackChannels = unitFileReader.getNumberOfUnits();
		if (backchannelNumber >= numberOfBackChannels) {
			throw new IllegalArgumentException("This voice has " + numberOfBackChannels
					+ " backchannels only. so it doesn't support unit number " + backchannelNumber);
		}

		VocalizationUnit bUnit = unitFileReader.getUnit(backchannelNumber);
		long start = bUnit.startTime;
		int duration = bUnit.duration;
		Datagram[] frames = null;
		try {
			frames = audioTimeline.getDatagrams(start, duration);
		} catch (IOException e) {
			throw new SynthesisException("Can not read data from timeline file " + e);
		}
		// Generate audio from frames
		LinkedList<Datagram> datagrams = new LinkedList<Datagram>();
		datagrams.addAll(Arrays.asList(frames));
		DoubleDataSource audioSource = new DatagramDoubleDataSource(datagrams);
		// audioSource.getAllData();
		return (new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), aft.getFormat()));
	}

	/**
	 * Re-synthesize given vocalization using HNM technology
	 * 
	 * @param backchannelNumber
	 *            unit index
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	@Override
	public AudioInputStream reSynthesize(int backchannelNumber, AudioFileFormat aft) throws SynthesisException {
		float[] pScalesArray = { 1.0f };
		float[] tScalesArray = { 1.0f };
		float[] tScalesTimes = { 1.0f };
		float[] pScalesTimes = { 1.0f };
		return synthesizeUsingF0Modification(backchannelNumber, pScalesArray, pScalesTimes, tScalesArray, tScalesTimes, aft);
	}

	/**
	 * Impose target intonation contour on given vocalization using HNM technology
	 * 
	 * @param sourceIndex
	 *            unit index of vocalization
	 * @param targetIndex
	 *            unit index of target intonation
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	@Override
	public AudioInputStream synthesizeUsingImposedF0(int sourceIndex, int targetIndex, AudioFileFormat aft)
			throws SynthesisException {

		if (!f0ContourImposeSupport) {
			throw new SynthesisException("Mary configuration of this voice doesn't support intonation contour imposition");
		}

		int numberOfUnits = vHNMFeaturesReader.getNumberOfUnits();
		if (sourceIndex >= numberOfUnits || targetIndex >= numberOfUnits) {
			throw new IllegalArgumentException("sourceIndex(" + sourceIndex + ") and targetIndex(" + targetIndex
					+ ") are should be less than number of available units (" + numberOfUnits + ")");
		}

		double[] sourceF0 = this.vIntonationReader.getContour(sourceIndex);
		double[] targetF0coeffs = this.vIntonationReader.getIntonationCoeffs(targetIndex);
		double[] sourceF0coeffs = this.vIntonationReader.getIntonationCoeffs(sourceIndex);

		if (targetF0coeffs == null || sourceF0coeffs == null) {
			return reSynthesize(sourceIndex, aft);
		}

		if (targetF0coeffs.length == 0 || sourceF0coeffs.length == 0) {
			return reSynthesize(sourceIndex, aft);
		}

		double[] targetF0 = Polynomial.generatePolynomialValues(targetF0coeffs, sourceF0.length, 0, 1);
		sourceF0 = Polynomial.generatePolynomialValues(sourceF0coeffs, sourceF0.length, 0, 1);

		assert targetF0.length == sourceF0.length;
		float[] tScalesArray = { 1.0f };
		float[] tScalesTimes = { 1.0f };
		float[] pScalesArray = new float[targetF0.length];
		float[] pScalesTimes = new float[targetF0.length];
		double skipSizeInSeconds = this.vIntonationReader.getSkipSizeInSeconds();
		double windowSizeInSeconds = this.vIntonationReader.getWindowSizeInSeconds();
		for (int i = 0; i < targetF0.length; i++) {
			pScalesArray[i] = (float) (targetF0[i] / sourceF0[i]);
			pScalesTimes[i] = (float) (i * skipSizeInSeconds + 0.5 * windowSizeInSeconds);
		}

		return synthesizeUsingF0Modification(sourceIndex, pScalesArray, pScalesTimes, tScalesArray, tScalesTimes, aft);
	}

	/**
	 * modify intonation contour using HNM technology
	 * 
	 * @param backchannelNumber
	 *            unit index of vocalization
	 * @param pScalesArray
	 *            pitch scales array
	 * @param pScalesTimes
	 *            pitch scale times
	 * @param tScalesArray
	 *            time scales array
	 * @param tScalesTimes
	 *            time scale times
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	private AudioInputStream synthesizeUsingF0Modification(int backchannelNumber, float[] pScalesArray, float[] pScalesTimes,
			float[] tScalesArray, float[] tScalesTimes, AudioFileFormat aft) throws SynthesisException {

		if (backchannelNumber > vHNMFeaturesReader.getNumberOfUnits()) {
			throw new IllegalArgumentException("requesting unit should not be more than number of units");
		}

		if (!f0ContourImposeSupport) {
			throw new SynthesisException("Mary configuration of this voice doesn't support intonation contour imposition");
		}

		BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams(tScalesArray, tScalesTimes, pScalesArray,
				pScalesTimes); // Prosody from modification factors above

		HntmSpeechSignal hnmSignal = vHNMFeaturesReader.getHntmSpeechSignal(backchannelNumber);
		HntmSynthesizer hs = new HntmSynthesizer();
		HntmSynthesizedSignal xhat = hs.synthesize(hnmSignal, null, null, pmodParams, null, analysisParams, synthesisParams);

		AudioFormat af;
		if (aft == null) { // default audio format
			float sampleRate = 16000.0F; // 8000,11025,16000,22050,44100
			int sampleSizeInBits = 16; // 8,16
			int channels = 1; // 1,2
			boolean signed = true; // true,false
			boolean bigEndian = false; // true,false
			af = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		} else {
			af = aft.getFormat();
		}

		double[] audio_double = xhat.output;
		/* Normalise the signal before return, this will normalise between 1 and -1 */
		double MaxSample = MathUtils.getAbsMax(audio_double);
		for (int i = 0; i < audio_double.length; i++) {
			audio_double[i] = 0.3 * (audio_double[i] / MaxSample);
		}

		// DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), aft.getFormat());
		DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);

		return oais;
	}

}
