/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.util.math;

import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;

/**
 * Dynamic programming to compute similarity measure
 * 
 * @author sathish
 * 
 */

public class DTW {

	private static final double INFINITE = 1.0e+32;
	double[][] signal;
	double[][] reference;
	// the slope constraint value
	double slope = 0.0;
	double[] weights;
	String distanceFunction;
	double[] sigma2 = null;
	double costValue;

	/**
	 * Dynamic time warping (DTW) cost signal and reference Default 'Euclidean' distance function
	 * 
	 * @param signal
	 *            signal
	 * @param reference
	 *            reference
	 */
	public DTW(double[][] signal, double[][] reference) {
		this.signal = signal;
		this.reference = reference;
		this.distanceFunction = "Euclidean";
		setCost(dpDistance());
	}

	/**
	 * Dynamic time warping (DTW) cost signal and reference distanceFunction = {"Euclidean" or "Absolute"}
	 * 
	 * @param signal
	 *            signal
	 * @param reference
	 *            reference
	 * @param distanceFunction
	 *            distance function
	 */
	public DTW(double[][] signal, double[][] reference, String distanceFunction) {
		this.signal = signal;
		this.reference = reference;
		this.distanceFunction = distanceFunction;
		setCost(dpDistance());
	}

	/**
	 * DTW using Mahalanobis distance (Variance computation from external module)
	 * 
	 * @param signal
	 *            signal
	 * @param reference
	 *            reference
	 * @param sigma2
	 *            sigma2
	 */
	public DTW(double[][] signal, double[][] reference, double[] sigma2) {
		this.signal = signal;
		this.reference = reference;
		this.sigma2 = sigma2;
		this.distanceFunction = "Mahalanobis";
		setCost(dpDistance());

	}

	public class Node {
		public int x;
		public int y;
		public double value;
		public double frameDist;
		public boolean edgeNode;
		public Node prevNode;

		Node(int x, int y, boolean isWeight) {
			this.x = x;
			this.y = y;

			if (isWeight)
				this.frameDist = weights[y] * frameDistance(reference[y], signal[x], distanceFunction);
			else
				this.frameDist = frameDistance(reference[y], signal[x], distanceFunction);

			this.value = -1;
		}
	}

	public class RecurssiveDTW {
		Node[][] nodes;
		int xlen;
		int ylen;
		double dpCost;
		double pathLength;
		double dpNormalizedCost;

		RecurssiveDTW(int xlen, int ylen) {
			this.xlen = xlen;
			this.ylen = ylen;
			nodes = new Node[xlen][ylen];
			this.dpCost = rdpSearch(xlen - 1, ylen - 1);
			this.pathLength = this.getPathLength();
			this.dpNormalizedCost = this.dpCost / this.pathLength;
		}

		public double getPathLength() {
			double sumDist = 0;
			Node pNode = nodes[xlen - 1][ylen - 1].prevNode;
			Node cNode = nodes[xlen - 1][ylen - 1];
			while (pNode != null && cNode != null) {
				sumDist += euclideanDistance(pNode.x, pNode.y, cNode.x, cNode.y);
				pNode = pNode.prevNode;
				cNode = cNode.prevNode;
			}
			return sumDist;
		}

		public void printBestPath() {

			Node cNode = nodes[xlen - 1][ylen - 1];

			// int dist = 0;
			// while(cNode != null){
			// dist++;
			// cNode = cNode.prevNode;
			// }
			// double[][] path = new double[dist][dist];
			// cNode = nodes[xlen-1][ylen-1];
			// for(int i=0, j=0; cNode != null; i++, j++){
			// dist++;
			// path[i][j] = ;
			// }

			System.out.println("Printing best path:");
			while (cNode != null) {
				System.out.println(cNode.x + " " + cNode.y);
				cNode = cNode.prevNode;
			}
			// return path;
		}

		public int[][] getBestPath() {
			int[][] bestPath = null;

			Node cNode = nodes[xlen - 1][ylen - 1];
			int numItems = 0;
			while (cNode != null) {
				// System.out.println(cNode.x+" "+cNode.y);
				cNode = cNode.prevNode;
				numItems++;
			}

			bestPath = new int[numItems][2];

			int i = 0;
			while (cNode != null) {
				bestPath[i][0] = cNode.x;
				bestPath[i][1] = cNode.y;
				cNode = cNode.prevNode;
				i++;
			}

			return bestPath;
		}

		public double euclideanDistance(double x1, double y1, double x2, double y2) {
			double xsQ = (x1 - x2) * (x1 - x2);
			double ysQ = (y1 - y2) * (y1 - y2);
			return Math.sqrt(xsQ + ysQ);
		}

