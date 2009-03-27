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

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.DirectedGraph;
import marytts.cart.DirectedGraphNode;
import marytts.cart.FeatureVectorCART;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.FeatureVectorLeafNode;
import marytts.cart.impose.FeatureArrayIndexer;
import marytts.cart.impose.MaryNode;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;

/**
 * @author marc
 *
 */
public class AgglomerativeClusterer
{
    private static final float SINGLE_ITEM_IMPURITY = 1000;
    private FeatureVector[] features;
    private Map<LeafNode, Double> impurities = new HashMap<LeafNode, Double>();
    private DistanceMeasure dist;
    private FeatureDefinition featureDefinition;
    private int numByteFeatures;
    private int[] availableFeatures;
    
    public AgglomerativeClusterer(FeatureVector[] features, FeatureDefinition featureDefinition, DistanceMeasure dist)
    {
        this.features = features;
        this.dist = dist;
        this.featureDefinition = featureDefinition;
        this.numByteFeatures = featureDefinition.getNumberOfByteFeatures();
        List<String> featuresToUse = new ArrayList<String>();
        for (int i=0; i<numByteFeatures; i++) {
            String f = featureDefinition.getFeatureName(i);
            if (!f.contains("phoneme") && !f.contains("halfphone") &&
                    !f.contains("vc") && !f.contains("ctype")
                    && !f.contains("cvox") && !f.contains("edge")
                    && !f.contains("vfront") && !f.contains("vlng")
                    && !f.contains("vheight") && !f.contains("cplace")
                    && !f.contains("vrnd") && !f.contains("selection_next_phone_class")) {
                featuresToUse.add(f);
                System.out.println("adding feature "+f);
            } else {
                System.err.println("ignoring feature "+f);
            }
        }
        availableFeatures = new int[featuresToUse.size()];
        for (int i=0; i<availableFeatures.length; i++) {
            availableFeatures[i] = featureDefinition.getFeatureIndex(featuresToUse.get(i));
        }
        
    }
    
    public DirectedGraph cluster()
    {
        DirectedGraph graph = new DirectedGraph(featureDefinition);
        graph.setRootNode(new DirectedGraphNode(null, null));
        return cluster(graph, new ArrayList<String>());
    }
    
