/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.unitselection.data;

import marytts.features.FeatureVector;

/**
 * This class represents the section of a feature file which constitutes a sentence.
 * 
 * @author marc
 *
 */
public class Syllable {

	private FeatureFileReader features;
	private int firstUnitIndex;
	private int lastUnitIndex;

	public Syllable(FeatureFileReader features, int firstUnitIndex, int lastUnitIndex) {
		this.features = features;
		this.firstUnitIndex = firstUnitIndex;
		this.lastUnitIndex = lastUnitIndex;
	}

	public int getFirstUnitIndex() {
		return firstUnitIndex;
	}

	public int getLastUnitIndex() {
		return lastUnitIndex;
	}

	/**
	 * Seek for the syllable nucleus (with feature "ph_vc" == "+") from first to last unit; if none is found, return the last unit
	 * in the syllable
	 * 
	 * @return i if fv.getByteFeature(fiVowel) is fvVowel_Plus, lastUnitIndex otherwise
	 */
	public int getSyllableNucleusIndex() {
		int fiVowel = features.getFeatureDefinition().getFeatureIndex("ph_vc");
		byte fvVowel_Plus = features.getFeatureDefinition().getFeatureValueAsByte(fiVowel, "+");
		for (int i = firstUnitIndex; i <= lastUnitIndex; i++) {
			FeatureVector fv = features.getFeatureVector(i);
			if (fv.getByteFeature(fiVowel) == fvVowel_Plus) {
				return i;
			}
		}
		return lastUnitIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Syllable)) {
			return false;
		}
		Syllable other = (Syllable) o;
		return features.equals(other.features) && firstUnitIndex == other.firstUnitIndex && lastUnitIndex == other.lastUnitIndex;
	}
}
