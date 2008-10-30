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

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.FeatureVectorLeafNode;
import marytts.cart.LeafNode.FloatLeafNode;
import marytts.cart.LeafNode.IntAndFloatArrayLeafNode;
import marytts.cart.LeafNode.IntArrayLeafNode;
import marytts.cart.LeafNode.StringAndFloatLeafNode;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.MaryHeader;

import org.apache.log4j.Logger;

/**
 * IO functions for CARTs in MaryCART format
 * 
 * @author Marcela Charfuelan
 */
public class MaryCARTWriter{

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Dump the CARTs in MaryCART format
     * 
     * @param destDir the destination directory
     */
    public void dumpMaryCART(CART cart, String destFile)
    throws IOException
    {
        logger.debug("Dumping CART in MaryCART format to "+destFile+".new ...");
        
        //Open the destination file (cart.bin) and output the header       
        DataOutputStream out = new DataOutputStream(new
                BufferedOutputStream(new 
                FileOutputStream(destFile)));
        //create new CART-header and write it to output file     
        MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
        hdr.writeTo(out);

        //write number of nodes
        out.writeInt(cart.getNumNodes());
        String name = "";
        //dump name and CART
        out.writeUTF(name);
        
        //dump CART
        dumpBinary(cart.getRootNode(), out);
      
        //finish
        out.close();
        logger.debug(" ... done\n");
    }     
    
