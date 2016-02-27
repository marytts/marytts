/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.cart.impose;

import java.util.Arrays;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * A class branched from FeatureFileIndexer which works directly on a feature array, rather than extending FeatureFileReader.
 * 
 * @author Marc Schr&ouml;der
 * 
 */
public class FeatureArrayIndexer {

	private MaryNode tree = null;
	private int[] featureSequence = null;
	private FeatureComparator c = new FeatureComparator(-1, null);
	private UnitIndexComparator cui = new UnitIndexComparator();
	private FeatureVector[] featureVectors;
	private FeatureDefinition featureDefinition;

	private long numberOfLeaves = 0;

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Constructor which takes an array of feature vectors and launches an indexing operation according to a feature sequence
	 * constraint.
	 * 
	 * @param featureVectors
	 *            an array of feature vectors
	 * @param featureDefinition
	 *            a feature definition to make sense of the feature vectors
	 * @param setFeatureSequence
	 *            An array of indexes indicating the hierarchical order (or, equivalently, the sequence) of the features to use
	 *            for the indexing.
	 * 
	 */
	public FeatureArrayIndexer(FeatureVector[] featureVectors, FeatureDefinition featureDefinition, int[] setFeatureSequence) {
		this(featureVectors, featureDefinition);
		deepSort(setFeatureSequence);
	}

	/**
	 * Constructor which takes an array of feature vectors and launches an indexing operation according to a feature sequence
	 * constraint.
	 * 
	 * @param featureVectors
	 *            an array of feature vectors
	 * @param featureDefinition
	 *            a feature definition to make sense of the feature vectors
	 * @param setFeatureSequence
	 *            An array of feature names indicating the hierarchical order (or, equivalently, the sequence) of the features to
	 *            use for the indexing.
	 * 
	 */
	public FeatureArrayIndexer(FeatureVector[] featureVectors, FeatureDefinition featureDefinition, String[] setFeatureSequence) {
		this(featureVectors, featureDefinition);
		deepSort(setFeatureSequence);
	}

	/**
	 * Constructor which loads the feature vector array but does not launch an indexing operation.
	 * 
	 * @param featureVectors
	 *            an array of feature vectors
	 * @param featureDefinition
	 *            a feature definition to make sense of the feature vectors
	 * 
	 */
	public FeatureArrayIndexer(FeatureVector[] featureVectors, FeatureDefinition featureDefinition) {
		this.featureVectors = featureVectors;
		this.featureDefinition = featureDefinition;
	}

	/********************/
	/* INDEXING METHODS */
	/********************/

	/**
	 * A local sort at a particular node along the deep sorting operation. This is a recursive function.
	 * 
	 * @param currentFeatureIdx
	 *            The currently tested feature.
	 * @param currentNode
	 *            The current node, holding the currently processed zone in the array of feature vectors.
	 */
	private void sortNode(int currentFeatureIdx, MaryNode currentNode) {
		/* If we have reached a leaf, do a final sort according to the unit index and return: */
		if (currentFeatureIdx == featureSequence.length) {
			Arrays.sort(featureVectors, currentNode.from, currentNode.to, cui);
			numberOfLeaves++;
			/*
			 * System.out.print( "LEAF ! (" + (currentNode.to-currentNode.from) + " units)" ); for ( int i = currentNode.from; i <
			 * currentNode.to; i++ ) { System.out.print( " (" + featureVectors[i].getUnitIndex() + " 0)" ); } System.out.println(
			 * "" );
			 */
			return;
		}
		/* Else: */
		int currentFeature = featureSequence[currentFeatureIdx];
		FeatureVector.FeatureType featureType = featureVectors[0].getFeatureType(currentFeature);
		/* Register the feature currently used for the splitting */
		currentNode.setFeatureIndex(currentFeature);
		/* Perform the sorting according to the currently considered feature: */
		/* 1) position the comparator onto the right feature */
		c.setFeatureIdx(currentFeature, featureType);
		/* 2) do the sorting */
		Arrays.sort(featureVectors, currentNode.from, currentNode.to, c);

		/*
		 * Then, seek for the zones where the feature value is the same, and launch the next sort level on these.
		 */
		int nVal = featureDefinition.getNumberOfValues(currentFeature);
		currentNode.split(nVal);
		int nextFrom = currentNode.from;
		int nextTo = currentNode.from;
		for (int i = 0; i < nVal; i++) {
			nextFrom = nextTo;
			// System.out.print( "Next node begins at " + nextFrom );
			while ((nextTo < currentNode.to) && (featureVectors[nextTo].getFeatureAsInt(currentFeature) == i)) {
				// System.out.print( " " + featureVectors[nextTo].getFeatureAsInt( currentFeature ) );
				nextTo++;
			}
			// System.out.println( " and ends at " + nextTo + " for a total of " + (nextTo-nextFrom) + " units." );
			if ((nextTo - nextFrom) != 0) {
				MaryNode nod = new MaryNode(nextFrom, nextTo);
				currentNode.setChild(i, nod);
				// System.out.print("(" + i + " isByteOf " + currentFeature + ")" );
				sortNode(currentFeatureIdx + 1, nod);
			} else
				currentNode.setChild(i, null);
		}
	}

