package marytts.htsengine;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.cart.io.HTSCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

public class CartTreeSet {
    
    private Logger logger = Logger.getLogger("CartTreeSet");
    private int numStates;
    private CART[] durTree; // CART trees for duration <p>
    private CART[] lf0Tree; // CART trees for log F0 <p>
    private CART[] mcpTree; // CART trees for spectrum <p>
    private CART[] strTree; // CART trees for strengths <p>
    private CART[] magTree; // CART trees for Fourier magnitudes<p>
    
    
    HTSCARTReader htsReader = new HTSCARTReader(); 
    
    /** Loads all the CART trees */
    public void loadTreeSet(HMMData htsData, FeatureDefinition featureDef) throws Exception {
      try {
        
        /* The number of states is normally 5 but to be sure it can be read from the first
         * int value of the pdf duration file. */
        DataInputStream data_in = new DataInputStream (
                                  new BufferedInputStream(
                                  new FileInputStream(htsData.getPdfDurFile())));
        numStates = data_in.readInt();
        data_in.close (); 
          
        /* DUR, LF0 and MCP are required as minimum for generating voice */
        /* the duration tree has only one state */
        durTree = htsReader.load(1, htsData.getTreeDurFile(), htsData.getPdfDurFile(), featureDef);
        lf0Tree = htsReader.load(numStates, htsData.getPdfLf0File(), htsData.getPdfLf0File(), featureDef);
        mcpTree = htsReader.load(numStates, htsData.getTreeMcpFile(), htsData.getPdfMcpFile(), featureDef);
        
        /* STR and MAG are optional for generating mixed excitation */ 
        if( htsData.getTreeStrFile() != null)
          strTree = htsReader.load(numStates, htsData.getTreeStrFile(), htsData.getPdfStrFile(), featureDef);
        if( htsData.getTreeMagFile() != null)
          magTree = htsReader.load(numStates, htsData.getTreeMagFile(), htsData.getPdfMagFile(), featureDef);
        
      } catch (Exception e) {
        logger.debug("Exception: " + e.getMessage());
        throw new Exception("LoadTreeSet: " + e.getMessage());
      
      }
        
    }
    
    public int getNumStates(){ return numStates; }
    
    
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
     
      // With a fv then i can search in a cart tree, and the function returns a node 
      //CHECK!!!
      // the duration tree has only one state
      node = durTree[0].interpretToNode(fv, 1);
      assert ( node instanceof PdfLeafNode );
        
      meanVector = ((PdfLeafNode)node).getMean();
      varVector = ((PdfLeafNode)node).getVariance();
     
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
        // with a fv then i can search in a cart tree, and the function returns a node 
        //CHECK!!!
        node = lf0Tree[s].interpretToNode(fv, 1);
        assert ( node instanceof PdfLeafNode );
        
        mean = ((PdfLeafNode)node).getMean();
        var = ((PdfLeafNode)node).getVariance();
        
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
        // with a fv then i can search in a cart tree, and the function returns a node 
        //CHECK!!!
        node = mcpTree[s].interpretToNode(fv, 1);
        assert ( node instanceof PdfLeafNode );
        
        mean = ((PdfLeafNode)node).getMean();
        var = ((PdfLeafNode)node).getVariance();
        
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
        // with a fv then i can search in a cart tree, and the function returns a node 
        //CHECK!!!
        node = strTree[s].interpretToNode(fv, 1);
        assert ( node instanceof PdfLeafNode );
        
        mean = ((PdfLeafNode)node).getMean();
        var = ((PdfLeafNode)node).getVariance();
        
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
        // with a fv then i can search in a cart tree, and the function returns a node 
        //CHECK!!!
        node = magTree[s].interpretToNode(fv, 1);
        assert ( node instanceof PdfLeafNode );
        
        mean = ((PdfLeafNode)node).getMean();
        var = ((PdfLeafNode)node).getVariance();
        
        // make a copy of mean and variance in m
        for(i=0; i<mean.length; i++){
          m.setMagMean(s, i, mean[i]);
          m.setMagVariance(s, i, var[i]);
        }     
      }
    }

}
