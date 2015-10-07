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

import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.htsengine.HMMData;
import marytts.htsengine.HTSPStream;
import marytts.htsengine.HTSVocoder;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;

/**
 * MLSA Synthesis technology to synthesize vocalizations
 * 
 * @author Sathish Pammi
 */

public class MLSASynthesisTechnology extends VocalizationSynthesisTechnology {

	protected MLSAFeatureFileReader vMLSAFeaturesReader;
	protected VocalizationIntonationReader vIntonationReader;
	protected HMMData htsData;
	protected HTSVocoder par2speech;
	protected boolean imposePolynomialContour = true;

	public MLSASynthesisTechnology(String mlsaFeatureFile, String intonationFeatureFile, String mixedExcitationFile,
			boolean imposePolynomialContour) throws MaryConfigurationException {

		try {
			vMLSAFeaturesReader = new MLSAFeatureFileReader(mlsaFeatureFile);
			if (intonationFeatureFile != null) {
				this.vIntonationReader = new VocalizationIntonationReader(intonationFeatureFile);
			} else {
				this.vIntonationReader = null;
			}
		} catch (IOException ioe) {
			throw new MaryConfigurationException("Problem with loading mlsa feature file", ioe);
		}

		if (vMLSAFeaturesReader.getNumberOfUnits() <= 0) {
			throw new MaryConfigurationException("mlsa feature file doesn't contain any data");
		}

		this.imposePolynomialContour = imposePolynomialContour;

		try {
			htsData = new HMMData();
			htsData.setUseMixExc(true);
			htsData.setUseFourierMag(false); /* use Fourier magnitudes for pulse generation */
			FileInputStream mixedFiltersStream = new FileInputStream(mixedExcitationFile);
			htsData.setNumFilters(5);
			htsData.setOrderFilters(48);
			htsData.readMixedExcitationFilters(mixedFiltersStream);
			htsData.setPdfStrStream(null);
			// [min][max]
			htsData.setF0Std(1.0); // variable for f0 control, multiply f0 [1.0][0.0--5.0]
			htsData.setF0Mean(0.0); // variable for f0 control, add f0 [0.0][0.0--100.0]
		} catch (Exception e) {
			throw new MaryConfigurationException("htsData initialization failed.. ", e);
		}

		par2speech = new HTSVocoder();

	}

	/**
	 * Synthesize given vocalization using MLSA vocoder
	 * 
	 * @param backchannelNumber
	 *            unit index
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 */
	public AudioInputStream synthesize(int backchannelNumber, AudioFileFormat aft) throws SynthesisException {

		if (backchannelNumber > vMLSAFeaturesReader.getNumberOfUnits()) {
			throw new IllegalArgumentException("requesting unit should not be more than number of units");
		}

		if (backchannelNumber < 0) {
			throw new IllegalArgumentException("requesting unit index should not be less than zero");
		}

		double[] lf0 = vMLSAFeaturesReader.getUnitLF0(backchannelNumber);
		boolean[] voiced = vMLSAFeaturesReader.getVoicedFrames(backchannelNumber);
		double[][] mgc = vMLSAFeaturesReader.getUnitMGCs(backchannelNumber);
		double[][] strengths = vMLSAFeaturesReader.getUnitStrengths(backchannelNumber);

		return synthesizeUsingMLSAVocoder(mgc, strengths, lf0, voiced, aft);
	}

