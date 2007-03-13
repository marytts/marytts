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

/**
 * This class can build a CART either reading from a text file or from a binary
 * file. The format is an extended Wagon format: A node looks like this:
 * ((comparison)(daughter1)(daugther2)(daughter3)...(daughterN))
 * 
 * (comparison) is of form: (<feature-index> is <value>) : binary decision
 * node; a feature-index beginning with b means byte feature; beginning with s
 * means short feature (<feature-index> < <value>) : binary float decision node (<feature-index>
 * oneByteOf <n>) : n-ary byte decision node (<feature-index> oneShortOf <n>) :
 * n-ary short decision node
 * 
 * (daughter) is either a node (see above) or a leaf: ((<index1> <float1>)...(<indexN>
 * <floatN>)) : leaf with unit indices and (dummy) floats
 * 
 * @author Anna Hunecke
 */
public class CARTWagonFormat implements CART {

    private Logger logger = Logger.getLogger("CARTWagonFormat");

    private Node rootNode;

    private Node lastNode;

    private int openBrackets;

    private int numNodes;

    // knows the index numbers and types of the features
    protected FeatureDefinition featDef;

    /**
     * Creates a new CART by reading from the given reader. This method is to be
     * called when a CART created by Wagon is read in.
     * 
     * @param reader
     *            the source of the CART data
     * @throws IOException
     *             if errors occur while reading the data
     */
    public CARTWagonFormat(BufferedReader reader,
            FeatureDefinition featDefinition) throws IOException {
        featDef = featDefinition;
        openBrackets = 0;
        String line = reader.readLine(); // first line is empty, read again
        // each line corresponds to a node
        line = reader.readLine();
        numNodes = 0;
        // for each line
        while (line != null) {
            if (!line.startsWith(";;")) {
                // parse the line and add the node
                parseAndAdd(line);
                numNodes++;
            }
            line = reader.readLine();
        }
        // make sure we closed as many brackets as we opened
        if (openBrackets != 0) {
            throw new IOException("Error loading CART: bracket mismatch");
        }
    }

    /**
     * Build a new empty cart
     * 
     */
    public CARTWagonFormat() {
    }

    /**
     * Load the cart from the given file
     * 
     * @param fileName
     *            the file to load the cart from
     * @param featDefinition
     *            the feature definition
     * @param dummy
     *            unused, just here for compatibility with the FeatureFileIndexer.
     * @throws IOException
     *             if a problem occurs while loading
     */
    public void load(String fileName, FeatureDefinition featDefinition, String[] dummy )
            throws IOException {
        //System.out.println("Loading file");
        // open the CART-File and read the header
        DataInput raf = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
        MaryHeader maryHeader = new MaryHeader(raf);
        if (!maryHeader.hasLegalMagic()) {
            throw new IOException("No MARY database file!");
        }
        if (!maryHeader.hasCurrentVersion()) {
            throw new IOException("Wrong version of database file");
        }
        if (maryHeader.getType() != MaryHeader.CARTS) {
            throw new IOException("No CARTs file");
        }
        //System.out.println("Reading CART");
        // discard number of CARTs and CART name
        // TODO: Change format of CART-File
        numNodes = raf.readInt();
        raf.readUTF();

        // load the CART
        featDef = featDefinition;
        // get the backtrace information
        openBrackets = 0;
        // Read in the first node
        String cart;
        int nodeIndex = 0;
        // for each node
        while (nodeIndex < numNodes) {
            // parse the line and add the node
            int length = raf.readInt();
            char[] cartChars = new char[length];
            for (int i = 0; i < length; i++) {
                cartChars[i] = raf.readChar();
            }
            cart = new String(cartChars);
            // System.out.println(cart);
            parseAndAdd(cart);

            nodeIndex++;
        }
        // make sure we closed as many brackets as we opened
        if (openBrackets != 0) {
            throw new IOException("Error loading CART: bracket mismatch: "
                    + openBrackets);
        }
        // Now count all candidates once, so that getNumberOfCandidates()
        // will return the correct figure.
        if (rootNode instanceof DecisionNode)
            ((DecisionNode)rootNode).countCandidates();
        //System.out.println("Done");
    }

