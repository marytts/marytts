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
package marytts.signalproc.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import marytts.util.io.FileUtils;

/**
 * A wrapper class to read fields in Festival UTT files
 * 
 * @author Oytun T&uuml;rk
 */
public class FestivalUtt extends AlignmentData {
	public Labels[] labels;
	public String[] keys;

	public FestivalUtt() {
		this("");
	}

	public FestivalUtt(String festivalUttFile) {
		keys = new String[6];
		keys[0] = "==Segment==";
		keys[1] = "==Target==";
		keys[2] = "==Syllable==";
		keys[3] = "==Word==";
		keys[4] = "==IntEvent==";
		keys[5] = "==Phrase==";

		labels = new Labels[keys.length];

		if (FileUtils.exists(festivalUttFile)) {
			read(festivalUttFile);
		}
	}

	public void read(String festivalUttFile) {
		String allText = null;
		try {
			allText = FileUtils.getFileAsString(new File(festivalUttFile), "ASCII");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (allText != null) {
			String[] lines = allText.split("\n");

			int i, j;
			int[] boundInds = new int[keys.length];
			Arrays.fill(boundInds, -1);
			int boundCount = 0;
			for (i = 0; i < keys.length; i++) {
				for (j = 0; j < lines.length; j++) {
					if (lines[j].compareTo(keys[i]) == 0) {
						boundInds[i] = j;
						break;
					}
				}
			}

			for (i = 0; i < keys.length; i++) {
				if (boundInds[i] > -1) {
					int fromIndex = boundInds[i] + 1;
					int toIndex = (i < keys.length - 1 ? boundInds[i + 1] - 1 : lines.length - 1);
					labels[i] = createLabels(lines, fromIndex, toIndex);
					// Shift all valuesRest by one, and put the f0 into valuesRest[0]
					if (keys[i].compareTo("==Target==") == 0) {
						for (j = 0; j < labels[i].items.length; j++) {
							int numTotalValuesRest = 0;
							if (labels[i].items[j].valuesRest != null) {
								double[] tmpValues = new double[labels[i].items[j].valuesRest.length];
								System.arraycopy(labels[i].items[j].valuesRest, 0, tmpValues, 0,
										labels[i].items[j].valuesRest.length);
								labels[i].items[j].valuesRest = new double[tmpValues.length + 1];
								labels[i].items[j].valuesRest[0] = Double.valueOf(labels[i].items[j].phn);
								System.arraycopy(labels[i].items[j].valuesRest, 0, tmpValues, 1,
										labels[i].items[j].valuesRest.length);
							} else {
								labels[i].items[j].valuesRest = new double[1];
								labels[i].items[j].valuesRest[0] = Double.valueOf(labels[i].items[j].phn);
							}
						}
					}
					//
				} else
					labels[i] = null;
			}
		}
	}

	private Labels createLabels(String[] lines, int fromIndex, int toIndex) {
		String[] relevantLines = new String[toIndex - fromIndex + 1];
		System.arraycopy(lines, fromIndex, relevantLines, 0, relevantLines.length);
		Labels l = new Labels(relevantLines, 2);
		return l;
	}

	public static FestivalUtt readFestivalUttFile(String festivalUttFile) {
		FestivalUtt f = new FestivalUtt(festivalUttFile);

		return f;
	}

	public static void main(String[] args) {
		FestivalUtt f = new FestivalUtt("d:/a.utt");

		System.out.println("Test completed...");
	}
}
