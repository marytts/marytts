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

import java.io.*;

import org.apache.log4j.Logger;

/**
 * Set of all HMM pdfs models for duration, log f0, mel-cepstrum, bandpass 
 * voicing strengths and Fourier magnitudes (the signal processing part of 
 * Fourier magnitudes is not implemented yet).
 * This function also implements functions for searching particular pdfs in the 
 * model set. 
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSModelSet {
	
	private int numState;             /* # of HMM states for individual HMM */
	private int lf0Stream;            /* # of stream for log f0 modeling */
	private int mcepVsize;            /* vector size for mcep modeling */
	private int strVsize;             /* vector size for strengths modeling */
	private int magVsize;             /* vector size for Fourier magnitudes modeling */

	private int numLf0Pdf[];          /* # of pdfs for each state position (log F0) */
	private int numMcepPdf[];         /* # of pdfs for each state position (mcep) */
	private int numStrPdf[];          /* # of pdfs for each state position (str) */
	private int numMagPdf[];          /* # of pdfs for each state position (mag) */
	private int numDurPdf;            /* # of pdfs for duration */

	private double durPdf[][];        /* pdfs for duration [#states][#leaves]*/
	private double mcepPdf[][][];     /* pdfs for mcep     [#states][#leaves][vectorsize]   */
	private double strPdf[][][];      /* pdfs for str      [#states][#leaves][vectorsize]   */
	private double magPdf[][][];      /* pdfs for mag      [#states][#leaves][vectorsize]   */

	private double lf0Pdf[][][][];     /* pdfs for lf0      [#states][#leaves][#streams][lf0_vectorsize] */ 
	                           /* lf0_vectorsize = 4: mean, variance, voiced weight, and unvoiced weight */
    
    private Logger logger = Logger.getLogger("ModelSet");
  	
	public int getNumState(){ return numState; }
	public int getLf0Stream(){ return lf0Stream; }
	public int getMcepVsize(){ return mcepVsize; }
	public int getStrVsize(){ return strVsize; }
	public int getMagVsize(){ return magVsize; }
	public int getnumDurPdf(){ return numDurPdf; }
	
	/** This function finds duration pdf from pdf array and calculates the duration in frames
	 *  for this model.
	 * @param m
	 * @param rho
	 * @param diffdur
	 * @return
	 */
	public double findDurPdf(HTSModel m, boolean firstPh, boolean lastPh, double rho, double diffdur) {
		
	  double data, mean, variance, dd;
	  int s;
	  int n_state = numState;
	  
	  /* NOTE 1: the indexes in the tree.inf file start in 1 ex. dur_s2_1, but here are stored */
      /* in durpdf[i][j] array which starts in i=0, so when finding this dur pdf, the idx should */
      /* be idx-1 !!!*/
	  int idx = m.getDurPdf()-1;
	 
	  dd = diffdur;
      
	  for(s=0; s<n_state; s++){
	    mean = durPdf[idx][s];
		variance = durPdf[idx][n_state+s];
		data = mean + rho*variance;
        
        /* check if the model is initial/final pause, if so reduce the length of the pause 
         * to 10% of the calculated value. */       
        if(m.getPhoneName().contentEquals("_") && (firstPh || lastPh ))
          data = data * 0.1;
		
		m.setDur(s, (int)(data+dd+0.5));
		if(m.getDur(s) < 1 )
		  m.setDur(s, 1);
               
		m.setTotalDur(m.getTotalDur() + m.getDur(s));
        
		dd = dd + ( data - (double)m.getDur(s) );		
	  }
      
	  return dd; 
		  		  
    } /* method FindDurPdf */
	

	/** FindLF0PDF : find required pdf for log F0 from pdf array
	 * @param s
	 * @param m
	 * @param uvthresh
	 */
	public void findLf0Pdf(int s, HTSModel m, double uvthresh) {
	   int stream;
	   double val;
	   /* idx-1 because index number start in 1 but they are stored starting in 0, see NOTE 1. */
	   int idx = m.getLf0Pdf(s)-1;
	   
	   int nstream = lf0Stream;
	   for(stream=0; stream<nstream; stream++) {
		  val=lf0Pdf[s][idx][stream][0];
		  m.setLf0Mean(s, stream, val);
		  m.setLf0Variance(s, stream, lf0Pdf[s][idx][stream][1]);
		                                           //vw  = lf0pdf[i][j][k][2]; /* voiced weight */
                                                   //uvw = lf0pdf[i][j][k][3]; /* unvoiced weight */
		  if(stream == 0) {
			  //System.out.println("  weight=" + lf0pdf[s][idx][stream][2]);
			  if(lf0Pdf[s][idx][stream][2] > uvthresh)
				 m.setVoiced(s, true);
			  else
				 m.setVoiced(s,false);
		  }
	   }	
	 }  /* method findLf0Pdf */
	
	
	/** FindMcpPDF : find pdf for mel-cepstrum from pdf array
	 * @param s
	 * @param m
	 */
	public void findMcpPdf(int s, HTSModel m) {
	  int i,j;
	  int idx = m.getMcepPdf(s)-1;
	  for(i=0, j=0; j<mcepVsize; i++,j++)
		m.setMcepMean(s, i, mcepPdf[s][idx][j]);
	  for(i=0, j=mcepVsize; j<(2*mcepVsize); i++,j++)
		m.setMcepVariance(s, i, mcepPdf[s][idx][j]);
	  
	}
	
	/** FindStrPDF : find pdf for strengths from pdf array
	 * @param s
	 * @param m
	 */
	public void findStrPdf(int s, HTSModel m) {
	  int i,j;
	  int idx = m.getStrPdf(s)-1;
	  for(i=0, j=0; j<strVsize; i++,j++)
		m.setStrMean(s, i, strPdf[s][idx][j]);
	  for(i=0, j=strVsize; j<(2*strVsize); i++,j++)
		m.setStrVariance(s, i, strPdf[s][idx][j]);
	  
	}
	
	/** FindMagPDF : find pdf for Fourier magnitudes from pdf array
	 * @param s
	 * @param m
	 */
	public void findMagPdf(int s, HTSModel m) {
	  int i,j;
	  int idx = m.getMagPdf(s)-1;
	  for(i=0, j=0; j<magVsize; i++,j++)
		m.setMagMean(s, i, magPdf[s][idx][j]);
	  for(i=0, j=magVsize; j<(2*magVsize); i++,j++)
		m.setMagVariance(s, i, magPdf[s][idx][j]);
	  
	}
	
    
    
	/** This function loads the models set contained in files .pdf, it uses as input the names of 
	 * the files .pdf, these file names are in HMMData. 
     * DUR, LF0 and MCP are required as minimum for generating voice,
     * STR and MAG are optional for generating mixed excitation.  */
	public void loadModelSet(HMMData htsData) throws Exception {
	
	  DataInputStream data_in;
	  int i, j, k, l;
	  double vw, uvw;
	   
      try {        	   	
        /*________________________________________________________________*/
        /*-------------------- load pdfs for duration --------------------*/ 
  	    data_in = new DataInputStream (new BufferedInputStream(new FileInputStream(htsData.getPdfDurFile())));
        logger.info("LoadModelSet reading: " + htsData.getPdfDurFile());
          
        /* read the number of states & the number of pdfs (leaf nodes) */
        /* read the number of HMM states, this number is the same for all pdf's. */		 
	    numState = data_in.readInt();
	    //System.out.println("LoadModelSet: nstate = " + nstate);
	    
	    /* check number of states */
	    if( numState < 0 )
          throw new Exception("LoadModelSet: #HMM states must be positive value.");
        
	     
	    /* read the number of duration pdfs */
	    numDurPdf = data_in.readInt();
		
	    /* Now we know the number of duration pdfs and the vector size which is */
	    /* the number of states in each HMM. Here the vector size is 2*nstate because*/
	    /* the first nstate correspond to the mean and the second nstate correspond */
	    /* to the diagonal variance vector, the mean and variance are copied here in */
	    /* only one vector. */
	    /* 2*nstate because the vector size for duration is the number of states */
	    durPdf = new double[numDurPdf][2*numState];
	    
	    /* read pdfs (mean & variance) */
	    for ( i = 0; i < numDurPdf; i++){
	      for ( j = 0; j < (2 * numState); j++) {
	    	  durPdf[i][j] = data_in.readFloat();
	    	  //System.out.println("durpdf[" + i + "]" + "[" + j + "]:" + durpdf[i][j]);
	      }
	    }  
	    data_in.close (); 
	    data_in=null;
	    
	    /*________________________________________________________________*/
	    /*-------------------- load pdfs for mcep ------------------------*/
 	    data_in = new DataInputStream (new BufferedInputStream(new FileInputStream (htsData.getPdfMcpFile())));
        logger.info("LoadModelSet reading: " + htsData.getPdfMcpFile());
        /* read vector size for spectrum */
        mcepVsize = data_in.readInt();
	    //System.out.println("LoadModelSet: mcepvsize = " + mcepvsize);
	  
	    if( mcepVsize < 0 )
           throw new Exception("LoadModelSet: vector size of mel-cepstrum part must be positive."); 
	    
	    /* Now we need the number of pdf's for each state */
	    mcepPdf = new double[numState][][];
	    numMcepPdf = new int[numState];
	    for(i=0; i< numState; i++){
	       numMcepPdf[i] = data_in.readInt();
	       if( numMcepPdf[i] < 0 )
               throw new Exception("LoadModelSet: #mcep pdf at state " + i + " must be positive value.");
	       //System.out.println("nmceppdf[" + i + "] = " + nmceppdf[i]);
	       /* Now i know the size of mceppdf[#states][#leaves][vectorsize] */
	       /* so i can allocate memory for mceppdf[][][] */
	       mcepPdf[i] = new double[numMcepPdf[i]][2*mcepVsize];         
	    }
	    
	    /* read  mcep pdfs (mean, variance). (2*mcepvsize because mean and diag variance */
	    /* are allocated in only one vector. */
	    for(i=0; i< numState; i++){
	      for( j=0; j<numMcepPdf[i]; j++){  	  
	    	  for( k=0; k<(2*mcepVsize); k++ ){
	    		  mcepPdf[i][j][k] = data_in.readFloat();
	    		  //System.out.println("mcep["+ i + "][" + j + "][" + k + "] =" + mceppdf[i][j][k]);
	    	  }
	      }
	      //System.out.println("loaded nmceppdf[" + i + "] = " + j);
	    }
	    data_in.close (); 
	    data_in=null;
	    
	    /*____________________________________________________________________*/
	    /*-------------------- load pdfs for strengths------------------------*/
        if( htsData.getPdfStrFile() != null) {
        
 	    data_in = new DataInputStream (new BufferedInputStream(new FileInputStream (htsData.getPdfStrFile())));
        logger.info("LoadModelSet reading: " + htsData.getPdfStrFile());
        /* read vector size for strengths */
        strVsize = data_in.readInt();
	    //System.out.println("LoadModelSet: strvsize = " + strvsize);
	  
	    if( strVsize < 0 )
          throw new Exception("LoadModelSet: vector size of strengths part must be positive.");
	    
	    /* Now we need the number of pdf's for each state */
	    strPdf = new double[numState][][];
	    numStrPdf = new int[numState];
	    for(i=0; i< numState; i++){
	       numStrPdf[i] = data_in.readInt();
	       if( numStrPdf[i] < 0 )
             throw new Exception("LoadModelSet: #str pdf at state " + i + " must be positive value.");
	       //System.out.println("nstrpdf[" + i + "] = " + nstrpdf[i]);
	       /* Now i know the size of strpdf[#states][#leaves][vectorsize] */
	       /* so i can allocate memory for strpdf[][][] */
	       strPdf[i] = new double[numStrPdf[i]][2*strVsize];         
	    }
	    
	    /* read str pdfs (mean, variance). (2*strvsize because mean and diag variance */
	    /* are allocated in only one vector. */
	    for(i=0; i< numState; i++){
	      for( j=0; j<numStrPdf[i]; j++){  	  
	    	  for( k=0; k<(2*strVsize); k++ ){
	    		  strPdf[i][j][k] = data_in.readFloat();
	    		  //System.out.println("strpdf["+ i + "][" + j + "][" + k + "] =" + strpdf[i][j][k]);
	    	  }
	      }
	      //System.out.println("loaded nstrpdf[" + i + "] = " + j);
	    }
	    data_in.close (); 
	    data_in=null;
        
        } /* if STR file is not null */
        
	    /*____________________________________________________________________*/
	    /*-------------------- load pdfs for Fourier magnitudes --------------*/
        if( htsData.getPdfMagFile() != null ) {
 	    data_in = new DataInputStream (new BufferedInputStream(new FileInputStream (htsData.getPdfMagFile())));
        logger.info("LoadModelSet reading: " + htsData.getPdfMagFile());
        /* read vector size for Fourier magnitudes */
        magVsize = data_in.readInt();
	    //System.out.println("LoadModelSet: magvsize = " + magvsize);
	  
	    if( magVsize < 0 )
          throw new Exception("LoadModelSet: vector size of Fourier magnitudes part must be positive.");
	    
	    /* Now we need the number of pdf's for each state */
	    magPdf = new double[numState][][];
	    numMagPdf = new int[numState];
	    for(i=0; i< numState; i++){
	       numMagPdf[i] = data_in.readInt();
	       if( numMagPdf[i] < 0 )
             throw new Exception("LoadModelSet: #mag pdf at state " + i + " must be positive value.");
	       //System.out.println("nmagpdf[" + i + "] = " + nmagpdf[i]);
	       /* Now i know the size of magpdf[#states][#leaves][vectorsize] */
	       /* so i can allocate memory for magpdf[][][] */
	       magPdf[i] = new double[numMagPdf[i]][2*magVsize];         
	    }
	    
	    /* read mag pdfs (mean, variance). (2*magvsize because mean and diag variance */
	    /* are allocated in only one vector. */
	    for(i=0; i< numState; i++){
	      for( j=0; j<numMagPdf[i]; j++){  	  
	    	  for( k=0; k<(2*magVsize); k++ ){
	    		  magPdf[i][j][k] = data_in.readFloat();
	    		  //System.out.println("magpdf["+ i + "][" + j + "][" + k + "] =" + magpdf[i][j][k]);
	    	  }
	      }
	      //System.out.println("loaded nmagpdf[" + i + "] = " + j);
	    }
	    
	    data_in.close (); 
	    data_in=null;
        } /*  if MAG file is not null */ 
       
	    /*____________________________________________________________________*/
	    /*-------------------- load pdfs for Log F0 --------------*/
	    data_in = new DataInputStream (new BufferedInputStream(new FileInputStream (htsData.getPdfLf0File())));
        logger.info("LoadModelSet reading: " + htsData.getPdfLf0File());
	    /* read the number of streams for f0 modeling */
        lf0Stream  = data_in.readInt();
	    //System.out.println("LoadModelSet: lf0stream = " + lf0stream);
	  
	    if( lf0Stream < 0 )
          throw new Exception("LoadModelSet:  #stream for log f0 part must be positive value.");
	    
	    /* read the number of pdfs for each state position */
	    lf0Pdf = new double[numState][][][];
	    numLf0Pdf = new int[numState];
	    for(i=0; i< numState; i++){
	       numLf0Pdf[i] = data_in.readInt();
	       if( numLf0Pdf[i] < 0 )
             throw new Exception("LoadModelSet: #lf0 pdf at state " + i + " must be positive value.");
	       //System.out.println("nlf0pdf[" + i + "] = " + nlf0pdf[i]);
	       /* Now i know the size of pdfs for lf0 [#states][#leaves][#streams][lf0_vectorsize] */
	       /* lf0_vectorsize = 4: mean, variance, voiced weight, and unvoiced weight */
	       /* so i can allocate memory for lf0pdf[][][] */
	       lf0Pdf[i] = new double[numLf0Pdf[i]][lf0Stream][4];         
	    }
	    
	    /* read lf0 pdfs (mean, variance and weight).  */
	    for(i=0; i< numState; i++){
	      for( j=0; j<numLf0Pdf[i]; j++){  	  
	    	  for( k=0; k<lf0Stream; k++ ){
	    		  for( l=0; l<4; l++){
	    		    lf0Pdf[i][j][k][l] = data_in.readFloat();
	    		    //System.out.println("magpdf["+ i + "][" + j + "][" + k + "] =" + magpdf[i][j][k]);
	    		  }
	    		  vw  = lf0Pdf[i][j][k][2]; /* voiced weight */
	              uvw = lf0Pdf[i][j][k][3]; /* unvoiced weight */
	              if (vw<0.0 || uvw<0.0 || vw+uvw<0.99 || vw+uvw>1.01 )
                    throw new Exception("LoadModelSet: voiced/unvoiced weights must be within 0.99 to 1.01.");
	    	  }
	      }
	      //System.out.println("loaded nlf0pdf[" + i + "] = " + j);
	    }
	    
	    data_in.close (); 
	    data_in=null; 
	    
	  
	} catch (FileNotFoundException e) {
	    logger.debug("LoadModelSet: " + e.getMessage());
        throw new FileNotFoundException("LoadModelSet: " + e.getMessage());
	} catch (IOException e) {
	    logger.debug("LoadModelSet: " + e.getMessage());
        throw new IOException("LoadModelSet: " + e.getMessage());
	}
	
  } /* method loadModelSet */ 
	
 
 
    
	
	
} /* class ModelSet */
