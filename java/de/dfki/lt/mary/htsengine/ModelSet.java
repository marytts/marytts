package de.dfki.lt.mary.htsengine;

import java.io.*;
import java.io.IOException;

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
public class ModelSet {
	
	private static int nstate;               /* # of HMM states for individual HMM */
	private static int lf0stream;            /* # of stream for log f0 modeling */
	private static int mcepvsize;            /* vector size for mcep modeling */
	private static int strvsize;             /* vector size for strengths modeling */
	private static int magvsize;             /* vector size for Fourier magnitudes modeling */

	private static int nlf0pdf[];             /* # of pdfs for each state position (log F0) */
	private static int nmceppdf[];            /* # of pdfs for each state position (mcep) */
	private static int nstrpdf[];             /* # of pdfs for each state position (str) */
	private static int nmagpdf[];             /* # of pdfs for each state position (mag) */
	private static int ndurpdf;               /* # of pdfs for duration */

	private static double durpdf[][];         /* pdfs for duration [#states][#leaves]*/
	private static double mceppdf[][][];      /* pdfs for mcep     [#states][#leaves][vectorsize]   */
	private static double strpdf[][][];       /* pdfs for str      [#states][#leaves][vectorsize]   */
	private static double magpdf[][][];       /* pdfs for mag      [#states][#leaves][vectorsize]   */

	private static double lf0pdf[][][][];     /* pdfs for lf0      [#states][#leaves][#streams][lf0_vectorsize] */ 
	                           /* lf0_vectorsize = 4: mean, variance, voiced weight, and unvoiced weight */
	
	
	public int get_nstate(){ return nstate; }
	public int get_lf0stream(){ return lf0stream; }
	public int get_mcepvsize(){ return mcepvsize; }
	public int get_strvsize(){ return strvsize; }
	public int get_magvsize(){ return magvsize; }
	public int get_ndurpdf(){ return ndurpdf; }
	
	/** This function finds duration pdf from pdf array and calculates the duration in frames
	 *  for this model.
	 * @param m
	 * @param rho
	 * @param diffdur
	 * @return
	 */
	public double FindDurPDF(Model m, double rho, double diffdur) {
		
	  double data, mean, variance, dd;
	  int s;
	  int n_state = nstate;
	  
	  /* NOTE 1: the indexes in the tree.inf file start in 1 ex. dur_s2_1, but here are stored */
      /* in durpdf[i][j] array which starts in i=0, so when finding this dur pdf, the idx should */
      /* be idx-1 !!!*/
	  int idx = m.get_durpdf()-1;
	 
	  dd = diffdur;
	  
	  for(s=0; s<n_state; s++){
	    mean = durpdf[idx][s];
		variance = durpdf[idx][n_state+s];
		data = mean + rho*variance;
		
		m.set_dur(s, (int)(data+dd+0.5));
		if(m.get_dur(s) < 1 )
		  m.set_dur(s, 1);
		System.out.print("dur[" + s + "]=" + m.get_dur(s) + " ");
		m.set_totaldur(m.get_totaldur() + m.get_dur(s));
		dd = dd + ( data - (double)m.get_dur(s) );		
	  }
	 
	  return dd; 
		  		  
    } /* method FindDurPDf */
	

	/** FindLF0PDF : find required pdf for log F0 from pdf array
	 * @param s
	 * @param m
	 * @param uvthresh
	 */
	public void FindLF0PDF(int s, Model m, double uvthresh) {
	   int stream;
	   double val;
	   /* idx-1 because index number start in 1 but they are stored starting in 0, see NOTE 1. */
	   int idx = m.get_lf0pdf(s)-1;
	   
	   int nstream = lf0stream;
	   for(stream=0; stream<nstream; stream++) {
		  val=lf0pdf[s][idx][stream][0];
		  m.set_lf0mean(s, stream, val);
		  m.set_lf0variance(s, stream, lf0pdf[s][idx][stream][1]);
		                                           //vw  = lf0pdf[i][j][k][2]; /* voiced weight */
                                                   //uvw = lf0pdf[i][j][k][3]; /* unvoiced weight */
		  if(stream == 0) {
			  //System.out.println("  weight=" + lf0pdf[s][idx][stream][2]);
			  if(lf0pdf[s][idx][stream][2] > uvthresh)
				 m.set_voiced(s, true);
			  else
				 m.set_voiced(s,false);
		  }
	   }	
	 }  /* method findLF0PDF */
	
	
	/** FindMcpPDF : find pdf for mel-cepstrum from pdf array
	 * @param s
	 * @param m
	 */
	public void FindMcpPDF(int s, Model m) {
	  int i,j;
	  int idx = m.get_mceppdf(s)-1;
	  for(i=0, j=0; j<mcepvsize; i++,j++)
		m.set_mcepmean(s, i, mceppdf[s][idx][j]);
	  for(i=0, j=mcepvsize; j<(2*mcepvsize); i++,j++)
		m.set_mcepvariance(s, i, mceppdf[s][idx][j]);
	  
	}
	
