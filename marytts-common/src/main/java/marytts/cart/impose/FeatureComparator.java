/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.cart.impose;

import java.util.Comparator;

import marytts.features.FeatureVector;

public class FeatureComparator implements Comparator<FeatureVector> {

	/** The index of the feature to be compared in the feature vector. */
	private int I = -1;
	private FeatureVector.FeatureType type = null;

	/**
	 * Constructor which initializes the feature index.
	 * 
	 * @param setI
	 *            The index of the feature to be compared on the next run of the comparator.
	 * @param featureType
	 *            feature Type
	 */
	public FeatureComparator(int setI, FeatureVector.FeatureType featureType) {
		setFeatureIdx(setI, featureType);
	}

	/**
	 * Accessor to set the feature index.
	 * 
	 * @param setI
	 *            The index of the feature to be compared on the next run of the comparator.
	 * @param featureType
	 *            feature type
	 */
	public void setFeatureIdx(int setI, FeatureVector.FeatureType featureType) {
		I = setI;
		type = featureType;
	}

	/**
	 * Access the index of the currently compared feature.
	 * 
	 * @return The index of the feature which the comparator currently deals with.
	 */
	public int getFeatureIdx() {
		return (I);
	}

	/**
	 * Compares two feature vectors according to their values at an internal index previously set by this.setFeatureIdx().
	 * 
	 * @param a
	 *            The first vector.
	 * @param b
	 *            The second vector.
	 * @return a negative integer, zero, or a positive integer as the feature at index I for v1 is less than, equal to, or greater
	 *         than the feature at index I for v2.
	 * 
	 *         check {@link #setFeatureIdx(int setI, FeatureVector.FeatureType featureType)} .
	 */
	public int compare(FeatureVector a, FeatureVector b) {
		switch (type) {
		case byteValued:
			return a.byteValuedDiscreteFeatures[I] - b.byteValuedDiscreteFeatures[I];
		case shortValued:
			int offset = a.byteValuedDiscreteFeatures.length;
			return a.shortValuedDiscreteFeatures[I - offset] - b.shortValuedDiscreteFeatures[I - offset];
		case floatValued:
			int offset2 = a.byteValuedDiscreteFeatures.length + a.shortValuedDiscreteFeatures.length;
			float delta = a.continuousFeatures[I - offset2] - b.continuousFeatures[I - offset2];
			if (delta > 0)
				return 1;
			else if (delta < 0)
				return -1;
			return 0;
		default:
			throw new IllegalStateException("compare called with feature index " + I + " and feature type " + type);
		}

	}

	/**
	 * The equals() method asked for by the Comparable interface. Returns true if the compared object is a FeatureComparator with
	 * the same internal index, false otherwise.
	 * 
	 * @param obj
	 *            obj
	 * @return false if obj is not instanceof featurecomparator, or if ((featurecomparator) obj).getfeatureIdx() is different from
	 *         this.I, true otherwise
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof FeatureComparator))
			return false;
		else if (((FeatureComparator) obj).getFeatureIdx() != this.I)
			return false;
		return (true);
	}
}

/**
 * An additional comparator for the unit indexes in the feature vectors.
 * 
 * @return a.unitIndex - b.unitIndex
 */
class UnitIndexComparator implements Comparator<FeatureVector> {

	public int compare(FeatureVector a, FeatureVector b) {
		return a.unitIndex - b.unitIndex;
	}

}