    /**
     * Creates a node from the given input line and add it to the CART.
     * 
     * @param line
     *            a line of input to parse
     * @throws IOException
     *             if the line has an unexpected format
     */
    private void parseAndAdd(String line) throws IOException {
        // remove whitespace
        line = line.trim();

        // at beginning of String there should be at least two opening brackets
        if (!(line.startsWith("(("))) {
            throw new IOException("Invalid input line for CART: " + line);
        }
        if (Character.isLetter(line.charAt(2))) { // we have a node
            openBrackets++; // do not count first bracket

            // get the properties of the node
            StringTokenizer tokenizer = new StringTokenizer(line, " ");
            String feature = tokenizer.nextToken().substring(2);
            String type = tokenizer.nextToken();
            String value = tokenizer.nextToken();
            value = value.substring(0, value.length() - 1);

            // build new node depending on type

            Node nextNode;
            if (type.equals("is")) {
                if (featDef.isByteFeature(feature)) {
                    nextNode = new DecisionNode.BinaryByteDecisionNode(feature, value, featDef);
                } else {
                    nextNode = new DecisionNode.BinaryShortDecisionNode(feature, value, featDef);
                }
            } else {
                if (type.equals("<")) {
                    nextNode = new DecisionNode.BinaryFloatDecisionNode(feature, Float
                            .parseFloat(value), featDef);
                } else {
                    if (type.equals("isShortOf")) {
                        nextNode = new DecisionNode.ShortDecisionNode(feature, Integer
                                .parseInt(value), featDef);
                    } else {
                        if (type.equals("isByteOf")) {
                            nextNode = new DecisionNode.ByteDecisionNode(feature, Integer
                                    .parseInt(value), featDef);
                        } else {
                            throw new IOException("Unknown node type : " + type);
                        }
                    }
                }
            }

            if (lastNode != null) {
                // this node is the daughter of the last node
                nextNode.setMother(lastNode);
                ((DecisionNode) lastNode).addDaughter(nextNode);
            } else {
                // this is the rootNode
                rootNode = nextNode;
                nextNode.setIsRoot(true);
            }

            // go one step down
            lastNode = nextNode;

        } else { // we have a leaf

            StringTokenizer tokenizer = new StringTokenizer(line, " ");
            // build new leaf node
            Node nextNode = new LeafNode(tokenizer);

            // set the relations of this node to the others
            if (lastNode == null) { // this node is the root
                rootNode = nextNode;
                nextNode.setIsRoot(true);
            } else { // this node is a daughter of lastNode
                nextNode.setMother(lastNode);
                ((DecisionNode) lastNode).addDaughter(nextNode);
            }

            // look at the bracketing at the end of the line:
            // get the last token out of the tokenizer
            String lastToken = tokenizer.nextToken();

            // lastToken should look like "0))"
            // more than two brackets mean that this is
            // the last daughter of one or more nodes
            int length = lastToken.length();
            //start looking at the characters after "0))"
            int index = lastToken.indexOf(')')+2;

            while (index < length) { // while we have more characters
                char nextChar = lastToken.charAt(index);
                if (nextChar == ')') {
                    // if the next character is a closing bracket
                    openBrackets--;
                    // this is the last daughter of lastNode,
                    // try going one step up
                    if (lastNode.isRoot()) {
                        if (index + 1 != length) {
                            // lastNode should not be the root,
                            // unless we are at the last bracket
                            throw new IOException(
                                    "Too many closing brackets in line " + line);
                        }
                    } else { // you can go one step up
                        nextNode = lastNode;
                        lastNode = lastNode.getMother();
                    }
                } else {
                    // nextChar is not a closing bracket;
                    // something went wrong here
                    throw new IOException("Expected closing bracket in line "
                            + line + ", but found " + nextChar);
                }
                index++;
            }
            // for debugging
            int nodeIndex = nextNode.getNodeIndex();
           

        }
    }

    /**
     * Convert the given Mary node tree into a CART with the leaves containing
     * featureVectors
     * 
     * @param tree
     *            the tree
     * @param ffi
     *            the feature file indexer containing the feature vectors
     */
    public CARTWagonFormat(MaryNode tree, FeatureArrayIndexer ffi) {
        featDef = ffi.getFeatureDefinition();
        numNodes = 0;
        addDaughters(null, tree, ffi);
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
        numNodes++;
        if (currentTreeNode == null) {
            motherCARTNode.addDaughter(null);
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
            LeafNode leaf = new LeafNode(featureVectors);

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

    /**
     * Passes the given item through this CART and returns the
     * interpretation.
     *
     * @param target the target to analyze
     * @param minNumberOfCandidates the minimum number of candidates requested.
     * If this is 0, walk down the CART until the leaf level.
     *
     * @return the interpretation
     */
    public Object interpret(Target target,int minNumberOfCandidates) {
        Node currentNode = rootNode;
        Node prevNode = null;

        FeatureVector featureVector = target.getFeatureVector();

        // logger.debug("Starting cart at "+nodeIndex);
        while (currentNode.getNumberOfCandidates() > minNumberOfCandidates
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
        if (currentNode.getNumberOfCandidates() < minNumberOfCandidates
                && prevNode != null) {
            currentNode = prevNode;
        }

        assert currentNode.getNumberOfCandidates() >= minNumberOfCandidates
            || currentNode == rootNode; 
        
        // get the indices from the leaf node
        int[] result = currentNode.getAllIndices();
        
        logger.debug("For target "+target+", selected " + result.length + " units");
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
     */
    public static void replaceLeafByCart(CARTWagonFormat cart, LeafNode leaf){
        DecisionNode mother = (DecisionNode) leaf.getMother();
        Node newNode = cart.getRootNode();
        mother.replaceDaughter(newNode, leaf.getNodeIndex());
        newNode.setMother(mother);
        newNode.setIsRoot(false);
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
        return numNodes;
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
            rootNode.toWagonFormat(null, null, pw);
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
}
