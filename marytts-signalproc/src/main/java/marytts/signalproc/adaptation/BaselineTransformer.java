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

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.prosody.PitchTransformationData;

/**
 * Baseline class for voice conversion transformation
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineTransformer {
	public BaselinePreprocessor preprocessor;
	public BaselineFeatureExtractor featureExtractor;
	public BaselinePostprocessor postprocessor;
	BaselineTransformerParams params;

	public BaselineTransformer(BaselinePreprocessor pp, BaselineFeatureExtractor fe, BaselinePostprocessor po,
			BaselineTransformerParams pa) {
		preprocessor = new BaselinePreprocessor(pp);
		featureExtractor = new BaselineFeatureExtractor(fe);
		postprocessor = new BaselinePostprocessor(po);
		params = new BaselineTransformerParams(pa);
	}

	// Baseline version does nothing, override in derived classes
	public boolean checkParams() throws IOException {
		return true;
	}

	// Baseline version does nothing, override in derived classes
	public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet) throws UnsupportedAudioFileException {

	}

	public static void transformOneItem(BaselineAdaptationItem inputItem, BaselineAdaptationItem outputItem,
			BaselineTransformerParams tfmParams, VocalTractTransformationFunction vttFunction,
			VocalTractTransformationData vtData, PitchTransformationData pMap) throws UnsupportedAudioFileException, IOException {

	}

	public static boolean isScalingsRequired(double[] pscales, double[] tscales, double[] escales, double[] vscales) {
		int i;
		for (i = 0; i < pscales.length; i++) {
			if (pscales[i] != 1.0)
				return true;
		}

		for (i = 0; i < tscales.length; i++) {
			if (tscales[i] != 1.0)
				return true;
		}

		for (i = 0; i < escales.length; i++) {
			if (escales[i] != 1.0)
				return true;
		}

		for (i = 0; i < vscales.length; i++) {
			if (vscales[i] != 1.0)
				return true;
		}

		return false;
	}
}
