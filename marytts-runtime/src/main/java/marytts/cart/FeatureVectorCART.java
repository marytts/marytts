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
package marytts.cart;

import java.io.IOException;

import marytts.cart.impose.FeatureArrayIndexer;
import marytts.cart.impose.MaryNode;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * @author marc
 * 
 */
public class FeatureVectorCART extends CART {

	/**
	 * Convert the given Mary node tree into a CART with the leaves containing featureVectors
	 * 
	 * @param tree
	 *            the tree
	 * @param ffi
	 *            the feature file indexer containing the feature vectors
	 */
	public FeatureVectorCART(MaryNode tree, FeatureArrayIndexer ffi) {
		featDef = ffi.getFeatureDefinition();
		addDaughters(null, tree, ffi);
		if (rootNode instanceof DecisionNode) {
			((DecisionNode) rootNode).countData();
		}
	}

	public void load(String fileName, FeatureDefinition featDefinition, String[] setFeatureSequence) throws IOException {
		throw new IllegalStateException("load() not implemented for FeatureVectorCART");
	}

	/**
	 * Add the given tree node as a daughter to the given mother node
	 * 
	 * @param motherCARTNode
	 *            the mother node
	 * @param currentTreeNode
	 *            the tree node that we want to add
	 * @param ffi
	 *            the feature file indexer containing the feature vectors
	 */
	private void addDaughters(DecisionNode motherCARTNode, MaryNode currentTreeNode, FeatureArrayIndexer ffi) {
		if (currentTreeNode == null) {
			LeafNode l = new LeafNode.FeatureVectorLeafNode(new FeatureVector[0]);
			motherCARTNode.addDaughter(l);
			return;
		}
		if (currentTreeNode.isNode()) { // if we are not at a leaf

			// System.out.print("Adding node, ");
			// the next daughter
			DecisionNode daughterNode = null;
			// the number of daughters of the next daughter
			int numDaughters;
			// the index of the next feature
			int nextFeatIndex = currentTreeNode.getFeatureIndex();
			// System.out.print("featureIndex = "+nextFeatIndex+"\n");
			if (featDef.isByteFeature(nextFeatIndex)) {
				// if we have a byte feature, build a byte decision node
				numDaughters = featDef.getNumberOfValues(nextFeatIndex);
				daughterNode = new DecisionNode.ByteDecisionNode(nextFeatIndex, numDaughters, featDef);
			} else {
				if (featDef.isShortFeature(nextFeatIndex)) {
					// if we have a short feature, build a short decision node
					numDaughters = featDef.getNumberOfValues(nextFeatIndex);
					daughterNode = new DecisionNode.ShortDecisionNode(nextFeatIndex, numDaughters, featDef);
				} else {
					// feature is of type float, currently not supported in ffi
					throw new IllegalArgumentException("Found float feature in FeatureFileIndexer!");
				}
			}

			if (motherCARTNode == null) {
				// if the mother is null, the current node is the root
				rootNode = daughterNode;
				daughterNode.setIsRoot(true);
			} else {
				// if the current node is not the root,
				// set mother and daughter accordingly
				motherCARTNode.addDaughter(daughterNode);
			}
			// for every daughter go in recursion
			for (int i = 0; i < numDaughters; i++) {
				MaryNode nextChild = currentTreeNode.getChild(i);
				addDaughters(daughterNode, nextChild, ffi);

			}
		} else {
			// we are at a leaf node
			// System.out.println("Adding leaf");
			// get the feature vectors
			FeatureVector[] featureVectors = ffi.getFeatureVectors(currentTreeNode.getFrom(), currentTreeNode.getTo());
			// build a new leaf
			LeafNode leaf = new LeafNode.FeatureVectorLeafNode(featureVectors);

			if (motherCARTNode == null) {
				// if the mother is null, the current node is the root
				rootNode = leaf;
			} else {
				// set mother and daughter
				motherCARTNode.addDaughter(leaf);
			}
		}
	}

}
