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
 *   <li>= - the feature is equal to the value 
 *   <li>> - the feature is greater than the value 
 *   <li>MATCHES - the feature matches the regular expression stored in value 
 *   <li>IN - [[[TODO: still guessing because none of the CART's in
 *     Flite seem to use IN]]] the value is in the list defined by the
 *     feature.
 * </ul>
 *
 * <p>[[[TODO: provide support for the IN operator.]]]
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
public class CARTWagonFormat implements CART {

    private Logger logger = Logger.getLogger("CARTImpl");
    private String name = null;
   
    /**
     * The CART. Entries can be DecisionNode or LeafNode.  An
     * ArrayList could be used here -- I chose not to because I
     * thought it might be quicker to avoid dealing with the dynamic
     * resizing.
     */
    Node[] cart = null;

    /**
     * The number of nodes in the CART.
     */
    transient int curNode = 0;
    
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
     * @param nodes the number of nodes to read for this cart
     *
     * @throws IOException if errors occur while reading the data
     */ 
    public CARTWagonFormat(BufferedReader reader, String name) throws IOException {
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
     * @return the CART
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
    	for (int i = 0; i < numNodes; i++) {
    	    
    	    String nodeCreationLine = raf.readUTF();
    	    parseAndAdd(nodeCreationLine);
    	}
	
    }
  
    /**
     * Dumps this CART to the output stream.
     *
     * @param os the output stream
     *
     * @throws IOException if an error occurs during output
     */
    public void dumpBinary(DataOutputStream os) throws IOException {
	os.writeInt(cart.length);
	for (int i = 0; i < cart.length; i++) {
	    cart[i].dumpBinary(os);
	}
    }
    
    
    
