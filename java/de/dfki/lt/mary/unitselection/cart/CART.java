/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.FeatureArrayIndexer;
import de.dfki.lt.mary.unitselection.FeatureFileIndexer;
import de.dfki.lt.mary.unitselection.MaryNode;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

import de.dfki.lt.mary.unitselection.voiceimport.MaryHeader;

public abstract class CART 
{

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    protected Node rootNode;

    // knows the index numbers and types of the features used in DecisionNodes
    protected FeatureDefinition featDef;

    /**
     * Build a new empty cart
     * 
     */
    public CART() {
    }

    
    /**
     * Load the cart from the given file
     * @param fileName the file to load the cart from
     * @param featDefinition the feature definition
     * @param setFeatureSequence a sequence of features for indexing the feature vectors. Used to
     * initialise a FeatureFileIndexer but unused in the case of an actual CART tree.
     * @throws IOException if a problem occurs while loading
     */
    public abstract void load(String fileName, FeatureDefinition featDefinition, String[] setFeatureSequence ) throws IOException;

    /**
     * Passes the given item through this CART and returns the
     * leaf Node, or the Node it stopped walking down.
     *
     * @param target the target to analyze
     * @param minNumberOfData the minimum number of data requested.
     * If this is 0, walk down the CART until the leaf level.
     *
     * @return the Node
     */
    public Node interpretToNode(Target target, int minNumberOfData) {
        Node currentNode = rootNode;
        Node prevNode = null;

        FeatureVector featureVector = target.getFeatureVector();

        // logger.debug("Starting cart at "+nodeIndex);
        while (currentNode.getNumberOfData() > minNumberOfData
                && !(currentNode instanceof LeafNode)) {
            // while we have not reached the bottom,
            // get the next node based on the features of the target
            prevNode = currentNode;
            currentNode = ((DecisionNode) currentNode)
                    .getNextNode(featureVector);
            // logger.debug(decision.toString() + " result '"+
            // decision.findFeature(item) + "' => "+ nodeIndex);
        }
        // Now usually we will have gone down one level too far
        if (currentNode.getNumberOfData() < minNumberOfData
                && prevNode != null) {
            currentNode = prevNode;
        }

        assert currentNode.getNumberOfData() >= minNumberOfData
            || currentNode == rootNode; 
        
        return currentNode;
        
    }

    /**
     * Passes the given item through this CART and returns the
     * interpretation.
     *
     * @param target the target to analyze
     * @param minNumberOfData the minimum number of data requested.
     * If this is 0, walk down the CART until the leaf level.
     *
     * @return the interpretation
     */
    public Object interpret(Target target, int minNumberOfData) {
        
        // get the indices from the leaf node
        Object result = this.interpretToNode(target, minNumberOfData).getAllData();
        
        return result;

    }

    /**
     * Get the first leaf node in this tree. Subsequent leaf nodes can be called
     * via leafNode.getNextLeafNode().
     * @return the first leaf node, or null if the tree has no leaves.
     */
    public LeafNode getFirstLeafNode()
    {
        if (rootNode instanceof LeafNode) return (LeafNode) rootNode;
        assert rootNode instanceof DecisionNode;
        return ((DecisionNode)rootNode).getNextLeafNode(0);
    }
    
   
    /**
     * In this tree, replace the given leaf with the given CART
     * @param cart the CART
     * @param leaf the leaf
     * @return the ex-root node from cart which now replaces leaf.
     */
    public static Node replaceLeafByCart(CART cart, LeafNode leaf){
        DecisionNode mother = (DecisionNode) leaf.getMother();
        Node newNode = cart.getRootNode();
        mother.replaceDaughter(newNode, leaf.getNodeIndex());
        newNode.setMother(mother);
        newNode.setIsRoot(false);
        return newNode;
    }
    
 
    
    /**
     * Get the root node of this CART
     * 
     * @return the root node
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * Get the number of nodes in this CART
     * 
     * @return the number of nodes
     */
    public int getNumNodes() {
        if (rootNode == null) return 0;
        return rootNode.getNumberOfNodes();
    }

    /**
     * Dumps this CART to the output stream in WagonFormat.
     * 
     * @param os
     *            the output stream
     * 
     * @throws IOException
     *             if an error occurs during output
     */
    public void dumpBinary(DataOutput os) throws IOException {
        try {
            rootNode.toWagonFormat((DataOutputStream) os, null, null);
        } catch (IOException ioe) {
            IOException newIOE = new IOException(
                    "Error dumping CART to output stream");
            newIOE.initCause(ioe);
            throw newIOE;
        }
    }

    /**
     * Debug output to a text file
     * 
     * @param pw
     *            the print writer of the text file
     * @throws IOException
     */
    public void toTextOut(PrintWriter pw) throws IOException {
        try {
            rootNode.toWagonFormat(null, "", pw);
            pw.flush();
            pw.close();
        } catch (IOException ioe) {
            IOException newIOE = new IOException(
                    "Error dumping CART to standard output");
            newIOE.initCause(ioe);
            throw newIOE;
        }
    }

    /**
     * Write the given String to the given data output (Replacement for
     * writeUTF)
     * 
     * @param str
     *            the String
     * @param out
     *            the data output
     */
    public static void writeStringToOutput(String str, DataOutput out)
            throws IOException {
        out.writeInt(str.length());
        out.writeChars(str);
    }
    
    public String toString(){
        return this.rootNode.toString("");
    }
}
