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
package marytts.signalproc.adaptation;

import marytts.util.string.StringUtils;

/**
 * This class keeps information on each specific training item For example, a training item for a sentence based voice conversion
 * training database could be a wav file, the corresponding text transcription, label file, pitch contour file, etc. The training
 * set is a collection of BaseTrainingItem objects
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineAdaptationItem {
	// A decomposition of the file into its sinus+noise+transients+residual components
	// audioFile = sinesFile+noiseFile+transientsFile+residualFile
	public String sinesFile; // Sinusoids
	public String noiseFile; // Noise
	public String transientsFile; // Transients
	public String residualFile; // Residual (what remains after all model based decomposition)
	//

	public String labelFile; // Labels
	public String pitchFile; // f0 contour in binary format (.ptc)
	public String f0File; // f0 contour in ESPS format (.f0)
	public String pitchMarkFile; // Pitch marks
	public String energyFile; // Energy contour
	public String textFile; // Text
	public String rawMfccFile; // Raw mel frequency cepstral coefficients
	public String mfccFile; // Mel frequency cepstral coefficients
	public String lsfFile; // Line spectral frequencies
	public String lpcFile; // Linear prediction coefficients
	public String lpResidualFile; // Time-domain residual waveform after LP inverse filtering
	public String cepsFile; // Cepstrum coefficients file
	public String eggFile; // Electro-glottograph file

	// Mary TTS outputs to specify target features for tests, transplantation, etc
	public String targetFestivalUttFile; // FESTIVAL_UTT output which contains target timing and f0s (also the labels)
											// This needs to be mapped with actual labels (i.e. labelFile) and f0s (pitchFile) to
											// obtain required prosody modification factors

	public String targetLabelFile; // Target labels for mapping
	public String targetPitchFile; // Target pitch file, to be used in transplantations
	public String targetF0File; // Target pitch file, to be used in transplantations
	public String targetEnergyFile; // Target energy file, to be used in transplantations
	public String targetWavFile; // Target waveform file
	//

	public String audioFile; // Original waveform file

	public BaselineAdaptationItem() {

	}

	public BaselineAdaptationItem(BaselineAdaptationItem existing) {
		sinesFile = existing.sinesFile;
		noiseFile = existing.noiseFile;
		transientsFile = existing.transientsFile;
		residualFile = existing.residualFile;

		labelFile = existing.labelFile;
		pitchFile = existing.pitchFile;
		f0File = existing.f0File;
		pitchMarkFile = existing.pitchMarkFile;
		energyFile = existing.energyFile;
		textFile = existing.textFile;
		rawMfccFile = existing.rawMfccFile;
		mfccFile = existing.mfccFile;
		lsfFile = existing.lsfFile;
		lpcFile = existing.lpcFile;
		lpResidualFile = existing.lpResidualFile;
		cepsFile = existing.cepsFile;
		eggFile = existing.eggFile;

		targetFestivalUttFile = existing.targetFestivalUttFile;
		targetLabelFile = existing.targetLabelFile;
		targetPitchFile = existing.targetPitchFile;
		targetF0File = existing.targetF0File;
		targetEnergyFile = existing.targetEnergyFile;
		targetWavFile = existing.targetWavFile;

		audioFile = existing.audioFile;
	}

	public void setFromWavFilename(String referenceFilename) {
		audioFile = referenceFilename;

		sinesFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.SINUSOID_EXTENSION_DEFAULT); // Sinusoids
		noiseFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.NOISE_EXTENSION_DEFAULT); // Noise
		transientsFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TRANSIENT_EXTENSION_DEFAULT); // Transients
		residualFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.RESIDUAL_EXTENSION_DEFAULT); // Residual (what
																													// remains
																													// after all
																													// model based
																													// decomposition)

		labelFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.LABEL_EXTENSION_DEFAULT); // Labels
		pitchFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.PITCH_EXTENSION_DEFAULT); // f0 contour
		f0File = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.F0_EXTENSION_DEFAULT); // f0 contour
		pitchMarkFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.PITCHMARK_EXTENSION_DEFAULT); // Pitch marks
		energyFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.ENERGY_EXTENSION_DEFAULT); // Energy contour
		textFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TEXT_EXTENSION_DEFAULT); // Text
		mfccFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.MFCC_EXTENSION_DEFAULT); // Mel frequency cepstral
																											// coefficients
		rawMfccFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.RAWMFCC_EXTENSION_DEFAULT);
		lsfFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.LSF_EXTENSION_DEFAULT); // Line spectral
																										// frequencies
		lpcFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.LPC_EXTENSION_DEFAULT); // Linear prediction
																										// coefficients
		lpResidualFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.LPRESIDUAL_EXTENSION_DEFAULT); // Time-domain
																														// residual
																														// waveform
																														// after
																														// LP
																														// inverse
																														// filtering
		cepsFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.CEPSTRUM_EXTENSION_DEFAULT); // Cepstrum
																												// coefficients
																												// file
		eggFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.EGG_EXTENSION_DEFAULT); // Electro-glottograph file

		targetFestivalUttFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TARGETFESTIVALUTT_EXTENSION_DEFAULT); // FESTIVAL_UTT
																																	// file
		targetLabelFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TARGETLABEL_EXTENSION_DEFAULT); // Target
																														// labels
																														// for
																														// mapping
		targetPitchFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TARGETPITCH_EXTENSION_DEFAULT); // Target
																														// pitch
																														// for
																														// copy
																														// synthesis
		targetF0File = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TARGETF0_EXTENSION_DEFAULT); // Target pitch
																													// for copy
																													// synthesis
		targetEnergyFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TARGETENERGY_EXTENSION_DEFAULT); // Target
																															// energy
																															// file,
																															// to
																															// be
																															// used
																															// in
																															// transplantations
		targetWavFile = StringUtils.modifyExtension(audioFile, BaselineAdaptationSet.TARGETWAV_EXTENSION_DEFAULT); // Target
																													// waveform
																													// file
	}
}