    private DirectedGraph cluster(DirectedGraph graphSoFar, List<String> featuresSoFar)
    {
        Set<Integer> prevFeatureIndices = new HashSet<Integer>();
        int[] featureList = new int[featuresSoFar.size()+1];
        for (int i=0; i<featuresSoFar.size(); i++) {
            featureList[i] = featureDefinition.getFeatureIndex(featuresSoFar.get(i));
            prevFeatureIndices.add(featureList[i]);
        }
        FeatureArrayIndexer fai = new FeatureArrayIndexer(features, featureDefinition);
        int iBestFeature = -1;
        double minGI = Double.POSITIVE_INFINITY;
        // Loop over all unused discrete features, and compute their Global Impurity
        for (int f=0; f<availableFeatures.length; f++) {
            int fi = availableFeatures[f];
            if (prevFeatureIndices.contains(fi)) continue;
            featureList[featureList.length-1] = fi;
            fai.deepSort(featureList);
            CART testCART = new FeatureVectorCART(fai.getTree(), fai);
            assert testCART.getRootNode().getNumberOfData() == features.length;
            double gi = 0;
            // Global Impurity measures the average distance of an instance
            // to the other instances in the same leaf.
            // Global Impurity is computed as follows:
            // GI = 1/N * sum(|l| * I(l)), where
            // N = total number of instances (feature vectors);
            // |l| = the number of instances in a leaf;
            // I(l) = the impurity of the leaf.
            for (LeafNode leaf : testCART.getLeafNodes()) {
                gi += leaf.getNumberOfData() * computeImpurity(leaf);
            }
            gi /= features.length;
            //System.out.println("Feature "+featureList.length+" using "+featureDefinition.getFeatureName(fi)+" yields GI "+gi);
            if (gi < minGI) {
                minGI = gi;
                iBestFeature = fi;
            }
        }
        System.out.println("Best feature "+featureList.length+": "+featureDefinition.getFeatureName(iBestFeature)+"(GI="+minGI+")");
        featuresSoFar.add(featureDefinition.getFeatureName(iBestFeature));
        featureList[featureList.length-1] = iBestFeature;
        fai.deepSort(featureList);
        CART bestFeatureCart = new FeatureVectorCART(fai.getTree(), fai);
        // Now walk through cartSoFar and bestFeatureCart in parallel,
        // and add the leaves of bestFeatureCart into cartSoFar in order
        // to enable clustering:
        Node fNode = bestFeatureCart.getRootNode();
        Node gNode = graphSoFar.getRootNode();
        
        List<DirectedGraphNode> newLeavesList = new ArrayList<DirectedGraphNode>();
        updateGraphFromTree((DecisionNode)fNode, (DirectedGraphNode) gNode, newLeavesList);
        DirectedGraphNode[] newLeaves = newLeavesList.toArray(new DirectedGraphNode[0]);
        //debugOut(graphSoFar);
        System.out.println("On level "+featureList.length+", "+newLeaves.length+" leaves before clustering");

        float[][] deltaGI = new float[newLeaves.length][newLeaves.length];
        for (int i=0; i<newLeaves.length; i++) {
            for (int j=i+1; j<newLeaves.length; j++) {
                deltaGI[i][j] = (float) computeDeltaGI(newLeaves[i], newLeaves[j]);
            }
        }
        
        // Now cluster the leaves
        float minDeltaGI;
        int bestPair1, bestPair2;
        do {
            minDeltaGI = 0; // if we cannot find any that is better, stop.
            bestPair1 = bestPair2 = -1;
            for (int i=0; i<newLeaves.length; i++) {
                if (newLeaves[i] == null) continue;
                for (int j=i+1; j<newLeaves.length; j++) {
                    if (newLeaves[j] == null) continue;
                    if (deltaGI[i][j] < minDeltaGI) {
                        bestPair1 = i;
                        bestPair2 = j;
                        minDeltaGI = deltaGI[i][j];
                    }
                }
            }
            if (minDeltaGI < 0) { // found something to merge
                mergeLeaves(newLeaves[bestPair1], newLeaves[bestPair2]);
                //System.out.println("Merged leaves "+bestPair1+" and "+bestPair2+" (deltaGI: "+minDeltaGI+")");
                newLeaves[bestPair2] = null;
                // Update deltaGI table:
                for (int i=0; i<bestPair2; i++) {
                    deltaGI[i][bestPair2] = Float.NaN;
                }
                for (int j=bestPair2+1; j<newLeaves.length; j++) {
                    deltaGI[bestPair2][j] = Float.NaN;
                }
                for (int i=0; i<bestPair1; i++) {
                    if (newLeaves[i] != null)
                        deltaGI[i][bestPair1] = (float) computeDeltaGI(newLeaves[i], newLeaves[bestPair1]);
                }
                for (int j=bestPair1+1; j<newLeaves.length; j++) {
                    if (newLeaves[j] != null)
                        deltaGI[bestPair1][j] = (float) computeDeltaGI(newLeaves[bestPair1], newLeaves[j]);
                }
            }
        } while (minDeltaGI < 0);

        int nLeavesLeft = 0;
        for (int i=0; i<newLeaves.length; i++) {
            if (newLeaves[i] != null) nLeavesLeft++;
        }
        System.out.println("On level "+featureList.length+", "+nLeavesLeft+" leaves after clustering");
        
        
        
        
        
        if (featureList.length < 6) return cluster(graphSoFar, featuresSoFar);
        
        //debugOut(graphSoFar);
        return graphSoFar;
    }
    
    /**
     * The impurity of a leaf node is computed as follows:
     * I(l) = 2/(|l|*(|l|-1)) * sum over all pairs(distance of pair),
     * where |l| = the number of instances in the leaf.
     * @param leaf
     * @return
     */
    private double computeImpurity(LeafNode leaf)
    {
        if (!(leaf instanceof FeatureVectorLeafNode))
            throw new IllegalArgumentException("Currently only feature vector leaf nodes are supported");
        if (impurities.containsKey(leaf)) return impurities.get(leaf);
        FeatureVectorLeafNode l = (FeatureVectorLeafNode) leaf;
        FeatureVector[] fvs = l.getFeatureVectors();
        int n = fvs.length;
        if (n < 2) return SINGLE_ITEM_IMPURITY;
        double impurity = 0;
        //System.out.println("Leaf has "+n+" items, computing "+(n*(n-1)/2)+" distances");
        for (int i=0; i<n; i++) {
            for (int j=i+1; j<n; j++) {
                impurity += dist.distance(fvs[i], fvs[j]);
            }
        }
        impurity *= 2./(n*(n-1));
        impurities.put(leaf, impurity);
        return impurity;
    }
    
