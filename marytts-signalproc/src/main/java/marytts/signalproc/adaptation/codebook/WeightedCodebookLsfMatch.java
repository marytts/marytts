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
package marytts.signalproc.adaptation.codebook;

import marytts.signalproc.adaptation.VocalTractMatch;

/**
 * 
 * Wrapper class for a single weighted codebook entry LSF match
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookLsfMatch extends VocalTractMatch {
	public WeightedCodebookEntry entry;
	public int[] indices;
	public double[] weights;
	public int totalMatches; // This is less than or equal to the length of the above items (i.e. number of
								// best codebook matches). Can be lower when the codebook has less entries

	public WeightedCodebookLsfMatch(int numMaxMatches, int lpOrder) {
		init(numMaxMatches, lpOrder);
	}

	// Create a dummy match to use original input lsfs
	// (Should not be used in real codebook matching but only for control purposes)
	public WeightedCodebookLsfMatch(double[] sourceLsfs, double[] targetLsfs) {
		init(1, sourceLsfs.length);
		entry = new WeightedCodebookEntry(sourceLsfs, targetLsfs, null, null);
	}

	public void init(int numMaxMatches, int lpOrder) {
		if (numMaxMatches > 0 && lpOrder > 0) {
			entry = new WeightedCodebookEntry(lpOrder, 0);
			indices = new int[numMaxMatches];
			weights = new double[numMaxMatches];
			totalMatches = numMaxMatches;
		} else {
			entry = null;
			indices = null;
			weights = null;
			totalMatches = 0;
		}
	}
}
