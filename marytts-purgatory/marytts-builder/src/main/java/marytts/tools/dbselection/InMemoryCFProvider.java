/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.tools.dbselection;

/**
 * @author marc
 *
 */
public class InMemoryCFProvider implements CoverageFeatureProvider {

	private byte[][] data;
	private int[] ids;

	/**
	 * Instantiate an in-memory coverage feature provider
	 * 
	 * @param data
	 *            the data to provide.
	 * @param ids
	 *            optionally, an array of unique id numbers. if this is null, the index number is used as the id number.
	 * @throws NullPointerException
	 *             if data is null
	 * @throws IllegalArgumentException
	 *             if ids is given but has different length than data.
	 */
	public InMemoryCFProvider(byte[][] data, int[] ids) {
		if (data == null) {
			throw new NullPointerException("Null data");
		}
		this.data = data;
		this.ids = ids;
		if (ids != null && data.length != ids.length) {
			throw new IllegalArgumentException("ID array does not have same length as data vector");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.dbselection.CoverageFeatureProvider#getCoverageFeatures(int)
	 */
	@Override
	public byte[] getCoverageFeatures(int i) {
		return data[i];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.dbselection.CoverageFeatureProvider#getNumSentences()
	 */
	@Override
	public int getNumSentences() {
		return data.length;
	}

	@Override
	public int getID(int i) {
		if (ids != null) {
			return ids[i];
		}
		return i;
	}

}
