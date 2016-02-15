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

package marytts.unitselection.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * @author marc
 *
 */
public class SyllableIterator implements Iterator<Syllable> {

	private FeatureFileReader features;
	private int fromUnitIndex;
	private int toUnitIndex;

	private final int fiPhone;
	private final byte fvPhone_0;
	private final byte fvPhone_Silence;
	private final int fiSylStart;
	private final int fiSylEnd;
	private final boolean isHalfphone;
	private final int fiLR;
	private final int fvLR_L;
	private final int fvLR_R;

	private int i;
	private Syllable nextSyllable = null;

	/**
	 * Create a syllable iterator over the given feature file, starting from the given fromUnitIndex and reaching up to (and
	 * including) the given toUnitIndex
	 * 
	 * @param features
	 *            features
	 * @param fromUnitIndex
	 *            fromUnitIndex
	 * @param toUnitIndex
	 *            toUnitIndex
	 */
	public SyllableIterator(FeatureFileReader features, int fromUnitIndex, int toUnitIndex) {
		this.features = features;
		this.fromUnitIndex = fromUnitIndex;
		this.toUnitIndex = toUnitIndex;

		FeatureDefinition featureDefinition = features.getFeatureDefinition();
		fiPhone = featureDefinition.getFeatureIndex("phone");
		fvPhone_0 = featureDefinition.getFeatureValueAsByte(fiPhone, "0");
		fvPhone_Silence = featureDefinition.getFeatureValueAsByte(fiPhone, "_");
		fiSylStart = featureDefinition.getFeatureIndex("segs_from_syl_start");
		fiSylEnd = featureDefinition.getFeatureIndex("segs_from_syl_end");
		String halfphoneFeature = "halfphone_lr";
		if (featureDefinition.hasFeature(halfphoneFeature)) {
			isHalfphone = true;
			fiLR = featureDefinition.getFeatureIndex(halfphoneFeature);
			fvLR_L = featureDefinition.getFeatureValueAsByte(fiLR, "L");
			fvLR_R = featureDefinition.getFeatureValueAsByte(fiLR, "R");
		} else {
			isHalfphone = false;
			fiLR = fvLR_L = fvLR_R = 0;
		}

		i = fromUnitIndex;
	}

	public synchronized boolean hasNext() {
		if (nextSyllable == null) {
			prepareNextSyllable();
		}
		return nextSyllable != null;
	}

	public synchronized Syllable next() {
		if (nextSyllable == null) {
			prepareNextSyllable();
		}
		if (nextSyllable == null) {
			// no more syllables
			throw new NoSuchElementException("no more syllables!");
		}
		Syllable retval = nextSyllable;
		nextSyllable = null;
		return retval;
	}

	public void remove() {
		throw new UnsupportedOperationException("This iterator cannot remove syllables");
	}

	private void prepareNextSyllable() {
		if (nextSyllable != null) {
			return;
		}
		if (i > toUnitIndex) {
			return;
		}
		// if we get here, then i is the index of a unit before or at a syllable start
		while (i <= toUnitIndex && !isSyllableStart(i)) {
			i++;
		}
		if (i > toUnitIndex) {
			return;
		}
		int iSyllableStart = i;
		while (i <= toUnitIndex && !isSyllableEnd(i)) {
			i++;
		}
		if (i > toUnitIndex) {
			return;
		}
		int iSyllableEnd = i;
		nextSyllable = new Syllable(features, iSyllableStart, iSyllableEnd);
	}

	private boolean isSyllableStart(int index) {
		FeatureVector fv = features.getFeatureVector(index);

		return fv.getByteFeature(fiPhone) != fvPhone_0 // not an edge unit
				&& fv.getByteFeature(fiPhone) != fvPhone_Silence // not silence
				&& fv.getByteFeature(fiSylStart) == 0 // first segment in syllable
				&& (!isHalfphone || fv.getByteFeature(fiLR) == fvLR_L); // if halfphone, it's the left half
	}

	private boolean isSyllableEnd(int index) {
		FeatureVector fv = features.getFeatureVector(index);

		return fv.getByteFeature(fiPhone) != fvPhone_0 // not an edge unit
				&& fv.getByteFeature(fiPhone) != fvPhone_Silence // not silence
				&& fv.getByteFeature(fiSylEnd) == 0 // last segment in syllable
				&& (!isHalfphone || fv.getByteFeature(fiLR) == fvLR_R); // if halfphone, it's the right half
	}
}
