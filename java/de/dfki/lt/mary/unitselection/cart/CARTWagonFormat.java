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

import org.apache.log4j.Logger;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.MaryNode;
import de.dfki.lt.mary.unitselection.FeatureFileIndexer;

import java.io.*;
import java.util.*;

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

    private final static int MAGIC = 0x4d415259; // "MARY"

    private final static int VERSION = 1;

    private final static int CARTS = 1;

    /**
     * Defines how many units should be selected on backtrace
     */
    private int backtrace = 500;

    private Node rootNode;

    private Node lastNode;

    private int openBrackets;

    private Node lastMotherNode = null;

    private int nextDaughterIndex;

    private int numNodes;

    // knows the index numbers and types of the features
    private static FeatureDefinition featDef;

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
     * @throws IOException
     *             if a problem occurs while loading
     */
    public void load(String fileName, FeatureDefinition featDefinition)
            throws IOException {
        System.out.println("Loading file");
        // open the CART-File and read the header
        DataInput raf = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
        if (raf.readInt() != MAGIC) {
            throw new IOException("No MARY database file!");
        }
        if (raf.readInt() != VERSION) {
            throw new IOException("Wrong version of database file");
        }
        if (raf.readInt() != CARTS) {
            throw new IOException("No CARTs file");
        }
        System.out.println("Reading CART");
        // discard number of CARTs and CART name
        // TODO: Change format of CART-File
        numNodes = raf.readInt();
        raf.readUTF();

        // load the CART
        featDef = featDefinition;
        // get the backtrace information
        String backtraceString = MaryProperties
                .getProperty("english.cart.backtrace");
        backtrace = 100; // default backtrace value
        if (backtraceString != null) {
            backtrace = Integer.parseInt(backtraceString.trim());
        }
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
        System.out.println("Done");
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
                    nextNode = new BinaryByteDecisionNode(feature, value);
                } else {
                    nextNode = new BinaryShortDecisionNode(feature, value);
                }
            } else {
                if (type.equals("<")) {
                    nextNode = new BinaryFloatDecisionNode(feature, Float
                            .parseFloat(value));
                } else {
                    if (type.equals("isShortOf")) {
                        nextNode = new ShortDecisionNode(feature, Integer
                                .parseInt(value));
                    } else {
                        if (type.equals("isByteOf")) {
                            nextNode = new ByteDecisionNode(feature, Integer
                                    .parseInt(value));
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
            int index = 3; // start looking at the characters after "0))"

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
            if (!((DecisionNode) lastNode).hasMoreDaughters(nodeIndex + 1)) {
                System.out
                        .println("Node is last node, but has no closing brackets");
            }

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
    public CARTWagonFormat(MaryNode tree, FeatureFileIndexer ffi) {
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
            MaryNode currentTreeNode, FeatureFileIndexer ffi) {
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
                daughterNode = new ByteDecisionNode(nextFeatIndex, numDaughters);
            } else {
                if (featDef.isShortFeature(nextFeatIndex)) {
                    // if we have a short feature, build a short decision node
                    numDaughters = featDef.getNumberOfValues(nextFeatIndex);
                    daughterNode = new ShortDecisionNode(nextFeatIndex,
                            numDaughters);
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
     * Passes the given target through this CART and returns the interpretation.
     * 
     * @param object
     *            the item to analyze
     * 
     * @return the interpretation
     */
    public Object interpret(Target target) {
        Node currentNode = rootNode;

        FeatureVector featureVector = target.getFeatureVector();

        // logger.debug("Starting cart at "+nodeIndex);
        while (!(currentNode instanceof LeafNode)) {
            // while we have not reached the bottom,
            // get the next node based on the features of the target
            currentNode = ((DecisionNode) currentNode)
                    .getNextNode(featureVector);
            // logger.debug(decision.toString() + " result '"+
            // decision.findFeature(item) + "' => "+ nodeIndex);
        }

        // get the indices from the leaf node
        int[] result = ((LeafNode) currentNode).getAllIndices();

        int limit = backtrace;
        // set backtrace to false (default)
        boolean backtrace = false;
        Node motherNode = currentNode.getMother();
        while (result.length < limit && motherNode != null) {
            // set backtrace to true if we have not enough units
            backtrace = true;
            result = ((DecisionNode) motherNode).getAllIndices();
            // get the mother node
            motherNode = motherNode.getMother();
        }
        if (backtrace) {
            logger.debug("Selected " + result.length + " units on backtrace");
        } else {
            logger.debug("Selected " + result.length
                    + " units without backtrace");
        }
        return result;

    }

    /**
     * Get the feature vectors of the leaf that is to the right of last leaf
     * (Each call of the method gives back the next leaf)
     * 
     * @return the next leaf or null, if no more leafs are there
     */
    public FeatureVector[] getNextFeatureVectors() {
        if (lastMotherNode == null) {
            // if this method has not been called before
            // set lastMotherNode to root node
            // (mother of the last pseudo-leaf)
            lastMotherNode = rootNode;
            // set index to 0
            // (index of the last pseudo-leaf)
            nextDaughterIndex = -1;
        }
        // increase the index to index of next daughter
        nextDaughterIndex += 1;

        // set return value to null
        FeatureVector[] featureVectors = null;

        // test, if mother node has any daughters left
        if (!((DecisionNode) lastMotherNode)
                .hasMoreDaughters(nextDaughterIndex)) {
            // mother has no more daughters
            if (!(lastMotherNode.isRoot())) {
                // if lastMother is not the root, go one step up
                nextDaughterIndex = lastMotherNode.getNodeIndex();
                lastMotherNode = lastMotherNode.getMother();
                featureVectors = getNextFeatureVectors();
            }
            // else do nothing (return null)
        } else {
            // get the next daughter
            Node nextDaughter = ((DecisionNode) lastMotherNode)
                    .getDaughter(nextDaughterIndex);
            if (nextDaughter == null) {
                // null-daughter; call again
                featureVectors = getNextFeatureVectors();
            } else {
                if (nextDaughter instanceof DecisionNode) {
                    // the next leaf is the leftmost daughter of nextDaughter
                    // go one step down
                    lastMotherNode = nextDaughter;
                    nextDaughterIndex = -1;
                    featureVectors = getNextFeatureVectors();
                } else {
                    // the next leaf is the sister of the last leaf
                    featureVectors = ((LeafNode) nextDaughter)
                            .getFeatureVectors();
                }
            }
        }
        return featureVectors;
    }

    /**
     * Replace a leaf by a CART
     * 
     * @param cart
     *            the new CART
     */
    public void replaceLeafByCart(CARTWagonFormat cart) {
        // replace the leaf by the CART
        ((DecisionNode) lastMotherNode).replaceDaughter(cart.getRootNode(),
                nextDaughterIndex);
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

    /**
     * A node for the CART. All node types inherit from this class
     */
    static abstract class Node {

        // isRoot should be set to true if this node is the root node
        protected boolean isRoot;

        // every node except the root node has a mother
        protected Node mother;

        // the index of the node in the daughters array of its mother
        protected int index;

        /**
         * set the mother node of this node
         * 
         * @param node
         *            the mother node
         */
        public void setMother(Node node) {
            this.mother = node;
        }

        /**
         * Get the mother node of this node
         * 
         * @return the mother node
         */
        public Node getMother() {
            return mother;
        }

        /**
         * Set isRoot to the given value
         * 
         * @param isRoot
         *            the new value of isRoot
         */
        public void setIsRoot(boolean isRoot) {
            this.isRoot = isRoot;
        }

        /**
         * Get the setting of isRoot
         * 
         * @return the setting of isRoot
         */
        public boolean isRoot() {
            return isRoot;
        }

        /**
         * Set the index of this node
         * 
         * @param index
         *            the index
         */
        public void setNodeIndex(int index) {
            this.index = index;
        }

        /**
         * Get the index of this node
         * 
         * @return the index
         */
        public int getNodeIndex() {
            return index;
        }

        /**
         * Get all unit indices from all leaves below this node
         * 
         * @return an int array containing the indices
         */
        public abstract int[] getAllIndices();

        /**
         * Writes the Cart to the given DataOut in Wagon Format
         * 
         * @param out
         *            the outputStream
         * @param extension
         *            the extension that is added to the last daughter
         */
        public abstract void toWagonFormat(DataOutputStream out,
                String extension, PrintWriter pw) throws IOException;

    }

    /**
     * A decision node that determines the next Node to go to in the CART. All
     * decision nodes inherit from this class
     */
    static abstract class DecisionNode extends Node {

        // a decision node has an array of daughters
        protected Node[] daughters;

        // the feature index
        protected int featureIndex;

        // the feature name
        protected String feature;

        // remember last added daughter
        protected int lastDaughter;

        /**
         * Construct a new DecisionNode
         * 
         * @param feature
         *            the feature
         * @param numDaughters
         *            the number of daughters
         */
        public DecisionNode(String feature, int numDaughters) {
            this.feature = feature;
            this.featureIndex = featDef.getFeatureIndex(feature);
            daughters = new Node[numDaughters];
            isRoot = false;
        }

        /**
         * Construct a new DecisionNode
         * 
         * @param featureIndex
         *            the feature index
         * @param numDaughters
         *            the number of daughters
         */
        public DecisionNode(int featureIndex, int numDaughters) {
            this.featureIndex = featureIndex;
            this.feature = featDef.getFeatureName(featureIndex);
            daughters = new Node[numDaughters];
            isRoot = false;
        }

        /**
         * Get the name of the feature
         * 
         * @return the name of the feature
         */
        public String getFeatureName() {
            return feature;
        }

        /**
         * Add a daughter to the node
         * 
         * @param daughter
         *            the new daughter
         */
        public void addDaughter(Node daughter) {
            if (lastDaughter > daughters.length - 1) {
                throw new RuntimeException("Can not add daughter number "
                        + (lastDaughter + 1) + ", since node has only "
                        + daughters.length + " daughters!");
            }
            daughters[lastDaughter] = daughter;
            if (daughter != null) {
                daughter.setNodeIndex(lastDaughter);
            }
            lastDaughter++;
        }

        /**
         * Get the daughter at the specified index
         * 
         * @param index
         *            the index of the daughter
         * @return the daughter (potentially null); if index out of range: null
         */
        public Node getDaughter(int index) {
            if (index > daughters.length - 1 || index < 0) {
                return null;
            }
            return daughters[index];
        }

        /**
         * Replace daughter at given index with another daughter
         * 
         * @param newDaughter
         *            the new daughter
         * @param index
         *            the index of the daughter to replace
         */
        public void replaceDaughter(Node newDaughter, int index) {
            if (index > daughters.length - 1 || index < 0) {
                throw new RuntimeException("Can not replace daughter number "
                        + index + ", since daughter index goes from 0 to "
                        + (daughters.length - 1) + "!");
            }
            daughters[index] = newDaughter;
        }

        /**
         * Tests, if the given index refers to a daughter
         * 
         * @param index
         *            the index
         * @return true, if the index is in range of the daughters array
         */
        public boolean hasMoreDaughters(int index) {
            return (index > -1 && index < daughters.length);
        }

        /**
         * Get all unit indices from all leaves below this node
         * 
         * @return an int array containing the indices
         */
        public int[] getAllIndices() {
            int[] result = null;
            if (daughters.length == 2) {
                // if we have just two daughters, merging the indices is trivial
                int[] indices1 = daughters[0].getAllIndices();
                int[] indices2 = daughters[1].getAllIndices();
                result = new int[indices1.length + indices2.length];
                System.arraycopy(indices1, 0, result, 0, indices1.length);
                System.arraycopy(indices2, 0, result, indices1.length,
                        indices2.length);
            } else {
                // we have more than two daughters
                // for each daughter, get her indices
                // and merge them with the other indices
                int[] indices1 = new int[0];
                for (int i = 0; i < daughters.length; i++) {
                    int[] indices2 = daughters[i].getAllIndices();
                    result = new int[indices1.length + indices2.length];
                    System.arraycopy(indices1, 0, result, 0, indices1.length);
                    System.arraycopy(indices2, 0, result, indices1.length,
                            indices2.length);
                    indices1 = result;
                }
            }
            return result;
        }

        /**
         * Writes the Cart to the given DataOut in Wagon Format
         * 
         * @param out
         *            the outputStream
         * @param extension
         *            the extension that is added to the last daughter
         */
        public void toWagonFormat(DataOutputStream out, String extension,
                PrintWriter pw) throws IOException {
            if (out != null) {
                // dump to output stream
                // two open brackets + definition of node
                writeStringToOutput("((" + getNodeDefinition(), out);
            } else {
                // dump to Standard out
                // two open brackets + definition of node
                // System.out.println("(("+getNodeDefinition());
            }
            if (pw != null) {
                // dump to print writer
                // two open brackets + definition of node
                pw.println("((" + getNodeDefinition());
            }
            // add the daughters
            for (int i = 0; i < daughters.length; i++) {

                if (daughters[i] == null) {
                    String nullDaughter = "";

                    if (i + 1 != daughters.length) {
                        nullDaughter = "((() 0))";

                    } else {
                        // extension must be added to last daughter
                        if (extension != null) {
                            nullDaughter = "((() 0)))" + extension;

                        } else {
                            // we are in the root node, add a closing bracket
                            nullDaughter = "((() 0)))";
                        }
                    }

                    if (out != null) {
                        // dump to output stream
                        writeStringToOutput(nullDaughter, out);
                    } else {
                        // dump to Standard out
                        // System.out.println(nullDaughter);
                    }
                    if (pw != null) {
                        pw.print(" " + nullDaughter);
                    }
                } else {
                    if (i + 1 != daughters.length) {

                        daughters[i].toWagonFormat(out, "", pw);
                    } else {

                        // extension must be added to last daughter
                        if (extension != null) {
                            daughters[i]
                                    .toWagonFormat(out, ")" + extension, pw);
                        } else {
                            // we are in the root node, add a closing bracket
                            daughters[i].toWagonFormat(out, ")", pw);
                        }
                    }
                }
            }
        }

        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public abstract String getNodeDefinition();

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public abstract Node getNextNode(FeatureVector featureVector);

    }

    /**
     * A binary decision Node that compares two byte values.
     */
    static class BinaryByteDecisionNode extends DecisionNode {

        // the value of this node
        private byte value;

        /**
         * Create a new binary String DecisionNode.
         * 
         * @param feature
         *            the string used to get a value from an Item
         * @param value
         *            the value to compare to
         */
        public BinaryByteDecisionNode(String feature, String value) {
            super(feature, 2);
            this.value = featDef.getFeatureValueAsByte(value, feature);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            byte val = featureVector.getByteFeature(featureIndex);
            Node returnNode;
            if (val == value) {
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            }
            return returnNode;
        }

        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " is " + value + ")";
        }

    }

    /**
     * A binary decision Node that compares two short values.
     */
    static class BinaryShortDecisionNode extends DecisionNode {

        // the value of this node
        private short value;

        /**
         * Create a new binary String DecisionNode.
         * 
         * @param feature
         *            the string used to get a value from an Item
         * @param value
         *            the value to compare to
         */
        public BinaryShortDecisionNode(String feature, String value) {
            super(feature, 2);
            this.value = featDef.getFeatureValueAsShort(value, feature);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            short val = featureVector.getShortFeature(featureIndex);
            Node returnNode;
            if (val == value) {
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            }
            return returnNode;
        }

        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " is " + value + ")";
        }

    }

    /**
     * A binary decision Node that compares two float values.
     */
    static class BinaryFloatDecisionNode extends DecisionNode {

        // the value of this node
        private float value;

        /**
         * Create a new binary String DecisionNode.
         * 
         * @param feature
         *            the string used to get a value from an Item
         * @param value
         *            the value to compare to
         */
        public BinaryFloatDecisionNode(String feature, float value) {
            super(feature, 2);
            this.value = value;
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            float val = featureVector.getContinuousFeature(featureIndex);
            Node returnNode;
            if (val < value) {
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            }
            return returnNode;
        }

        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " < " + value + ")";
        }

    }

    /**
     * An decision Node with an arbitrary number of daughters. Value of the
     * target corresponds to the index number of next daughter.
     */
    static class ByteDecisionNode extends DecisionNode {

        /**
         * Build a new byte decision node
         * 
         * @param feature
         *            the feature name
         * @param numDaughters
         *            the number of daughters
         */
        public ByteDecisionNode(String feature, int numDaughters) {
            super(feature, numDaughters);
        }

        /**
         * Build a new byte decision node
         * 
         * @param feature
         *            the feature name
         * @param numDaughters
         *            the number of daughters
         */
        public ByteDecisionNode(int featureIndex, int numDaughters) {
            super(featureIndex, numDaughters);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            return daughters[featureVector.getByteFeature(featureIndex)];
        }

        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " isByteOf " + daughters.length + ")";
        }

    }

    /**
     * An decision Node with an arbitrary number of daughters. Value of the
     * target corresponds to the index number of next daughter.
     */
    static class ShortDecisionNode extends DecisionNode {

        /**
         * Build a new short decision node
         * 
         * @param feature
         *            the feature name
         * @param numDaughters
         *            the number of daughters
         */
        public ShortDecisionNode(String feature, int numDaughters) {
            super(feature, numDaughters);
        }

        /**
         * Build a new short decision node
         * 
         * @param featureIndex
         *            the feature index
         * @param numDaughters
         *            the number of daughters
         */
        public ShortDecisionNode(int featureIndex, int numDaughters) {
            super(featureIndex, numDaughters);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            return daughters[featureVector.getShortFeature(featureIndex)];
        }

        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " isShortOf " + daughters.length + ")";
        }

    }

    /**
     * The leaf of a CART.
     */
    static class LeafNode extends Node {

        private int[] indices;

        private FeatureVector[] featureVectors;

        /**
         * Create a new LeafNode.
         * 
         * @param tok
         *            the String Tokenizer containing the String with the
         *            indices
         * @param openBrackets
         *            the number of opening brackets at the first token
         */
        public LeafNode(StringTokenizer tok) {
            super();
            isRoot = false;
            // read the indices from the tokenized String
            // lines are of form
            // ((<index1> <float1>)...(<indexN> <floatN>)) 0))
            int numTokens = tok.countTokens();
            int index = 0;
            if (numTokens == 2) { // we do not have any indices
                // discard useless token
                tok.nextToken();
                indices = new int[0];
            } else {
                indices = new int[(numTokens - 1) / 2];

                while (index * 2 < numTokens - 1) { // while we are not at the
                                                    // last token
                    String nextToken = tok.nextToken();
                    if (index == 0) {
                        // we are at first token, discard all open brackets
                        nextToken = nextToken.substring(4);
                    } else {
                        // we are not at first token, only one open bracket
                        nextToken = nextToken.substring(1);
                    }
                    // store the index of the unit
                    indices[index] = Integer.parseInt(nextToken);
                    // discard next token
                    tok.nextToken();
                    // increase index
                    index++;
                }
            }
        }

        /**
         * Build a new leaf node containing the given feature vectors
         * 
         * @param featureVectors
         *            the feature vectors
         */
        public LeafNode(FeatureVector[] featureVectors) {
            this.featureVectors = featureVectors;
        }

        /**
         * Get the feature vectors of this node
         * 
         * @return the feature vectors
         */
        public FeatureVector[] getFeatureVectors() {
            return featureVectors;
        }

        /**
         * Get all unit indices
         * 
         * @return an int array containing the indices
         */
        public int[] getAllIndices() {
            return indices;
        }

        /**
         * Retrieve the indices from the feature vectors and store them in the
         * indices field
         */
        private void retrieveIndices() {
            indices = new int[featureVectors.length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = featureVectors[i].getUnitIndex();
            }
        }

        /**
         * Writes the Cart to the given DataOut in Wagon Format
         * 
         * @param out
         *            the outputStream
         * @param extension
         *            the extension that is added to the last daughter
         */
        public void toWagonFormat(DataOutputStream out, String extension,
                PrintWriter pw) throws IOException {
            if (indices == null) {
                // get the indices from the feature vectors
                retrieveIndices();
            }
            StringBuffer sb = new StringBuffer();
            // open three brackets
            sb.append("(((");
            // for each index, write the index and then a pseudo float
            for (int i = 0; i < indices.length; i++) {
                sb.append("(" + indices[i] + " 0)");
                if (i + 1 != indices.length) {
                    sb.append(" ");
                }
            }
            // write the ending
            sb.append(") 0))" + extension);
            // dump the whole stuff
            if (out != null) {
                // write to output stream

                writeStringToOutput(sb.toString(), out);
            } else {
                // write to Standard out
                // System.out.println(sb.toString());
            }
            if (pw != null) {
                // dump to printwriter
                pw.print(" ((() 0))" + extension);
            }
        }

    }
}
