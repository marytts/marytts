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
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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
  int numMix;                     /* Number of mixtures */
  private double gvweightsMcp[];  /* weights for each mixture */
  private double gvmeanMcp[][];   /* global mean vector */
  private double gvcovInvMcp[][]; /* global inverse diagonal covariance */
  
  private double gvweightsLf0[];  /* weights for each mixture */
  private double gvmeanLf0[][];   /* global mean vector */
  private double gvcovInvLf0[][]; /* global inverse diagonal covariance */
  
  private double gvweightsStr[];  /* weights for each mixture */
  private double gvmeanStr[][];   /* global mean vector */
  private double gvcovInvStr[][]; /* global inverse diagonal covariance */
  
  private double gvweightsMag[];  /* weights for each mixture */
  private double gvmeanMag[][];   /* global mean vector */
  private double gvcovInvMag[][]; /* global inverse diagonal covariance */
  
  private int totalNumIter;
  private int firstIter;
  public int getTotalNumIter(){ return totalNumIter; }
  public void setTotalNumIter(int var){totalNumIter = var;}
  public void incTotalNumIter(int var){totalNumIter += var;}
  public int getFirstIter(){ return firstIter; }
  public void setFirstIter(int val){ firstIter = val; }
    
  private Logger logger = MaryUtils.getLogger("GVModelSet");
  
  public int getNumMix() {return numMix;}
  public double[] getGVweightsMcp(){ return gvweightsMcp; }
  public double[][] getGVmeanMcp(){ return gvmeanMcp; }
  public double[][] getGVcovInvMcp(){ return gvcovInvMcp; }
  
  public double[] getGVweightsLf0(){ return gvweightsLf0; }
  public double[][] getGVmeanLf0(){ return gvmeanLf0; }
  public double[][] getGVcovInvLf0(){ return gvcovInvLf0; }
  
  public double[] getGVweightsStr(){ return gvweightsStr; }
  public double[][] getGVmeanStr(){ return gvmeanStr; }
  public double[][] getGVcovInvStr(){ return gvcovInvStr; }
  
  public double[] getGVweightsMag(){ return gvweightsMag; }
  public double[][] getGVmeanMag(){ return gvmeanMag; }
  public double[][] getGVcovInvMag(){ return gvcovInvMag; }
  
  
  public void loadGVModelSet(HMMData htsData) throws Exception {
    
    /* allocate memory for the arrays and load the data from file */

    double gvcov;
    int order;
    DataInputStream data_in;
    String gvFile;
    
    /* If using Gaussian mixture models the format of the gv files should
     * include the number of gaussians, if not using GmmGV then 1 single gaussian is assumed. */
    boolean useGmmGV = htsData.getUseGmmGV();
    
    if( htsData.getUseGV() && useGmmGV ){
        logger.debug("GVModelSet: error useGV and useGmmGV are both true, just one can be true, the format of gmm-gv and gv files is diferent");
        throw new Exception("GVModelSet: error useGV and useGmmGV are both true, just one can be true, the format of gmm-gv and gv files is diferent");  
    }
    
    /* Here global variance vectors are loaded from corresponding files */
    int m, i,nmix;
    try { 
     if(htsData.getUseGV() || htsData.getUseGmmGV() ){
      /* GV for Mcp */   
      if( (gvFile=htsData.getPdfMcpGVFile()) != null){     
        data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
        logger.info("LoadGVModelSet reading: " + gvFile);
      
        if(useGmmGV)
          numMix = data_in.readShort();   /* first short is the number of mixtures in Gaussian model */
        else
          numMix = 1;                   /* no mixtures */  
        order = data_in.readShort();    /* second short is the order of static vector */
        gvweightsMcp = new double[numMix];
        gvmeanMcp = new double[numMix][order];  /* allocate memory of this size */
        gvcovInvMcp = new double[numMix][order];
        for (m = 0; m < numMix; m++){
          if(useGmmGV)
            gvweightsMcp[m] = data_in.readFloat(); /* first float of each block should be the mixture weight */
          else
            gvweightsMcp[m] = 1.0;   /* no mixtures */  
          for ( i = 0; i < order; i++){
            gvmeanMcp[m][i] = data_in.readFloat();
            //System.out.format("gvmeanMcp[%d][%d]=%.4f\n",m,i,gvmeanMcp[m][i]);
          }
          for ( i = 0; i < order; i++){
            gvcovInvMcp[m][i] = 1.0/data_in.readFloat();
            //System.out.format("gvcovMcp[%d][%d]=%.4f\n",m,i,1.0/gvcovInvMcp[m][i]);
          }
        }
        data_in.close (); 
      }
      /* GV for Lf0 */
      if( (gvFile=htsData.getPdfLf0GVFile()) != null){     
          data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
          logger.info("LoadGVModelSet reading: " + gvFile);
          if(useGmmGV)
            numMix = data_in.readShort();   /* first short is the number of mixtures in Gaussian model */
          else
            numMix = 1;                     /* no mixtures */     
          order = data_in.readShort();      /* second short is the order of static vector */
          gvweightsLf0 = new double[numMix];
          gvmeanLf0 = new double[numMix][order];  /* allocate memory of this size */
          gvcovInvLf0 = new double[numMix][order];
          for (m = 0; m < numMix; m++){
            if(useGmmGV)
              gvweightsLf0[m] = data_in.readFloat(); /* first float of each block should be the mixture weight */
            else
              gvweightsLf0[m] = 1.0;   /* no mixtures */  
            for ( i = 0; i < order; i++){
              gvmeanLf0[m][i] = data_in.readFloat();
              //System.out.format("gvmeanLf0[%d][%d]=%.4f\n",m,i,gvmeanLf0[m][i]);
            }
            for ( i = 0; i < order; i++){
              gvcovInvLf0[m][i] = 1.0/data_in.readFloat();
              //System.out.format("gvcovLf0[%d][%d]=%.4f\n",m,i,1.0/gvcovInvLf0[m][i]);
            }
          }
          data_in.close ();         
      }
      /* No mixtures for str and mag */
      useGmmGV = false;
      /* GV for Str */   
      if( (gvFile=htsData.getPdfStrGVFile()) != null){     
          data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
          logger.info("LoadGVModelSet reading: " + gvFile);
        
          if(useGmmGV)
            numMix = data_in.readShort();   /* first short is the number of mixtures in Gaussian model */
          else
            numMix = 1;  /* no mixtures */      
          order = data_in.readShort();    /* second short is the order of static vector */
          gvweightsStr = new double[numMix];
          gvmeanStr = new double[numMix][order];  /* allocate memory of this size */
          gvcovInvStr = new double[numMix][order];
          for (m = 0; m < numMix; m++){
            if(useGmmGV)
              gvweightsLf0[m] = data_in.readFloat(); /* first float of each block should be the mixture weight */
            else
              gvweightsLf0[m] = 1.0;   /* no mixtures */  
            for ( i = 0; i < order; i++)
              gvmeanStr[m][i] = data_in.readFloat();
            for ( i = 0; i < order; i++)
              gvcovInvStr[m][i] = 1.0/data_in.readFloat();
          }
          data_in.close ();         
      }
   
      /* GV for Mag */   
      if( (gvFile=htsData.getPdfMagGVFile()) != null){     
          data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
          logger.info("LoadGVModelSet reading: " + gvFile);
        
          if(useGmmGV)
            numMix = data_in.readShort();   /* first short is the number of mixtures in Gaussian model */
          else
            numMix = 1;  /* no mixtures */         
          order = data_in.readShort();    /* second short is the order of static vector */
          gvweightsMag = new double[numMix];
          gvmeanMag = new double[numMix][order];  /* allocate memory of this size */
          gvcovInvMag = new double[numMix][order];
          for (m = 0; m < numMix; m++){
            if(useGmmGV)
              gvweightsLf0[m] = data_in.readFloat(); /* first float of each block should be the mixture weight */
            else
              gvweightsLf0[m] = 1.0;   /* no mixtures */  
            for ( i = 0; i < order; i++)
              gvmeanMag[m][i] = data_in.readFloat();
            for ( i = 0; i < order; i++)
              gvcovInvMag[m][i] = 1.0/data_in.readFloat();
          }
          data_in.close ();         
      }
      
    } /* if UseGV or useGmmGV */

      } catch (FileNotFoundException e) {
          logger.debug("GVModelSet: " + e.getMessage());
          throw new FileNotFoundException("GVModelSet: " + e.getMessage());
      } catch (IOException e) {
          logger.debug("GVModelSet: " + e.getMessage());
          throw new IOException("GVModelSet: " + e.getMessage());
      } 

  }  /* loadGlobalVariance method */
    
    
}
