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
import java.io.InputStreamReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.cart.Node;
import marytts.cart.DecisionNode;
import marytts.cart.DecisionNode.*;
import marytts.cart.LeafNode;
import marytts.cart.LeafNode.*;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.MaryHeader;

/**
 * IO functions for CARTs in MaryCART format
 * 
 * @author Marcela Charfuelan
 */
public class MaryCARTReader
{
    private Node rootNode;

    // knows the index numbers and types of the features used in DecisionNodes
    private FeatureDefinition featDef;
   
    private Node lastNode;
    
    
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
    public Node load(String fileName, FeatureDefinition featDefinition, String[] dummy )
            throws IOException {
        //System.out.println("Loading file");
        // open the CART-File and read the header
        
        int i, j, length;
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
        int numNodes = raf.readInt();     // number of nodes including empty nodes
        raf.readUTF();
        int numDecNodes = raf.readInt();  // number of decision nodes
        int numLeafNodes = raf.readInt(); // number of leaves, it does not include empty leaves
        
        // Load the CART
        featDef = featDefinition;       
        Vector<String> decNodeLines = new Vector<String>();
        
        // (1) Read the decision lines first and keep them in a Vector of strings
        for(j= 0; j<numDecNodes; j++){
          length = raf.readInt();
          decNodeLines.add(readNextLine(raf, length));
          //System.out.println(decNodeLines.elementAt(j));
        }
        // (2) Parse each of the lines in Vector decNodeLines and add the nodes.
        // Whenever a leaf node is required, we need to read a new line of raf.
        // The leaf nodes with id will be created in the order they appear in the tree.
        int numLine=0;
        addDecisionNode(raf, decNodeLines, numLine);
   
        // set the rootNode as the rootNode of cart
        
        return rootNode;
       
    }

    private int addDecisionNode(DataInput raf, Vector<String> decNodeLines, int numLine)
        throws IOException{
        
        String line;
        line = decNodeLines.elementAt(numLine);
        //System.out.println("(" + (numLine+1) + ") Creating decision node: " + line);
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
          
        String decId = tokenizer.nextToken();
        String feature = tokenizer.nextToken();
        String type = tokenizer.nextToken();
        String value = tokenizer.nextToken();
          
        // build new node depending on type
        Node nextNode = createDecisionNode(decId, feature, type, value);
        
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
          
        // If the last node created has children, create those children until it does not have more
        // continue reading the elements in line so we know wether to add a leaf or a new decision node
        while (tokenizer.hasMoreTokens()) {
            
          String nextNodeId = tokenizer.nextToken();
          if(nextNodeId.contentEquals("0") || nextNodeId.startsWith("id")) {
              // read next line in raf file, create with that line a leafNode and add the node as daughter of current node
              // if netxtNodeId = 0 create an empty leaf and add the node as daughter of current node              
              // check that the number in decId is the same as in the read line from raf.
              nextNode = createLeafNode(raf,nextNodeId);
            
              // set the relations of this node to the others
              if (lastNode == null) { // this node is the root
                 rootNode = nextNode;
                 nextNode.setIsRoot(true);
              } else { // this node is a daughter of lastNode
                 nextNode.setMother(lastNode);
                 ((DecisionNode) lastNode).addDaughter(nextNode);
              }
            
          } else if(nextNodeId.startsWith("-")) {
              // we need to create a new decision node
              // call recursivelly this program, making the new decision node the lastNode 
              numLine++;
              numLine = addDecisionNode(raf, decNodeLines, numLine);             
          }
          
        }  // while more tokens in line
        
        // This program should return when all the tokens in one line of decNodeLines are processed.
        // When finishing the tokens in one line we need to go one step up
        lastNode = lastNode.getMother();
       
        return numLine;
    }
    
    
    /** Read next line from binary file. */
    private String readNextLine(DataInput raf, int length) 
             throws IOException{
      
      String cartLine = "";           
      try {       
        char[] cartChars = new char[length];
        for (int i = 0; i < length; i++) {
          cartChars[i] = raf.readChar();
        }
        cartLine = new String(cartChars); 
      } catch (IOException eof) {}
      
      return cartLine;
        
    }
    