	/** FindStrPDF : find pdf for strengths from pdf array
	 * @param s
	 * @param m
	 */
	public void FindStrPDF(int s, Model m) {
	  int i,j;
	  int idx = m.get_strpdf(s)-1;
	  for(i=0, j=0; j<strvsize; i++,j++)
		m.set_strmean(s, i, strpdf[s][idx][j]);
	  for(i=0, j=strvsize; j<(2*strvsize); i++,j++)
		m.set_strvariance(s, i, strpdf[s][idx][j]);
	  
	}
	
	/** FindMagPDF : find pdf for Fourier magnitudes from pdf array
	 * @param s
	 * @param m
	 */
	public void FindMagPDF(int s, Model m) {
	  int i,j;
	  int idx = m.get_magpdf(s)-1;
	  for(i=0, j=0; j<magvsize; i++,j++)
		m.set_magmean(s, i, magpdf[s][idx][j]);
	  for(i=0, j=magvsize; j<(2*magvsize); i++,j++)
		m.set_magvariance(s, i, magpdf[s][idx][j]);
	  
	}
	
	/** This function loads the models set contained in files .pdf, it uses as input the names of 
	 * the files .pdf, these file names are in HMMData. */
	public static void LoadModelSet(HMMData hts_data) {
	
	  DataInputStream data_in;
	  int i, j, k, l;
	  double vw, uvw;
	   
      try {        	   	
        /*________________________________________________________________*/
        /*-------------------- load pdfs for duration --------------------*/ 
  	    data_in = new DataInputStream (new FileInputStream(HMMData.PdfDurFile()));
        System.out.println("LoadModelSet reading: " + HMMData.PdfDurFile());
          
        /* read the number of states & the number of pdfs (leaf nodes) */
        /* read the number of HMM states, this number is the same for all pdf's. */		 
	    nstate = data_in.readInt();
	    System.out.println("LoadModelSet: nstate = " + nstate);
	    
	    /* I do not know if this is the best way of handling this...*/
	    /* I need to log this error and exit */
	    if( nstate < 0 )
	      System.err.println("LoadModelSet: #HMM states must be positive value.");
	     
	    /* read the number of duration pdfs */
	    ndurpdf = data_in.readInt();
	    System.out.println("LoadModelSet: ndurpdf = " + ndurpdf);
	    
	    if( nstate < 0 )
		  System.err.println("LoadModelSet: #duration pdf must be positive value.");
		
	    /* Now we know the number of duration pdfs and the vector size which is */
	    /* the number of states in each HMM. Here the vector size is 2*nstate because*/
	    /* the first nstate correspond to the mean and the second nstate correspond */
	    /* to the diagonal variance vector, the mean and variance are copied here in */
	    /* only one vector. */
	    /* 2*nstate because the vector size for duration is the number of states */
	    durpdf = new double[ndurpdf][2*nstate];
	    
	    /* read pdfs (mean & variance) */
	    for ( i = 0; i < ndurpdf; i++){
	      for ( j = 0; j < (2 * nstate); j++) {
	    	  durpdf[i][j] = data_in.readFloat();
	    	  //System.out.println("durpdf[" + i + "]" + "[" + j + "]:" + durpdf[i][j]);
	      }
	    }  
	    data_in.close (); 
	    data_in=null;
	    
	    /*________________________________________________________________*/
	    /*-------------------- load pdfs for mcep ------------------------*/
 	    data_in = new DataInputStream (new FileInputStream (HMMData.PdfMcpFile()));
        System.out.println("LoadModelSet reading: " + HMMData.PdfMcpFile());
        /* read vector size for spectrum */
        mcepvsize = data_in.readInt();
	    System.out.println("LoadModelSet: mcepvsize = " + mcepvsize);
	  
	    if( mcepvsize < 0 )
		   System.err.println("LoadModelSet: vector size of mel-cepstrum part must be positive.");
	    
	    /* Now we need the number of pdf's for each state */
	    mceppdf = new double[nstate][][];
	    nmceppdf = new int[nstate];
	    for(i=0; i< nstate; i++){
	       nmceppdf[i] = data_in.readInt();
	       if( nmceppdf[i] < 0 )
	    	 System.err.println("LoadModelSet: #mcep pdf at state " + i + " must be positive value.");
	       System.out.println("nmceppdf[" + i + "] = " + nmceppdf[i]);
	       /* Now i know the size of mceppdf[#states][#leaves][vectorsize] */
	       /* so i can allocate memory for mceppdf[][][] */
	       mceppdf[i] = new double[nmceppdf[i]][2*mcepvsize];         
	    }
	    
	    /* read  mcep pdfs (mean, variance). (2*mcepvsize because mean and diag variance */
	    /* are allocated in only one vector. */
	    for(i=0; i< nstate; i++){
	      for( j=0; j<nmceppdf[i]; j++){  	  
	    	  for( k=0; k<(2*mcepvsize); k++ ){
	    		  mceppdf[i][j][k] = data_in.readFloat();
	    		  //System.out.println("mcep["+ i + "][" + j + "][" + k + "] =" + mceppdf[i][j][k]);
	    	  }
	      }
	      System.out.println("loaded nmceppdf[" + i + "] = " + j);
	    }
	    data_in.close (); 
	    data_in=null;
	    
	    /*____________________________________________________________________*/
	    /*-------------------- load pdfs for strengths------------------------*/
 	    data_in = new DataInputStream (new FileInputStream (HMMData.PdfStrFile()));
        System.out.println("LoadModelSet reading: " + HMMData.PdfStrFile());
        /* read vector size for strengths */
        strvsize = data_in.readInt();
	    System.out.println("LoadModelSet: strvsize = " + strvsize);
	  
	    if( strvsize < 0 )
		   System.err.println("LoadModelSet: vector size of strengths part must be positive.");
	    
	    /* Now we need the number of pdf's for each state */
	    strpdf = new double[nstate][][];
	    nstrpdf = new int[nstate];
	    for(i=0; i< nstate; i++){
	       nstrpdf[i] = data_in.readInt();
	       if( nstrpdf[i] < 0 )
	    	 System.err.println("LoadModelSet: #str pdf at state " + i + " must be positive value.");
	       System.out.println("nstrpdf[" + i + "] = " + nstrpdf[i]);
	       /* Now i know the size of strpdf[#states][#leaves][vectorsize] */
	       /* so i can allocate memory for strpdf[][][] */
	       strpdf[i] = new double[nstrpdf[i]][2*strvsize];         
	    }
	    
	    /* read str pdfs (mean, variance). (2*strvsize because mean and diag variance */
	    /* are allocated in only one vector. */
	    for(i=0; i< nstate; i++){
	      for( j=0; j<nstrpdf[i]; j++){  	  
	    	  for( k=0; k<(2*strvsize); k++ ){
	    		  strpdf[i][j][k] = data_in.readFloat();
	    		  //System.out.println("strpdf["+ i + "][" + j + "][" + k + "] =" + strpdf[i][j][k]);
	    	  }
	      }
	      System.out.println("loaded nstrpdf[" + i + "] = " + j);
	    }
	    data_in.close (); 
	    data_in=null;
	    
	    /*____________________________________________________________________*/
	    /*-------------------- load pdfs for Fourier magnitudes --------------*/
 	    data_in = new DataInputStream (new FileInputStream (HMMData.PdfMagFile()));
        System.out.println("LoadModelSet reading: " + HMMData.PdfMagFile());
        /* read vector size for Fourier magnitudes */
        magvsize = data_in.readInt();
	    System.out.println("LoadModelSet: magvsize = " + magvsize);
	  
	    if( magvsize < 0 )
		   System.err.println("LoadModelSet: vector size of Fourier magnitudes part must be positive.");
	    
	    /* Now we need the number of pdf's for each state */
	    magpdf = new double[nstate][][];
	    nmagpdf = new int[nstate];
	    for(i=0; i< nstate; i++){
	       nmagpdf[i] = data_in.readInt();
	       if( nmagpdf[i] < 0 )
	    	 System.err.println("LoadModelSet: #mag pdf at state " + i + " must be positive value.");
	       System.out.println("nmagpdf[" + i + "] = " + nmagpdf[i]);
	       /* Now i know the size of magpdf[#states][#leaves][vectorsize] */
	       /* so i can allocate memory for magpdf[][][] */
	       magpdf[i] = new double[nmagpdf[i]][2*magvsize];         
	    }
	    
	    /* read mag pdfs (mean, variance). (2*magvsize because mean and diag variance */
	    /* are allocated in only one vector. */
	    for(i=0; i< nstate; i++){
	      for( j=0; j<nmagpdf[i]; j++){  	  
	    	  for( k=0; k<(2*magvsize); k++ ){
	    		  magpdf[i][j][k] = data_in.readFloat();
	    		  //System.out.println("magpdf["+ i + "][" + j + "][" + k + "] =" + magpdf[i][j][k]);
	    	  }
	      }
	      System.out.println("loaded nmagpdf[" + i + "] = " + j);
	    }
	    
	    data_in.close (); 
	    data_in=null;
	    
	    /*____________________________________________________________________*/
	    /*-------------------- load pdfs for Log F0 --------------*/
	    data_in = new DataInputStream (new FileInputStream (HMMData.PdfLf0File()));
        System.out.println("LoadModelSet reading: " + HMMData.PdfLf0File());
	    /* read the number of streams for f0 modeling */
        lf0stream  = data_in.readInt();
	    System.out.println("LoadModelSet: lf0stream = " + lf0stream);
	  
	    if( lf0stream < 0 )
		   System.err.println("LoadModelSet:  #stream for log f0 part must be positive value.");
	    
	    /* read the number of pdfs for each state position */
	    lf0pdf = new double[nstate][][][];
	    nlf0pdf = new int[nstate];
	    for(i=0; i< nstate; i++){
	       nlf0pdf[i] = data_in.readInt();
	       if( nlf0pdf[i] < 0 )
	    	 System.err.println("LoadModelSet: #lf0 pdf at state " + i + " must be positive value.");
	       System.out.println("nlf0pdf[" + i + "] = " + nlf0pdf[i]);
	       /* Now i know the size of pdfs for lf0 [#states][#leaves][#streams][lf0_vectorsize] */
	       /* lf0_vectorsize = 4: mean, variance, voiced weight, and unvoiced weight */
	       /* so i can allocate memory for lf0pdf[][][] */
	       lf0pdf[i] = new double[nlf0pdf[i]][lf0stream][4];         
	    }
	    
	    /* read lf0 pdfs (mean, variance and weight).  */
	    for(i=0; i< nstate; i++){
	      for( j=0; j<nlf0pdf[i]; j++){  	  
	    	  for( k=0; k<lf0stream; k++ ){
	    		  for( l=0; l<4; l++){
	    		    lf0pdf[i][j][k][l] = data_in.readFloat();
	    		    //System.out.println("magpdf["+ i + "][" + j + "][" + k + "] =" + magpdf[i][j][k]);
	    		  }
	    		  vw  = lf0pdf[i][j][k][2]; /* voiced weight */
	              uvw = lf0pdf[i][j][k][3]; /* unvoiced weight */
	              if (vw<0.0 || uvw<0.0 || vw+uvw<0.99 || vw+uvw>1.01 )
	            	System.err.println("LoadModelSet: voiced/unvoiced weights must be within 0.99 to 1.01.");
	    	  }
	      }
	      System.out.println("loaded nlf0pdf[" + i + "] = " + j);
	    }
	    
	    data_in.close (); 
	    data_in=null; 
	    
	  
	} catch (FileNotFoundException e) {
	    System.err.println("FileNotFoundException: " + e.getMessage());
	} catch (IOException e) {
	    System.err.println("Caught IOException: " + e.getMessage());	
	}
	
  } /* method loadModelSet() */ 
	
 
	
	
} /* class ModelSet */
