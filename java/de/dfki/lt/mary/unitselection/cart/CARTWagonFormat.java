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

import java.io.*;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.*;

/**
 * Implementation of a Classification and Regression Tree (CART) that is
 * used more like a binary decision tree, with each node containing a
 * decision or a final value.  The decision nodes in the CART trees
 * operate on an Item and have the following format:
 *
 * <pre>
 *   NODE feat operand value qfalse 
 * </pre>
 *
 * <p>Where <code>feat</code> is an string that represents a feature
 * to pass to the <code>findFeature</code> method of an item.
 *
 * <p>The <code>value</code> represents the value to be compared against
 * the feature obtained from the item via the <code>feat</code> string.
 * The <code>operand</code> is the operation to do the comparison.  The
 * available operands are as follows:
 *
 * <ul>
 *   <li>&lt; - the feature is less than value 
 *
 * <p>For &lt; and >, this CART coerces the value and feature to
 * float's. For =, this CART coerces the value and feature to string and
 * checks for string equality. For MATCHES, this CART uses the value as a
 * regular expression and compares the obtained feature to that.
 *
 * <p>A CART is represented by an array in this implementation. The
 * <code>qfalse</code> value represents the index of the array to go to if
 * the comparison does not match. In this implementation, qtrue index
 * is always implied, and represents the next element in the
 * array. The root node of the CART is the first element in the array.
 *
 * <p>The interpretations always start at the root node of the CART
 * and continue until a final node is found.  The final nodes have the
 * following form:
 *
 * <pre>
 *   LEAF value
 * </pre>
 *
 * <p>Where <code>value</code> represents the value of the node.
 * Reaching a final node indicates the interpretation is over and the
 * value of the node is the interpretation result.
 */
public class CARTWagonFormat{

    private Logger logger = Logger.getLogger("CARTWagonFormat");
    
    /**
     * Defines how many units should be selected
     * on backtrace
     */
    private int backtrace = 500;

    private Node rootNode;
    
    private Node lastNode;
    private int openBrackets;
  
    
    /**
     * Creates a new CART by reading from the given reader.
     *
     * @param reader the source of the CART data
     * @throws IOException if errors occur while reading the data
     */ 
    public CARTWagonFormat(BufferedReader reader) throws IOException {
        openBrackets = 0;
        String line = reader.readLine(); // first line is empty, read again 
        line = reader.readLine(); 
        while (line != null) {
            //System.out.println(line);
            if (!line.startsWith(";;")) {
                parseAndAdd(line);
            }
            line = reader.readLine();
        }
        if (openBrackets != 0){
            throw new Error("Something went wrong here");
        }
    }
    
    

    /**
     * Loads a CART from the input random access file.
     *
     * @param raf the random access file from which to read, with its 
     * file pointer properly positioned.
     *
     * @throws IOException if an error occurs during output
     *
     * Note that cart nodes are really saved as strings that
     * have to be parsed.
     */
    public CARTWagonFormat(RandomAccessFile raf, int numNodes, String name) throws IOException {
        
    	String backtraceString = 
    	    MaryProperties.getProperty("english.cart.backtrace");
    	backtrace = 100;
    	if (backtraceString != null){
    	    backtrace = Integer.parseInt(backtraceString.trim());
    	}
    	openBrackets = 0;
        String cart = raf.readUTF();
        StringTokenizer tok = new StringTokenizer("\n");
    	while (tok.hasMoreTokens()) {
    	   
    	    parseAndAdd(tok.nextToken());
    	}
	
    }
  
    /**
     * Dumps this CART to the output stream in WagonFormat.
     *
     * @param os the output stream
     *
     * @throws IOException if an error occurs during output
     */
    public void dumpBinary(DataOutputStream os) throws IOException {
        StringBuffer sb = new StringBuffer();
        rootNode.toWagonFormat(sb);
        os.writeUTF(sb.toString());
    }
    
    
    
