/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.cart.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.MaryHeader;

/**
 * IO functions for CARTs in WagonCART format
 * 
 * @author Anna Hunecke, Marc Schröder, Marcela Charfuelan
 */
public class WagonCARTReader {
    
   private Node rootNode;
   private Node lastNode;

   // knows the index numbers and types of the features used in DecisionNodes
   private FeatureDefinition featDef;
  
   private int openBrackets;
   
   // Since it is not known from the wagon file lines which kind of leaves 
   // should be read, a treeType argument should be provided when creating 
   // this class. The treeType will determine the leafType.
   private int leafType;

   // added because StringCART
   private int targetFeature;
   
   /**
    * When creating a WagonCARTReader provide a tree type:
    * @param treeType 
    *         ClasificationTree, ExtendedClassificationTree, RegressionTree, or
    *         TopLevelTree.
    *         
    *   <p>ClasificationTree          --> IntArrayLeafNode
    *   <p>ExtendedClassificationTree --> IntAndFloatArrayLeafNode
    *   <p>RegressionTree             --> FloatLeafNode
    *   <p>TopLevelTree               --> FeatureVectorLeafNode
    *   <p>StringCART                 --> StringAndFloatLeafNode
    */
   public WagonCARTReader(String treeType) {
     if(treeType.contentEquals("ClassificationTree")  )
       leafType = 1;
     else if(treeType.contentEquals("ExtendedClassificationTree") )
       leafType = 2;
     else if(treeType.contentEquals("RegressionTree") )
       leafType = 3;
     else if(treeType.contentEquals("TopLeavelTree") )
       leafType = 4;  
     else
       throw new IllegalArgumentException("Tree type: " + treeType + " not supported.");  
   }
   
   /**
    * When creating a WagonCARTReader for StringCART provide a target feature:
    * 
    * @param targetFea target feature
    */
   public WagonCARTReader(int targetFea) {
     leafType = 5;  // for StringCART --> StringAndFloatLeafNode
     targetFeature = targetFea;  
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
   protected LeafNode createLeafNode(String line) {
      if(leafType == 1)         
        return(createIntArrayLeafNode(line));           // 1 for ClassificationTree
      else if(leafType == 2)    
        return(createIntAndFloatArrayLeafNode(line));   // 2 for ExtendedClassificationTree
      else if(leafType == 3)    
        return(createFloatLeafNode(line));              // 3 for RegressionTree
      else if(leafType == 4)    
        return(createFeatureVectorLeafNode(line));      // 4 for TopLeavelTree
      else if(leafType == 5)
        return(createStringAndFloatLeafNode(line));     // 5 for StringCART
      else
        return null;
   }
   
   
   // in case of using the reader more than once for different root nodes.
   private void cleadReader() {
       rootNode = null;
       lastNode = null;
       featDef = null;
       openBrackets = 0;
   }
   
   /**
    * 
    * This loads a cart from a wagon tree in textual format, from a reader.
    * 
    * @param reader the Reader providing the wagon tree
    * @param featDefinition
    * @throws IOException 
    */
   public Node load(BufferedReader reader, FeatureDefinition featDefinition) throws IOException{
       cleadReader();
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
       
       return rootNode;
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
   // TODO: CHECK! do we need that String[] dummy???
    public Node load(String fileName, FeatureDefinition featDefinition, String[] dummy )
            throws IOException {
        cleadReader();
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
        
        return rootNode;
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

        
 
   
    protected LeafNode createIntArrayLeafNode(String line) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        int index = 0;
        // The data to be saved in the leaf node:
        int[] indices;
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
        return new LeafNode.IntArrayLeafNode(indices);
    }
    
    
    protected LeafNode createIntAndFloatArrayLeafNode(String line) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        int index = 0;
        // The data to be saved in the leaf node:
        int[] indices;
        // The floats to be saved in the leaf node:
        float[] probs;
        
        //System.out.println("Line: "+line+", numTokens: "+numTokens);
        
        if (numTokens == 2) { // we do not have any indices
            // discard useless token
            tok.nextToken();
            indices = new int[0];
            probs = new float[0];
        } else {
            indices = new int[(numTokens - 1) / 2];
            // same length
            probs = new float[indices.length];
            
            while (index * 2 < numTokens - 1){
                String token = tok.nextToken();
                if (index == 0){
                    token = token.substring(4);
                }else{
                    token = token.substring(1);
                }
                //System.out.println("int-token: "+token);
                indices[index] = Integer.parseInt(token);
                    
                token = tok.nextToken();
                int lastIndex = token.length() - 1;
                if ((index*2) == (numTokens - 3)){
                    token = token.substring(0,lastIndex-1);
                    if (token.equals("inf")){
                        probs[index]=10000;
                        index++;
                        continue;
                    }
                    if (token.equals("nan")){
                        probs[index]=-1;
                        index++;
                        continue;
                    }
                }else{
                    token = token.substring(0,lastIndex);
                    if (token.equals("inf")){
                        probs[index]=1000000;
                        index++;
                        continue;
                    }
                    if (token.equals("nan")){
                        probs[index]=-1;
                        index++;
                        continue;
                    }
                }
                //System.out.println("float-token: "+token);
                probs[index] = Float.parseFloat(token);
                index++;    
            } // end while
       
        } // end if
        
        return new LeafNode.IntAndFloatArrayLeafNode(indices,probs);
    }
    
    

    protected LeafNode createFloatLeafNode(String line) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        if (numTokens != 2) { // we need exactly one value pair
            throw new IllegalArgumentException("Expected two tokens in line, got "+numTokens+": '"+line+"'");
        }

        // The data to be saved in the leaf node:
        float[] data = new float[2]; // stddev and mean;
        String nextToken = tok.nextToken();
        nextToken = nextToken.substring(2);
        try {
            data[0] = Float.parseFloat(nextToken);
        } catch (NumberFormatException nfe) {
            data[0] = 0; // cannot make sense of the standard deviation
        }
        nextToken = tok.nextToken();
        nextToken = nextToken.substring(0, nextToken.indexOf(")"));
        try {
            data[1] = Float.parseFloat(nextToken);
        } catch (NumberFormatException nfe) {
            data[1] = 0;
        }
        return new LeafNode.FloatLeafNode(data);
    }
    
    


