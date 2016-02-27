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
package marytts.machinelearning;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JFrame;

import marytts.signalproc.display.FunctionGraph;
import marytts.util.math.Polynomial;

/**
 * Hierarchical clustering training algorithm
 * 
 * Reference: Stephen C. Johnson, 1967, "Hierarchical clustering schemes", Proc. Psychometrika, Vol. 32 No. 3, pp. 241-254.
 *
 * This version is adapted to work with a distance function between polynomials.
 *
 * @author Sathish Pammi
 */
public class PolynomialHierarchicalClusteringTrainer {

	private static final double INFINITE = 10000000.0;
	private static final int CLUSTER_DEFAULT_SIZE = 5;
	private HashSet<String> dataPointSet;
	private ArrayList<Cluster> clusterList;
	private HashMap<String, Double> distanceTableMap;
	private boolean isSimilarityMeasure; // true - similarity measure
											// false - dissimilarity measure
	private double MINDISTANCE;
	Polynomial[] polynomials;

	/**
	 * Constructor of the Hierarchical trainer
	 * 
	 * @param polynomials
	 *            - array of polynomial coefficients ( minimum length of polynomials should be three )
	 * @throws NullPointerException
	 *             if input polynomial array is null
	 * @throws IllegalArgumentException
	 *             if length of array of polynomial coefficients is less than three
	 */
	public PolynomialHierarchicalClusteringTrainer(Polynomial[] polynomials) {

		if (polynomials == null) {
			throw new NullPointerException("Input polynomial array should not be null");
		}

		if (polynomials.length <= 2) {
			throw new IllegalArgumentException("Number of samples for clustering should be more than two.");
		}

		dataPointSet = new HashSet<String>();
		distanceTableMap = new HashMap<String, Double>();
		clusterList = new ArrayList<Cluster>();
		this.polynomials = polynomials;
		// What is the type of measure? (similarity measure or distance measure)
		setSimilarityMeasure(true);
		computeSampleDistances();
		initializeClustering();

	}

	/**
	 * To find distance between two clusters
	 * 
	 * @param xCluster
	 *            a cluster that contains a set of polynomial coeffs.
	 * @param yCluster
	 *            a cluster that contains a set of polynomial coeffs.
	 * @param linkageType
	 *            the linkage type used for Hierarchical clustering. possible values: 'Complete', 'Average' or 'Short'
	 * @return distance between two clusters
	 * @throws NullPointerException
	 *             if received clusters are null
	 * @throws IllegalArgumentException
	 *             if linkageType is other than 'Complete', 'Average' or 'Short'
	 */
	private double getClusterDistance(Cluster xCluster, Cluster yCluster, String linkageType) {

		if (xCluster == null || yCluster == null) {
			throw new NullPointerException("Input clusters should not be null");
		}

		if (!("Short".equals(linkageType) || "Complete".equals(linkageType) || "Average".equals(linkageType))) {
			throw new IllegalArgumentException("Only Short, Complete, or Average linkage clustering supported");
		}

		ArrayList<String> xPoints = xCluster.getAllDataPoints();
		ArrayList<String> yPoints = yCluster.getAllDataPoints();
		ArrayList<Double> distanceList = new ArrayList<Double>();

		double distance = 0.0;
		int nDistances = 0;

		for (int i = 0; i < xPoints.size(); i++) {
			for (int j = 0; j < yPoints.size(); j++) {

				String xyDistance = xPoints.get(i) + "_" + yPoints.get(j);
				// System.out.println(xyDistance);
				if (distanceTableMap.containsKey(xyDistance)) {
					distanceList.add(distanceTableMap.get(xyDistance));
					distance = distanceTableMap.get(xyDistance) + distance;
					nDistances++;
				} else {
					xyDistance = yPoints.get(j) + "_" + xPoints.get(i);
					if (distanceTableMap.containsKey(xyDistance)) {
						distanceList.add(distanceTableMap.get(xyDistance));
						distance = distanceTableMap.get(xyDistance) + distance;
						nDistances++;
					}
				}
			}
		}

		if (linkageType.equals("Short")) {
			Double[] data = distanceList.toArray(new Double[0]);
			double min = Double.NaN;
			for (int i = 0; i < data.length; i++) {
				if (Double.isNaN(data[i]))
					continue;
				if (Double.isNaN(min) || data[i] < min)
					min = data[i];
			}
			return min;
		} else if (linkageType.equals("Complete")) {
			Double[] data = distanceList.toArray(new Double[0]);
			double max = Double.NaN;
			for (int i = 0; i < data.length; i++) {
				if (Double.isNaN(data[i]))
					continue;
				if (Double.isNaN(max) || data[i] > max)
					max = data[i];
			}
			return max;
		}

		// default 'Average'
		return ((double) distance / nDistances);
	}

