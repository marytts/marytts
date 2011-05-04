/**   
*           The HMM-Based Speech Synthesis System (HTS)             
*                       HTS Working Group                           
*                                                                   
*                  Department of Computer Science                   
*                  Nagoya Institute of Technology                   
*                               and                                 
*   Interdisciplinary Graduate School of Science and Engineering    
*                  Tokyo Institute of Technology                    
*                                                                   
*                Portions Copyright (c) 2001-2006                       
*                       All Rights Reserved.
*                         
*              Portions Copyright 2000-2007 DFKI GmbH.
*                      All Rights Reserved.                  
*                                                                   
*  Permission is hereby granted, free of charge, to use and         
*  distribute this software and its documentation without           
*  restriction, including without limitation the rights to use,     
*  copy, modify, merge, publish, distribute, sublicense, and/or     
*  sell copies of this work, and to permit persons to whom this     
*  work is furnished to do so, subject to the following conditions: 
*                                                                   
*    1. The source code must retain the above copyright notice,     
*       this list of conditions and the following disclaimer.       
*                                                                   
*    2. Any modifications to the source code must be clearly        
*       marked as such.                                             
*                                                                   
*    3. Redistributions in binary form must reproduce the above     
*       copyright notice, this list of conditions and the           
*       following disclaimer in the documentation and/or other      
*       materials provided with the distribution.  Otherwise, one   
*       must contact the HTS working group.                         
*                                                                   
*  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
*  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
*  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
*  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
*  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
*  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
*  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
*  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
*  PERFORMANCE OF THIS SOFTWARE.                                    
*                                                                   
*/

package marytts.htsengine;

import marytts.cart.CART;
import marytts.cart.Node;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.cart.io.HTSCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * Set of CART trees used in HMM synthesis.
 * 
 * @author Marcela Charfuelan
 */
public class CartTreeSet {
    
    private Logger logger = MaryUtils.getLogger("CartTreeSet");
    
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
    public void setNumStates(int val){ numStates = val; }
    public int getLf0Stream(){ return lf0Stream; }
    public int getMcepVsize(){ return mcepVsize; }
    public int getStrVsize(){ return strVsize; }
    public int getMagVsize(){ return magVsize; }
    
    
    /** Loads all the CART trees */
    public void loadTreeSet(HMMData htsData, FeatureDefinition featureDef, String trickyPhones) throws Exception {
      try {
        
        // Check if there are tricky phones, and create a PhoneTranslator object
        PhoneTranslator phTranslator = new PhoneTranslator(trickyPhones);
             
        /* DUR, LF0 and MCP are required as minimum for generating voice. 
        * The duration tree has only one state.
        * The size of the vector in duration is the number of states. */
        if(htsData.getTreeDurFile() != null){
          durTree = htsReader.load(1, htsData.getTreeDurFile(), htsData.getPdfDurFile(), featureDef, phTranslator);  
          numStates = htsReader.getVectorSize();
        }
        
        if(htsData.getTreeLf0File() != null){
          lf0Tree = htsReader.load(numStates, htsData.getTreeLf0File(), htsData.getPdfLf0File(), featureDef, phTranslator);
          lf0Stream = htsReader.getVectorSize();
        }
        
        if( htsData.getTreeMcpFile() != null){
          mcpTree = htsReader.load(numStates, htsData.getTreeMcpFile(), htsData.getPdfMcpFile(), featureDef, phTranslator);
          mcepVsize = htsReader.getVectorSize();
        }
        
        /* STR and MAG are optional for generating mixed excitation */ 
        if( htsData.getTreeStrFile() != null){
           strTree = htsReader.load(numStates, htsData.getTreeStrFile(), htsData.getPdfStrFile(), featureDef, phTranslator);
           strVsize = htsReader.getVectorSize();
        }
        if( htsData.getTreeMagFile() != null){
          magTree = htsReader.load(numStates, htsData.getTreeMagFile(), htsData.getPdfMagFile(), featureDef, phTranslator);
          magVsize = htsReader.getVectorSize();
        }
        
      } catch (Exception e) {
        throw new Exception("LoadTreeSet failed: ", e);
      
      }
        
    }
    