	/**
	 * Launches a deep sort on the array of feature vectors. This is public because it can be used to re-index the previously read
	 * feature file.
	 * 
	 * @param setFeatureSequence
	 *            An array of feature indexes, indicating the sequence of features according to which the sorting should be
	 *            performed.
	 */
	public void deepSort(int[] setFeatureSequence) {
		featureSequence = setFeatureSequence;
		numberOfLeaves = 0;
		tree = new MaryNode(0, featureVectors.length);
		sortNode(0, tree);
	}

	/**
	 * Launches a deep sort on the array of feature vectors. This is public because it can be used to re-index the previously read
	 * feature file.
	 * 
	 * @param setFeatureSequence
	 *            An array of feature names, indicating the sequence of features according to which the sorting should be
	 *            performed.
	 */
	public void deepSort(String[] setFeatureSequence) {
		featureSequence = featureDefinition.getFeatureIndexArray(setFeatureSequence);
		numberOfLeaves = 0;
		tree = new MaryNode(0, featureVectors.length);
		sortNode(0, tree);
	}

	/**
	 * Fill a particular node of a pre-specified tree. This is a recursive function.
	 * 
	 * @param currentNode
	 *            The current node, holding the currently processed zone in the array of feature vectors.
	 */
	private void fillNode(MaryNode currentNode) {
		/* If we have reached a leaf, do a final sort according to the unit index and return: */
		if (currentNode.isLeaf()) {
			Arrays.sort(featureVectors, currentNode.from, currentNode.to, cui);
			numberOfLeaves++;
			/*
			 * System.out.print( "LEAF ! (" + (currentNode.to-currentNode.from) + " units)" ); for ( int i = currentNode.from; i <
			 * currentNode.to; i++ ) { System.out.print( " (" + featureVectors[i].getUnitIndex() + " 0)" ); } System.out.println(
			 * "" );
			 */
			return;
		}
		/* Else: */
		int currentFeature = currentNode.featureIndex;
		FeatureVector.FeatureType featureType = featureVectors[0].getFeatureType(currentFeature);
		/* Perform the sorting according to the currently considered feature: */
		/* 1) position the comparator onto the right feature */
		c.setFeatureIdx(currentFeature, featureType);
		/* 2) do the sorting */
		Arrays.sort(featureVectors, currentNode.from, currentNode.to, c);

		/*
		 * Then, seek for the zones where the feature value is the same, and launch the next sort level on these.
		 */
		int nVal = featureDefinition.getNumberOfValues(currentFeature);
		int nextFrom = currentNode.from;
		int nextTo = currentNode.from;
		for (int i = 0; i < nVal; i++) {
			nextFrom = nextTo;
			// System.out.print( "Next node begins at " + nextFrom );
			while ((nextTo < currentNode.to) && (featureVectors[nextTo].getFeatureAsInt(currentFeature) == i)) {
				// System.out.print( " " + featureVectors[nextTo].getFeatureAsInt( currentFeature ) );
				nextTo++;
			}
			// System.out.println( " and ends at " + nextTo + " for a total of " + (nextTo-nextFrom) + " units." );
			if ((nextTo - nextFrom) != 0) {
				MaryNode nod = currentNode.getChild(i);
				if (nod != null) {
					nod.from = nextFrom;
					nod.to = nextTo;
					fillNode(nod);
				}
			} else
				currentNode.setChild(i, null);
		}
	}

	/**
	 * Fill a tree which specifies a feature hierarchy but no corresponding units.
	 * 
	 * @param specTree
	 *            A specific tree
	 * 
	 */
	public void deepFill(MaryNode specTree) {
		tree = specTree;
		numberOfLeaves = 0;
		sortNode(0, tree);
	}

	/***************************/
	/* QUERY/RETRIEVAL METHODS */
	/***************************/

	/**
	 * Retrieve an array of unit features which complies with a specific target specification, according to an underlying tree.
	 * 
	 * @param v
	 *            A feature vector for which to send back an array of complying unit indexes.
	 * @return A query result, comprising an array of feature vectors and the depth level which was actually reached.
	 * 
	 * @see FeatureArrayIndexer#deepSort(int[])
	 * @see FeatureArrayIndexer#deepFill(MaryNode)
	 */
	public FeatureFileIndexingResult retrieve(FeatureVector v) {
		int level = 0;
		/* Check if the tree is there */
		if (tree == null) {
			throw new RuntimeException("Can't retrieve candidate units if a tree has not been built."
					+ " (Run this.deepSort(int[]) or this.deepFill(MaryNode) first.)");
		}
		/* Walk down the tree */
		MaryNode n = tree;
		MaryNode next = null;
		while (!n.isLeaf()) {
			next = n.getChild(v.getFeatureAsInt(n.getFeatureIndex()));
			/* Check if the next node is a dead branch */
			if (next != null) {
				n = next;
				level++;
			} else
				break;
		}
		/* Dereference the reached node or leaf */
		FeatureFileIndexingResult qr = new FeatureFileIndexingResult(getFeatureVectors(n.from, n.to), level);
		return (qr);
	}