	/**
	 * To initialize each sample as a single cluster
	 * 
	 */
	private void initializeClustering() {
		assert dataPointSet != null;
		assert clusterList != null;

		Iterator<String> it = dataPointSet.iterator();
		while (it.hasNext()) {
			ArrayList<String> dataSet = new ArrayList<String>();
			dataSet.add(it.next());
			Cluster aCluster = new Cluster(dataSet);
			clusterList.add(aCluster);
		}
	}

	/**
	 * To compute distances between all samples (i.e. all polynomials)
	 */
	private void computeSampleDistances() {

		assert polynomials != null;
		assert polynomials.length > 2;
		assert dataPointSet != null;
		assert distanceTableMap != null;

		int observations = polynomials.length;
		int polynomialOrder = polynomials[0].getOrder();
		int[] clusterIndices = new int[observations];

		// compute distace between two indices
		double[][] dist = new double[observations][observations];
		for (int i = 0; i < observations; i++) {
			dataPointSet.add("" + i);
			for (int j = 0; j < observations; j++) {
				dist[i][j] = Polynomial.polynomialPearsonProductMomentCorr(polynomials[i].coeffs, polynomials[j].coeffs);
				distanceTableMap.put(i + "_" + j, (new Double(dist[i][j])));
			}
		}
	}

	/**
	 * To get the type of measure used for cluster data
	 * 
	 * @return true, if similarity metric used to cluster the data false, if dissimilarity metric used to cluster the data
	 */
	private boolean hasSimilarityMeasure() {
		return this.isSimilarityMeasure;
	}

	/**
	 * To compute distance between to two clusters
	 * 
	 * @param xCluster
	 *            cluster one that contains a set of polynomial coeffs.
	 * @param yCluster
	 *            cluster two that contains a set of polynomial coeffs.
	 * @return double - distance between two clusters By default, it uses 'average' approach to compute distance
	 * @throws IllegalArgumentException
	 *             if input clusters are null
	 */
	private double getClusterDistance(Cluster xCluster, Cluster yCluster) {
		return getClusterDistance(xCluster, yCluster, "Average");
	}

	/**
	 * clustering with default target cluster size and default linkage type It uses 'Average' linkage clustering approach as
	 * default
	 */
	private void clustering() {
		clustering(CLUSTER_DEFAULT_SIZE, "Average");
	}

	/**
	 * clustering with default linkage type It uses 'Average' linkage clustering approach as default
	 * 
	 * @param tagetClusterSize
	 *            target cluster size
	 */
	private void clustering(int tagetClusterSize) {
		clustering(tagetClusterSize, "Average");
	}

