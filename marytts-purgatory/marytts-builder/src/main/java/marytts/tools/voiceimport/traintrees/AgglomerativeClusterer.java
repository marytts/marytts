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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.DirectedGraph;
import marytts.cart.DirectedGraphNode;
import marytts.cart.FeatureVectorCART;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.FeatureVectorLeafNode;
import marytts.cart.impose.FeatureArrayIndexer;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * @author marc
 *
 */
public class AgglomerativeClusterer {
	private static final float SINGLE_ITEM_IMPURITY = 0;
	private FeatureVector[] trainingFeatures;
	private FeatureVector[] testFeatures;
	private Map<LeafNode, Double> impurities = new HashMap<LeafNode, Double>();
	private FeatureDefinition featureDefinition;
	private int numByteFeatures;
	private int[] availableFeatures;
	// private double globalMean;
	private double globalStddev;
	private DistanceMeasure dist;

	private double minFSGI, minCriterion;
	private int iBestFeature;

	private float[][] squaredDistances;

	private DirectedGraph graph;
	private int[] prevFeatureList;
	private double prevFSGI;
	private double prevTestDataDistance;
	private boolean canClusterMore = true;

	public AgglomerativeClusterer(FeatureVector[] features, FeatureDefinition featureDefinition, List<String> featuresToUse,
			DistanceMeasure dist) {
		this(features, featureDefinition, featuresToUse, dist, 0.1f);
	}

	public AgglomerativeClusterer(FeatureVector[] features, FeatureDefinition featureDefinition, List<String> featuresToUse,
			DistanceMeasure dist, float proportionTestData) {
		// Now replace all feature vectors with feature vectors whose unit index
		// corresponds to the distance matrix in squaredDistance:
		for (int i = 0; i < features.length; i++) {
			features[i] = new FeatureVector(features[i].getByteValuedDiscreteFeatures(),
					features[i].getShortValuedDiscreteFeatures(), features[i].getContinuousFeatures(), i);
		}

		this.dist = dist;

		this.globalStddev = Math.sqrt(((F0ContourPolynomialDistanceMeasure) dist).computeVariance(features));

		System.out.println("Global stddev: " + globalStddev);
		/*
		 * // Get an estimate of the global mean by sampling: estimateGlobalMean(features, dist);
		 * 
		 * // Precompute distances and set unit index features accordingly System.out.println("Precomputing distances..."); long
		 * startTime = System.currentTimeMillis(); squaredDistances = new float[features.length-1][]; for (int i=0;
		 * i<features.length-1; i++) { squaredDistances[i] = new float[features.length - i -1]; for (int j=i+1; j<features.length;
		 * j++) { squaredDistances[i][j-i-1] = dist.squaredDistance(features[i], features[j]); } }
		 * 
		 * long endTime = System.currentTimeMillis();
		 * System.out.println("Computed distances between "+features.length+" items in "+(endTime-startTime)+" ms");
		 */
		int nSkip = (int) (1 / proportionTestData); // we use every nSkip'th feature vector as test data
		int numTestFeatures = features.length / nSkip;
		if (numTestFeatures * nSkip < features.length)
			numTestFeatures++;
		this.testFeatures = new FeatureVector[numTestFeatures];
		this.trainingFeatures = new FeatureVector[features.length - testFeatures.length];
		int iTest = 0, iTrain = 0;
		for (int i = 0; i < features.length; i++) {
			if (i % nSkip == 0) {
				testFeatures[iTest++] = features[i];
			} else {
				trainingFeatures[iTrain++] = features[i];
			}
		}

		this.featureDefinition = featureDefinition;
		this.numByteFeatures = featureDefinition.getNumberOfByteFeatures();
		if (featuresToUse != null) {
			availableFeatures = new int[featuresToUse.size()];
			for (int i = 0; i < availableFeatures.length; i++) {
				availableFeatures[i] = featureDefinition.getFeatureIndex(featuresToUse.get(i));
			}
		} else { // no features given, use all byte-valued features
			availableFeatures = new int[numByteFeatures];
			for (int i = 0; i < numByteFeatures; i++) {
				availableFeatures[i] = i;
			}
		}

		graph = new DirectedGraph(featureDefinition);
		graph.setRootNode(new DirectedGraphNode(null, null));
		prevFeatureList = new int[0];
		prevFSGI = Double.POSITIVE_INFINITY;
		prevTestDataDistance = Double.POSITIVE_INFINITY;
		canClusterMore = true;
	}

