/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.unitselection.cart;

import java.io.BufferedReader;
import java.io.IOException;

import de.dfki.lt.mary.unitselection.FeatureArrayIndexer;
import de.dfki.lt.mary.unitselection.MaryNode;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

/**
 * @author marc
 *
 */
public class FeatureVectorCART extends CART {

    /**
     * Convert the given Mary node tree into a CART with the leaves containing
     * featureVectors
     * 
     * @param tree
     *            the tree
     * @param ffi
     *            the feature file indexer containing the feature vectors
     */
     public FeatureVectorCART(MaryNode tree, FeatureArrayIndexer ffi) {
        featDef = ffi.getFeatureDefinition();
        addDaughters(null, tree, ffi);
    }
     
    public void load(String fileName, FeatureDefinition featDefinition, String[] setFeatureSequence ) throws IOException
    {
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
    private void addDaughters(DecisionNode motherCARTNode,
            MaryNode currentTreeNode, FeatureArrayIndexer ffi) {
        if (currentTreeNode == null) {
            LeafNode l = new LeafNode.FeatureVectorLeafNode(new FeatureVector[0]);
            motherCARTNode.addDaughter(l);
            l.setMother(motherCARTNode);
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
                    daughterNode = new DecisionNode.ShortDecisionNode(nextFeatIndex,
                            numDaughters, featDef);
                } else {
                    // feature is of type float, currently not supported in ffi
                    throw new IllegalArgumentException(
                            "Found float feature in FeatureFileIndexer!");
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
                daughterNode.setMother(motherCARTNode);
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
            FeatureVector[] featureVectors = ffi.getFeatureVectors(
                    currentTreeNode.getFrom(), currentTreeNode.getTo());
            // build a new leaf
            LeafNode leaf = new LeafNode.FeatureVectorLeafNode(featureVectors);

            if (motherCARTNode == null) {
                // if the mother is null, the current node is the root
                rootNode = leaf;
            } else {
                // set mother and daughter
                leaf.setMother(motherCARTNode);
                motherCARTNode.addDaughter(leaf);
            }
        }
    }


}
