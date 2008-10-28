package marytts.cart.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.DecisionNode.BinaryByteDecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.cart.Node;
import marytts.features.FeatureDefinition;
import marytts.htsengine.HTSNode;
import marytts.htsengine.HTSQuestion;
import marytts.htsengine.HTSTree;
import marytts.htsengine.PhoneTranslator;
import marytts.tools.voiceimport.MaryHeader;


/**
 * Reader functions for CARTs in HTS format
 * 
 * @author Marcela Charfuelan
 */
public class HTSCARTReader
{
    //private Node rootNode;  //there will be one root node for each CART[state]

    // knows the index numbers and types of the features used in DecisionNodes
    private FeatureDefinition featDef;
   
    //private Node lastNode;
    
    private Logger logger = Logger.getLogger("HTSCARTReader");
    
    /**
     * Load the cart from the given file
     * 
     * @param fileName
     *            the file to load the cart from
     * @param featDefinition
     *            the feature definition
     * @return CART[]
     *            returns an array of CART trees, one per state.
     * @throws IOException
     *             if a problem occurs while loading
     */
    public CART[] load(int numStates, String fileName, FeatureDefinition featDefinition)
            throws Exception {
        //System.out.println("Loading file");
        
        featDef = featDefinition;
      
        int i, j, length, state;
        DataInput raf = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
        
        BufferedReader s = null;
        String line, aux;
        
        // create the number of carts it is going to read
        CART treeSet[] = new CART[numStates];
        for(i=0; i<numStates; i++)
           treeSet[i] = new CART();
        
        assert featDefinition != null : "Feature Definition was not set";
            
        try {   
          /* read lines of tree-*.inf fileName */ 
          s = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            
          //System.out.println("LoadTreeSet reading: " + fileName + " tree type: " + type);
          logger.info("LoadTreeSet reading: " + fileName);
          
          // skip questions section
          while((line = s.readLine()) != null) {
              if (line.indexOf("QS") < 0 ) break;   /* a new state is indicated by {*}[2], {*}[3], ... */
          }
          
          while((line = s.readLine()) != null) {
             
            if(line.indexOf("{*}") >= 0 ){  /* this is the indicator of a new state-tree */
              aux = line.substring(line.indexOf("[")+1, line.indexOf("]")); 
              state = Integer.parseInt(aux);
              //System.out.println("Loading tree type=" + type + " TREE STATE: " +  t.get_state() );
                            
              ////////////////////
              // the state numbers start in 2 until 7
              //Node rootNode = loadStateTree(s, false);
              //treeSet[state-2].setRootNode(rootNode);   /* load one tree per state */
              System.out.println("Loading state:" + state);
              treeSet[state-2].setRootNode(loadStateTree(s, false));
              
            }
           
          } /* while */  
          if (s != null)
            s.close();
          
          /* check that the tree was correctly loaded */
          if( treeSet.length == 0 ) {
            logger.debug("LoadTreeSet: error no trees loaded from " + fileName);  
            throw new Exception("LoadTreeSet: error no trees loaded from  " + fileName);   
          }
          
          
        } catch (FileNotFoundException e) {
            logger.debug("FileNotFoundException: " + e.getMessage());
            throw new FileNotFoundException("LoadTreeSet: " + e.getMessage());
        }
          
            
        return treeSet;
       
    }

/** Load a tree per state
 * @param s: text scanner of the whole tree-*.inf file
 * @param t: tree to be filled
 * @param type: corresponds to one of DUR, logF0, MCP, STR and MAG
 * @param debug: when true print out detailled information
 */
private Node loadStateTree(BufferedReader s, boolean debug) throws Exception {
    
  Node rootNode=null;
  Node lastNode=null;
  
  StringTokenizer sline;
  String aux,buf;
  
  // create an empy binary decision node with unique id=0
  Node nextNode = new DecisionNode.BinaryByteDecisionNode(0, featDef);
  
  // this is the rootNode
  rootNode = nextNode;
  nextNode.setIsRoot(true);
 
  int iaux, feaIndex;
  
  Node node = null;
  aux = s.readLine();   /* next line for this state tree must be { */
  int id;
  if(aux.indexOf("{") >= 0 ) {
    while ( (aux = s.readLine()) != null && aux.indexOf("}") < 0 ){  /* last line for this state tree must be } */
        /* then parse this line, it contains 4 fields */
        /* 1: node index #  2: Question name 3: NO # node 4: YES # node */
        sline = new StringTokenizer(aux);
      
        /* 1:  gets index node and looks for the node whose idx = buf */
        buf = sline.nextToken();      
        if(buf.startsWith("-"))
          id = Integer.parseInt(buf.substring(1)); 
        else
          id = Integer.parseInt(buf);  
        
        //System.out.println("searching node id=" + id);
        //if(id==239)
        //    System.out.println("searching node id=" + id);   
        node = findDecisionNode(rootNode,id);
        //System.out.println();
      
        if(node == null)
            throw new Exception("LoadTree: Node not found, index = " +  buf); 
        else {    
          /* 2: gets question name and question name val */
          buf = sline.nextToken();
          String [] fea_val = buf.split("=");   /* splits featureName=featureValue*/
          feaIndex = featDef.getFeatureIndex(fea_val[0]);
         
          /* Replace back punctuation values */
          /* what about tricky phones, if using halfphones it would not be necessary */
          if(fea_val[0].contains("sentence_punc") || fea_val[0].contains("prev_punctuation") || fea_val[0].contains("next_punctuation"))
              fea_val[1] = PhoneTranslator.replaceBackPunc(fea_val[1]);
          else if(fea_val[0].contains("phoneme") )
              fea_val[1] = PhoneTranslator.replaceBackTrickyPhones(fea_val[1]);
          
          // add featureName and featureValue to the decision nod
          ((BinaryByteDecisionNode) node).setFeatureAndFeatureValue(fea_val[0], fea_val[1]);
         
          // add NO and YES indexes to the daughther nodes 
          /* NO index */
          buf = sline.nextToken();
          //System.out.print("  NO:" + buf + "   ");
          
          if(buf.startsWith("-")) {  // Decision node
            iaux = Integer.parseInt(buf.substring(1));
            // create an empty binary decision node with unique id
            BinaryByteDecisionNode auxnode = new DecisionNode.BinaryByteDecisionNode(iaux, featDef);
            auxnode.setMother(node);
            ((DecisionNode) node).replaceDaughter(auxnode, 0);
          } else {                  // LeafNode
            iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_")+1, buf.length()-1));
            // create an empty PdfLeafNode, just with id
            PdfLeafNode auxnode = new LeafNode.PdfLeafNode(iaux);
            auxnode.setMother(node);
            ((DecisionNode) node).replaceDaughter(auxnode, 0);
          }
          /* YES index */
          buf = sline.nextToken();
          //System.out.print("  YES: " + buf + "   ");
          if(buf.startsWith("-")) {
            iaux = Integer.parseInt(buf.substring(1));
            // create an empty binary decision node with unique id=0
            BinaryByteDecisionNode auxnode = new DecisionNode.BinaryByteDecisionNode(iaux, featDef);
            auxnode.setMother(node);
            ((DecisionNode) node).replaceDaughter(auxnode, 1);
          } else {  /*convert name of node to node index number */
            iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_")+1, buf.length()-1));
            // create an empty PdfLeafNode, just with id
            PdfLeafNode auxnode = new LeafNode.PdfLeafNode(iaux);
            auxnode.setMother(node);
            ((DecisionNode) node).replaceDaughter(auxnode, 1);
          }
        }  /* if node not null */
        sline=null;
    } /* while there is another line and the line does not contain }*/
  }  /* if not "{" */
 
  return rootNode;
  
} /* method loadTree() */

 
private Node findDecisionNode(Node node, int numId){
    
    Node aux = null;  
      
      if (node instanceof DecisionNode){
        //System.out.print(" id=" + ((DecisionNode)node).getUniqueDecisionNodeId());  
        if( ((DecisionNode)node).getUniqueDecisionNodeId() == numId ) 
          return node;
        else {
          for(int i=0; i< ((DecisionNode)node).getNumberOfDaugthers(); i++){
            aux = findDecisionNode( ((DecisionNode)node).getDaughter(i), numId );
            if(aux != null)
              return aux;
          }
        }
      }
      return aux;
       
  } /* method findDecisionNode */

private Node findNode(Node node, int numId){
  
  Node aux = null;  
    
    if (node instanceof DecisionNode){
      //System.out.print(" id=" + ((DecisionNode)node).getUniqueDecisionNodeId());  
      if( ((DecisionNode)node).getUniqueDecisionNodeId() == numId ) 
        return node;
      else {
        for(int i=0; i< ((DecisionNode)node).getNumberOfDaugthers(); i++){
          aux = findNode( ((DecisionNode)node).getDaughter(i), numId );
          if(aux != null)
            return aux;
        }
      }
    } else if( node instanceof LeafNode){
       //System.out.println(" id=" + ((LeafNode)node).getUniqueLeafId()); 
       if( ((LeafNode)node).getUniqueLeafId() == numId )
         return node;
    }
 
    return aux;
     
} /* method findNode */

    
public static void main(String[] args) throws IOException, InterruptedException{
    /* configure log info */
    org.apache.log4j.BasicConfigurator.configure();
    
    String contextFile = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/cmu_us_arctic_slt_a0001.pfeats";
    Scanner context = new Scanner(new BufferedReader(new FileReader(contextFile)));
    String strContext="";
    while (context.hasNext()) {
      strContext += context.nextLine(); 
      strContext += "\n";
    }
    context.close();
    //    System.out.println(strContext);
    FeatureDefinition feaDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);
      
    CART[] mgcTree;
    int numStates = 5;
    String fileName = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/tree-lf0.inf";
    
    HTSCARTReader htsReader = new HTSCARTReader(); 
    try {
      mgcTree = htsReader.load(numStates, fileName, feaDef);
    } catch (Exception e) {
        System.out.println(e.getMessage()); 
    }

 }




    
}
