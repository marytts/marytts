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
public class DatabaseCFProvider implements CoverageFeatureProvider {

	private DBHandler dbHandler;
	private int[] sentenceIDs;

	public DatabaseCFProvider(DBHandler dbHandler, String condition) {
		this.dbHandler = dbHandler;
		this.sentenceIDs = dbHandler.getIdListOfType("dbselection", condition);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.dbselection.CoverageFeatureProvider#getCoverageFeatures(int)
	 */
	@Override
	public byte[] getCoverageFeatures(int i) {
		return dbHandler.getFeatures(sentenceIDs[i]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.dbselection.CoverageFeatureProvider#getNumSentences()
	 */
	@Override
	public int getNumSentences() {
		return sentenceIDs.length;
	}

	@Override
	public int getID(int i) {
		return sentenceIDs[i];
	}

	/**
	 * Get in-memory access to a subset of the features. The features will be accessible from the in-memory provider with index
	 * numbers 0..(len-1).
	 * 
	 * @param off
	 *            first index of features to be made available in the in-memory provider.
	 * @param len
	 *            number of features to provide.
	 * @return an in-memory context feature provider.
	 * @throws IndexOutOfBoundsException
	 *             if off or off+len-1 are outside the range from 0 to getNumSentences()-1.
	 */
	public InMemoryCFProvider getFeaturesInMemory(int off, int len) {
		int[] ids = new int[len];
		System.arraycopy(sentenceIDs, off, ids, 0, len);
		byte[][] subset = dbHandler.getFeaturesBulk(ids);
		return new InMemoryCFProvider(subset, ids);
	}
}
