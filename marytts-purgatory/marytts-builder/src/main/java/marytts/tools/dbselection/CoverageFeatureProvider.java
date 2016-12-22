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
 * This interface is used to access the coverage features for a given corpus.
 * 
 * @author marc
 * 
 */
public interface CoverageFeatureProvider {

	/**
	 * Get the total number of sentences provided by this provider.
	 * 
	 * @return number of sentences
	 */
	public int getNumSentences();

	/**
	 * Get the i-th coverage features from this provider.
	 * 
	 * @param i
	 *            the index number of the features to retrieve.
	 * @return the coverage features.
	 * @throws IndexOutOfBoundsException
	 *             if i is not in the range from 0 to getNumSentences()-1.
	 */
	public byte[] getCoverageFeatures(int i);

	/**
	 * Get the unique ID number of the i-th sentence. This may or may not be the same as i. However, it can be assumed that IDs
	 * are ordered: if i &gt; j, getID(i) &gt; getID(j).
	 * 
	 * @param i
	 *            i
	 * @return ID
	 */
	public int getID(int i);
}