	public static final int MAXDEPTH = 0;
	public static final int MAXLEVEL = 1;
	public static final int MINUNITS = 2;

	/**
	 * Retrieve an array of unit features which complies with a specific target specification, according to an underlying tree,
	 * and given a stopping condition.
	 * 
	 * @param v
	 *            A feature vector for which to send back an array of complying unit indexes.
	 * @param condition
	 *            A constant indicating a stopping criterion, among: FeatureFileIndexer.MAXDEPTH : walk the tree until its leaves
	 *            (maximum depth); FeatureFileIndexer.MAXLEVEL : walk the tree until a certain depth level;
	 *            FeatureFileIndexer.MINUNITS : walk the tree until a certain number of units is reached.
	 * @param parameter
	 *            A parameter interpreted according to the above condition: MAXDEPTH &rarr; parameter is ignored; MAXLEVEL &rarr;
	 *            parameter = maximum level to reach; MINUNITS &rarr; parameter = lower bound on the number of units to return.
	 * 
	 * @return A query result, comprising an array of feature vectors and the depth level which was actually reached.
	 * 
	 * @see FeatureArrayIndexer#deepSort(int[])
	 * @see FeatureArrayIndexer#deepFill(MaryNode)
	 */
	public FeatureFileIndexingResult retrieve(FeatureVector v, int condition, int parameter) {
		int level = 0;
		/* Check if the tree is there */
		if (tree == null) {
			throw new RuntimeException("Can't retrieve candidate units if a tree has not been built."
					+ " (Run this.deepSort(int[]) or this.deepFill(MaryNode) first.)");
		}
		// /**/
		// /* TODO: Do we want the warning below? */
		// /*if ( (condition == MAXLEVEL) && (featureSequence != null) && (parameter > featureSequence.length) ) {
		// System.out.println( "WARNING: you asked for more levels [" + maxLevel
		// + "] than the length of the underlying feature sequence[" + featureSequence.length + "]. Proceeding anyways." );
		// }*/
		/* Walk down the tree */
		MaryNode n = tree;
		MaryNode next = null;
		while (!n.isLeaf()) {
			next = n.getChild(v.getFeatureAsInt(n.getFeatureIndex()));
			/* Check for the number of units in the next node */
			if ((condition == MINUNITS) && ((next.to - next.from) < parameter))
				break;
			/* Check if the next node is a dead branch */
			if (next != null) {
				n = next;
				level++;
			} else
				break;
			/* Check for the current level */
			if ((condition == MAXLEVEL) && (level == parameter))
				break;
		}
		/* Dereference the reached node or leaf */
		FeatureFileIndexingResult qr = new FeatureFileIndexingResult(getFeatureVectors(n.from, n.to), level);
		return (qr);
	}

	/***************************/
	/* MISCELLANEOUS ACCESSORS */
	/***************************/

	/**
	 * Get the feature sequence, as an information about the underlying tree structure.
	 * 
	 * @return the feature sequence
	 */
	public int[] getFeatureSequence() {
		return featureSequence;
	}

	/**
	 * Get the tree
	 * 
	 * @return the tree
	 */
	public MaryNode getTree() {
		return tree;
	}

	/**
	 * Get the feature vectors from the big array according to the given indices
	 * 
	 * @param from
	 *            the start index
	 * @param to
	 *            the end index
	 * @return the feature vectors
	 */
	public FeatureVector[] getFeatureVectors(int from, int to) {
		FeatureVector[] vectors = new FeatureVector[to - from];
		for (int i = from; i < to; i++) {
			vectors[i - from] = featureVectors[i];
		}
		return vectors;
	}

	public FeatureDefinition getFeatureDefinition() {
		return featureDefinition;
	}

	/**
	 * Get the number of leaves.
	 * 
	 * @return The number of leaves, or -1 if the tree has not been computed.
	 */
	public long getNumberOfLeaves() {
		if (tree == null)
			return (-1);
		return (numberOfLeaves);
	}

	/**
	 * Get the theoretical number of leaves, given a feature sequence.
	 * 
	 * @param feaSeq
	 *            feaSeq
	 * @return The number of leaves, or -1 if the capacity of the long integer was blown.
	 */
	public long getTheoreticalNumberOfLeaves(int[] feaSeq) {
		long ret = 1;
		for (int i = 0; i < feaSeq.length; i++) {
			// System.out.println( "Feature [" + i + "] has [" + featureDefinition.getNumberOfValues( featureSequence[i] ) +
			// "] values."
			// + "(Number of leaves = [" + ret + "].)" );
			ret *= featureDefinition.getNumberOfValues(feaSeq[i]);
			if (ret < 0)
				return (-1);
		}
		return (ret);
	}
}
