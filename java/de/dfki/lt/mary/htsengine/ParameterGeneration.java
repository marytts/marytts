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

/**
 * Parameter generation out of trained HMMs.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class ParameterGeneration {
	
  public static final double INFTY   = ((double) 1.0e+38);
  public static final double INFTY2  = ((double) 1.0e+19);
  public static final double INVINF  = ((double) 1.0e-38);
  public static final double INVINF2 = ((double) 1.0e-19);
  public static final double LTPI    = 1.83787706640935;    /* log(2*PI) */
	

  private PStream mceppst;
  private PStream strpst;
  private PStream magpst;
  private PStream lf0pst;
  private boolean voiced[];
  
  public double get_mcep(int i, int j){ return mceppst.get_par(i, j); }
  public int get_mcep_order(){ return mceppst.get_order(); }
  public int get_mcep_T(){ return mceppst.get_T(); }
  
  
  public double get_str(int i, int j){ return strpst.get_par(i, j); }
  public int get_str_order(){ return strpst.get_order(); }
  
  public double get_mag(int i, int j){ return magpst.get_par(i, j); }
  public int get_mag_order(){ return magpst.get_order(); }
  
  public double get_lf0(int i, int j){ return lf0pst.get_par(i, j); }
  public int get_lf0_order(){ return lf0pst.get_order(); }
  
  public boolean get_voiced(int i){ return voiced[i]; }
	
  /* Inverse of a given double */
  /* We actually need the inverse of the matrix of covariance, but since this matrix */ 
  /* is a diagonal matrix, then we just need to calculate the inverse of each of the  */
  /* numbers in the diagonal. */
  private double finv(double x) {
	  
	if( x >= INFTY2 ) return 0.0;
	if( x <= -INFTY2 ) return 0.0;
	if( x <= INVINF2 && x >= 0 ) return INFTY;
	if( x >= -INVINF2 && x < 0 ) return -INFTY;
	
	return ( 1.0 / x );
	  
  }
  
  /** HTS maximum likelihood parameter generation
  * @param um  : utterance model sequence after processing Mary context features
  * @param ms  : HMM pdfs model set.
  */
  public void HTS_MaximumLikelihoodParameterGeneration(UttModel um, HMMData hts_data){
	  
	int frame, Tframe, lf0frame;
	int state, lw, rw, k, n, i;
	boolean nobound;
	Model m;
    ModelSet ms = hts_data.getModelSet();
    
	/* Initialisation of PStream objects */
  	/* Initialise Parameter generation using UttModel um and Modelset ms */
  	/* initialise PStream objects for all the parameters that are going to be generated: */
  	/* mceppst, strpst, magpst, lf0pst */
	/* Here i should pass the window files to initialise the dynamic windows dw */
	/* for the moment the dw are all the same and hard-coded */
	mceppst = new PStream(ms.get_mcepvsize(), um.get_totalframe());
	strpst  = new PStream(ms.get_strvsize(), um.get_totalframe());
	magpst  = new PStream(ms.get_strvsize(), um.get_totalframe());
	/* for lf0 count just the number of lf0frames that are voiced or non-zero */
	lf0pst  = new PStream(ms.get_lf0stream(), um.get_lf0frame());   
	
	Tframe = lf0frame = 0;
	voiced = new boolean[um.get_totalframe()];
	
	for(i=0; i<um.getNumUttModel(); i++){
        m = um.getUttModel(i);          		
        for(state=0; state<ms.get_nstate(); state++)
      	 for(frame=0; frame<m.get_dur(state); frame++) {
      		voiced[Tframe] = m.get_voiced(state);
      		Tframe++;
      		if(m.get_voiced(state))
      		  lf0frame++;
      	 }
    }
	/* mcepframe and lf0frame are used in the original code to initialise the T field */
	/* in each pst, but here the pst are already initialised .... */
	System.out.println("ParGen: Tframe=" + Tframe + " lf0frame=" + lf0frame);
	
	
	Tframe = 0;
	lf0frame = 0;
	/* copy pdfs */
	for(i=0; i<um.getNumUttModel(); i++){
      m = um.getUttModel(i);          		
      for(state=0; state<ms.get_nstate(); state++) {
    	  
        //System.out.println("m.get_dur(state="+ state + ")=" + m.get_dur(state));
        
      	for(frame=0; frame<m.get_dur(state); frame++) {
             
      	  /* copy pdfs for mcep */
      	  for(k=0; k<ms.get_mcepvsize(); k++){
      		mceppst.set_mseq(Tframe, k, m.get_mcepmean(state, k));
      		mceppst.set_ivseq(Tframe, k, finv(m.get_mcepvariance(state, k)));
      	  }
      	  
      	  /* copy pdf for str */
      	  for(k=0; k<ms.get_strvsize(); k++){
      		strpst.set_mseq(Tframe, k, m.get_strmean(state, k));
      		strpst.set_ivseq(Tframe, k, finv(m.get_strvariance(state, k)));
      	  }
      	  
      	  /* copy pdf for mag */
      	  for(k=0; k<ms.get_magvsize(); k++){
      		magpst.set_mseq(Tframe, k, m.get_magmean(state, k));
      		magpst.set_ivseq(Tframe, k, finv(m.get_magvariance(state, k)));
    	  }
      	  
      	  /* copy pdfs for lf0 */ 
      	  for(k=0; k<ms.get_lf0stream(); k++){
      		lw = lf0pst.get_dw_width(k, PStream.WLEFT);
      		rw = lf0pst.get_dw_width(k, PStream.WRIGHT);
      		nobound = true;
      		/* check if current frame is voiced/unvoiced boundary or not */
      		for(n=lw; n<=rw; n++)
      		  if( (Tframe+n) <= 0 || um.get_totalframe() < (Tframe+n))
      			 nobound = false;
      		  else
      			 nobound = ( nobound && voiced[Tframe+n] );
      		/* copy pdfs */
      		if( voiced[Tframe] ) {
      		  lf0pst.set_mseq(lf0frame, k, m.get_lf0mean(state, k));
      		  if( nobound || k==0 )
      			lf0pst.set_ivseq(lf0frame, k, finv(m.get_lf0variance(state, k)));
      		  else  /* the variances for dynamic feature are set to inf on v/uv boundary */
      			lf0pst.set_ivseq(lf0frame, k, 0.0);
      		}
      		
    	  }
      	  
      	  if( voiced[Tframe] )
             lf0frame++;      	  
      	  Tframe++;
      	  
      	} /* for each frame in this state */
      } /* for each state in this model */
	}  /* for each model in this utterance */ 
	
	System.out.println("After copying pdfs to PStreams Tframe=" + Tframe + " lf0frame=" + lf0frame);
	//System.out.println("mseq[" + Tframe + "][" + k + "]=" + mceppst.get_mseq(Tframe, k) + "   " + m.get_mcepmean(state, k));
		
	/* parameter generation for mcep */
	System.out.println("Parameter generation for mlpg: ");
	mceppst.mlpg();

	/* parameter generation for str */
	System.out.println("Parameter generation for str: ");
	strpst.mlpg();

	/* parameter generation for mag */
	System.out.println("Parameter generation for mag: ");
	magpst.mlpg();
	   
	/* parameter generation for lf0 */
	System.out.println("Parameter generation for lf0: ");
	if (lf0frame>0)
	  lf0pst.mlpg();

	  
  }  /* method HTS_mlpg */
  
  
  
} /* class ParameterGeneration */