    protected LeafNode createFeatureVectorLeafNode(String line) {
     
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
     * Fill the FeatureVector leafs of a tree with the given feature vectors.
     * This function is only used in TopLeavelTree.
     * @param root node of the tree.
     * @param featureVectors the feature vectors.
     */
    public void fillLeafs(Node root, FeatureVector[] featureVectors){
      if(leafType == 4) {
        rootNode = root;  
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
     } else
       throw new IllegalArgumentException("The leaves of this tree are not FeatureVectorLeafNode.");
             
        
    }   
    
    
    protected LeafNode createStringAndFloatLeafNode(String line) {
        // CHECK! if this works, is it necessary a different target feature for each 
        // StringAndFloatLeafNode ??
        int tf = this.targetFeature;  
        
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        int index = 0;
        
        List<Integer> indexList = new ArrayList<Integer>();
        List<Float> probList = new ArrayList<Float>();
                
        //System.out.println("Line: "+line+", numTokens: "+numTokens);
        
        if (numTokens == 2) { // we do not have any indices
            // discard useless token
            tok.nextToken();
        } else {
            
            while (index * 2 < numTokens - 1){
                String token = tok.nextToken();
                if (index == 0){
                    token = token.substring(4);
                }else{
                    token = token.substring(1);
                }
                //System.out.println("int-token: "+token);
                indexList.add((int) this.featDef.getFeatureValueAsShort(tf, token));//getFeatureIndex(token));
                    
                token = tok.nextToken();
                int lastIndex = token.length() - 1;
                if ((index*2) == (numTokens - 3)){
                    token = token.substring(0,lastIndex-1);
                    if (token.equals("inf")){
                        probList.add(100000f);
                        index++;
                        continue;
                    }
                    if (token.equals("nan")){
                        probList.add(-1f);
                        index++;
                        continue;
                    }
                }else{
                    token = token.substring(0,lastIndex);
                    if (token.equals("inf")){
                        probList.add(100000f);
                        index++;
                        continue;
                    }
                    if (token.equals("nan")){
                        probList.add(-1f);
                        index++;
                        continue;
                    }
                }
                //System.out.println("float-token: "+token);
                probList.add(Float.parseFloat(token));
                index++;    
            } // end while
       
        } // end if
        
        assert(indexList.size() == probList.size());
        
        // The data to be saved in the leaf node:
        int[] indices = new int[indexList.size()];
        // The floats to be saved in the leaf node:
        float[] probs = new float[probList.size()];
        
        
        for (int i = 0 ; i < indexList.size(); i++){
            indices[i] = indexList.get(i);
            probs[i]   = probList.get(i);
        }
        
        return new LeafNode.StringAndFloatLeafNode(indices,probs,this.featDef,tf);
    }

}