	public DirectedGraph getGraph() {
		return graph;
	}

	public boolean canClusterMore() {
		return canClusterMore;
	}

	public DirectedGraph cluster() {
		if (!canClusterMore)
			return null;
		long startTime = System.currentTimeMillis();
		int[] newFeatureList = new int[prevFeatureList.length + 1];
		System.arraycopy(prevFeatureList, 0, newFeatureList, 0, prevFeatureList.length);

		// Step 1: Feature selection
		// We look for the feature that yields the best (=lowest) global impurity.
		// Stop criterion: when the best feature does not substantially add new leaves.
		FeatureArrayIndexer fai = new FeatureArrayIndexer(trainingFeatures, featureDefinition);
		// Count previous number of leaves:
		fai.deepSort(prevFeatureList);
		CART prevCART = new FeatureVectorCART(fai.getTree(), fai);
		int prevNLeaves = 0;
		for (LeafNode leaf : prevCART.getLeafNodes()) {
			if (leaf != null && leaf.getNumberOfData() > 0)
				prevNLeaves++;
		}
		iBestFeature = -1;
		minFSGI = Double.POSITIVE_INFINITY;
		minCriterion = Double.POSITIVE_INFINITY;
		Set<Future<?>> openJobs = new HashSet<Future<?>>();
		// Loop over all unused discrete features, and compute their Global Impurity
		for (int f = 0; f < availableFeatures.length; f++) {
			int fi = availableFeatures[f];
			boolean featureAlreadyUsed = false;
			for (int i = 0; i < prevFeatureList.length; i++) {
				if (prevFeatureList[i] == fi) {
					featureAlreadyUsed = true;
					break;
				}
			}
			if (featureAlreadyUsed)
				continue;
			newFeatureList[newFeatureList.length - 1] = fi;
			fai.deepSort(newFeatureList);
			CART testCART = new FeatureVectorCART(fai.getTree(), fai);
			assert testCART.getRootNode().getNumberOfData() == trainingFeatures.length;
			verifyFeatureQuality(fi, testCART, prevNLeaves);
		}

		newFeatureList[newFeatureList.length - 1] = iBestFeature;
		fai.deepSort(newFeatureList);
		CART bestFeatureCart = new FeatureVectorCART(fai.getTree(), fai);
		int nLeaves = 0;
		for (LeafNode leaf : bestFeatureCart.getLeafNodes()) {
			if (leaf != null && leaf.getNumberOfData() > 0)
				nLeaves++;
		}
		long featSelectedTime = System.currentTimeMillis();

		// Now walk through graphSoFar and bestFeatureCart in parallel,
		// and add the leaves of bestFeatureCart into graphSoFar in order
		// to enable clustering:
		Node fNode = bestFeatureCart.getRootNode();
		Node gNode = graph.getRootNode();

		List<DirectedGraphNode> newLeavesList = new ArrayList<DirectedGraphNode>();
		updateGraphFromTree((DecisionNode) fNode, (DirectedGraphNode) gNode, newLeavesList);
		DirectedGraphNode[] newLeaves = newLeavesList.toArray(new DirectedGraphNode[0]);
		System.out.printf("Level %2d: %25s (%5d leaves, gi=%7.3f -->", newFeatureList.length,
				featureDefinition.getFeatureName(iBestFeature), newLeaves.length, minFSGI);

		float[][] deltaGI = new float[newLeaves.length - 1][];
		for (int i = 0; i < newLeaves.length - 1; i++) {
			deltaGI[i] = new float[newLeaves.length - i - 1];
			for (int j = i + 1; j < newLeaves.length; j++) {
				deltaGI[i][j - i - 1] = (float) computeDeltaGI(newLeaves[i], newLeaves[j]);
			}
		}
		int numLeavesLeft = newLeaves.length;

		// Now cluster the leaves
		float minDeltaGI, threshold;
		int bestPair1, bestPair2;
		do {
			// threshold = 100*(float)(Math.log(numLeavesLeft)-Math.log(numLeavesLeft-1));
			// threshold = (float)(Math.log(numLeavesLeft)-Math.log(numLeavesLeft-1));
			threshold = 0;
			// threshold = 0.01f;
			minDeltaGI = threshold; // if we cannot find any that is better, stop.
			bestPair1 = bestPair2 = -1;
			for (int i = 0; i < newLeaves.length - 1; i++) {
				if (newLeaves[i] == null)
					continue;
				for (int j = i + 1; j < newLeaves.length; j++) {
					if (newLeaves[j] == null)
						continue;
					if (deltaGI[i][j - i - 1] < minDeltaGI) {
						bestPair1 = i;
						bestPair2 = j;
						minDeltaGI = deltaGI[i][j - i - 1];
					}
				}
			}
			// System.out.printf("NumLeavesLeft=%4d, threshold=%f, minDeltaGI=%f\n", numLeavesLeft, threshold, minDeltaGI);
			if (minDeltaGI < threshold) { // found something to merge
				mergeLeaves(newLeaves[bestPair1], newLeaves[bestPair2]);
				numLeavesLeft--;
				// System.out.println("Merged leaves "+bestPair1+" and "+bestPair2+" (deltaGI: "+minDeltaGI+")");
				newLeaves[bestPair2] = null;
				// Update deltaGI table:
				for (int i = 0; i < bestPair2; i++) {
					deltaGI[i][bestPair2 - i - 1] = Float.NaN;
				}
				for (int j = bestPair2 + 1; j < newLeaves.length; j++) {
					deltaGI[bestPair2][j - bestPair2 - 1] = Float.NaN;
				}
				for (int i = 0; i < bestPair1; i++) {
					if (newLeaves[i] != null)
						deltaGI[i][bestPair1 - i - 1] = (float) computeDeltaGI(newLeaves[i], newLeaves[bestPair1]);
				}
				for (int j = bestPair1 + 1; j < newLeaves.length; j++) {
					if (newLeaves[j] != null)
						deltaGI[bestPair1][j - bestPair1 - 1] = (float) computeDeltaGI(newLeaves[bestPair1], newLeaves[j]);
				}
			}
		} while (minDeltaGI < threshold);

		int nLeavesLeft = 0;
		List<LeafNode> survivors = new ArrayList<LeafNode>();
		for (int i = 0; i < newLeaves.length; i++) {
			if (newLeaves[i] != null) {
				nLeavesLeft++;
				survivors.add((LeafNode) ((DirectedGraphNode) newLeaves[i]).getLeafNode());
			}
		}

		long clusteredTime = System.currentTimeMillis();

		System.out.printf("%5d leaves, gi=%7.3f).", nLeavesLeft, computeGlobalImpurity(survivors));

		deltaGI = null;
		impurities.clear();

		float testDist = rmsDistanceTestData(graph);
		System.out.printf(" Distance test data: %5.3f", testDist);

		System.out.printf(" | fs %5dms, cl %5dms", (featSelectedTime - startTime), (clusteredTime - featSelectedTime));

		System.out.println();

		// Stop criterion: stop if feature selection does not succeed in reducing global impurity further,
		// and at the same time, the test data approximation is getting worse.
		if (minFSGI > prevFSGI && testDist > prevTestDataDistance) {
			canClusterMore = false;
		}
		// Iteration step:
		prevFeatureList = newFeatureList;
		prevFSGI = minFSGI;
		prevTestDataDistance = testDist;

		return graph;
	}

