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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
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
 * @author Anna Hunecke, Marc Schröder
 */

public abstract class WagonCART extends CART 
{
    private Node lastNode;
    
    private int openBrackets;

    public WagonCART()
    {
        super();
    }
    
    /**
     * Creates a new CART by reading from the given reader. This method is to be
     * called when a CART created by Wagon is read in.
     * 
     * @param reader
     *            the source of the CART data
     * @throws IOException
     *             if errors occur while reading the data
     */
    public WagonCART(BufferedReader reader, FeatureDefinition featDefinition)
            throws IOException {
        this.load(reader, featDefinition);

    }
    
    /**
     * 
     * This loads a cart from a wagon tree in textual format, from a reader.
     * This method exists to be called from the constructor.
     * 
     * @param reader the Reader providing the wagon tree
     * @param featDefinition
     * @throws IOException 
     */
    protected void load(BufferedReader reader, FeatureDefinition featDefinition) throws IOException{
        featDef = featDefinition;
        openBrackets = 0;
        String line = reader.readLine(); 
        if (line.equals("")){// first line is empty, read again
            line = reader.readLine();
        }
        // each line corresponds to a node
        // for each line
        while (line != null) {
            if (!line.startsWith(";;")
                    && !line.equals("")) {
                // parse the line and add the node
                
                
                parseAndAdd(line);
            }
            line = reader.readLine();
        }
        // make sure we closed as many brackets as we opened
        if (openBrackets != 0) {
            throw new IOException("Error loading CART: bracket mismatch");
        }
        // Now count all data once, so that getNumberOfData()
        // will return the correct figure.
        if (rootNode instanceof DecisionNode)
            ((DecisionNode)rootNode).countData();
        
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
        int numNodes = raf.readInt();
        raf.readUTF();

        // load the CART
        featDef = featDefinition;
        // get the backtrace information
        openBrackets = 0;
        // Not elegant, but robust
        try {
            while (true) {
                // parse the line and add the node
                int length = raf.readInt();
                char[] cartChars = new char[length];
                for (int i = 0; i < length; i++) {
                    cartChars[i] = raf.readChar();
                }
                String cart = new String(cartChars);
                // System.out.println(cart);
                parseAndAdd(cart);
            }
        } catch (EOFException eof) {}

        // make sure we closed as many brackets as we opened
        if (openBrackets != 0) {
            throw new IOException("Error loading CART: bracket mismatch: "
                    + openBrackets);
        }
        // Now count all data once, so that getNumberOfData()
        // will return the correct figure.
        if (rootNode instanceof DecisionNode)
            ((DecisionNode)rootNode).countData();
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
        if (Character.isLetter(line.charAt(2))
                && !line.substring(2, 6).equals("nan ")) { // we have a node
            openBrackets++; // do not count first bracket

            // get the properties of the node
            StringTokenizer tokenizer = new StringTokenizer(line, " ");
            String feature = tokenizer.nextToken().substring(2);
            String type = tokenizer.nextToken();
            String value = tokenizer.nextToken();
            value = value.substring(0, value.length() - 1);
            // some values are enclosed in double quotes:
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 2)
                value = value.substring(1, value.length() - 1);

            // build new node depending on type

            Node nextNode;
            try {
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
                
            } catch (Exception exc) {
                throw new RuntimeException("Cannot create decision node for cart line: '"+line+"'", exc);
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

            Node nextNode = createLeafNode(line);

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
            StringTokenizer tokenizer = new StringTokenizer(line, " ");
            for (int i=0, numTokens=tokenizer.countTokens(); i<numTokens-1; i++) {
                tokenizer.nextToken();
            }
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
     * For a line representing a leaf in Wagon format, create a leaf.
     * This method decides which implementation of LeafNode is used, i.e.
     * which data format is appropriate.
     * Lines are of the form
     * ((<index1> <float1>)...(<indexN> <floatN>)) 0))
     * 
     * @param line a line from a wagon cart file, representing a leaf
     * @return a leaf node representing the line.
     */
    protected abstract LeafNode createLeafNode(String line);
}