	/**
	 * Re-synthesize given vocalization using MLSA (it is same as synthesize())
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
		return synthesize(backchannelNumber, aft);
	}

	/**
	 * Impose target intonation contour on given vocalization using MLSA technology
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

		if (sourceIndex > vMLSAFeaturesReader.getNumberOfUnits() || targetIndex > vMLSAFeaturesReader.getNumberOfUnits()) {
			throw new IllegalArgumentException("requesting unit should not be more than number of units");
		}

		if (sourceIndex < 0 || targetIndex < 0) {
			throw new IllegalArgumentException("requesting unit index should not be less than zero");
		}

		boolean[] voiced = vMLSAFeaturesReader.getVoicedFrames(sourceIndex);
		double[][] mgc = vMLSAFeaturesReader.getUnitMGCs(sourceIndex);
		double[][] strengths = vMLSAFeaturesReader.getUnitStrengths(sourceIndex);

		double[] lf0 = null;

		if (!this.imposePolynomialContour) {
			lf0 = MathUtils.arrayResize(vMLSAFeaturesReader.getUnitLF0(targetIndex), voiced.length);
		} else {
			double[] targetF0coeffs = this.vIntonationReader.getIntonationCoeffs(targetIndex);
			double[] sourceF0coeffs = this.vIntonationReader.getIntonationCoeffs(sourceIndex);
			if (targetF0coeffs == null || sourceF0coeffs == null) {
				return reSynthesize(sourceIndex, aft);
			}

			if (targetF0coeffs.length == 0 || sourceF0coeffs.length == 0) {
				return reSynthesize(sourceIndex, aft);
			}
			double[] f0Contour = Polynomial.generatePolynomialValues(targetF0coeffs, voiced.length, 0, 1);
			lf0 = new double[f0Contour.length];
			for (int i = 0; i < f0Contour.length; i++) {
				lf0[i] = Math.log(f0Contour[i]);
			}
		}

		return synthesizeUsingMLSAVocoder(mgc, strengths, lf0, voiced, aft);
	}

	/**
	 * Synthesize using MLSA vocoder
	 * 
	 * @param mgc
	 *            mgc features
	 * @param strengths
	 *            strengths
	 * @param lf0
	 *            logf0 features
	 * @param voiced
	 *            voiced frames
	 * @param aft
	 *            audio file format
	 * @return AudioInputStream of synthesized vocalization
	 * @throws SynthesisException
	 *             if failed to synthesize vocalization
	 * @throws SynthesisException
	 *             if log f0 values for voiced frames are not in the natural pitch range ( 30 to 1000 in Hertzs)
	 */
	private AudioInputStream synthesizeUsingMLSAVocoder(double[][] mgc, double[][] strengths, double[] lf0, boolean[] voiced,
			AudioFileFormat aft) throws SynthesisException {

		assert lf0.length == mgc.length;
		assert mgc.length == strengths.length;

		for (int i = 0; i < lf0.length; i++) {
			if (lf0[i] > 0 && (Math.log(30) > lf0[i] || Math.log(1000) < lf0[i])) {
				throw new SynthesisException("given log f0 values should be in the natural pitch range ");
			}
		}

		int mcepVsize = vMLSAFeaturesReader.getMGCVectorSize();
		int lf0Vsize = vMLSAFeaturesReader.getLF0VectorSize();
		int strVsize = vMLSAFeaturesReader.getSTRVectorSize();

		HTSPStream lf0Pst = null;
		HTSPStream mcepPst = null;
		HTSPStream strPst = null;

		try {
			lf0Pst = new HTSPStream(lf0Vsize * 3, lf0.length, HMMData.FeatureType.LF0, 0); // multiplied by 3 required for
																							// real-time synthesis
			mcepPst = new HTSPStream(mcepVsize * 3, mgc.length, HMMData.FeatureType.MGC, 0);
			strPst = new HTSPStream(strVsize * 3, strengths.length, HMMData.FeatureType.STR, 0);
		} catch (Exception e) {
			throw new SynthesisException("HTSPStream initialiaztion failed.. " + e);
		}

		int lf0VoicedFrame = 0;
		for (int i = 0; i < lf0.length; i++) {
			if (voiced[i]) {
				lf0Pst.setPar(lf0VoicedFrame, 0, lf0[i]);
				lf0VoicedFrame++;
			}

			for (int j = 0; j < mcepPst.getOrder(); j++) {
				mcepPst.setPar(i, j, mgc[i][j]);
			}

			for (int j = 0; j < strPst.getOrder(); j++) {
				strPst.setPar(i, j, strengths[i][j]);
			}
		}

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

		double[] audio_double = null;
		try {
			audio_double = par2speech.htsMLSAVocoder(lf0Pst, mcepPst, strPst, null, voiced, htsData, null);
		} catch (Exception e) {
			throw new SynthesisException("MLSA vocoding failed .. " + e);
		}

		/* Normalise the signal before return, this will normalise between 1 and -1 */
		double MaxSample = MathUtils.getAbsMax(audio_double);
		for (int i = 0; i < audio_double.length; i++) {
			audio_double[i] = 0.3 * (audio_double[i] / MaxSample);
		}

		return new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);
	}

}