	private void verifyFeatureQuality(int fi, CART testCART, int prevNLeaves) {
		List<LeafNode> leaves = new ArrayList<LeafNode>();
		int nLeaves = 0;
		for (LeafNode leaf : testCART.getLeafNodes()) {
			if (leaf.isEmpty())
				continue;
			leaves.add(leaf);
			nLeaves++;
		}
		if (nLeaves <= prevNLeaves) { // this feature adds no leaf
			return; // will not consider this further
		}
		double gi = computeGlobalImpurity(leaves, minCriterion);
		// More leaves cost a bit:
		double sizeBias = Math.log((float) nLeaves / prevNLeaves);
		assert sizeBias > 0;
		// double sizeBias = (float)nLeaves/prevNLeaves;
		// assert sizeBias > 1;

		// System.out.printf("%30s: GI=%.3f bias=%.7f\n", featureDefinition.getFeatureName(fi),gi,sizeBias);
		double criterion = gi;
		/*
		 * if (gi > globalMean) { // The best one is the one that can reach a small gi with a small increase in number of leaves
		 * criterion = globalMean + (gi-globalMean) * (1+sizeBias); } else { // leave as is, no size bias }
		 */
		if (criterion < minCriterion) {
			setMinCriterion(criterion);
			setMinFSGI(gi);
			setBestFeature(fi);
		}

	}

