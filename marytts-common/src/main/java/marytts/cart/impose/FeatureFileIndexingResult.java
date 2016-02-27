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

import marytts.features.FeatureVector;

/**
 * A helper class to return the query results from the FeatureFileIndexer.
 * 
 * @author sacha
 * 
 */
public class FeatureFileIndexingResult {
	public FeatureVector[] v = null;
	public int level = -1;

	public FeatureFileIndexingResult(FeatureVector[] setV, int setLevel) {
		this.v = setV;
		this.level = setLevel;
	}

	public int[] getUnitIndexes() {
		int[] ret = new int[v.length];
		for (int i = 0; i < v.length; i++) {
			ret[i] = v[i].getUnitIndex();
		}
		return (ret);
	}
}
