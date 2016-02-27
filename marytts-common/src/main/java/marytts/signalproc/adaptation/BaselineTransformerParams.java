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

import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.adaptation.smoothing.SmoothingDefinitions;
import marytts.signalproc.analysis.EnergyFileHeader;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.signalproc.analysis.PitchFileHeader;

/**
 * Baseline class for voice conversion transformation parameters All specific implementations of transformation stage of a given
 * voice conversion algorithm should use a parameter set that is derived from this class
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineTransformerParams extends BaselineParams {
	public String inputFolder; // Folder of input files to be transformed
	public String outputBaseFolder; // Base folder of output files
	public String outputFolder; // Individual folder of output files (Note that this is automatically generated using parameter
								// values)
	public String outputFolderInfoString; // An information string to be appended as a prefix to the output folder
	public boolean isSourceToTarget; // if true source is transformed to target, else target is transformed to source
	public boolean isDisplayProcessingFrameCount; // Display processed frame indices while transforming?

	public ProsodyTransformerParams prosodyParams;

	public LsfFileHeader lsfParams;
	public PitchFileHeader ptcParams;
	public EnergyFileHeader energyParams;
	public MfccFileHeader mfccParams;

	public boolean isForcedAnalysis;
	public boolean isVocalTractTransformation;

	public boolean isSeparateProsody;
	public boolean isSaveVocalTractOnlyVersion;
	public boolean isFixedRateVocalTractConversion;

	public boolean isTemporalSmoothing;
	public int smoothingMethod;
	public int smoothingNumNeighbours;
	public int smoothingState;
	public String smoothedVocalTractFile;

	public boolean isSourceVocalTractSpectrumFromModel;
	public boolean isResynthesizeVocalTractFromSourceModel;
	public boolean isVocalTractMatchUsingTargetModel;
	public boolean isLsfsFromTargetFile;

	public String pitchMappingFile;

	// For copy-paste prosody
	public boolean isPitchFromTargetFile;
	public int pitchFromTargetMethod;
	public boolean isDurationFromTargetFile;
	public int durationFromTargetMethod;
	public boolean isEnergyFromTargetFile;

	public int targetAlignmentFileType;
	public static final int LABELS = 1;
	public static final int FESTIVAL_UTT = 2;
	//

	public static final double MINIMUM_ALLOWED_PITCH_SCALE = 0.6;
	public static final double MAXIMUM_ALLOWED_PITCH_SCALE = 2.5;
	public static final double MINIMUM_ALLOWED_TIME_SCALE = 0.6;
	public static final double MAXIMUM_ALLOWED_TIME_SCALE = 2.5;

	public BaselineTransformerParams() {
		inputFolder = "";
		outputBaseFolder = "";
		outputFolder = "";
		outputFolderInfoString = "";
		isSourceToTarget = true;
		isDisplayProcessingFrameCount = false;

		prosodyParams = new ProsodyTransformerParams();
		lsfParams = new LsfFileHeader();
		ptcParams = new PitchFileHeader();
		energyParams = new EnergyFileHeader();
		mfccParams = new MfccFileHeader();

		isForcedAnalysis = false;
		isSourceVocalTractSpectrumFromModel = true;
		isVocalTractTransformation = true;

		isResynthesizeVocalTractFromSourceModel = false;
		isVocalTractMatchUsingTargetModel = false;

		isSeparateProsody = true;
		isSaveVocalTractOnlyVersion = true;
		isFixedRateVocalTractConversion = true;

		isTemporalSmoothing = false;
		smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
		smoothingNumNeighbours = SmoothingDefinitions.DEFAULT_NUM_NEIGHBOURS;
		smoothingState = SmoothingDefinitions.NONE;
		smoothedVocalTractFile = "";

		isSourceVocalTractSpectrumFromModel = false;
		isResynthesizeVocalTractFromSourceModel = false;
		isVocalTractMatchUsingTargetModel = false;

		pitchMappingFile = "";

		isPitchFromTargetFile = false;
		pitchFromTargetMethod = ProsodyTransformerParams.SENTENCE_MEAN_STDDEV;
		isDurationFromTargetFile = false;
		durationFromTargetMethod = ProsodyTransformerParams.SENTENCE_DURATION;
		isEnergyFromTargetFile = false;
		isLsfsFromTargetFile = false;

		targetAlignmentFileType = BaselineTransformerParams.LABELS;
	}

	public BaselineTransformerParams(BaselineTransformerParams existing) {
		inputFolder = existing.inputFolder;
		outputBaseFolder = existing.outputBaseFolder;
		outputFolder = existing.outputFolder;
		outputFolderInfoString = existing.outputFolderInfoString;
		isSourceToTarget = existing.isSourceToTarget;
		isDisplayProcessingFrameCount = existing.isDisplayProcessingFrameCount;

		prosodyParams = new ProsodyTransformerParams(existing.prosodyParams);
		lsfParams = new LsfFileHeader(existing.lsfParams);
		ptcParams = new PitchFileHeader(existing.ptcParams);
		energyParams = new EnergyFileHeader(existing.energyParams);
		mfccParams = new MfccFileHeader(existing.mfccParams);

		isForcedAnalysis = existing.isForcedAnalysis;
		isVocalTractTransformation = existing.isVocalTractTransformation;

		isSeparateProsody = existing.isSeparateProsody;
		isSaveVocalTractOnlyVersion = existing.isSaveVocalTractOnlyVersion;
		isFixedRateVocalTractConversion = existing.isFixedRateVocalTractConversion;

		isTemporalSmoothing = existing.isTemporalSmoothing;
		smoothingMethod = existing.smoothingMethod;
		smoothingNumNeighbours = existing.smoothingNumNeighbours;
		smoothingState = existing.smoothingState;
		smoothedVocalTractFile = existing.smoothedVocalTractFile;

		isSourceVocalTractSpectrumFromModel = existing.isSourceVocalTractSpectrumFromModel;
		isResynthesizeVocalTractFromSourceModel = existing.isResynthesizeVocalTractFromSourceModel;
		isVocalTractMatchUsingTargetModel = existing.isVocalTractMatchUsingTargetModel;

		pitchMappingFile = existing.pitchMappingFile;

		isPitchFromTargetFile = existing.isPitchFromTargetFile;
		pitchFromTargetMethod = existing.pitchFromTargetMethod;
		isDurationFromTargetFile = existing.isDurationFromTargetFile;
		durationFromTargetMethod = existing.durationFromTargetMethod;
		isEnergyFromTargetFile = existing.isEnergyFromTargetFile;
		isLsfsFromTargetFile = existing.isLsfsFromTargetFile;

		targetAlignmentFileType = existing.targetAlignmentFileType;
	}
}