	/**
	 * Estimate the mean of all *distances* in the training set.
	 * 
	 * @param leaves
	 *            leaves
	 * @return computeglobalimpurity(leaves, double.Positive_infinity)
	 */
	/*
	 * private void estimateGlobalMean(FeatureVector[] data, DistanceMeasure dist) { int sampleSize = 100000;
	 * System.out.println("Estimating global mean by random sampling "+sampleSize+" distances"); long startTime =
	 * System.currentTimeMillis(); // Compute mean and stddev using recurrence relation, attributed by Donald Knuth // (The Art of
	 * Computer Programming, Volume 2: Seminumerical Algorithms, Section 4.2.2) // to B.P. Welford, Technometrics, 4, (1962),
	 * 419-420. // M(1) = x(1), M(k) = M(k-1) + (x(k) - M(k-1))/k // S(1) = 0, S(k) = S(k-1) + (x(k) - M(k-1))*(x(k)-M(k)) // for
	 * 2 <= k <= n, then sigma = sqrt(S(n)/(n-1)) // globalMean = 0; Random random = new Random(); for (int k=1; k<sampleSize;
	 * k++) { int i = random.nextInt(data.length); int j = random.nextInt(data.length); double xk = dist.distance(data[i],
	 * data[j]); double mk = globalMean + (xk - globalMean) / k; globalMean = mk; } //globalMean = Math.sqrt(globalMean); long
	 * endTime = System.currentTimeMillis();
	 * System.out.println("Computation of "+sampleSize+" distances took "+(endTime-startTime)+" ms");
	 * System.out.println("Global mean distance = "+globalMean); }
	 */

	private double computeGlobalImpurity(List<LeafNode> leaves) {
		return computeGlobalImpurity(leaves, Double.POSITIVE_INFINITY);
	}

	/**
	 * Compute global impurity as the weighted sum of leaf impurities. stop when cutoff value is reached or surpassed.
	 * 
	 * @param leaves
	 *            leaves
	 * @param cutoff
	 *            cutoff
	 * @return gi
	 */
	private double computeGlobalImpurity(List<LeafNode> leaves, double cutoff) {
		cutoff *= trainingFeatures.length;
		double gi = 0;
		// Global Impurity measures the average distance of an instance
		// to the other instances in the same leaf.
		// Global Impurity is computed as follows:
		// GI = 1/N * sum(|l| * I(l)), where
		// N = total number of instances (feature vectors);
		// |l| = the number of instances in a leaf;
		// I(l) = the impurity of the leaf.
		int numLeaves = 0;
		for (LeafNode leaf : leaves) {
			if (leaf.isEmpty())
				continue;
			gi += leaf.getNumberOfData() * computeImpurity(leaf);
			numLeaves++;
			if (gi >= cutoff) { // too high, stop it
				// System.out.println("Cutoff exceeded, breaking");
				break;
			}
		}
		gi /= trainingFeatures.length;
		return gi;
	}

	private double computeImpurity(LeafNode leaf) {
		/*
		 * impurities.remove(leaf); double i1 = computeMutualDistanceImpurity(leaf); impurities.remove(leaf); double i2 =
		 * computeVarianceImpurity(leaf); System.out.printf("mdi=%.3f, vi=%.3f\n", i1, i2); return i2;
		 */
		// return computeMutualDistanceImpurity(leaf);
		return computeVarianceImpurity(leaf);
	}

