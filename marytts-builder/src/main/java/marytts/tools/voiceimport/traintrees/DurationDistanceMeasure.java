/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.tools.voiceimport.traintrees;

import java.io.IOException;

import marytts.features.FeatureVector;
import marytts.unitselection.data.UnitFileReader;

/**
 * @author marc
 * 
 */
public class DurationDistanceMeasure implements DistanceMeasure {
	private UnitFileReader units;

	public DurationDistanceMeasure(UnitFileReader units) throws IOException {
		this.units = units;
	}

	/**
	 * Compute the distance between the f0 contours corresponding to the given feature vectors. From the feature vectors, only
	 * their unit index number is used.
	 * 
	 * @param fv1
	 *            fv1
	 * @param fv2
	 *            fv2
	 * @return math.abs(d1 - d2)
	 * @see marytts.tools.voiceimport.traintrees.DistanceMeasure#distance(marytts.features.FeatureVector,
	 *      marytts.features.FeatureVector)
	 */
	public float distance(FeatureVector fv1, FeatureVector fv2) {
		float d1 = units.getUnit(fv1.getUnitIndex()).duration / (float) units.getSampleRate();
		float d2 = units.getUnit(fv2.getUnitIndex()).duration / (float) units.getSampleRate();
		return Math.abs(d1 - d2);
	}

	/**
	 * Compute the distance between the f0 contours corresponding to the given feature vectors. From the feature vectors, only
	 * their unit index number is used.
	 * 
	 * @param fv1
	 *            fv1
	 * @param fv2
	 *            fv2
	 * @return diff * diff
	 * @see marytts.tools.voiceimport.traintrees.DistanceMeasure#distance(marytts.features.FeatureVector,
	 *      marytts.features.FeatureVector)
	 */
	public float squaredDistance(FeatureVector fv1, FeatureVector fv2) {
		float d1 = units.getUnit(fv1.getUnitIndex()).duration / (float) units.getSampleRate();
		float d2 = units.getUnit(fv2.getUnitIndex()).duration / (float) units.getSampleRate();
		float diff = d1 - d2;
		return diff * diff;
	}
}