    public void toTextOut(CART cart, PrintWriter pw) throws IOException {
        try {
            int id[] = new int[2];
            id[0] = 0;  // number of decision nodes
            id[1] = 0;  // number of leaf nodes
            
            //System.out.println("Total number of nodes:" + rootNode.getNumberOfNodes());
            setUniqueNodeId(cart.getRootNode(), id);
            pw.println("Num decision nodes= " + id[0] + "  Num leaf nodes= " + id[1]);
            printDecisionNodes(cart.getRootNode(),null, pw);
            pw.println("\n----------------\n");
            printLeafNodes(cart.getRootNode(), null, pw);
            
            pw.flush();
            pw.close();
        } catch (IOException ioe) {
            IOException newIOE = new IOException(
                    "Error dumping CART to standard output");
            newIOE.initCause(ioe);
            throw newIOE;
        }
    }
    
    
    private String setUniqueNodeId(Node node, int id[]) throws IOException{
     
      int thisIdNode; 
      String leafstr = "";
      
      // if the node is decision node  
      if(node.getNumberOfNodes() > 1) {  
        id[0]++;
        ((DecisionNode) node).setUniqueDecisionNodeId(id[0]);
        String strNode = "";
        ((DecisionNode) node).setDecNodeStr("");
        //this.decNodeStr = "";
        thisIdNode = id[0];

        strNode = "-" + thisIdNode + " " + ((DecisionNode) node).getNodeDefinition() + " ";
 
        // add Ids to the daughters
        for (int i = 0; i < ((DecisionNode) node).getNumberOfDaugthers(); i++) {            
            strNode += setUniqueNodeId(((DecisionNode) node).getDaughter(i), id);
        }
        ((DecisionNode) node).setDecNodeStr(strNode);
        return "-" + thisIdNode + " ";

      } else {   // the node is a leaf node
         
         if( node instanceof FeatureVectorLeafNode ) 
           leafstr = setUniqueNodeId(((FeatureVectorLeafNode) node), id);
         else if( node instanceof FloatLeafNode ) 
           leafstr = setUniqueNodeId(((FloatLeafNode) node), id);
         else if( node instanceof IntAndFloatArrayLeafNode ) 
           leafstr = setUniqueNodeId(((IntAndFloatArrayLeafNode) node), id);
         else if( node instanceof IntArrayLeafNode ) 
           leafstr = setUniqueNodeId(((IntArrayLeafNode) node), id);
         else if( node instanceof StringAndFloatLeafNode ) 
           leafstr = setUniqueNodeId(((StringAndFloatLeafNode) node), id);
          
        return leafstr;
          
      }
        
    }
    
    
    private void dumpBinary(Node rootNode, DataOutput os) throws IOException {
        try {
            
            int id[] = new int[2];
            id[0] = 0;  // number of decision nodes
            id[1] = 0;  // number of leaf nodes           
            // first add unique identificators to decision nodes and leaf nodes           
            setUniqueNodeId(rootNode, id);
            
            // write the number of decision nodes and the number of leaves.
            os.writeInt(id[0]);
            os.writeInt(id[1]);
            // lines that start with a negative number are decision nodes
            printDecisionNodes(rootNode, (DataOutputStream) os, null);
            // lines that start with id are leaf nodes
            printLeafNodes(rootNode, (DataOutputStream) os, null);
            
        } catch (IOException ioe) {
            IOException newIOE = new IOException(
                    "Error dumping CART to output stream");
            newIOE.initCause(ioe);
            throw newIOE;
        }
    }
    
    
    
    
   private void printDecisionNodes(Node node, DataOutputStream out, PrintWriter pw) throws IOException {
        
     // if the node is decision node  
     if(node.getNumberOfNodes() > 1) {   
        if (out != null) {
            // dump to output stream
            writeStringToOutput(((DecisionNode) node).getDecNodeStr(), out);
        } else {
            // dump to Standard out
            // two open brackets + definition of node
            // System.out.println(this.decNodeStr);
        }
        if (pw != null) {
            // dump to print writer
            pw.println(((DecisionNode) node).getDecNodeStr());
        }
        // add the daughters
        for (int i = 0; i < ((DecisionNode) node).getNumberOfDaugthers(); i++) { 
            if(((DecisionNode) node).getDaughter(i).getNumberOfNodes() > 1)
              printDecisionNodes(((DecisionNode) node).getDaughter(i),out, pw);
        }
    }
   }
     
     
   /** This function will print the leaf nodes only, but it goes through all the decision nodes. */
   private void printLeafNodes(Node node, DataOutputStream out, PrintWriter pw) throws IOException {
       // If the node does not have leaves then it just return.
       // I we are in a decision node then print the leaves of the daughters.
      Node nextNode;
      if(node.getNumberOfNodes() > 1) {   
         for (int i = 0; i < ((DecisionNode) node).getNumberOfDaugthers(); i++) {
           nextNode =  ((DecisionNode) node).getDaughter(i);
           printLeafNodes(nextNode, out, pw);
         }
      } else {
          if( node instanceof FeatureVectorLeafNode ) 
            printLeafNodes(((FeatureVectorLeafNode) node), out, pw);
          else if( node instanceof FloatLeafNode ) 
              printLeafNodes(((FloatLeafNode) node), out, pw);
          // need to have StringAndFloatLeafNode before IntAndFloatArrayLeafNode, because
          // the former extends the latter
          else if( node instanceof StringAndFloatLeafNode ) 
              printLeafNodes(((StringAndFloatLeafNode) node), out, pw);
          else if( node instanceof IntAndFloatArrayLeafNode ) 
              printLeafNodes(((IntAndFloatArrayLeafNode) node), out, pw);
          else if( node instanceof IntArrayLeafNode ) 
              printLeafNodes(((IntArrayLeafNode) node), out, pw);
      }
   }
     
   
   private void printLeafNodes(FeatureVectorLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       StringBuffer sb = new StringBuffer();
       
       if( node.getUniqueLeafId() != 0) {
        FeatureVector fv[] = node.getFeatureVectors(); 
      
        sb.append("id" + node.getUniqueLeafId() + " FeatureVectorLeafNode " + fv.length + " ");
      
       //make sure that we have a feature vector array, this is done when calling getFeatureVectors().     
       // for each index, write the index and then a pseudo float
       for (int i = 0; i < fv.length; i++) {
           sb.append(  fv[i].getUnitIndex() + " ");               
       }
       
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
           pw.println(sb.toString());
       }
       }
   }
   
   public String setUniqueNodeId(FeatureVectorLeafNode node, int id[]) {     
       
       FeatureVector fv[] = node.getFeatureVectors(); 
       if( fv.length > 0 ){
         id[1]++;  
         node.setUniqueLeafId(id[1]);
         return  "id" + id[1] + " ";
       }
       else {
         node.setUniqueLeafId(0);  // empty leaf
         return  "0 ";
       }          
   }
   
   
   private void printLeafNodes(FloatLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       // this has not been tested!!!
       String s = "id" + node.getUniqueLeafId() + " FloatLeafNode 1 "
           + node.getStDeviation() // stddev
           + " "
           + node.getMean(); // mean
           
       // dump the whole stuff
       if (out != null) {
           // write to output stream
           writeStringToOutput(s, out);
           
       } else {
           // write to Standard out
           // System.out.println(sb.toString());
       }
       if (pw != null) {
           // dump to printwriter
           pw.println(s);
       }  
   }
    
   private String setUniqueNodeId(FloatLeafNode node, int id[]) {      
       if( node.getDataLength() > 0 ){
           id[1]++;  
           node.setUniqueLeafId(id[1]);
           return  "id" + id[1] + " "; 
         }
         else {
           node.setUniqueLeafId(0);  // empty leaf
           return  "0 ";
         }           
   }
  
   
   private void printLeafNodes(IntAndFloatArrayLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       StringBuffer sb = new StringBuffer();
       int data[] = node.getIntData();
       float floats[] = node.getFloatData();
       
       if( node.getUniqueLeafId() != 0) {
       sb.append("id" + node.getUniqueLeafId() + " IntAndFloatArrayLeafNode " + data.length + " ");
           
       // for each index, write the index and then its float
       for (int i = 0; i < data.length; i++) {
           sb.append(data[i] + " " + floats[i] + " ");
       }
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
           pw.println(sb.toString());
       }
       }  
   }
   
   private String setUniqueNodeId(IntAndFloatArrayLeafNode node, int id[]) {
       if(node.getIntData().length > 0){
           id[1]++;  
           node.setUniqueLeafId(id[1]);
           return  "id" + id[1] + " ";  
       } else {
           node.setUniqueLeafId(0);  // empty leaf
           return  "0 ";
       }
   }
   
   private void printLeafNodes(IntArrayLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       StringBuffer sb = new StringBuffer();
       int data[] = node.getIntData();
       
       if( node.getUniqueLeafId() != 0) {          
       sb.append("id" + node.getUniqueLeafId() + " IntArrayLeafNode " + data.length + " ");
       
       for (int i = 0; i < data.length; i++) {
           sb.append(data[i] + " ");
       }
       
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
           pw.println(sb.toString());
       }
       }
   }

   private String setUniqueNodeId(IntArrayLeafNode node, int id[]){
       if(node.getIntData().length > 0){              
         id[1]++;  
         node.setUniqueLeafId(id[1]);
         return  "id" + id[1] + " ";
       } else {
         node.setUniqueLeafId(0);  // empty leaf
         return  "0 ";
       } 
         
   }
   
   
   private void printLeafNodes(StringAndFloatLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       
       StringBuffer sb = new StringBuffer();
       int data[] = node.getIntData();
       float floats[] = node.getFloatData();
       
       if( node.getUniqueLeafId() != 0) {
       sb.append("id" + node.getUniqueLeafId() + " StringAndFloatLeafNode " + data.length + " ");
     
       // for each index, write the index and then its float
       for (int i = 0; i < data.length; i++) {
           sb.append(data[i] + " " + floats[i] + " ");
       }
      
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
           pw.println(sb.toString());
       }
       }  
   }
   
   private String setUniqueNodeId(StringAndFloatLeafNode node, int id[]){
       if(node.getIntData().length > 0){
           id[1]++;  
           node.setUniqueLeafId(id[1]);
           return  "id" + id[1] + " ";
         } else {
           node.setUniqueLeafId(0);  // empty leaf
           return  "0 ";
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
   private static void writeStringToOutput(String str, DataOutput out)
           throws IOException {
       out.writeInt(str.length());
       out.writeChars(str);
   }
}
