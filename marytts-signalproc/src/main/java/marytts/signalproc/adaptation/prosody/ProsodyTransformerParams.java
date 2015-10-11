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
package marytts.signalproc.adaptation.prosody;

import marytts.signalproc.adaptation.BaselineParams;

/**
 * Parameters for prosody transformation.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class ProsodyTransformerParams extends BaselineParams {

	public static final int CUSTOM_TRANSFORMATION = -10;
	public static final int NO_TRANSFORMATION = 0;

	public int energyTransformationMethod; // Energy contour transformation method

	// //
	public int durationTransformationMethod; // Duration transformation method

	// Duration transformation method types
	public static final int PHONEME_DURATIONS = 1;
	public static final int TRIPHONE_DURATIONS = 2;
	public static final int SENTENCE_DURATION = 3;
	public static final int GLOBAL_AVERAGE = 4;
	//

	// //
	public int pitchTransformationMethod; // Pitch transformaiton method
	// Pitch transformaiton method types
	public static final int USE_ONLY_PSCALES = -1;
	//

	// Global transformation types
	public static final int GLOBAL_MEAN = 1;
	public static final int GLOBAL_STDDEV = 2;
	public static final int GLOBAL_RANGE = 3;
	public static final int GLOBAL_SLOPE = 4;
	public static final int GLOBAL_INTERCEPT = 5;
	public static final int GLOBAL_MEAN_STDDEV = 6;
	public static final int GLOBAL_MEAN_SLOPE = 7;
	public static final int GLOBAL_INTERCEPT_STDDEV = 8;
	public static final int GLOBAL_INTERCEPT_SLOPE = 9;
	//

	// Local transformations (i.e. sentence level, based on median of N-best sentence matches in the training database)
	public static final int SENTENCE_MEAN = 21;
	public static final int SENTENCE_STDDEV = 22;
	public static final int SENTENCE_RANGE = 23;
	public static final int SENTENCE_SLOPE = 24;
	public static final int SENTENCE_INTERCEPT = 25;
	public static final int SENTENCE_MEAN_STDDEV = 26;
	public static final int SENTENCE_MEAN_SLOPE = 27;
	public static final int SENTENCE_INTERCEPT_STDDEV = 28;
	public static final int SENTENCE_INTERCEPT_SLOPE = 29;
	public static final int FULL_CONTOUR = 30;
	//

	// These are for GLOBAL_XXX cases of pitchTransformationMethod only
	public boolean isUseInputMeanPitch; // For GLOBAL tfms: Estimate mean from input f0s? Otherwise from codebook
	public boolean isUseInputStdDevPitch; // For GLOBAL tfms: Estimate std. dev. from input f0s? Otherwise from codebook
	public boolean isUseInputRangePitch; // For GLOBAL tfms: Estimate range from input f0s? Otherwise from codebook
	public boolean isUseInputInterceptPitch; // For GLOBAL tfms: Estimate intercept from input f0s? Otherwise from codebook
	public boolean isUseInputSlopePitch; // For GLOBAL tfms: Estimate slope from input f0s? Otherwise from codebook
	// //

	public int pitchStatisticsType; // Type for pitch statistics to use

	// ///

	public ProsodyTransformerParams() {
		pitchTransformationMethod = NO_TRANSFORMATION;
		durationTransformationMethod = NO_TRANSFORMATION;
		energyTransformationMethod = NO_TRANSFORMATION;

		isUseInputMeanPitch = false;
		isUseInputStdDevPitch = false;
		isUseInputRangePitch = false;
		isUseInputInterceptPitch = false;
		isUseInputSlopePitch = false;

		pitchStatisticsType = PitchStatistics.DEFAULT_STATISTICS;
	}

	public ProsodyTransformerParams(ProsodyTransformerParams existing) {
		pitchTransformationMethod = existing.pitchTransformationMethod;
		durationTransformationMethod = existing.durationTransformationMethod;
		energyTransformationMethod = existing.energyTransformationMethod;

		isUseInputMeanPitch = existing.isUseInputMeanPitch;
		isUseInputStdDevPitch = existing.isUseInputStdDevPitch;
		isUseInputRangePitch = existing.isUseInputRangePitch;
		isUseInputInterceptPitch = existing.isUseInputInterceptPitch;
		isUseInputSlopePitch = existing.isUseInputSlopePitch;

		pitchStatisticsType = existing.pitchStatisticsType;
	}
}