	/**
	 * Clustering with user-defined target cluster size
	 * 
	 * @param tagetClusterSize
	 *            target cluster size
	 * @param linkageType
	 *            the linkage type used for Hierarchical clustering. Possible values are 'Average', 'Complete', and 'Short'
	 */
	private void clustering(int tagetClusterSize, String linkageType) {

		assert clusterList != null;

		int minClusterOne = 0;
		int minClusterTwo = 0;
		double minDistance;

		for (int i = clusterList.size(); i > tagetClusterSize; i--) {

			minDistance = this.MINDISTANCE;

			for (int j = 0; j < clusterList.size(); j++) {
				Cluster clusterOne = clusterList.get(j);
				for (int k = (j + 1); k < clusterList.size(); k++) {
					Cluster clusterTwo = clusterList.get(k);
					double distance = getClusterDistance(clusterOne, clusterTwo, linkageType);

					if (hasSimilarityMeasure()) {
						if (distance < minDistance) {
							minDistance = distance;
							minClusterOne = j;
							minClusterTwo = k;
						}
					} else {
						if (distance > minDistance) {
							minDistance = distance;
							minClusterOne = j;
							minClusterTwo = k;
						}
					}
				}
			}

			Cluster clusterOne = clusterList.get(minClusterOne);
			Cluster clusterTwo = clusterList.get(minClusterTwo);
			clusterOne.mergeCluster(clusterTwo); // merge two clusters
			clusterList.remove(clusterTwo); // remove one of them
		}
		printClusterData();
	}

	/**
	 * Print cluster information
	 */
	private void printClusterData() {

		assert clusterList != null;

		System.out.println("Total No of Clusters: " + clusterList.size());
		Iterator<Cluster> it = clusterList.iterator();
		for (int noCluster = 1; it.hasNext(); noCluster++) {
			Cluster aCluster = it.next();
			ArrayList<String> listPoints = aCluster.getAllDataPoints();
			System.out.println("Cluster Number : " + noCluster);
			for (int i = 0; i < listPoints.size(); i++) {
				System.out.print(listPoints.get(i) + " ");
			}
			System.out.println();
		}

	}

	/**
	 * Set the type of measure used to cluster the data
	 * 
	 * @param isSimilarityMeasure
	 *            whether the measure is a similarity measure (value true) or a dissimilarity measure (value false)
	 */
	private void setSimilarityMeasure(boolean isSimilarityMeasure) {
		this.isSimilarityMeasure = isSimilarityMeasure;
		if (this.isSimilarityMeasure) {
			this.MINDISTANCE = INFINITE;
		} else {
			this.MINDISTANCE = -1 * INFINITE;
		}
	}

	/**
	 * This function clusters polynomials using Hierarchical (agglomerative approach) clustering procedure, using a polynomial
	 * distance function. Training consists of four steps: 1. Convert object features to distance matrix. 2. Set each object as a
	 * cluster (thus if we have 6 objects, we will have 6 clusters in the beginning) 3. Iterate until number of clusters is equal
	 * to the given target number of clusters - Merge two closest clusters - Update distance matrix
	 * 
	 * @param tagetClusterSize
	 *            the target cluster size
	 * @param linkageType
	 *            the linkage type used for Hierarchical clustering ('Average', 'Complete', or 'Short')
	 * 
	 * @return the trained clusters
	 * @throws IllegalArgumentException
	 *             if target cluster size is not less than initialized number of samples
	 */
	public PolynomialCluster[] train(int tagetClusterSize, String linkageType) {

		if (clusterList.size() <= tagetClusterSize) {
			throw new IllegalArgumentException("taget cluster size should be less than number of samples");
		}

		if (!("Short".equals(linkageType) || "Complete".equals(linkageType) || "Average".equals(linkageType))) {
			throw new IllegalArgumentException("Only Short, Complete, or Average linkage clustering supported");
		}

		clustering(tagetClusterSize, linkageType);

		// Now fill the clusters with their means and members:
		PolynomialCluster[] clusters = new PolynomialCluster[tagetClusterSize];

		int noClusters = clusterList.size();

		// if below condition fails, it is a BUG
		assert clusterList.size() == tagetClusterSize : "After clustering, number of clusters and the target cluster size should be same, but now the number of clusters are "
				+ clusterList.size();

		for (int i = 0; i < tagetClusterSize; i++) {

			Cluster cl = clusterList.get(i);
			ArrayList<String> dataPoints = cl.getAllDataPoints();
			Polynomial[] members = new Polynomial[dataPoints.size()];

			for (int j = 0; j < dataPoints.size(); j++) {
				members[j] = this.polynomials[(new Integer(dataPoints.get(j))).intValue()];
			}

			Polynomial meanMembers = Polynomial.mean(members);
			clusters[i] = new PolynomialCluster(meanMembers, members);

		}

		return clusters;
	}