    /**
     * Creates a node from the given input line and add it to the CART.
     * It expects the TOTAL line to come before any of the nodes.
     *
     * @param line a line of input to parse
     */
    protected void parseAndAdd(String line) {
        //go through beginning of String and count opening brackets
        
        int index = 0; 
        char nextChar = line.charAt(index);
        while (nextChar == '('){
            openBrackets++;
          
            nextChar = line.charAt(index);
        }
        if (Character.isLetter(nextChar)){ // we have a node
            openBrackets--; //do not count first bracket
            
            //get the properties of the node
            StringTokenizer tokenizer = new StringTokenizer(line," ");
            String feature = tokenizer.nextToken().substring(index);
            String type = tokenizer.nextToken();
            String value = tokenizer.nextToken();
            value = value.substring(0,value.length()-1);
            
            //build new node depending on type
            Node nextNode;
            if (type.equals("is")){
                nextNode = new BinaryStringDecisionNode(feature,value,line);
            } else {
                if (type.equals("<")){
                    nextNode = new BinaryFloatDecisionNode(feature,Float.parseFloat(value),line);
                } else {
                    if (type.equals("isShortOf")){
                        nextNode = new UnaryShortDecisionNode(feature,Integer.parseInt(value),line);
                    } else {
                        if (type.equals("isByteOf")){
                            nextNode = new UnaryByteDecisionNode(feature,Integer.parseInt(value),line);
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
            }
            
            //go on step down
            lastNode = nextNode;
            
        } else { // we have a leaf
            openBrackets-=2; //do not count first two brackets
            
            StringTokenizer tokenizer = new StringTokenizer(line," ");      
            //build new leaf node
            Node nextNode = new LeafNode(tokenizer, index,line);
            
            //this node is a daughter of lastNode
            if (lastNode == null){
                rootNode = nextNode;
            } else {
            nextNode.setMother(lastNode);
            ((DecisionNode) lastNode).addDaughter(nextNode);
            }
            
            //look at the bracketing at the end of the line:            
            //get the last token out of the tokenizer
            String lastToken = tokenizer.nextToken();
            
            //lastToken should look like "0))"
            //more than two brackets mean that this is 
            //the last daughter of one or more nodes
            int length = lastToken.length()-3;
            while (length > 0){
                nextChar = lastToken.charAt(1); 
                if (nextChar == ')'){
                    openBrackets--;
                    //this is the last daughter of lastNode, go one step up
                    lastNode = lastNode.getMother();
                }    
                length--;
            }
            
        }
    }
    
    //only there for compatibility, to be removed later
    public Object interpret(Object object){
        return null;
    }
    //only there for compatibility, to be removed later
    public void correctNumbers(List units){}
    
    /**
     * Passes the given target through this CART and returns the
     * interpretation.
     *
     * @param object the item to analyze
     *
     * @return the interpretation
     */
    public int[] interpret(Target target) {
        //start interpretation from the root
        return interpret(target,rootNode, backtrace);        
    }
    
    public int[] interpret(Target target, Node currentNode, int limit){
        //logger.debug("Starting cart at "+nodeIndex);
        while (!(currentNode instanceof LeafNode)) { 
            //while we have not reached the bottom,
            //get the next node based on the features of the target
           currentNode = ((DecisionNode)currentNode).getNextNode(target);
	        //logger.debug(decision.toString() + " result '"+ decision.findFeature(item) + "' => "+ nodeIndex);
        }
        
        //get the indices from the leaf node
        int[] result = ((LeafNode) currentNode).getValues();
        
        //set backtrace to false (default)
        boolean backtrace = false;
        while (result.length<limit){
            //set backtrace to true if we have not enough units
            backtrace = true;
            
            //logger.debug("Selected "+result.length
            //      +" units. Selecting more units...");
            
            //get the mother node
            Node motherNode = currentNode.getMother(); 
            if (motherNode instanceof BinaryDecisionNode){
                //if the mother is a binary node, get the sister node
                //of the node selected before
                Node unusedNode = ((BinaryDecisionNode)motherNode).getUnusedNode();
                //go down the node and add the indices you find to result
                result = backtrace(result,unusedNode,limit,target);
            } else {
                //if the node is a unary node, get all sisters
                //of the node selected before
                Node[] unusedNodes = ((UnaryDecisionNode)motherNode).getUnusedNodes();
                int index = 0;
                while (result.length<limit 
                        && index<unusedNodes.length){ 
                    //while we need more units and have more nodes,
                    //go down a sister node and add the indices you find to result
                    result = backtrace(result,unusedNodes[index],limit,target);
                    index++;
                }
            }
            //result = backtrace(result,(DecisionNode)cart[previousNode],limit,item);
        }
        if (backtrace){
            logger.debug("Selected "+result.length+" units on backtrace in cart "+name);
        } else {
            logger.debug("Selected "+result.length+" units, no backtrace in cart "+name);
        }
        return result;
        
    }

    private int[] backtrace(int[] oldItems, Node unusedNode, int limit, Target target){
        int[] result, newItems;
       
        //if node is a leaf, new items are all unit indices in leaf
        if (unusedNode instanceof LeafNode){
            newItems = ((LeafNode) unusedNode).getValues();
        } else { //else go down the node 
            newItems = 
                interpret(target,unusedNode,limit-oldItems.length);
        }
        
        if (newItems !=null){ //if we have found more unit indices
            //make a new array containing old and new items
            result = new int[oldItems.length+newItems.length];
            System.arraycopy(oldItems,0,result,0, oldItems.length);
            System.arraycopy(newItems,0,result,oldItems.length, newItems.length);  
        } else { //if we do not have new indices, return old indices
            result = oldItems;
        }
        return result;
    }
    
    /**
     * Return the name of this cart.
     * @return
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * A node for the CART.
     */
    static abstract class Node {
        
        //every node except the root node has a mother
        protected Node mother;
        //the string that defines the node
        protected String line;
        
        /**
         * Dumps the binary form of this node.
         * @param os the output stream to output the node on
         * @throws IOException if an IO error occurs
	    */
        public void dumpBinary(DataOutputStream os) throws IOException{
            os.writeUTF(line);
        }
    
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
        
        
    }

    /**
     * A decision node that determines the next Node to go to in the CART.
     */
    static abstract class DecisionNode extends Node {
        
        /**
         * Add a daughter to the node
         * @param daughter the new daughter
         */
        public abstract void addDaughter(Node daughter);
        
        /**
         * select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public abstract Node getNextNode(Target target);
    }
    
    /**
     * A binary decision node 
     */
    abstract static class BinaryDecisionNode extends DecisionNode {
        
        //a binary decision node has two daughters
        protected Node daughter0;
        protected Node daughter1;
        //the feature
        protected String feature;
        //remember the last decision
        protected int lastDecision;
        //remember the last added daughter
        protected int lastDaughter;
        
        /**
         * Build a new binary decision node
         * @param line the String representation of this node
         */
        public BinaryDecisionNode (String line){
            super();
            this.line = line;
            lastDaughter = 0;
            lastDecision = 1;
      
        }
        
        /**
         * Add a daughter to the node
         * @param daughter the new daughter
         */
        public void addDaughter(Node daughter){
            if (lastDaughter>1){
                throw new Error("Can not add a third daughter to binary node");
            }
            if (lastDaughter == 0){
                daughter0 = daughter;
            } else {
                daughter1 = daughter;
            }
            lastDaughter++;
        }
        
        /**
         * Get the node that was not selected
         * the last time 
         * @return the unused node
         */
        public Node getUnusedNode(){
            //get the daughter that was not used
            if (lastDecision == 0){
                return daughter1;
            } else { 
                return daughter0; }
        }            
    }
        
    /**
     * A binary decision Node that compares two string values.
     */
    static class BinaryStringDecisionNode extends BinaryDecisionNode {
        
        //the value of this node
        private String value;
        
         /**
         * Create a new binary String DecisionNode.
         * @param feature the string used to get a value from an Item
         * @param value the value to compare to
         * @param line the String representation of this node
         */
        public BinaryStringDecisionNode(String feature,
                                String value, String line) {
            super(line);
            this.feature = feature;
            this.value = value;
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(Target target) {
            String val = target.getStringValue(feature);
            Node returnNode;
            if (val.equals(value)){
                returnNode = daughter0;
                lastDecision = 0;
            } else {
                returnNode = daughter1;
                lastDecision = 1;
            } 
            return returnNode;
        }
       
    }
    
        
    /**
     * A binary decision Node that compares two float values.
     */
    static class BinaryFloatDecisionNode extends BinaryDecisionNode {
        
        //the value of this node
        private float value;
        
         /**
         * Create a new binary Float DecisionNode.
         * @param feature the string used to get a value from an Item
         * @param value the value to compare to
         * @param line the String representation of this node
         */
        public BinaryFloatDecisionNode(String feature,
                                float value, String line) {
            super(line);
            this.feature = feature;
            this.value = value;
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(Target target) {
            float val = target.getFloatValue(feature);
            Node returnNode;
            if (val < value){
                returnNode = daughter0;
                lastDecision = 0;
            } else {
                returnNode = daughter1;
                lastDecision = 1;
            } 
            return returnNode;
        }
        
    }

    /**
     * An unary decision node 
     */
    abstract static class UnaryDecisionNode extends DecisionNode {
        
        //a unary decision node has several daughters
        protected Node[] daughters;
        //the feature
        protected String feature;
        //remember the last decision
        protected int lastDecision;
        //remember last added daughter
        protected int lastDaughter;
        
        
        /**
         * Build a new unary decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         * @param line the String representation of this node
         */
        public UnaryDecisionNode (String feature,
                                 int numDaughters,
                                 String line){
            super();
            daughters = new Node[numDaughters];
            lastDecision = 1;
            this.feature = feature;
             this.line = line;
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
         * Get all daughters that were not selected
         * last time
         * @return the unused daughters
         */
        public Node[] getUnusedNodes(){
            Node[] unusedNodes = new Node[daughters.length-1];
            int index=0;
            for (int i =0; i<daughters.length;i++){
                if (i!= lastDecision){
                    unusedNodes[index] = (Node) daughters[i];
                    index++;
                }
            }
            return unusedNodes;
        }
           
    }
        
    /**
     * An unary decision Node that compares two byte values.
     */
    static class UnaryByteDecisionNode extends UnaryDecisionNode {
        
        /**
         * Build a new unary byte decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         * @param line the String representation of this node
         */
        public UnaryByteDecisionNode(String feature,
                                    int numDaughters,
                                    String line){
            super(feature, numDaughters,line);           
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(Target target) {
            lastDecision = target.getByteValue(feature);
            return daughters[lastDecision];
        }
        
    }
    
        
    /**
     * An unary decision Node that compares two short values.
     */
    static class UnaryShortDecisionNode extends UnaryDecisionNode {
        
        /**
         * Build a new unary short decision node 
         * @param feature the feature name
         * @param numDaughters the number of daughters
         * @param line the String representation of this node
         */
        public UnaryShortDecisionNode(String feature,
                                int numDaughters,
                                String line) {
            super(feature,numDaughters,line);
           
        }
        
        
        /**
         * Select a daughter node according to the value
         * in the given target
         * @param target the target
         * @return a daughter
         */
        public Node getNextNode(Target target) {
            lastDecision = target.getShortValue(feature);
            return daughters[lastDecision];
        }
        

    
    }

    

    /**
     * The leaf of a CART.
     */
    static class LeafNode extends Node {
        
        private int[] values;
        
        /**
         * Create a new LeafNode.
         * @param tok the String Tokenizer containing the String with the indices
         * @param openBrackets the number of opening brackets at the first token
         * @param line the String representation of this node
         */
        public LeafNode(StringTokenizer tok, int openBrackets, String line) {
            super();
            this.line = line;
            //read the indices from the tokenized String
            //lines are of form 
            //((<index1> <float1>)...(<indexN> <floatN>)) 0))
            int numTokens = tok.countTokens();
            int index = 0;
            values = new int[(numTokens-1)/2];
           
            while (index*2<numTokens-1){ //while we are not at the last token
                String nextToken = tok.nextToken();
                if (index == 0){ 
                    // we are at first token, discard all open brackets
                    nextToken = nextToken.substring(openBrackets-1);
                } else { 
                    //we are not at first token, only one open bracket
                    nextToken = nextToken.substring(1);   
                }
                //store the index of the unit
                values[index] = Integer.parseInt(nextToken);
                //discard next token
                tok.nextToken();
                //increase index
                index++;
            }
        }
        
        /**
         * Get the unit indices
         * @return the indices
         */
        public int[] getValues(){
            return values;
        }
       
        
    }
}