    /** Loads duration CART */
    public void loadDurationTree(String treeDurFile, String pdfDurFile, FeatureDefinition featureDef, String trickyPhones) throws Exception {
      try {
        
        // Check if there are tricky phones, and create a PhoneTranslator object
        PhoneTranslator phTranslator = new PhoneTranslator(trickyPhones);
             
        /* The duration tree has only one state.
        * The size of the vector in duration is the number of states. */  
        durTree = htsReader.load(1, treeDurFile, pdfDurFile, featureDef, phTranslator);  
        numStates = htsReader.getVectorSize();
                
      } catch (Exception e) {
        throw new Exception("LoadTreeSet failed: ", e);
      
      }
        
    }
  
    /***
     * Searches fv in durTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param htsData HMMData with configuration settings
     * @return duration
     * @throws Exception
     */
    public double searchDurInCartTree(HTSModel m, FeatureVector fv, HMMData htsData, double diffdur) throws Exception {
        return searchDurInCartTree(m, fv, htsData, false, false, diffdur);
    }
    public double searchDurInCartTree(HTSModel m, FeatureVector fv, HMMData htsData,
            boolean firstPh, boolean lastPh, double diffdur) 
      throws Exception {     
      int s, i;
      double data, dd;
      double rho = htsData.getRho();
      double durscale = htsData.getDurationScale();
      double meanVector[], varVector[];
      Node node;
     
      
      // the duration tree has only one state
      node = durTree[0].interpretToNode(fv, 1);
      
      if ( node instanceof PdfLeafNode ) { 
        //System.out.println("  PDF INDEX = " + ((PdfLeafNode)node).getUniqueLeafId() );  
        meanVector = ((PdfLeafNode)node).getMean();
        varVector = ((PdfLeafNode)node).getVariance();
      } else 
         throw new Exception("searchDurInCartTree: The node must be a PdfLeafNode");
          
      dd = diffdur;
      // in duration the length of the vector is the number of states.
      for(s=0; s<numStates; s++){
        data = meanVector[s] + rho * varVector[s];
      
        /* check if the model is initial/final pause, if so reduce the length of the pause 
         * to 10% of the calculated value. */       
//        if(m.getPhoneName().contentEquals("_") && (firstPh || lastPh ))
//          data = data * 0.1;
        
        data = data * durscale;                  
        m.setDur(s, (int)(data+dd+0.5));
        if(m.getDur(s) < 1 )
          m.setDur(s, 1);
        
        //System.out.println("   state: " + s + " dur=" + m.getDur(s) + "  dd=" + dd);               
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
      int s;
      Node node;
      for(s=0; s<numStates; s++) {          
        node = lf0Tree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          m.setLf0Mean(s, ((PdfLeafNode)node).getMean());
          m.setLf0Variance(s, ((PdfLeafNode)node).getVariance());
        } else 
            throw new Exception("searchLf0InCartTree: The node must be a PdfLeafNode");       
        // set voiced or unvoiced
        if(((PdfLeafNode)node).getVoicedWeight() > uvthresh)
            m.setVoiced(s, true);
        else
            m.setVoiced(s,false);       
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
      int s;
      Node node;
      for(s=0; s<numStates; s++) {         
        node = mcpTree[s].interpretToNode(fv, 1);       
        if ( node instanceof PdfLeafNode ) {       
          m.setMcepMean(s,((PdfLeafNode)node).getMean());
          m.setMcepVariance(s, ((PdfLeafNode)node).getVariance());
        } else
            throw new Exception("searchMcpInCartTree: The node must be a PdfLeafNode");
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
      int s;
      Node node;
      for(s=0; s<numStates; s++) {      
        node = strTree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          m.setStrMean(s, ((PdfLeafNode)node).getMean());
          m.setStrVariance(s, ((PdfLeafNode)node).getVariance());
        } else
            throw new Exception("searchStrInCartTree: The node must be a PdfLeafNode");    
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
      int s;
      Node node;
      for(s=0; s<numStates; s++) {        
        node = magTree[s].interpretToNode(fv, 1);
        if ( node instanceof PdfLeafNode ) {       
          m.setMagMean(s, ((PdfLeafNode)node).getMean());
          m.setMagVariance(s, ((PdfLeafNode)node).getVariance());
        } else
            throw new Exception("searchMagInCartTree: The node must be a PdfLeafNode");   
      }
    }

}
