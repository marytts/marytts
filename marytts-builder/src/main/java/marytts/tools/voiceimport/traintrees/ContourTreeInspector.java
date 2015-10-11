/**
 * Copyright 2009 DFKI GmbH.
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
package marytts.tools.voiceimport.traintrees;

import java.awt.Color;
import java.util.Random;

import javax.swing.JFrame;

import marytts.cart.DirectedGraph;
import marytts.cart.LeafNode;
import marytts.cart.LeafNode.IntArrayLeafNode;
import marytts.cart.io.DirectedGraphReader;
import marytts.signalproc.display.FunctionGraph;
import marytts.unitselection.data.FeatureFileReader;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;

public class ContourTreeInspector {

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		boolean showIndividualLeaves = true;
		DirectedGraph contourTree = new DirectedGraphReader().load(args[0]);
		FeatureFileReader contours = new FeatureFileReader(args[1]);
		int numLeaves = 0;
		int totalSyls = 0;
		for (LeafNode leaf : contourTree.getLeafNodes()) {
			numLeaves++;
			totalSyls += leaf.getNumberOfData();
			int[] contourIDs = (int[]) leaf.getAllData();
			int nZero = 0;
			for (int j = 0; j < contourIDs.length; j++) {
				float[] coeffs = contours.getFeatureVector(contourIDs[j]).getContinuousFeatures();
				if (ArrayUtils.isZero(coeffs))
					nZero++;
			}
			System.out.printf("Leaf %5d:%4d contours", numLeaves, contourIDs.length);
			if (nZero > 0)
				System.out.printf(" (%4d are zero)", nZero);
			System.out.println();
		}
		System.out.println("Tree has " + numLeaves + " leaves containing a total of " + totalSyls + " syllables (avg. "
				+ (totalSyls / numLeaves) + " syls per leaf)");
		for (LeafNode leaf : contourTree.getLeafNodes()) {
		}

		FunctionGraph f0Graph = new FunctionGraph(0, 0.01, new double[100]);
		f0Graph.setYMinMax(50, 300);
		f0Graph.setPrimaryDataSeriesStyle(Color.BLACK, FunctionGraph.DRAW_LINE, -1);
		JFrame jf = f0Graph.showInJFrame("Syllables in leaf", false, true);
		Random r = new Random();

		float[][] reps = new float[numLeaves][]; // the representants of each leaf;
		int iLeaf = 1;
		for (LeafNode leaf : contourTree.getLeafNodes()) {
			System.out.println("Leaf " + iLeaf + ":");
			System.out.println(leaf.getDecisionPath());
			f0Graph.updateData(0, 0.01, new double[100]);
			int n = leaf.getNumberOfData();
			IntArrayLeafNode ialn = (IntArrayLeafNode) leaf;
			int[] contourIDs = ialn.getIntData();
			float[] rep = null;
			for (int i = 0; i < n; i++) {
				double[] coeffs = ArrayUtils.copyFloat2Double(contours.getFeatureVector(contourIDs[i]).getContinuousFeatures());
				if (rep == null)
					rep = new float[coeffs.length];
				for (int c = 0; c < rep.length; c++) {
					rep[c] += coeffs[c];
				}
				if (showIndividualLeaves) {
					double[] pred = Polynomial.generatePolynomialValues(coeffs, 100, 0, 1);
					Color c = new Color(r.nextInt(150), r.nextInt(150), r.nextInt(150));
					pred = MathUtils.exp(pred);
					f0Graph.addDataSeries(pred, c, FunctionGraph.DRAW_LINE, -1);
				}
			}
			for (int c = 0; c < rep.length; c++) {
				rep[c] /= n;
			}
			reps[iLeaf] = rep;
			double[] pred = Polynomial.generatePolynomialValues(ArrayUtils.copyFloat2Double(rep), 100, 0, 1);
			pred = MathUtils.exp(pred);
			f0Graph.addDataSeries(pred, Color.BLACK, FunctionGraph.DRAW_LINEWITHDOTS, FunctionGraph.DOT_FULLCIRCLE);
			if (showIndividualLeaves) {
				jf.setTitle("Leaf " + iLeaf + " of " + numLeaves + ": " + n + " syllables");
				jf.repaint();
				Thread.sleep(2000);
			}
			iLeaf++;
		}

		f0Graph.updateData(0, 0.01, new double[100]);
		for (int i = 0; i < numLeaves; i++) {
			double[] coeffs = ArrayUtils.copyFloat2Double(reps[i]);
			double[] pred = Polynomial.generatePolynomialValues(coeffs, 100, 0, 1);
			Color c = new Color(r.nextInt(150), r.nextInt(150), r.nextInt(150));
			f0Graph.addDataSeries(pred, c, FunctionGraph.DRAW_LINE, -1);
		}
		jf.setTitle("Representatives of " + numLeaves + " leaves");
		jf.repaint();

	}
}
