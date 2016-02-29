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

package marytts.util.data;

import marytts.signalproc.analysis.Label;

/**
 * @author marc
 * 
 */
public class AlignLabelsUtils {

	public static int[][] alignLabels(Label[] labs1, Label[] labs2, double PDeletion, double PInsertion, double PSubstitution) {
		double PCorrect = 1.0 - (PDeletion + PInsertion + PSubstitution);
		int n = labs1.length;
		int m = labs2.length;
		double D;
		int[][] labelMap = null;

		if (n == 0 || m == 0) {
			D = m;
			return labelMap;
		}

		int i, j;
		double[][] d = new double[n + 1][m + 1];
		for (i = 0; i < d.length; i++) {
			for (j = 0; j < d[i].length; j++)
				d[i][j] = 0.0;
		}

		int[][] p = new int[n + 1][m + 1];
		for (i = 0; i < p.length; i++) {
			for (j = 0; j < p[i].length; j++)
				p[i][j] = 0;
		}

		double z = 1;
		d[0][0] = z;
		for (i = 1; i <= n; i++)
			d[i][0] = d[i - 1][0] * PDeletion;

		for (j = 1; j <= m; j++)
			d[0][j] = d[0][j - 1] * PInsertion;

		String strEvents = "DISC";
		double c;
		double tmp;
		for (i = 1; i <= n; i++) {
			for (j = 1; j <= m; j++) {
				if (labs1[i - 1].phn.compareTo(labs2[j - 1].phn) == 0)
					c = PCorrect;
				else
					c = PSubstitution;

				int ind = 1;
				d[i][j] = d[i - 1][j] * PDeletion;
				tmp = d[i][j - 1] * PInsertion;
				if (tmp > d[i][j]) {
					d[i][j] = tmp;
					ind = 2;
				}

				tmp = d[i - 1][j - 1] * c;
				if (tmp > d[i][j]) {
					d[i][j] = tmp;
					ind = 3;
				}

				if (ind == 3 && labs1[i - 1].phn.compareTo(labs2[j - 1].phn) == 0)
					ind = 4;

				// Events 1:Deletion, 2:Insertion, 3:Substitution, 4:Correct
				p[i][j] = ind;
			}
		}

		// Backtracking
		D = d[n][m];
		int k = 1;
		int[] E = new int[m * n];
		E[k - 1] = p[n][m];
		i = n + 1;
		j = m + 1;
		int t = m;
		while (true) {
			if (E[k - 1] == 3 || E[k - 1] == 4) {
				i = i - 1;
				j = j - 1;
			} else if (E[k - 1] == 2)
				j = j - 1;
			else if (E[k - 1] == 1)
				i = i - 1;

			if (p[i - 1][j - 1] == 0) {
				while (j > 1) {
					k = k + 1;
					j = j - 1;
					E[k - 1] = 2;
				}
				break;
			} else {
				k = k + 1;
				E[k - 1] = p[i - 1][j - 1];
			}
			t = t - 1;
		}

		// Reverse the order
		int[] Events = new int[k];
		for (t = k; t >= 1; t--)
			Events[t - 1] = E[k - t];

		int[][] tmpLabelMap = new int[n * m][2];
		int ind = 0;
		int ind1 = 0;
		int ind2 = 0;
		for (t = 1; t <= k; t++) {
			if (Events[t - 1] == 3 || Events[t - 1] == 4) // Substitution or correct
			{
				tmpLabelMap[ind][0] = ind1;
				tmpLabelMap[ind][1] = ind2;
				ind1++;
				ind2++;
				ind++;
			} else if (Events[t - 1] == 1) // An item in seq1 is deleted in seq2
			{
				ind1++;
			} else if (Events[t - 1] == 2) // An item is inserted in seq2
			{
				ind2++;
			}
		}

		if (ind > 0) {
			labelMap = new int[ind][2];
			for (i = 0; i < labelMap.length; i++) {
				labelMap[i][0] = tmpLabelMap[i][0];
				labelMap[i][1] = tmpLabelMap[i][1];
			}
		}

		return labelMap;
	}

	// This version assumes that there can only be insertions and deletions but no substitutions
	// (i.e. text based alignment with possible differences in pauses only)
	public static int[][] alignLabels(Label[] seq1, Label[] seq2) {
		return alignLabels(seq1, seq2, 0.05, 0.05, 0.05);
	}

}
