/* ----------------------------------------------------------------- */
/*           The HMM-Based Speech Synthesis Engine "hts_engine API"  */
/*           developed by HTS Working Group                          */
/*           http://hts-engine.sourceforge.net/                      */
/* ----------------------------------------------------------------- */
/*                                                                   */
/*  Copyright (c) 2001-2010  Nagoya Institute of Technology          */
/*                           Department of Computer Science          */
/*                                                                   */
/*                2001-2008  Tokyo Institute of Technology           */
/*                           Interdisciplinary Graduate School of    */
/*                           Science and Engineering                 */
/*                                                                   */
/* All rights reserved.                                              */
/*                                                                   */
/* Redistribution and use in source and binary forms, with or        */
/* without modification, are permitted provided that the following   */
/* conditions are met:                                               */
/*                                                                   */
/* - Redistributions of source code must retain the above copyright  */
/*   notice, this list of conditions and the following disclaimer.   */
/* - Redistributions in binary form must reproduce the above         */
/*   copyright notice, this list of conditions and the following     */
/*   disclaimer in the documentation and/or other materials provided */
/*   with the distribution.                                          */
/* - Neither the name of the HTS working group nor the names of its  */
/*   contributors may be used to endorse or promote products derived */
/*   from this software without specific prior written permission.   */
/*                                                                   */
/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            */
/* CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       */
/* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          */
/* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS */
/* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          */
/* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   */
/* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     */
/* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON */
/* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   */
/* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    */
/* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           */
/* POSSIBILITY OF SUCH DAMAGE.                                       */
/* ----------------------------------------------------------------- */
/**
 * Copyright 2011 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package marytts.htsengine;

import java.io.IOException;

import marytts.cart.CART;
import marytts.cart.Node;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.cart.io.HTSCARTReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.htsengine.HMMData.PdfFileFormat;
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
    private CART[] mgcTree;   // CART trees for spectrum 
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
    
    public int getVsize(HMMData.FeatureType type) {
        switch (type) {
        case MGC: return mcepVsize;
        case STR: return strVsize;
        case MAG: return magVsize;
        default: return 1; // DUR and LF0
        }
    }
    
    /** Loads all the CART trees */
    public void loadTreeSet(HMMData htsData, FeatureDefinition featureDef, PhoneTranslator trickyPhones) 
    throws IOException, MaryConfigurationException {
        // Check if there are tricky phones, and create a PhoneTranslator object
        PhoneTranslator phTranslator = trickyPhones;
             
        /* DUR, LF0 and Mgc are required as minimum for generating voice. 
        * The duration tree has only one state.
        * The size of the vector in duration is the number of states. */
        if(htsData.getTreeDurStream() != null) {
        	logger.debug("Loading duration tree...");
        	durTree = htsReader.load(1, htsData.getTreeDurStream(), htsData.getPdfDurStream(), PdfFileFormat.dur, featureDef, phTranslator);  
        	numStates = htsReader.getVectorSize();
        }
        
        if(htsData.getTreeLf0Stream() != null){
        	logger.debug("Loading log F0 tree...");
        	lf0Tree = htsReader.load(numStates, htsData.getTreeLf0Stream(), htsData.getPdfLf0Stream(), PdfFileFormat.lf0, featureDef, phTranslator);
        	lf0Stream = htsReader.getVectorSize();
        }
        
        if( htsData.getTreeMgcStream() != null){
        	logger.debug("Loading mgc tree...");
        	mgcTree = htsReader.load(numStates, htsData.getTreeMgcStream(), htsData.getPdfMgcStream(), PdfFileFormat.mgc, featureDef, phTranslator);
        	mcepVsize = htsReader.getVectorSize();
        }
        
        /* STR and MAG are optional for generating mixed excitation */ 
        if( htsData.getTreeStrStream() != null){
        	logger.debug("Loading str tree...");
        	strTree = htsReader.load(numStates, htsData.getTreeStrStream(), htsData.getPdfStrStream(), PdfFileFormat.str, featureDef, phTranslator);
        	strVsize = htsReader.getVectorSize();
        }
        if( htsData.getTreeMagStream() != null){
        	logger.debug("Loading mag tree...");
        	magTree = htsReader.load(numStates, htsData.getTreeMagStream(), htsData.getPdfMagStream(), PdfFileFormat.mag, featureDef, phTranslator);
        	magVsize = htsReader.getVectorSize();
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
        data = (meanVector[s] + rho * varVector[s]) * durscale;
      
        /* check if the model is initial/final pause, if so reduce the length of the pause 
         * to 10% of the calculated value. */       
//        if(m.getPhoneName().contentEquals("_") && (firstPh || lastPh ))
//          data = data * 0.1;
        
        m.setDur(s, (int)(data+dd+0.5));
        if(m.getDur(s) < 1 )
          m.setDur(s, 1);
        
        //System.out.format("   state=%d  dur=%d  dd=%f  mean=%f  vari=%f \n", s, m.getDur(s), dd, meanVector[s], varVector[s]);               
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
          //System.out.format("  state=%d  node_index=%d \n", s, ((PdfLeafNode)node).getUniqueLeafId());
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
      //m.printLf0Mean();
    }
      
    
    /***
     * Searches fv in mgcTree CART[] set of trees, per state, and fill the information in the
     * HTSModel m.
     * @param m HTSModel where mean and variances per state are copied          
     * @param fv context feature vector
     * @param featureDef Feature definition
     * @throws Exception
     */
    public void searchMgcInCartTree(HTSModel m, FeatureVector fv, FeatureDefinition featureDef) 
      throws Exception {     
      int s;
      Node node;
      for(s=0; s<numStates; s++) {         
        node = mgcTree[s].interpretToNode(fv, 1);       
        if ( node instanceof PdfLeafNode ) {       
          m.setMcepMean(s,((PdfLeafNode)node).getMean());         
          m.setMcepVariance(s, ((PdfLeafNode)node).getVariance());
        } else
            throw new Exception("searchMgcInCartTree: The node must be a PdfLeafNode");
      }
      //m.printMcepMean();
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
