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

import java.util.Iterator;

/**
 * This class represents the section of a feature file which constitutes a sentence.
 * 
 * @author marc
 *
 */
public class Sentence implements Iterable<Syllable> {

	private FeatureFileReader features;
	private int firstUnitIndex;
	private int lastUnitIndex;

	public Sentence(FeatureFileReader features, int firstUnitIndex, int lastUnitIndex) {
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

	public Iterator<Syllable> iterator() {
		return new SyllableIterator(features, firstUnitIndex, lastUnitIndex);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Sentence)) {
			return false;
		}
		Sentence other = (Sentence) o;
		return features.equals(other.features) && firstUnitIndex == other.firstUnitIndex && lastUnitIndex == other.lastUnitIndex;
	}
}
