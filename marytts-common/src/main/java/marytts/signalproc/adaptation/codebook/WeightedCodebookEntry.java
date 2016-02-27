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

import marytts.util.io.MaryRandomAccessFile;

/**
 * 
 * Wrapper class for a single weighted codebook entry
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookEntry {
	public WeightedCodebookSpeakerItem sourceItem;
	public WeightedCodebookSpeakerItem targetItem;

	public WeightedCodebookEntry() {
		this(0, 0);
	}

	public WeightedCodebookEntry(int lpOrder, int mfccDimension) {
		allocate(lpOrder, mfccDimension);
	}

	public WeightedCodebookEntry(double[] sourceLsfs, double[] targetLsfs, double[] sourceMfccs, double[] targetMfccs) {
		int lsfDimension = 0;
		int mfccDimension = 0;

		if (sourceLsfs != null && targetLsfs != null) {
			assert sourceLsfs.length == targetLsfs.length;
			lsfDimension = sourceLsfs.length;
		}

		if (sourceMfccs != null && targetMfccs != null) {
			assert sourceMfccs.length == targetMfccs.length;
			mfccDimension = sourceMfccs.length;
		}

		allocate(lsfDimension, mfccDimension);

		if (lsfDimension > 0)
			setLsfs(sourceLsfs, targetLsfs);

		if (mfccDimension > 0)
			setMfccs(sourceMfccs, targetMfccs);
	}

	public void allocate(int lpOrder, int mfccDimension) {
		if (lpOrder > 0 || mfccDimension > 0) {
			sourceItem = new WeightedCodebookSpeakerItem(lpOrder, mfccDimension);
			targetItem = new WeightedCodebookSpeakerItem(lpOrder, mfccDimension);
		} else {
			sourceItem = null;
			targetItem = null;
		}
	}

	public void setLsfs(double[] srcLsfs, double[] tgtLsfs) {
		sourceItem.setLsfs(srcLsfs);
		targetItem.setLsfs(tgtLsfs);
	}

	public void setMfccs(double[] srcMfccs, double[] tgtMfccs) {
		sourceItem.setMfccs(srcMfccs);
		targetItem.setMfccs(tgtMfccs);
	}

	public void write(MaryRandomAccessFile ler) {
		if (sourceItem != null && targetItem != null) {
			sourceItem.write(ler);
			targetItem.write(ler);
		}
	}

	public void read(MaryRandomAccessFile ler, int lpOrder, int mfccDimension) {
		sourceItem = new WeightedCodebookSpeakerItem();
		sourceItem.read(ler, lpOrder, mfccDimension);

		targetItem = new WeightedCodebookSpeakerItem();
		targetItem.read(ler, lpOrder, mfccDimension);
	}
}