		public double rdpSearch(int x, int y) {

			if (x < 0 || y < 0) {
				return INFINITE;
			}
			if (x == 0 && y == 0) {
				nodes[x][y] = new Node(0, 0, false);
				nodes[x][y].value = nodes[x][y].frameDist;
				nodes[x][y].prevNode = null;
				return nodes[x][y].value;
			} else if (x == 0 || y == 0) {
				nodes[x][y] = new Node(x, y, false);
				nodes[x][y].value = INFINITE;
				nodes[x][y].prevNode = nodes[0][0];
				return nodes[x][y].value;
			}

			if (nodes[x][y] == null)
				nodes[x][y] = new Node(x, y, false);
			if (nodes[x][y].value != -1)
				return nodes[x][y].value;

			double minV = INFINITE;

			double[] localD = new double[5];
			localD[0] = rdpSearch(x - 1, y - 1);
			localD[1] = rdpSearch(x - 2, y - 1);
			localD[2] = rdpSearch(x - 1, y - 2);
			localD[3] = rdpSearch(x, y - 1);
			localD[4] = rdpSearch(x - 1, y);

			minV = localD[0];

			if ((x - 1) >= 0 && (y - 1) >= 0)
				nodes[x][y].prevNode = nodes[x - 1][y - 1];

			if (localD[1] < minV) {
				minV = localD[1];
				if ((x - 2) >= 0 && (y - 1) >= 0)
					nodes[x][y].prevNode = nodes[x - 2][y - 1];
			}

			if (localD[2] < minV) {
				minV = localD[2];
				if ((x - 1) >= 0 && (y - 2) >= 0)
					nodes[x][y].prevNode = nodes[x - 1][y - 2];
			}

			if (localD[3] < minV) {
				minV = localD[3];
				if ((x) >= 0 && (y - 1) >= 0)
					nodes[x][y].prevNode = nodes[x][y - 1];
			}

			if (localD[4] < minV) {
				minV = localD[4];
				if ((x - 1) >= 0 && (y) >= 0)
					nodes[x][y].prevNode = nodes[x - 1][y];
			}

			nodes[x][y].value = minV + nodes[x][y].frameDist;
			return (nodes[x][y].value);

		}

	}

	/**
	 * Set cost of best path
	 * 
	 * @param cost
	 *            cost
	 */
	private void setCost(double cost) {
		this.costValue = cost;
	}

	/**
	 * Set global variance
	 * 
	 * @param sigma2
	 *            sigma2
	 */
	private void setCost(double[] sigma2) {
		this.sigma2 = sigma2;
	}

	/**
	 * Get cost of best path
	 * 
	 * @return cost
	 */
	public double getCost() {
		return this.costValue;
	}

	/**
	 * Get cost of best path
	 * 
	 * @return cost
	 */
	public double getNormalizedCost() {
		return ((double) (this.costValue * 2.0) / (reference.length + signal.length));
	}

	/**
	 * the major method to compute the matching score between selected test signal and reference.
	 * 
	 * @return DP cost
	 */
	private double dpDistance() {

		if ((signal == null) || (reference == null)) {
			return 1.0e+32;
		}
		if ((signal.length == 0) || (reference.length == 0)) {
			return 1.0e+32;
		}
		if (signal[0].length != reference[0].length) {
			throw new RuntimeException("Given signal vector order (" + signal[0].length + ") and reference vector order ("
					+ reference[0].length + ") are not same.");
		}
		weights = weightFunction(reference.length);

		RecurssiveDTW rdp = new RecurssiveDTW(signal.length, reference.length);
		// Node nd = new Node(signal.length - 1 , reference.length - 1);
		// double cost = rdp.dpNormalizedCost;
		return rdp.dpCost;
	}

	/**
	 * Euclidean distance
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @return sum
	 */
	public double EuclideanDistance(double[] x, double[] y) {

		double sum = 0;
		if (x.length != y.length) {
			throw new RuntimeException("Given array lengths were not equal.");
		}
		int d = x.length;
		for (int i = 0; i < d; i++) {
			sum = sum + (x[i] - y[i]) * (x[i] - y[i]);
		}
		sum = Math.sqrt(sum);
		return sum;
	}

	/**
	 * Absolute distance
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @return sum
	 */
	public double AbsDistance(double[] x, double[] y) {

		double sum = 0;
		if (x.length != y.length) {
			throw new RuntimeException("Given array lengths were not equal.");
		}
		int d = x.length;
		for (int i = 0; i < d; i++) {
			sum = sum + Math.abs(x[i] - y[i]);
		}
		return sum;
	}

	public double[] weightFunction(int windowLength) {

		double[] weightsF;
		Window w = new HammingWindow(windowLength);
		weightsF = w.getCoeffs();
		for (int i = 0; i < weightsF.length; i++) {
			weightsF[i] = 1 - weightsF[i];
		}
		return weightsF;
	}

	/**
	 * Mahalanobis distance between two feature vectors.
	 * 
	 * @param v1
	 *            A feature vector.
	 * @param v2
	 *            Another feature vector.
	 * @param sigma2
	 *            The variance of the distribution of the considered feature vectors.
	 * @return The mahalanobis distance between v1 and v2.
	 */
	private double mahalanobis(double[] v1, double[] v2, double[] sig2) {

		if (v1.length != v2.length)
			throw new RuntimeException("Given array lengths were not equal.");
		if (v1.length != sig2.length)
			throw new RuntimeException("Given array lengths were not equal.");

		double sum = 0.0;
		double diff = 0.0;
		for (int i = 0; i < v1.length; i++) {
			diff = v1[i] - v2[i];
			sum += ((diff * diff) / sig2[i]);
		}
		// System.err.println("Mahalanobis distance: "+sum);
		return (sum);
	}

	// methods to compute distance between two frames
	protected double frameDistance(double f1[], double f2[], String distanceType) {

		double dis = 0.0;
		if (distanceType == "Mahalanobis")
			dis = mahalanobis(f1, f2, sigma2);
		else if (distanceType == "Euclidean")
			dis = EuclideanDistance(f1, f2);
		else
			dis = AbsDistance(f1, f2);
		return dis;
	}
}
