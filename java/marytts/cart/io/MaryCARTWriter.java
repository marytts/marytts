package marytts.cart.io;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.*;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

import marytts.tools.voiceimport.MaryHeader;

public class MaryCARTWriter{

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    
    public void toTextOut(PrintWriter pw, CART cart) throws IOException {
        try {
            int leafs[]= new int[1];
            int decNodes[]= new int [1];
            leafs[0]=0;
            decNodes[0]=0;
           
            //System.out.println("Total number of nodes:" + rootNode.getNumberOfNodes());
            cart.getRootNode().addUniqueNodeId(leafs, decNodes);
            pw.println("Num decision nodes= " + decNodes[0] + "  Num leaf nodes= " + leafs[0]);
            //pw.println("\n----------------\n");
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
    
    /**
     * Dump the CARTs in the cart map in the new Format not wagon
     * to destinationDir/CARTS.bin 
     * 
     * @param destDir the destination directory
     */
    public void dumpMaryCART(String destFile, CART cart)
    throws IOException
    {
        System.out.println("Dumping CART in MaryCART format to "+destFile+".new ...");
        
        //rootNode = cart.getRootNode();
        
        //Open the destination file (cart.bin) and output the header       
        DataOutputStream out = new DataOutputStream(new
                BufferedOutputStream(new 
                FileOutputStream(destFile+".new.writer")));
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
        System.out.println(" ... done\n");
    }     
    
    
  
    private void dumpBinary(Node rootNode, DataOutput os) throws IOException {
        try {
            
            int leaves[]= new int[1];
            int decNodes[]= new int [1];
            leaves[0]=0;
            decNodes[0]=0;           
            // first add unique identificators to decision nodes and leaf nodes           
            rootNode.addUniqueNodeId(leaves, decNodes);
            
            // write the number of decision nodes and the number of leaves.
            os.writeInt(decNodes[0]);
            os.writeInt(leaves[0]);
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
            CART.writeStringToOutput(((DecisionNode) node).getDecNodeStr(), out);
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
       // I we are int he root print the leaves of the daughters.
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
          else if( node instanceof IntAndFloatArrayLeafNode ) 
              printLeafNodes(((IntAndFloatArrayLeafNode) node), out, pw);
          else if( node instanceof IntArrayLeafNode ) 
              printLeafNodes(((IntArrayLeafNode) node), out, pw);
          else if( node instanceof StringAndFloatLeafNode ) 
              printLeafNodes(((StringAndFloatLeafNode) node), out, pw);
      }
   }
     
   
   private void printLeafNodes(FeatureVectorLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       StringBuffer sb = new StringBuffer();
       
       if( node.getUniqueLeafId() != 0) {
        FeatureVector fv[] = node.getFeatureVectors(); 
       //sb.append("idFV" + getUniqueLeafId() + " ");
        sb.append("id" + node.getUniqueLeafId() + " FeatureVectorLeafNode " + fv.length + " ");
      
       //make sure that we have a feature vector array, this is done when calling getFeatureVectors().     
       // for each index, write the index and then a pseudo float
       for (int i = 0; i < fv.length; i++) {
           sb.append(  fv[i].getUnitIndex() + " ");               
       }
       
       // dump the whole stuff
       if (out != null) {
           // write to output stream
           CART.writeStringToOutput(sb.toString(), out);
           
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
   
   private void printLeafNodes(FloatLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       // How to test this??? remove the ** after testing!!!
       String s = "id" + node.getUniqueLeafId() + " FloatLeafNode 1 "
           + node.getStDeviation() // stddev
           + " "
           + node.getMean(); // mean
           
       // dump the whole stuff
       if (out != null) {
           // write to output stream
           CART.writeStringToOutput(s, out);
           
       } else {
           // write to Standard out
           // System.out.println(sb.toString());
       }
       if (pw != null) {
           // dump to printwriter
           pw.println(s);
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
           CART.writeStringToOutput(sb.toString(), out);
           
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
           CART.writeStringToOutput(sb.toString(), out);
           
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

   
   private void printLeafNodes(StringAndFloatLeafNode node, DataOutputStream out, PrintWriter pw) throws IOException {
       
       StringBuffer sb = new StringBuffer();
       int data[] = node.getIntData();
       float floats[] = node.getFloatData();
       FeatureDefinition fd = node.getFeatureDefinition();
       int tf = node.getTargetfeature();
       
       if( node.getUniqueLeafId() != 0) {
       sb.append("id" + node.getUniqueLeafId() + " StringAndFloatLeafNode " + data.length + " ");
     
       // for each index, write the index and then its float
       for (int i = 0; i < data.length; i++) {
           sb.append(fd.getFeatureValueAsString(tf, data[i]) + " " + floats[i] + " ");
       }
      
       // dump the whole stuff
       if (out != null) {
           // write to output stream
           CART.writeStringToOutput(sb.toString(), out);
           
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
}