	/**
	 * The impurity of a leaf node is computed as follows: I(l) = sqrt( 2/(|l|*(|l|-1)) * sum over all pairs(distance of pair) ),
	 * where |l| = the number of instances in the leaf.
	 * 
	 * @param leaf
	 *            leaf
	 * @return impurity
	 */
	/*
	 * private double computeMutualDistanceImpurity(LeafNode leaf) { if (!(leaf instanceof FeatureVectorLeafNode)) throw new
	 * IllegalArgumentException("Currently only feature vector leaf nodes are supported"); if (impurities.containsKey(leaf))
	 * return impurities.get(leaf); FeatureVectorLeafNode l = (FeatureVectorLeafNode) leaf; FeatureVector[] fvs =
	 * l.getFeatureVectors(); int[] leafIndices = new int[fvs.length]; for (int i=0; i<fvs.length; i++) { leafIndices[i] =
	 * fvs[i].getUnitIndex(); } int len = fvs.length; double impurity = globalMean * Math.exp(-(len-1)); if (len < 2) return
	 * impurity; double rmsDistance = 0; //System.out.println("Leaf has "+n+" items, computing "+(n*(n-1)/2)+" distances"); for
	 * (int i=0; i<len; i++) { int li = leafIndices[i]; for (int j=i+1; j<len; j++) { int lj = leafIndices[j]; if (li < lj) {
	 * rmsDistance += squaredDistances[li][lj-li-1]; } else if (lj < li) { rmsDistance += squaredDistances[lj][li-lj-1]; } } }
	 * rmsDistance *= 2./(len*(len-1)); rmsDistance = Math.sqrt(rmsDistance);
	 * 
	 * //System.out.println("len="+len+", baseI="+impurity+", rmsDist="+rmsDistance); impurity += rmsDistance;
	 * 
	 * // Normalise impurity: //impurity -= globalMean; //impurity /= globalStddev;
	 * 
	 * 
	 * // Penalty for small leaves: //impurity += (float)SINGLE_ITEM_IMPURITY/(len*len);
	 * 
	 * impurities.put(leaf, impurity); return impurity; }
	 */

	private double computeVarianceImpurity(LeafNode leaf) {
		if (!(leaf instanceof FeatureVectorLeafNode))
			throw new IllegalArgumentException("Currently only feature vector leaf nodes are supported");
		if (impurities.containsKey(leaf))
			return impurities.get(leaf);
		FeatureVectorLeafNode l = (FeatureVectorLeafNode) leaf;
		FeatureVector[] fvs = l.getFeatureVectors();
		int[] leafIndices = new int[fvs.length];
		for (int i = 0; i < fvs.length; i++) {
			leafIndices[i] = fvs[i].getUnitIndex();
		}
		int len = fvs.length;
		double impurity = globalStddev * Math.exp(-(len - 1));
		if (len < 2)
			return impurity;

		double variance = ((F0ContourPolynomialDistanceMeasure) dist).computeVariance(fvs);

		impurity += Math.sqrt(variance);

		impurities.put(leaf, impurity);
		return impurity;

	}

	private double computeDeltaGI(DirectedGraphNode dgn1, DirectedGraphNode dgn2) {
		// return computeMutualDistanceDeltaGI(dgn1, dgn2);
		return computeVarianceDeltaGI(dgn1, dgn2);
	}

