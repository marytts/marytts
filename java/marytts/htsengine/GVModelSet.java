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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.DecisionNode.BinaryByteDecisionNode;
import marytts.features.FeatureDefinition;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * Set of Global Mean and (diagonal) Variance for log f0, mel-cepstrum, bandpass 
 * voicing strengths and Fourier magnitudes (the signal processing part of 
 * Fourier magnitudes is not implemented yet).
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class GVModelSet {
  
  /* ____________________ GV related variables ____________________*/
  /* GV: Global mean and covariance (diagonal covariance only) */

  private double gvmeanMcp[];   /* global mean vector */
  private double gvcovInvMcp[]; /* global inverse diagonal covariance */
  
  private double gvmeanLf0[];   /* global mean vector */
  private double gvcovInvLf0[]; /* global inverse diagonal covariance */
  
  private double gvmeanStr[];   /* global mean vector */
  private double gvcovInvStr[]; /* global inverse diagonal covariance */
  
  private double gvmeanMag[];   /* global mean vector */
  private double gvcovInvMag[]; /* global inverse diagonal covariance */
  
  private Logger logger = MaryUtils.getLogger("GVModelSet");
  
  public double[] getGVmeanMcp(){ return gvmeanMcp; }
  public double[] getGVcovInvMcp(){ return gvcovInvMcp; }
  
  public double[] getGVmeanLf0(){ return gvmeanLf0; }
  public double[] getGVcovInvLf0(){ return gvcovInvLf0; }
  
  public double[] getGVmeanStr(){ return gvmeanStr; }
  public double[] getGVcovInvStr(){ return gvcovInvStr; }
  
  public double[] getGVmeanMag(){ return gvmeanMag; }
  public double[] getGVcovInvMag(){ return gvcovInvMag; }
  
  
  public void loadGVModelSet(HMMData htsData, FeatureDefinition featureDef, String trickyPhones) throws Exception {
    
    /* allocate memory for the arrays and load the data from file */
    int numMSDFlag, numStream, vectorSize, numDurPdf;
    double gvcov;
    DataInputStream data_in;
    String gvFile;
        
    /* Here global variance vectors are loaded from corresponding files */
    int m, i,nmix;
    try { 
     if(htsData.getUseGV()){
      // GV for Mcp             
      if( (gvFile=htsData.getPdfMcpGVFile()) != null)
        loadGvFromFile(gvFile, "mcp");
   
      // GV for Lf0
      if( (gvFile=htsData.getPdfLf0GVFile()) != null)
        loadGvFromFile(gvFile, "lf0");

      // GV for Str   
      if( (gvFile=htsData.getPdfStrGVFile()) != null)
        loadGvFromFile(gvFile, "str");      
   
      // GV for Mag  
      if( (gvFile=htsData.getPdfMagGVFile()) != null)
        loadGvFromFile(gvFile, "mag");  
      
      // gv-switch
     // if( (gvFile=htsData.getSwitchGVFile()) != null)
     //   loadSwitchGvFromFile(gvFile, featureDef, trickyPhones);      
      
    } 

      } catch (FileNotFoundException e) {
          logger.debug("GVModelSet: " + e.getMessage());
          throw new FileNotFoundException("GVModelSet: " + e.getMessage());
      } catch (IOException e) {
          logger.debug("GVModelSet: " + e.getMessage());
          throw new IOException("GVModelSet: " + e.getMessage());
      } 

  }
  
  
  private void loadGvFromFile(String gvFile, String par) throws Exception {
          
    int numMSDFlag, numStream, vectorSize, numDurPdf;
    DataInputStream data_in;
    int m, i;
    
    data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
    logger.info("LoadGVModelSet reading: " + gvFile);
                       
    numMSDFlag = data_in.readInt();
    numStream = data_in.readInt();  
    vectorSize = data_in.readInt();
    numDurPdf = data_in.readInt();
    
    if(par.contentEquals("mcp")){
      gvmeanMcp = new double[vectorSize];  
      gvcovInvMcp = new double[vectorSize];
      for ( i = 0; i < vectorSize; i++){
        gvmeanMcp[i] = data_in.readFloat();
        //gvcovInvMcp[i] = data_in.readFloat();     // case Derivative
        gvcovInvMcp[i] = 1/data_in.readFloat();  // case NEWTON in gv optimization
      }
    } else if(par.contentEquals("lf0")){
        gvmeanLf0 = new double[vectorSize];  
        gvcovInvLf0 = new double[vectorSize];
        for ( i = 0; i < vectorSize; i++){
          gvmeanLf0[i] = data_in.readFloat();
          //gvcovInvLf0[i] = data_in.readFloat();     // case Derivative  
          gvcovInvLf0[i] = 1/data_in.readFloat(); // case NEWTON in gv optimization
        }
    } else if(par.contentEquals("str")){
        gvmeanStr = new double[vectorSize];  
        gvcovInvStr = new double[vectorSize];
        for ( i = 0; i < vectorSize; i++){
          gvmeanStr[i] = data_in.readFloat();
          //gvcovInvStr[i] = data_in.readFloat();     // case Derivative
          gvcovInvStr[i] = 1/data_in.readFloat(); // case NEWTON in gv optimization
        }
    } else if(par.contentEquals("mag")){
        gvmeanMag = new double[vectorSize];  
        gvcovInvMag = new double[vectorSize];
        for ( i = 0; i < vectorSize; i++){
          gvmeanMag[i] = data_in.readFloat();
          //gvcovInvMag[i] = data_in.readFloat();     // case Derivative
          gvcovInvMag[i] = 1/data_in.readFloat(); // case NEWTON in gv optimization
        }
    } 
    data_in.close ();     
  }  
  
  
  public void loadSwitchGvFromFile(String gvFile, FeatureDefinition featDef, String trickyPhones)
  throws Exception {
   
   //featDef = featDefinition;
   //phTrans = phoneTranslator;
   PhoneTranslator phTrans = new PhoneTranslator(trickyPhones);      
      
   int i, j, length, state, feaIndex;
   BufferedReader s = null;
   String line, buf, aux;
   StringTokenizer sline;
   //phTrans = phTranslator;
   
           
   assert featDef != null : "Feature Definition was not set";
       
   try {   
     /* read lines of tree-*.inf fileName */ 
     s = new BufferedReader(new InputStreamReader(new FileInputStream(gvFile)));
     logger.info("load: reading " + gvFile);
     
     // skip questions section
     while((line = s.readLine()) != null) {
         if (line.indexOf("QS") < 0 ) break;   /* a new state is indicated by {*}[2], {*}[3], ... */
     }
     
     while((line = s.readLine()) != null) {            
       if(line.indexOf("{*}") >= 0 ){  /* this is the indicator of a new state-tree */
         aux = line.substring(line.indexOf("[")+1, line.indexOf("]")); 
         state = Integer.parseInt(aux);
         
         sline = new StringTokenizer(aux);
         
         /* 1:  gets index node and looks for the node whose idx = buf */
         buf = sline.nextToken();   
         
         /* 2: gets question name and question name val */
         buf = sline.nextToken();
         String [] fea_val = buf.split("=");   /* splits featureName=featureValue*/
         feaIndex = featDef.getFeatureIndex(fea_val[0]);
        
         /* Replace back punctuation values */
         /* what about tricky phones, if using halfphones it would not be necessary */
         if(fea_val[0].contentEquals("sentence_punc") || fea_val[0].contentEquals("prev_punctuation") || fea_val[0].contentEquals("next_punctuation")){
             //System.out.print("CART replace punc: " + fea_val[0] + " = " + fea_val[1]);
             fea_val[1] = phTrans.replaceBackPunc(fea_val[1]);
             //System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
         }
         else if(fea_val[0].contains("tobi_") ){
             //System.out.print("CART replace tobi: " + fea_val[0] + " = " + fea_val[1]);
             fea_val[1] = phTrans.replaceBackToBI(fea_val[1]);
             //System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
         }
         else if(fea_val[0].contains("phone") ){
             //System.out.print("CART replace phone: " + fea_val[0] + " = " + fea_val[1]);
             fea_val[1] = phTrans.replaceBackTrickyPhones(fea_val[1]);
             //System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
         }
         
         // add featureName and featureValue to the switch off gv phones          
                  
       }         
     } /* while */  
     if (s != null)
       s.close();
     
   } catch (FileNotFoundException e) {
       logger.debug("FileNotFoundException: " + e.getMessage());
       throw new FileNotFoundException("LoadTreeSet: " + e.getMessage());
   }
  
}
  
  
  
  
  
  
    
}
