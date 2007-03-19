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
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

/**
 * Top level tree
 * 
 * @author Anna Hunecke
 *
 */
public class TopLevelTree extends WagonCART
{

     /**
     * @param reader
     * @param featDefinition
     * @throws IOException
     */
    public TopLevelTree(BufferedReader reader, FeatureDefinition featDefinition)
            throws IOException 
    {
        super(reader,featDefinition);
    }

   

    /**
     * For a line representing a leaf in Wagon format, create a leaf.
     * This method decides which implementation of LeafNode is used, i.e.
     * which data format is appropriate.
     * This implementation creates an IntArrayLeafNode, representing the leaf
     * as an array of ints.
     * Lines are of the form
     * ((<index1> <float1>)...(<indexN> <floatN>)) 0))
     * 
     * @param line a line from a wagon cart file, representing a leaf
     * @return a leaf node representing the line.
     */
    protected LeafNode createLeafNode(String line) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        int index = 0;
        // The data to be saved in the leaf node:
        if (numTokens != 2) { 
            // leaf is not empty -> error
            throw new Error("Leaf in line "+line+" is not empty");
        }
        // discard useless token
        tok.nextToken();
        return new LeafNode.FeatureVectorLeafNode();
    }

    /**
     * Fill the leafs of this cart with the given feature vectors
     * 
     * @param featureVectors the feature vectors
     */
    public void fillLeafs(FeatureVector[] featureVectors){
        Node currentNode = rootNode;
        Node prevNode = null;
        
        //loop trough the feature vectors
        for (int i=0;i<featureVectors.length;i++){
            currentNode = rootNode;
            prevNode = null;
            FeatureVector featureVector = featureVectors[i];
            // logger.debug("Starting cart at "+nodeIndex);
            while (!(currentNode instanceof LeafNode)) {
                // while we have not reached the bottom,
                // get the next node based on the features of the target
                prevNode = currentNode;
                currentNode = ((DecisionNode) currentNode)
                .getNextNode(featureVector);
                // logger.debug(decision.toString() + " result '"+
                // decision.findFeature(item) + "' => "+ nodeIndex);
            }
            //add the feature vector to the leaf node
           ((LeafNode.FeatureVectorLeafNode)currentNode).addFeatureVector(featureVector); 
        }
        
    }
    
}