    /**
     * Creates a node from the given input line and add it to the CART.
     * 
     * @param line a line of input to parse
     */
    protected void parseAndAdd(String line) {
        //at beginning of String there should be at least two opening brackets
        if (! (line.startsWith("(("))){
            throw new Error("Invalid input line for CART: "+line);
        }
        if (Character.isLetter(line.charAt(2))){ // we have a node
            openBrackets++; //do not count first bracket
            
            //get the properties of the node
            StringTokenizer tokenizer = new StringTokenizer(line," ");
            String feature = tokenizer.nextToken().substring(2);
            String type = tokenizer.nextToken();
            String value = tokenizer.nextToken();
            value = value.substring(0,value.length()-1);
            
            //build new node depending on type
            Node nextNode;
            if (type.equals("is")){
                int featureIndex = Integer.parseInt(feature.substring(1));
                if (feature.startsWith("b")){
                    nextNode = new BinaryByteDecisionNode(featureIndex,Byte.parseByte(value));
                } else {
                    nextNode = new BinaryShortDecisionNode(featureIndex,Short.parseShort(value));
                }
            } else {
                int featureIndex = Integer.parseInt(feature);
                if (type.equals("<")){
                    nextNode = new BinaryFloatDecisionNode(featureIndex,Float.parseFloat(value));
                } else {
                    if (type.equals("isShortOf")){
                        nextNode = new ShortDecisionNode(featureIndex,Integer.parseInt(value));
                    } else {
                        if (type.equals("isByteOf")){
                            nextNode = new ByteDecisionNode(featureIndex,Integer.parseInt(value));
                        } else {
                            throw new Error ("Unknown type : "+type);
                        }
                    }
                }
            }
            
            if (lastNode != null){
                //this node is the daughter of the last node
                nextNode.setMother(lastNode);
                ((DecisionNode) lastNode).addDaughter(nextNode);      
            } else {
                //this is the rootNode
                rootNode = nextNode;
                nextNode.setIsRoot(true);
            }
            
            //go on step down
            lastNode = nextNode;
            
        } else { // we have a leaf
            StringTokenizer tokenizer = new StringTokenizer(line," ");      
            //build new leaf node
            Node nextNode = new LeafNode(tokenizer);
            
            //set the relations of this node to the others
            if (lastNode == null){ //this node is the root
                rootNode = nextNode;
                nextNode.setIsRoot(true);
            } else { //this node is a daughter of lastNode
                nextNode.setMother(lastNode);
                ((DecisionNode) lastNode).addDaughter(nextNode);
            }
            
            //look at the bracketing at the end of the line:            
            //get the last token out of the tokenizer
            String lastToken = tokenizer.nextToken();
            
            //lastToken should look like "0))"
            //more than two brackets mean that this is 
            //the last daughter of one or more nodes
            int length = lastToken.length();
            int index = 3; //start looking at the characters after "0))"
            while (index < length){ //while we have more characters
                char nextChar = lastToken.charAt(index); 
                if (nextChar == ')'){ 
                    //if the next character is a closing bracket
                    openBrackets--;
                    //this is the last daughter of lastNode, 
                    //try going one step up
                    if (lastNode.isRoot()){
                        if (index+1 != length){
                            //lastNode should not be the root,
                            //unless we are at the last bracket
                            throw new Error("Too many closing brackets in line "
                                            +line);
                        }
                    } else { //you can go one step up
                        lastNode = lastNode.getMother();
                    }
                } else {
                    //nextChar is not a closing bracket;
                    //something went wrong here
                    throw new Error("Expected closing bracket in line "
                                   +line+", but found "+nextChar);
                }
                index++;
            }
            
        }
    }
   
    /**
     * Passes the given target through this CART and returns the
     * interpretation.
     *
     * @param object the item to analyze
     *
     * @return the interpretation
     */
    public int[] interpret(Target target) {
        Node currentNode = rootNode;
        
        FeatureVector featureVector = target.getFeatureVector();
        
        //logger.debug("Starting cart at "+nodeIndex);
        while (!(currentNode instanceof LeafNode)) { 
            //while we have not reached the bottom,
            //get the next node based on the features of the target
           currentNode = ((DecisionNode)currentNode).getNextNode(featureVector);
	        //logger.debug(decision.toString() + " result '"+ decision.findFeature(item) + "' => "+ nodeIndex);
        }
        
        //get the indices from the leaf node
        int[] result = ((LeafNode) currentNode).getAllIndices();
        
        int limit = backtrace;
        //set backtrace to false (default)
        boolean backtrace = false;
        while (result.length<limit){
            //set backtrace to true if we have not enough units
            backtrace = true;
            
            //get the mother node
            Node motherNode = currentNode.getMother(); 
            
            result = ((DecisionNode) motherNode).getAllIndices();
        }
        if (backtrace){
            logger.debug("Selected "+result.length+" units on backtrace");
        } else {
            logger.debug("Selected "+result.length+" units without backtrace");
        }
        return result;
        
    }
    
    /**
     * A node for the CART.
     * All node types inherit from this class
     */
    static abstract class Node {
        
        //isRoot should be set to true if this node is the root node
        protected boolean isRoot;
        