	/**
	 * The delta in global impurity that would be caused by merging the two given leaves is computed as follows. Delta GI =
	 * (|l1|+|l2|) * I(l1 united with l2) - |l1| * I(l1) - |l2| * I(l2) = 1/N*(|l1|+|l2|-1) * (sum of all distances between items
	 * in l1 and items in l2 - |l2| * I(l1) - |l1| * I(l2) ) where N = sum of all |l| = total number of instances in the tree,
	 * |l| = number of instances in the leaf l
	 * 
	 * @param dgn1
	 *            dgn1
	 * @param dgn2
	 *            dgn2
	 * @return deltaGI
	 */
	private double computeMutualDistanceDeltaGI(DirectedGraphNode dgn1, DirectedGraphNode dgn2) {
		FeatureVectorLeafNode l1 = (FeatureVectorLeafNode) dgn1.getLeafNode();
		FeatureVectorLeafNode l2 = (FeatureVectorLeafNode) dgn2.getLeafNode();
		FeatureVector[] fv1 = l1.getFeatureVectors();
		int[] indices1 = new int[fv1.length];
		for (int i = 0; i < fv1.length; i++) {
			indices1[i] = fv1[i].getUnitIndex();
		}
		FeatureVector[] fv2 = l2.getFeatureVectors();
		int[] indices2 = new int[fv2.length];
		for (int j = 0; j < fv2.length; j++) {
			indices2[j] = fv2[j].getUnitIndex();
		}
		double deltaGI = 0;
		int len1 = l1.getNumberOfData();
		int len2 = l2.getNumberOfData();
		int len12 = len1 + len2;
		double imp1 = computeImpurity(l1);
		double imp2 = computeImpurity(l2);

		double imp12 = len1 * (len1 - 1) / 2 * imp1 * imp1 + len2 * (len2 - 1) / 2 * imp2 * imp2;
		// Sum of all distances across leaf boundaries:
		for (int i = 0; i < fv1.length; i++) {
			int li = indices1[i];
			for (int j = 0; j < fv2.length; j++) {
				int lj = indices2[j];
				if (li < lj) {
					imp12 += squaredDistances[li][lj - li - 1];
				} else if (lj < li) {
					imp12 += squaredDistances[lj][li - lj - 1];
				}
			}
		}
		imp12 *= 2. / (len12 * (len12 - 1));
		imp12 = Math.sqrt(imp12);

		deltaGI = 1. / trainingFeatures.length * (len12 * imp12 - len1 * imp1 - len2 * imp2);
		// Encourage small leaves to merge:
		// double sizeEffect = 0.01 * (1./((len1+len2)*(len1+len2)) - 1./(len1*len1) - 1./(len2*len2));
		// System.out.println("len1="+len1+", len2="+len2+", sizeEffect="+sizeEffect+", deltaGI="+deltaGI);
		// deltaGI += sizeEffect;
		return deltaGI;
	}

	private double computeVarianceDeltaGI(DirectedGraphNode dgn1, DirectedGraphNode dgn2) {
		FeatureVectorLeafNode l1 = (FeatureVectorLeafNode) dgn1.getLeafNode();
		FeatureVectorLeafNode l2 = (FeatureVectorLeafNode) dgn2.getLeafNode();
		FeatureVector[] fv1 = l1.getFeatureVectors();
		int[] indices1 = new int[fv1.length];
		for (int i = 0; i < fv1.length; i++) {
			indices1[i] = fv1[i].getUnitIndex();
		}
		FeatureVector[] fv2 = l2.getFeatureVectors();
		int[] indices2 = new int[fv2.length];
		for (int j = 0; j < fv2.length; j++) {
			indices2[j] = fv2[j].getUnitIndex();
		}
		double deltaGI = 0;
		int len1 = fv1.length;
		int len2 = fv2.length;
		double imp1 = computeImpurity(l1);
		double imp2 = computeImpurity(l2);

		FeatureVector[] fv12 = new FeatureVector[fv1.length + fv2.length];
		System.arraycopy(fv1, 0, fv12, 0, fv1.length);
		System.arraycopy(fv2, 0, fv12, fv1.length, fv2.length);
		int len12 = fv12.length;
		double imp12 = globalStddev * Math.exp(-(len12 - 1));
		double variance = ((F0ContourPolynomialDistanceMeasure) dist).computeVariance(fv12);
		imp12 += Math.sqrt(variance);

		deltaGI = 1. / trainingFeatures.length * (len12 * imp12 - len1 * imp1 - len2 * imp2);
		// System.out.printf("deltaGI=%.3f -- I(%d)=%.3f, I(%d)=%.3f => I(%d)=%.3f\n", deltaGI, len1, imp1, len2, imp2, len12,
		// imp12);
		return deltaGI;
	}

