package marytts.htsengine;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.Node;
import marytts.cart.LeafNode;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.cart.io.HTSCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

public class CartTreeSet {
    
    private Logger logger = Logger.getLogger("CartTreeSet");
    
    private CART[] durTree;   // CART trees for duration 
    private CART[] lf0Tree;   // CART trees for log F0 
    private CART[] mcpTree;   // CART trees for spectrum 
    private CART[] strTree;   // CART trees for strengths 
    private CART[] magTree;   // CART trees for Fourier magnitudes
    
    private int numStates;            /* # of HMM states for individual HMM */
    private int lf0Stream;            /* # of stream for log f0 modeling */
    private int mcepVsize;            /* vector size for mcep modeling */
    private int strVsize;             /* vector size for strengths modeling */
    private int magVsize;             /* vector size for Fourier magnitudes modeling */
   
    HTSCARTReader htsReader = new HTSCARTReader(); 
    
    public int getNumStates(){ return numStates; }
    public int getLf0Stream(){ return lf0Stream; }
    public int getMcepVsize(){ return mcepVsize; }
    public int getStrVsize(){ return strVsize; }
    public int getMagVsize(){ return magVsize; }
    
    
    /** Loads all the CART trees */
    public void loadTreeSet(HMMData htsData, FeatureDefinition featureDef) throws Exception {
      try {
             
        /* DUR, LF0 and MCP are required as minimum for generating voice. 
        * The duration tree has only one state.
        * The size of the vector in duration is the number of states. */  
        durTree = htsReader.load(1, htsData.getTreeDurFile(), htsData.getPdfDurFile(), featureDef);  
        numStates = htsReader.getVectorSize();
        
        lf0Tree = htsReader.load(numStates, htsData.getTreeLf0File(), htsData.getPdfLf0File(), featureDef);
        lf0Stream = htsReader.getVectorSize();
        
        mcpTree = htsReader.load(numStates, htsData.getTreeMcpFile(), htsData.getPdfMcpFile(), featureDef);
        mcepVsize = htsReader.getVectorSize();
        
        /* STR and MAG are optional for generating mixed excitation */ 
        if( htsData.getTreeStrFile() != null){
           strTree = htsReader.load(numStates, htsData.getTreeStrFile(), htsData.getPdfStrFile(), featureDef);
           strVsize = htsReader.getVectorSize();
        }
        if( htsData.getTreeMagFile() != null){
          magTree = htsReader.load(numStates, htsData.getTreeMagFile(), htsData.getPdfMagFile(), featureDef);
          magVsize = htsReader.getVectorSize();
        }
        
      } catch (Exception e) {
        logger.debug("Exception: " + e.getMessage());
        throw new Exception("LoadTreeSet: " + e.getMessage());
      
      }
        
    }
  
    /***
     * Searches fv in durTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @return duration
     * @throws Exception
     */
    public double searchDurInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef,
            boolean firstPh, boolean lastPh, double rho, double diffdur, double durscale) 
      throws Exception {     
      int s, i;
      double data, mean, variance, dd;
      double meanVector[], varVector[];
      Node node;
     
      // the duration tree has only one state
      node = durTree[0].interpretToNode(fv, 1);
      
      if ( node instanceof PdfLeafNode ) {       
        meanVector = ((PdfLeafNode)node).getMean();
        varVector = ((PdfLeafNode)node).getVariance();
      } else 
         throw new Exception("searchDurInCartTree: The node must be a PdfLeafNode");
     
      
      dd = diffdur;
      // in duration the length of the vector is the number of states.
      for(s=0; s<numStates; s++){
        mean = meanVector[s];
        variance = varVector[s];
        data = mean + rho * variance;
      
        /* check if the model is initial/final pause, if so reduce the length of the pause 
         * to 10% of the calculated value. */       
 //       if(m.getPhoneName().contentEquals("_") && (firstPh || lastPh ))
 //         data = data * 0.1;
        
        data = data * durscale; 
                  
        m.setDur(s, (int)(data+dd+0.5));
        if(m.getDur(s) < 1 )
          m.setDur(s, 1);
        
       // System.out.println("   state: " + s + " dur=" + m.getDur(s));
               
        m.setTotalDur(m.getTotalDur() + m.getDur(s));
        
        dd = dd + ( data - (double)m.getDur(s) );       
      }
      return dd; 
      
    }
    
    
    /***
     * Searches fv in Lf0Tree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchLf0InCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef, double uvthresh) 
      throws Exception {     
      int s, stream;
      double mean[], var[];
      Node node;
      for(s=0; s<numStates; s++) {
          
        node = lf0Tree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          mean = ((PdfLeafNode)node).getMean();
          var = ((PdfLeafNode)node).getVariance();
        } else 
            throw new Exception("searchLf0InCartTree: The node must be a PdfLeafNode");
        
        for(stream=0; stream<mean.length; stream++) {
          m.setLf0Mean(s, stream, mean[stream]);
          m.setLf0Variance(s, stream, var[stream]);
          if(stream == 0) {
              if(((PdfLeafNode)node).getVoicedWeight() > uvthresh)
                 m.setVoiced(s, true);
              else
                 m.setVoiced(s,false);
          }
       }         
      }
    }
      
    
    /***
     * Searches fv in mcpTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchMcpInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef) 
      throws Exception {     
      int s, i;
      double mean[], var[];
      Node node;
      for(s=0; s<numStates; s++) {
        node = mcpTree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          mean = ((PdfLeafNode)node).getMean();
          var = ((PdfLeafNode)node).getVariance();
        } else
            throw new Exception("searchMcpInCartTree: The node must be a PdfLeafNode");
        
        // make a copy of mean and variance in m
        for(i=0; i<mean.length; i++){
          m.setMcepMean(s, i, mean[i]);
          m.setMcepVariance(s, i, var[i]);
        }     
      }
    }
    
    /***
     * Searches fv in StrTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchStrInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef) 
      throws Exception {     
      int s, i;
      double mean[], var[];
      Node node;
      for(s=0; s<numStates; s++) {
        node = strTree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          mean = ((PdfLeafNode)node).getMean();
          var = ((PdfLeafNode)node).getVariance();
        } else
            throw new Exception("searchStrInCartTree: The node must be a PdfLeafNode");
        
        // make a copy of mean and variance in m
        for(i=0; i<mean.length; i++){
          m.setStrMean(s, i, mean[i]);
          m.setStrVariance(s, i, var[i]);
        }     
      }
    }
    
    /***
     * Searches fv in MagTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchMagInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef) 
      throws Exception {     
      int s, i;
      double mean[], var[];
      Node node;
      for(s=0; s<numStates; s++) {
        node = magTree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          mean = ((PdfLeafNode)node).getMean();
          var = ((PdfLeafNode)node).getVariance();
        } else
            throw new Exception("searchMagInCartTree: The node must be a PdfLeafNode");
        
        // make a copy of mean and variance in m
        for(i=0; i<mean.length; i++){
          m.setMagMean(s, i, mean[i]);
          m.setMagVariance(s, i, var[i]);
        }     
      }
    }

}