        //every node except the root node has a mother
        protected Node mother;
        
    
        /**
         * set the mother node of this node
         * @param node the mother node
         */
        public void setMother(Node node){
            this.mother = node;
        }
    
        /**
         * Get the mother node of this node
         * @return the mother node
         */
        public Node getMother(){
            return mother;
        }
        
        /**
         * Set isRoot to the given value
         * @param isRoot the new value of isRoot
         */
        public void setIsRoot(boolean isRoot){
            this.isRoot = isRoot;
        }
        
        /**
         * Get the setting of isRoot
         * @return the setting of isRoot
         */
        public boolean isRoot(){
            return isRoot;
        }
        
        /**
         * Get all unit indices from all leaves below this node
         * @return an int array containing the indices
         */
        public abstract int[] getAllIndices();
        
        
        /**
         * Writes the Cart to the given StringBuffer in Wagon Format
         * @param sb the StringBuffer
        */
        public abstract void toWagonFormat(StringBuffer sb);
        
        
    }

    /**
     * A decision node that determines the next Node to go to in the CART.
     * All decision nodes inherit from this class
     */
    static abstract class DecisionNode extends Node {
        
        //a decision node has an array of daughters
        protected Node[] daughters;
        //the feature index
        protected int featureIndex;
        //remember last added daughter
        protected int lastDaughter;
        
        /**
         * Construct a new DecisionNode
         * @param featureIndex the feature index 
         * @param numDaughters the number of daughters
         */
        public DecisionNode (int featureIndex,
                            int numDaughters){
            this.featureIndex = featureIndex;
            daughters = new Node[numDaughters];
            isRoot = false;
        }
        
        /**
         * Add a daughter to the node
         * @param daughter the new daughter
         */
        public void addDaughter(Node daughter){
            if (lastDaughter > daughters.length-1){
                throw new Error("Can not add daughter number "
                        +lastDaughter+1+", since node has only "
                        +daughters.length+" daughters!");
            }
            daughters[lastDaughter] = daughter;
            lastDaughter++;
        }
        
        /**
         * Get all unit indices from all leaves below this node
         * @return an int array containing the indices
         */
        public int[] getAllIndices(){
            int[] result = null;
            if (daughters.length == 2){
                //if we have just two daughters, merging the indices is trivial
                int[] indices1 = daughters[0].getAllIndices();
                int[] indices2 = daughters[1].getAllIndices();
                result = new int[indices1.length+indices2.length];
                System.arraycopy(indices1,0,result,0, indices1.length);
                System.arraycopy(indices2,0,result,indices1.length, indices2.length);  
            } else {
                //we have more than two daughters 
                //for each daughter, get her indices 
                //and merge them with the other indices 
                int[] indices1 = new int[0];
                for (int i=0;i<daughters.length;i++){
                    int[] indices2 = daughters[i].getAllIndices();
                    result = new int[indices1.length+indices2.length];
                    System.arraycopy(indices1,0,result,0, indices1.length);
                    System.arraycopy(indices2,0,result,indices1.length, indices2.length);  
                    indices1 = result;
                }
            }
            return result;
        }
        
         /**
         * Writes the Cart to the given StringBuffer in Wagon Format
         * @param sb the StringBuffer
        */
        public void toWagonFormat(StringBuffer sb){
            //first add two open brackets
            sb.append("((");
            //now the values of the node
            sb.append(getNodeDefinition());
            //add the daughters
            for (int i=0;i<daughters.length;i++){
                daughters[i].toWagonFormat(sb);
                if (i+1!=daughters.length){
                    sb.append("\n");
                }
            }
            //add a closing bracket
            sb.append(")");
        }
        
    
        /**
         * Gets the String that defines the decision done in the node
         * @return the node definition
         */
        public abstract String getNodeDefinition();
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public abstract Node getNextNode(FeatureVector featureVector);
        
    }
   
        
    /**
     * A binary decision Node that compares two byte values.
     */
    static class BinaryByteDecisionNode extends DecisionNode {
        
        //the value of this node
        private byte value;
        
         /**
         * Create a new binary String DecisionNode.
         * @param feature the string used to get a value from an Item
         * @param value the value to compare to
         */
        public BinaryByteDecisionNode(int feature,
                                    byte value) {
            super(feature,2);
            this.value = value;
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            byte val = featureVector.getByteValuedDiscreteFeature(featureIndex);
            Node returnNode;
            if (val == value){
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            } 
            return returnNode;
        }
       
         /**
         * Gets the String that defines the decision done in the node
         * @return the node definition
         */
        public String getNodeDefinition(){
            return "b"+featureIndex+" is "+value+")\n";
        }
        
    }
    