	private void mergeLeaves(DirectedGraphNode dgn1, DirectedGraphNode dgn2) {
		// Copy all data from dgn2 into dgn1
		FeatureVectorLeafNode l1 = (FeatureVectorLeafNode) dgn1.getLeafNode();
		FeatureVectorLeafNode l2 = (FeatureVectorLeafNode) dgn2.getLeafNode();
		FeatureVector[] fv1 = l1.getFeatureVectors();
		FeatureVector[] fv2 = l2.getFeatureVectors();
		FeatureVector[] newFV = new FeatureVector[fv1.length + fv2.length];
		System.arraycopy(fv1, 0, newFV, 0, fv1.length);
		System.arraycopy(fv2, 0, newFV, fv1.length, fv2.length);
		l1.setFeatureVectors(newFV);
		// then update all mother/daughter relationships
		Set<Node> dgn2Mothers = new HashSet<Node>(dgn2.getMothers());
		for (Node mother : dgn2Mothers) {
			if (mother instanceof DecisionNode) {
				DecisionNode dm = (DecisionNode) mother;
				dm.replaceDaughter(dgn1, dgn2.getNodeIndex(mother));
			} else if (mother instanceof DirectedGraphNode) {
				DirectedGraphNode gm = (DirectedGraphNode) mother;
				gm.setLeafNode(dgn1);
			}
			dgn2.removeMother(mother);
		}
		dgn2.setLeafNode(null);
		l2.setMother(null, 0);
		// and remove impurity entries:
		try {
			impurities.remove(l1);
			impurities.remove(l2);
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.err.println("Impurities: " + impurities + ", l1:" + l1 + ", l2:" + l2);
		}
	}

	private void updateGraphFromTree(DecisionNode treeNode, DirectedGraphNode graphNode, List<DirectedGraphNode> newLeaves) {
		int treeFeatureIndex = treeNode.getFeatureIndex();
		int treeNumDaughters = treeNode.getNumberOfDaugthers();
		DecisionNode graphDecisionNode = graphNode.getDecisionNode();
		if (graphDecisionNode != null) {
			// Sanity check: the two must be aligned: same feature, same number of children
			int graphFeatureIndex = graphDecisionNode.getFeatureIndex();
			assert treeFeatureIndex == graphFeatureIndex : "Tree indices out of sync!";
			assert treeNumDaughters == graphDecisionNode.getNumberOfDaugthers() : "Tree structure out of sync!";
			// OK, now recursively call ourselves for all daughters
			for (int i = 0; i < treeNumDaughters; i++) {
				// We expect the next tree node to be a decision node (unless it is an empty node),
				// because the level just above the leaves does not exist in graph yet.
				Node nextTreeNode = treeNode.getDaughter(i);
				if (nextTreeNode == null)
					continue;
				else if (nextTreeNode instanceof LeafNode) {
					assert ((LeafNode) nextTreeNode).getNumberOfData() == 0;
					continue;
				}
				assert nextTreeNode instanceof DecisionNode;
				DirectedGraphNode nextGraphNode = (DirectedGraphNode) graphDecisionNode.getDaughter(i);
				updateGraphFromTree((DecisionNode) nextTreeNode, nextGraphNode, newLeaves);
			}
		} else {
			// No structure in graph yet which corresponds to tree.
			// This is what we actually want to do.
			if (featureDefinition.isByteFeature(treeFeatureIndex)) {
				graphDecisionNode = new DecisionNode.ByteDecisionNode(treeFeatureIndex, treeNumDaughters, featureDefinition);
			} else {
				assert featureDefinition.isShortFeature(treeFeatureIndex) : "Only support byte and short features";
				graphDecisionNode = new DecisionNode.ShortDecisionNode(treeFeatureIndex, treeNumDaughters, featureDefinition);
			}
			assert treeNumDaughters == graphDecisionNode.getNumberOfDaugthers();
			graphNode.setDecisionNode(graphDecisionNode);
			for (int i = 0; i < treeNumDaughters; i++) {
				// we expect the next tree node to be a leaf node
				LeafNode nextTreeNode = (LeafNode) treeNode.getDaughter(i);
				// Now create the new daughter number i of graphDecisionNode.
				// It is a DirectedGraphNode containing no decision tree but
				// a leaf node, which is itself a DirectedGraphNode with no
				// decision node but a leaf node:
				if (nextTreeNode != null && nextTreeNode.getNumberOfData() > 0) {
					DirectedGraphNode daughterLeafNode = new DirectedGraphNode(null, nextTreeNode);
					DirectedGraphNode daughterNode = new DirectedGraphNode(null, daughterLeafNode);
					graphDecisionNode.addDaughter(daughterNode);
					newLeaves.add(daughterLeafNode);
				} else {
					graphDecisionNode.addDaughter(null);
				}
			}
		}
	}