	/**
	 * Main method
	 * 
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// Test clustering with random polynomials, and visualise result
		int order = 3;
		int numPolynomials = 100;
		int numClusters = 5;

		// Initialise with random data:
		Polynomial[] ps = new Polynomial[numPolynomials];
		for (int i = 0; i < numPolynomials; i++) {
			double[] coeffs = new double[order + 1];
			for (int c = 0; c < coeffs.length; c++) {
				coeffs[c] = Math.random();
			}
			ps[i] = new Polynomial(coeffs);
		}

		PolynomialHierarchicalClusteringTrainer phCT = new PolynomialHierarchicalClusteringTrainer(ps);
		// Train:
		PolynomialCluster[] clusters = phCT.train(5, "Average");

		// Visualise:
		FunctionGraph clusterGraph = new FunctionGraph(0, 1, new double[1]);
		clusterGraph.setYMinMax(0, 5);
		clusterGraph.setPrimaryDataSeriesStyle(Color.BLUE, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_FULLCIRCLE);
		JFrame jf = clusterGraph.showInJFrame("", false, true);
		for (int i = 0; i < clusters.length; i++) {
			double[] meanValues = clusters[i].getMeanPolynomial().generatePolynomialValues(100, 0, 1);
			clusterGraph.updateData(0, 1. / meanValues.length, meanValues);

			Polynomial[] members = clusters[i].getClusterMembers();
			for (int m = 0; m < members.length; m++) {
				double[] pred = members[m].generatePolynomialValues(meanValues.length, 0, 1);
				clusterGraph.addDataSeries(pred, Color.GRAY, FunctionGraph.DRAW_LINE, -1);
				jf.repaint();
			}

			jf.setTitle("Cluster " + (i + 1) + " of " + clusters.length + ": " + members.length + " members");
			jf.repaint();

			try {
				Thread.sleep(5000);
			} catch (InterruptedException ie) {
			}
		}
		// System.exit(0);
	}

	/**
	 * A class that contains samples of a cluster
	 * 
	 * @author sathish
	 *
	 */
	class Cluster {

		private ArrayList<String> dataPoints;
		private int clusterSize;

		/**
		 * Cluster constructor
		 * 
		 * @param dataSet
		 *            a arraylist of samples
		 * @throws NullPointerException
		 *             if input dataset is null
		 */
		public Cluster(ArrayList<String> dataSet) {
			if (dataSet == null) {
				throw new NullPointerException("Input dataset for a cluster should not be null");
			}
			this.dataPoints = dataSet;
			this.clusterSize = dataSet.size();
		}

		/**
		 * To return all datapoints in this cluster
		 * 
		 * @return ArrayList<String> of datapoints
		 */
		public ArrayList<String> getAllDataPoints() {
			return this.dataPoints;
		}

		/**
		 * Given cluster will be merged into this cluster
		 * 
		 * @param xCluster
		 *            a cluster that contains a set of polynomial coeffs.
		 * @throws NullPointerException
		 *             if given cluster is null
		 */
		public void mergeCluster(Cluster xCluster) {

			if (xCluster == null) {
				throw new NullPointerException("Input cluster should not be null");
			}

			ArrayList<String> xDataPoints = xCluster.getAllDataPoints();
			Iterator<String> it = xDataPoints.iterator();
			while (it.hasNext()) {
				this.dataPoints.add(it.next());
			}
			this.clusterSize = this.dataPoints.size();
		}

	}

}