    /**
     * The delta in global impurity that would be caused by merging the two given leaves
     * is computed as follows.
     * Delta GI = (|l1|+|l2|) * I(l1 united with l2) - |l1| * I(l1) - |l2| * I(l2)
     *          = 1/N*(|l1|+|l2|-1) * 
     *            (sum of all distances between items in l1 and items in l2
     *              - |l2| * I(l1) - |l1| * I(l2) )
     * where N = sum of all |l| = total number of instances in the tree,
     * |l| = number of instances in the leaf l 
     * @param dgn1
     * @param dgn2
     * @return
     */
    private double computeDeltaGI(DirectedGraphNode dgn1, DirectedGraphNode dgn2)
    {
        FeatureVectorLeafNode l1 = (FeatureVectorLeafNode) dgn1.getLeafNode();
        FeatureVectorLeafNode l2 = (FeatureVectorLeafNode) dgn2.getLeafNode();
        FeatureVector[] fv1 = l1.getFeatureVectors();
        FeatureVector[] fv2 = l2.getFeatureVectors();
        double deltaGI = 0;
        // Sum of all distances across leaf boundaries:
        for (int i=0; i<fv1.length; i++) {
            for (int j=0; j<fv2.length; j++) {
                deltaGI += dist.distance(fv1[i], fv2[j]);
            }
        }
        
        deltaGI -= l2.getNumberOfData() * computeImpurity(l1);
        deltaGI -= l1.getNumberOfData() * computeImpurity(l2);
        deltaGI /= l1.getNumberOfData()+l2.getNumberOfData()-1;
        deltaGI /= features.length;
        return deltaGI;
    }
    
    
    private void mergeLeaves(DirectedGraphNode dgn1, DirectedGraphNode dgn2)
    {
        // Copy all data from dgn2 into dgn1
        FeatureVectorLeafNode l1 = (FeatureVectorLeafNode) dgn1.getLeafNode();
        FeatureVectorLeafNode l2 = (FeatureVectorLeafNode) dgn2.getLeafNode();
        FeatureVector[] fv1 = l1.getFeatureVectors();
        FeatureVector[] fv2 = l2.getFeatureVectors();
        FeatureVector[] newFV = new FeatureVector[fv1.length+fv2.length];
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
            System.err.println("Impurities: "+impurities+", l1:"+l1+", l2:"+l2);
        }
    }
    
    
    private void updateGraphFromTree(DecisionNode treeNode, DirectedGraphNode graphNode, List<DirectedGraphNode> newLeaves)
    {
        int treeFeatureIndex = treeNode.getFeatureIndex();
        int treeNumDaughters = treeNode.getNumberOfDaugthers();
        DecisionNode graphDecisionNode = graphNode.getDecisionNode();
        if (graphDecisionNode != null) {
            // Sanity check: the two must be aligned: same feature, same number of children
            int graphFeatureIndex = graphDecisionNode.getFeatureIndex();
            assert treeFeatureIndex == graphFeatureIndex : "Tree indices out of sync!";
            assert treeNumDaughters == graphDecisionNode.getNumberOfDaugthers() : "Tree structure out of sync!";
            // OK, now recursively call ourselves for all daughters
            for (int i=0; i<treeNumDaughters; i++) {
                // We expect the next tree node to be a decision node (unless it is an empty node),
                // because the level just above the leaves does not exist in graph yet.
                Node nextTreeNode = treeNode.getDaughter(i);
                if (nextTreeNode == null) continue;
                else if (nextTreeNode instanceof LeafNode) {
                    assert ((LeafNode)nextTreeNode).getNumberOfData() == 0;
                    continue;
                }
                assert nextTreeNode instanceof DecisionNode;
                DirectedGraphNode nextGraphNode = (DirectedGraphNode) graphDecisionNode.getDaughter(i);
                updateGraphFromTree((DecisionNode)nextTreeNode, nextGraphNode, newLeaves);
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
            for (int i=0; i<treeNumDaughters; i++) {
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


    private void debugOut(DirectedGraph graph)
    {
        for (Iterator<Node> it = graph.getNodeIterator(); it.hasNext(); ) {
            Node next = it.next();
            debugOut(next);
        }
    }
    
    private void debugOut(CART graph)
    {
        Node root = graph.getRootNode();
        debugOut(root);
    }

    private void debugOut(Node node)
    {
        if (node instanceof DirectedGraphNode)
            debugOut((DirectedGraphNode)node);
        else if (node instanceof LeafNode)
            debugOut((LeafNode)node);
        else
            debugOut((DecisionNode)node);
    }
    
    private void debugOut(DirectedGraphNode node)
    {
        System.out.println("DGN");
        if (node.getLeafNode() != null) debugOut(node.getLeafNode());
        if (node.getDecisionNode() != null) debugOut(node.getDecisionNode());
    }
    
    private void debugOut(LeafNode node)
    {
        System.out.println("Leaf: "+node.getDecisionPath());
    }
    
    private void debugOut(DecisionNode node)
    {
        System.out.println("DN with "+node.getNumberOfDaugthers()+" daughters: "+node.toString());
        for (int i=0; i<node.getNumberOfDaugthers(); i++) {
            Node daughter = node.getDaughter(i);
            if (daughter == null) System.out.println("null");
            else debugOut(daughter);
        }
    }

}
