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
import de.dfki.lt.mary.unitselection.FeatureFileIndexer;

import java.io.*;
import java.util.*;

/**
 * This class can build a CART either reading from a text file
 * or from a binary file. 
 * The format is an extended Wagon format:
 * A node looks like this:
 * ((comparison)(daughter1)(daugther2)(daughter3)...(daughterN))
 * 
 * (comparison) is of form:
 * (<feature-index> is <value>) : binary decision node; a feature-index beginning
 * 								  with b means byte feature; beginning with 
 * 								  s means short feature
 * (<feature-index> < <value>) : binary float decision node
 * (<feature-index> oneByteOf <n>) : n-ary byte decision node
 * (<feature-index> oneShortOf <n>) : n-ary short decision node
 * 
 * (daughter) is either a node (see above) or a leaf:
 * ((<index1> <float1>)...(<indexN> <floatN>)) : leaf with unit indices 
 * 												 and (dummy) floats
 * 
 * @author Anna Hunecke
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
    
    //knows the index numbers´and types of the features
    private static FeatureDefinition featDef;
  
    
    /**
     * Creates a new CART by reading from the given reader.
     *
     * @param reader the source of the CART data
     * @throws IOException if errors occur while reading the data
     */ 
    public CARTWagonFormat(BufferedReader reader, 
            				FeatureDefinition featDefinition) throws IOException {
        featDef = featDefinition;
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
    public CARTWagonFormat(RandomAccessFile raf,
            				FeatureDefinition featDefinition) throws IOException {
        featDef = featDefinition;
    	String backtraceString = 
    	    MaryProperties.getProperty("english.cart.backtrace");
    	backtrace = 100;
    	if (backtraceString != null){
    	    backtrace = Integer.parseInt(backtraceString.trim());
    	}
    	openBrackets = 0;
        String cart = raf.readUTF();
        StringTokenizer tok = new StringTokenizer(cart,"\n");
    	while (tok.hasMoreTokens()) {
    	   
    	    parseAndAdd(tok.nextToken());
    	}
    	if (openBrackets != 0){
            throw new Error("Something went wrong here");
        }
    }
  
    /**
     * Build a new CART for the feature sequence in the 
     * FeatureFileIndexer
     * 
     * @param ffi the feature file indexer
     */
    public CARTWagonFormat(FeatureFileIndexer ffi){
        int[] featSeq = ffi.getFeatureSequence();
        addDaughters(null,featSeq,0);
    }
    
    /**
     * Add daughters to the given mother node
     * according to the given feature sequence
     * 
     * @param motherNode the mother node
     * @param featSeq the feature sequence
     * @param index the current index position in the sequence
     */
    public void addDaughters(DecisionNode motherNode, int[] featSeq, int index){
       //the next daughter
       DecisionNode daughterNode;
       //the number of daughters of the next daughter
       int numDaughters;
       //the index of the next feature
       int nextFeatIndex = featSeq[index];
       if (featDef.isByteFeature(nextFeatIndex)){
           numDaughters = featDef.getNumberOfValues(nextFeatIndex);
           daughterNode = new ByteDecisionNode(nextFeatIndex,numDaughters);
       } else {
           if (featDef.isShortFeature(nextFeatIndex)){
               numDaughters = featDef.getNumberOfValues(nextFeatIndex);
               daughterNode = new ShortDecisionNode(nextFeatIndex,numDaughters);
            } else {
                //feature is of type float, currently not supported in ffi
                throw new Error("Found float feature in FeatureFileIndexer!");
            }
       }
       if (motherNode == null){
           rootNode = daughterNode;
       } else {
           motherNode.addDaughter(daughterNode);
           daughterNode.setMother(motherNode);
       }
       //if we are not at the last feature
       if (index+1 != featSeq.length){
           //for each daughter, go in recursion
           for (int i = 0; i<numDaughters; i++){
               addDaughters(daughterNode,featSeq,index+1);
           }     
       } else {
           //add empty leaves
           for (int i=0;i<numDaughters;i++){
               daughterNode.addDaughter(new LeafNode());
           }
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
     * Dumps this CART to the standard out in WagonFormat.
     *
     * @throws IOException if an error occurs during output
     */
    public void toStandardOut() {
        StringBuffer sb = new StringBuffer();
        rootNode.toWagonFormat(sb);
        System.out.println(sb.toString());
    }
    
    
    /**
     * Creates a node from the given input line and add it to the CART.
     * 
     * @param line a line of input to parse
     */
    protected void parseAndAdd(String line) {
        //remove whitespace at beginning of string
        line = line.trim();
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
                if (featDef.isByteFeature(feature)){
                    nextNode = new BinaryByteDecisionNode(feature,value);
                } else {
                    nextNode = new BinaryShortDecisionNode(feature,value);
                }
            } else {
                if (type.equals("<")){
                    nextNode = new BinaryFloatDecisionNode(feature,Float.parseFloat(value));
                } else {
                    if (type.equals("isShortOf")){
                        nextNode = new ShortDecisionNode(feature,Integer.parseInt(value));
                    } else {
                        if (type.equals("isByteOf")){
                            nextNode = new ByteDecisionNode(feature,Integer.parseInt(value));
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
     * Replace a leaf by a CART
     * @param cart the new CART
     */
    public void replaceLeafByCart(CARTWagonFormat cart, FeatureVector vector){
        //find the mother node of the leaf you want to replace
        DecisionNode motherNode;
        Node currentNode = rootNode;
        while (!(currentNode instanceof LeafNode)) { 
            //while we have not reached the bottom,
            //get the next node based on the feature vector
           currentNode = ((DecisionNode)currentNode).getNextNode(vector);
        }
        //now we are at the leaf that we want to replace
        //get the mother
        motherNode = (DecisionNode) currentNode.getMother();
        //replace the leaf by the CART
        motherNode.replaceLeafByCart(cart,vector);
        //clean up
        currentNode.setMother(null);
        cart.setRootNode(null);
    }
    
     /**
     * Set the root node of this CART
     * 
     * @param newRoot the new root node
     */
    public void setRootNode(Node newRoot){
        rootNode = newRoot;
    }
    
    /**
     * Get the root node of this CART
     * 
     * @return the root node
     */
    public Node getRootNode(){
        return rootNode;
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
         * @param feature the feature  
         * @param numDaughters the number of daughters
         */
        public DecisionNode (String feature,
                            int numDaughters){
            this.featureIndex = featDef.getFeatureIndex(feature);
            daughters = new Node[numDaughters];
            isRoot = false;
        }
        
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
     
        /**
         * Replace the leaf you select according to the feature
         * vector by the given cart
         * @param cart the cart
         * @param featureVector the feature vector
         */
        public abstract void replaceLeafByCart(CARTWagonFormat cart, FeatureVector featureVector);
       
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
        public BinaryByteDecisionNode(String feature,
                                    String value) {
            super(feature,2);
            this.value = featDef.getFeatureValueAsByte(value,feature);
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            byte val = featureVector.getByteFeature(featureIndex);
            Node returnNode;
            if (val == value){
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            } 
            return returnNode;
        }
       
         /**
         * Replace the leaf you select according to the feature
         * vector by the given cart
         * @param cart the cart
         * @param featureVector the feature vector
         */
        public void replaceLeafByCart(CARTWagonFormat cart, FeatureVector featureVector){
            byte val = featureVector.getByteFeature(featureIndex);
            Node cartRootNode = cart.getRootNode();
            if (val == value){
                daughters[0] = cartRootNode;
            } else {
                daughters[1] = cartRootNode;
            } 
            cartRootNode.setMother(mother);
            cartRootNode.setIsRoot(false);
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
        public BinaryShortDecisionNode(String feature,
                					String value) {
            super(feature,2);
            this.value = featDef.getFeatureValueAsShort(value,feature);
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            short val = featureVector.getShortFeature(featureIndex);
            Node returnNode;
            if (val == value){
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            } 
            return returnNode;
        }
        
        /**
         * Replace the leaf you select according to the feature
         * vector by the given cart
         * @param cart the cart
         * @param featureVector the feature vector
         */
        public void replaceLeafByCart(CARTWagonFormat cart, FeatureVector featureVector){
            short val = featureVector.getShortFeature(featureIndex);
            Node cartRootNode = cart.getRootNode();
            if (val == value){
                daughters[0] = cartRootNode;
            } else {
                daughters[1] = cartRootNode;
            } 
            cartRootNode.setMother(mother);
            cartRootNode.setIsRoot(false);
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
        public BinaryFloatDecisionNode(String feature,
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
         * Replace the leaf you select according to the feature
         * vector by the given cart
         * @param cart the cart
         * @param featureVector the feature vector
         */
        public void replaceLeafByCart(CARTWagonFormat cart, FeatureVector featureVector){
            float val = featureVector.getContinuousFeature(featureIndex);
            Node cartRootNode = cart.getRootNode();
            if (val < value){
                daughters[0] = cartRootNode;
            } else {
                daughters[1] = cartRootNode;
            } 
            cartRootNode.setMother(mother);
            cartRootNode.setIsRoot(false);
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
         * Build a new byte decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         */
        public ByteDecisionNode(String feature,
                               int numDaughters){
            super(feature, numDaughters);           
        }
        
        /**
         * Build a new byte decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         */
        public ByteDecisionNode(int featureIndex,
                               int numDaughters){
            super(featureIndex, numDaughters);           
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            return daughters[featureVector.getByteFeature(featureIndex)];
        }
        
        /**
         * Replace the leaf you select according to the feature
         * vector by the given cart
         * @param cart the cart
         * @param featureVector the feature vector
         */
        public void replaceLeafByCart(CARTWagonFormat cart, FeatureVector featureVector){
            byte index = featureVector.getByteFeature(featureIndex);
            Node cartRootNode = cart.getRootNode();
            daughters[index] = cartRootNode;
            cartRootNode.setMother(mother);
            cartRootNode.setIsRoot(false);
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
         * Build a new short decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         */
        public ShortDecisionNode(String feature,
                               int numDaughters){
            super(feature, numDaughters);           
        }
        
         /**
         * Build a new short decision node 
         * @param featureIndex the feature index
         * @param numDaughters the number of daughters
         */
        public ShortDecisionNode(int featureIndex,
                               int numDaughters){
            super(featureIndex, numDaughters);           
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            return daughters[featureVector.getShortFeature(featureIndex)];
        }
        
        /**
         * Replace the leaf you select according to the feature
         * vector by the given cart
         * @param cart the cart
         * @param featureVector the feature vector
         */
        public void replaceLeafByCart(CARTWagonFormat cart, FeatureVector featureVector){
            short index = featureVector.getShortFeature(featureIndex);
            Node cartRootNode = cart.getRootNode();
            daughters[index] = cartRootNode;
            cartRootNode.setMother(mother);
            cartRootNode.setIsRoot(false);
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
        
       
        public LeafNode(){}
        
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
                if (i+1!=indices.length){
                    sb.append(" ");
                }
            }
            //write the ending
            sb.append(") 0))");
        }
        
        
    }
}

