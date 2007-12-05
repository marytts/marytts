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

package de.dfki.lt.mary.htsengine;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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
    
  private Logger logger = Logger.getLogger("GVModelSet");
  
  public double[] getGVmeanMcp(){ return gvmeanMcp; }
  public double[] getGVcovInvMcp(){ return gvcovInvMcp; }
  
  public double[] getGVmeanLf0(){ return gvmeanLf0; }
  public double[] getGVcovInvLf0(){ return gvcovInvLf0; }
  
  public double[] getGVmeanStr(){ return gvmeanStr; }
  public double[] getGVcovInvStr(){ return gvcovInvStr; }
  
  public double[] getGVmeanMag(){ return gvmeanMag; }
  public double[] getGVcovInvMag(){ return gvcovInvMag; }
  
  public void loadGVModelSet(HMMData htsData) throws Exception {
    
    /* allocate memory for the arrays and load the data from file */

    double gvcov;
    int order;
    DataInputStream data_in;
    String gvFile;
    /* Here global variance vectors are loaded from corresponding files */
    
    int i;
    if(htsData.getUseGV()){
      try { 
        
        /* GV for Mcp */  
        if( (gvFile=htsData.getPdfMcpGVFile()) != null) {        
          data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
          logger.info("LoadGVModelSet reading: " + gvFile);
          order = data_in.readShort();    /* first short is the order of static vector */   
          gvmeanMcp = new double[order];  /* allocate memory of this size */
          gvcovInvMcp = new double[order];          
          for ( i = 0; i < order; i++)
              gvmeanMcp[i] = data_in.readFloat();
          for ( i = 0; i < order; i++)
              gvcovInvMcp[i] = 1.0/data_in.readFloat();
          data_in.close (); 
        }
                    
        /* GV for lf0 */
        if( (gvFile = htsData.getPdfLf0GVFile()) != null) {
          data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
          logger.info("LoadGVModelSet reading: " + gvFile);
          order = data_in.readShort();    /* first short is the order of static vector */   
          gvmeanLf0 = new double[order];  /* allocate memory of this size */
          gvcovInvLf0 = new double[order];          
          for ( i = 0; i < order; i++)
              gvmeanLf0[i] = data_in.readFloat();
          for ( i = 0; i < order; i++)
              gvcovInvLf0[i] = 1.0/data_in.readFloat();
          data_in.close ();
        }
                   
          /* GV for Str */
        if( (gvFile = htsData.getPdfStrGVFile()) != null) {
          data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
          logger.info("LoadGVModelSet reading: " + gvFile);
          order = data_in.readShort();    /* first short is the order of static vector */   
          gvmeanStr = new double[order];  /* allocate memory of this size */
          gvcovInvStr = new double[order];          
          for ( i = 0; i < order; i++)
              gvmeanStr[i] = data_in.readFloat();
          for ( i = 0; i < order; i++)
              gvcovInvStr[i] = 1.0/data_in.readFloat();
          data_in.close ();
        }
                      
        /* GV for Mag */
        if( (gvFile = htsData.getPdfMagGVFile()) != null) {
          data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(gvFile)));
          logger.info("LoadGVModelSet reading: " + gvFile);
          order = data_in.readShort();    /* first short is the order of static vector */   
          gvmeanMag = new double[order];  /* allocate memory of this size */
          gvcovInvMag = new double[order];          
          for ( i = 0; i < order; i++)
              gvmeanMag[i] = data_in.readFloat();
          for ( i = 0; i < order; i++)
              gvcovInvMag[i] = 1.0/data_in.readFloat();
          data_in.close (); 
        }       

      } catch (FileNotFoundException e) {
          logger.debug("PStream: " + e.getMessage());
          throw new FileNotFoundException("PStream: " + e.getMessage());
      } catch (IOException e) {
          logger.debug("PStream: " + e.getMessage());
          throw new IOException("PStream: " + e.getMessage());
      }

    }
  }  /* loadGVSet method */
    
    
}