    /**
     * A binary decision Node that compares two short values.
     */
    static class BinaryShortDecisionNode extends DecisionNode {
        
        //the value of this node
        private short value;
        
         /**
         * Create a new binary String DecisionNode.
         * @param feature the string used to get a value from an Item
         * @param value the value to compare to
         */
        public BinaryShortDecisionNode(int feature,
                                    short value) {
            super(feature,2);
            this.value = value;
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            short val = featureVector.getShortValuedDiscreteFeature(featureIndex);
            Node returnNode;
            if (val == value){
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            } 
            return returnNode;
        }
        
        /**
         * Gets the String that defines the decision done in the node
         * @return the node definition
         */
        public String getNodeDefinition(){
            return "s"+featureIndex+" is "+value+")\n";
        }
       
    }
    
    /**
     * A binary decision Node that compares two float values.
     */
    static class BinaryFloatDecisionNode extends DecisionNode {
        
        //the value of this node
        private float value;
        
         /**
         * Create a new binary String DecisionNode.
         * @param feature the string used to get a value from an Item
         * @param value the value to compare to
         */
        public BinaryFloatDecisionNode(int feature,
                                    float value) {
            super(feature,2);
            this.value = value;
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            float val = featureVector.getContinuousFeature(featureIndex);
            Node returnNode;
            if (val < value){
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            } 
            return returnNode;
        }
        
        /**
         * Gets the String that defines the decision done in the node
         * @return the node definition
         */
        public String getNodeDefinition(){
            return "f"+featureIndex+" < "+value+")\n";
        }
        
       
    }
      
        
    /**
     * An decision Node with an arbitrary number of daughters.
     * Value of the target corresponds to the index number of next daughter.
     */
    static class ByteDecisionNode extends DecisionNode {
        
        /**
         * Build a new unary byte decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         */
        public ByteDecisionNode(int feature,
                               int numDaughters){
            super(feature, numDaughters);           
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            return daughters[featureVector.getByteValuedDiscreteFeature(featureIndex)];
        }
        
        /**
         * Gets the String that defines the decision done in the node
         * @return the node definition
         */
        public String getNodeDefinition(){
            return featureIndex+" isByteOf "+daughters.length+")\n";
        }
        
    }
    
    /**
     * An decision Node with an arbitrary number of daughters.
     * Value of the target corresponds to the index number of next daughter.
     */
    static class ShortDecisionNode extends DecisionNode {
        
        /**
         * Build a new unary byte decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         */
        public ShortDecisionNode(int feature,
                               int numDaughters){
            super(feature, numDaughters);           
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            return daughters[featureVector.getShortValuedDiscreteFeature(featureIndex)];
        }
        
        /**
         * Gets the String that defines the decision done in the node
         * @return the node definition
         */
        public String getNodeDefinition(){
            return featureIndex+" isShortOf "+daughters.length+")\n";
        }
        
    }
        
   
    

    /**
     * The leaf of a CART.
     */
    static class LeafNode extends Node {
        
        private int[] indices;
        
        /**
         * Create a new LeafNode.
         * @param tok the String Tokenizer containing the String with the indices
         * @param openBrackets the number of opening brackets at the first token
         */
        public LeafNode(StringTokenizer tok) {
            super();
            isRoot = false;
            //read the indices from the tokenized String
            //lines are of form 
            //((<index1> <float1>)...(<indexN> <floatN>)) 0))
            int numTokens = tok.countTokens();
            int index = 0;
            indices = new int[(numTokens-1)/2];
           
            while (index*2<numTokens-1){ //while we are not at the last token
                String nextToken = tok.nextToken();
                if (index == 0){ 
                    // we are at first token, discard all open brackets
                    nextToken = nextToken.substring(4);
                } else { 
                    //we are not at first token, only one open bracket
                    nextToken = nextToken.substring(1);   
                }
                //store the index of the unit
                indices[index] = Integer.parseInt(nextToken);
                //discard next token
                tok.nextToken();
                //increase index
                index++;
            }
        }
        
       
        /**
         * Get all unit indices
         * @return an int array containing the indices
         */
        public int[] getAllIndices(){
            return indices;
        }
        
         /**
         * Writes the Cart to the given StringBuffer in Wagon Format
         * @param sb the StringBuffer
        */
        public void toWagonFormat(StringBuffer sb){
            //open three brackets
            sb.append("(((");
            //for each index, write the index and then a pseudo float
            for (int i=0;i<indices.length;i++){
                sb.append("("+indices[i]+" 0)");
            }
            //write the ending
            sb.append(") 0))");
        }
        
        
    }
}