	private float rmsDistanceTestData(DirectedGraph graph) {
		// return rmsMutualDistanceTestData(graph);
		return rmsMeanDistanceTestData(graph);
	}

	private float rmsMeanDistanceTestData(DirectedGraph graph) {
		float avgDist = 0;
		for (int i = 0; i < testFeatures.length; i++) {
			int ti = testFeatures[i].getUnitIndex();
			FeatureVector[] leafData = (FeatureVector[]) graph.interpret(testFeatures[i]);
			float[] mean = ((F0ContourPolynomialDistanceMeasure) dist).computeMean(leafData);
			float oneDist = ((F0ContourPolynomialDistanceMeasure) dist).squaredDistance(testFeatures[i], mean);
			oneDist = (float) Math.sqrt(oneDist);
			avgDist += oneDist;
		}
		avgDist /= testFeatures.length;

		return avgDist;

	}

	private float rmsMutualDistanceTestData(DirectedGraph graph) {
		float avgDist = 0;
		for (int i = 0; i < testFeatures.length; i++) {
			int ti = testFeatures[i].getUnitIndex();
			FeatureVector[] leafData = (FeatureVector[]) graph.interpret(testFeatures[i]);
			float oneDist = 0;
			for (int j = 0; j < leafData.length; j++) {
				int lj = leafData[j].getUnitIndex();
				if (ti < lj) {
					oneDist += squaredDistances[ti][lj - ti - 1];
				} else if (lj < ti) {
					oneDist += squaredDistances[lj][ti - lj - 1];
				}
			}
			oneDist /= leafData.length;
			oneDist = (float) Math.sqrt(oneDist);
			avgDist += oneDist;
		}
		avgDist /= testFeatures.length;

		return avgDist;
	}

	private void setMinCriterion(double value) {
		minCriterion = value;
	}

	private void setMinFSGI(double value) {
		minFSGI = value;
	}

	private void setBestFeature(int featureIndex) {
		iBestFeature = featureIndex;
	}

	private void debugOut(DirectedGraph graph) {
		for (Iterator<Node> it = graph.getNodeIterator(); it.hasNext();) {
			Node next = it.next();
			debugOut(next);
		}
	}

	private void debugOut(CART graph) {
		Node root = graph.getRootNode();
		debugOut(root);
	}

	private void debugOut(Node node) {
		if (node instanceof DirectedGraphNode)
			debugOut((DirectedGraphNode) node);
		else if (node instanceof LeafNode)
			debugOut((LeafNode) node);
		else
			debugOut((DecisionNode) node);
	}

	private void debugOut(DirectedGraphNode node) {
		System.out.println("DGN");
		if (node.getLeafNode() != null)
			debugOut(node.getLeafNode());
		if (node.getDecisionNode() != null)
			debugOut(node.getDecisionNode());
	}

	private void debugOut(LeafNode node) {
		System.out.println("Leaf: " + node.getDecisionPath());
	}

	private void debugOut(DecisionNode node) {
		System.out.println("DN with " + node.getNumberOfDaugthers() + " daughters: " + node.toString());
		for (int i = 0; i < node.getNumberOfDaugthers(); i++) {
			Node daughter = node.getDaughter(i);
			if (daughter == null)
				System.out.println("null");
			else
				debugOut(daughter);
		}
	}

}