    private Node createDecisionNode(String id, String feature, String type, String value){
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
          throw new RuntimeException("Cannot create decision node for cart line: " 
                                     + " " + id + " " + feature + " " + type + " " + value, exc);
        }  
        return nextNode;
    }
    
    /** Get the next line from DataInput, check that the ids are the same 
     * and create a node as indicated in the line.
     * returns null if the new leaf node can not be created. */
    private Node createLeafNode(DataInput raf, String leafId) throws IOException{
  
      Node nextLeafNode = null;  
      int numIndices;  // Number of indices
      int[] indices;   // The data to be saved in the leaf node:
      float[] probs;   // The floats to be saved in the leaf node:     
      String probNum;  // to check if the float is not "inf" or "nan", is it necessary???
           
      if(leafId.contentEquals("0")){
         // create an empty node, will this work for all kinds of trees???
         indices = new int[0];
         nextLeafNode = new LeafNode.IntArrayLeafNode(indices);
         //System.out.println("  Creating empty leaf node: id=0");
          
      } else {
        
         // get next line from raf
         int length = raf.readInt();
         String line = readNextLine(raf, length);
         //System.out.println("  Creating leaf node: " + line);
          
         StringTokenizer tokenizer = new StringTokenizer(line, " ");      
         String id = tokenizer.nextToken();
         String typeLeafNode = tokenizer.nextToken();      
         String numData = tokenizer.nextToken();  
      
         if(leafId.contentEquals(id)) {
          
           if(typeLeafNode.contentEquals("FeatureVectorLeafNode")){
             numIndices = Integer.parseInt(numData);
             // here it should be FeatureVector???
             //FeatureVector[] fv = new FeatureVector[numFv];
             //nextLeafNode = new LeafNode.FeatureVectorLeafNode(fv);
          
             indices = new int[numIndices];
             for(int i=0; i<numIndices; i++)
               indices[i] = Integer.parseInt(tokenizer.nextToken());   
             nextLeafNode = new LeafNode.IntArrayLeafNode(indices);

           } else if(typeLeafNode.contentEquals("IntAndFloatArrayLeafNode")){
          
             numIndices = Integer.parseInt(numData);
             indices = new int[numIndices];
             probs = new float[numIndices];
             for(int i=0; i<numIndices; i++){
                indices[i] = Integer.parseInt(tokenizer.nextToken());
                probNum = tokenizer.nextToken();
                if(probNum.equals("inf"))
                  probs[i] = 1000;
                else if(probNum.equals("nan"))
                  probs[i] = -1;
                else
                  probs[i] = Float.parseFloat(probNum);
             }
             nextLeafNode = new LeafNode.IntAndFloatArrayLeafNode(indices,probs);
            
          } else if(typeLeafNode.contentEquals("FloatLeafNode")){
            // this will not work, need to be implemented
            indices = new int[0];
            nextLeafNode = new LeafNode.IntArrayLeafNode(indices);
            
          } else if(typeLeafNode.contentEquals("IntArrayLeafNode")){
            // this will not work, need to be implemented
            indices = new int[0];
            nextLeafNode = new LeafNode.IntArrayLeafNode(indices);
            
          } else if(typeLeafNode.contentEquals("StringAndFloatLeafNode")){
            // this will not work, need to be implemented
            indices = new int[0];
            nextLeafNode = new LeafNode.IntArrayLeafNode(indices);
            
          } else {
            System.out.println("Unknown leaf node format:" + typeLeafNode); 
          }
        
        } else
          System.out.println("Problem reading leaf index=" + leafId + " got from the file index=" + id);

      } // if id is not 0 
     
      return nextLeafNode;
    }
    
    
}
