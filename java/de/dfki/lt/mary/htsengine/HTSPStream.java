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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import org.apache.log4j.Logger;


/**
 * Data type and procedures used in parameter generation.
 * Contains means and variances of a particular model, 
 * mcep pdfs for a particular phoneme for example.
 * It also contains auxiliar matrices used in maximum likelihood 
 * parameter generation.
 * 
 * Java port and extension of HTS engine version 2.0 and GV from HTS version 2.1alpha.
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSPStream {
	
  public static final int WLEFT = 0;
  public static final int WRIGHT = 1;	
  
  private int feaType;     /* type of features it contains */  
  private int vSize;       /* vector size of observation vector (include static and dynamic features) */
  private int order;       /* vector size of static features */
  private int nT;          /* length, number of frames in utterance */
  private int width;       /* width of dynamic window */
  
  private double par[][];  /* output parameter vector, the size of this parameter is par[nT][vSize] */
  
  
  /* ____________________Matrices for parameter generation____________________ */
  private double mseq[][];   /* sequence of mean vector */
  private double ivseq[][];  /* sequence of inversed variance vector */
  private double g[];        /* for forward substitution */
  private double wuw[][];    /* W' U^-1 W  */
  private double wum[];      /* W' U^-1 mu */
  
  /* ____________________Dynamic window ____________________ */
  private HTSDWin dw;       /* Windows used to calculate dynamic features, delta and delta-delta */

  
  /* ____________________ GV related variables ____________________*/
  /* GV: Global mean and covariance (diagonal covariance only) */
  private double mean, var;  /* mean and variance for current utt eqs: (16), (17)*/
  private int maxGVIter     = 200;      /* max iterations in the speech parameter generation considering GV */
  private double GVepsilon  = 1.0E-4;  /* convergence factor for GV iteration */
  private double minEucNorm = 1.0E-2;  /* minimum Euclid norm of a gradient vector */ 
  private double stepInit   = 1.0;     /* initial step size */
  private double stepDec    = 0.5;     /* step size deceralation factor */
  private double stepInc    = 1.2;     /* step size acceleration factor */
  private double w1         = 1.0;     /* weight for HMM output prob. */
  private double w2         = 1.0;     /* weight for GV output prob. */
  double norm=0.0, GVobj=0.0, HMMobj=0.0;
 
  private Logger logger = Logger.getLogger("PStream");
  
  /* Constructor */
  public HTSPStream(int vector_size, int utt_length, int fea_type) throws Exception {
	/* In the c code for each PStream there is an InitDwin() and an InitPStream() */ 
	/* - InitDwin reads the window files passed as parameters for example: mcp.win1, mcp.win2, mcp.win3 */
	/*   for the moment the dynamic window is the same for all MCP, LF0, STR and MAG  */
	/*   The initialisation of the dynamic window is done with the constructor. */
	dw = new HTSDWin();
    feaType = fea_type; 
    vSize = vector_size;
    order = vector_size / dw.getNum(); 
    nT = utt_length;
    width = 3;            /* hard-coded to 3, in the c code is:  pst->width = pst->dw.max_L*2+1;  */
                          /* pst->dw.max_L is hard-code to 1, for all windows                     */
    par = new double[nT][order];
    
    /* ___________________________Matrices initialisation___________________ */
	mseq = new double[nT][vSize];
	ivseq = new double[nT][vSize];
	g = new double[nT];
	wuw = new double[nT][width];
	wum = new double[nT];   
    
  }

  public void setOrder(int val){ order=val; }
  public int getOrder(){ return order; }
  
  public double getPar(int i, int j){ return par[i][j]; }
  public int getT(){ return nT; }
  
  public void setMseq(int i, int j, double val){ mseq[i][j]=val; }
  public double getMseq(int i, int j){ return mseq[i][j]; }
  
  public void setIvseq(int i, int j, double val){ ivseq[i][j]=val; }
  public double getIvseq(int i, int j){ return ivseq[i][j]; }
  
  public void setG(int i, double val){ g[i]=val; }
  public double getG(int i){ return g[i]; }
  
  public void setWUW(int i, int j, double val){ wuw[i][j]=val; }
  public double getWUW(int i, int j){ return wuw[i][j]; }
  
  public void setWUM(int i, double val){ wum[i]=val; }
  public double getWUM(int i){ return wum[i]; }
  
  public int getDWwidth(int i, int j){ return dw.getWidth(i,j); }
  
  private void printWUW(int t){
	for(int i=0; i<width; i++)
	  System.out.print("WUW[" + t + "][" + i + "]=" + wuw[t][i] + "  ");
	System.out.println(""); 
  }
  
  
  /* mlpg: generate sequence of speech parameter vector maximizing its output probability for 
   * given pdf sequence */
  public void mlpg(HMMData htsData) {
	 int m,t;
	 int M = order;
	 boolean debug=false;

     if( htsData.getUseGV() && (feaType == HMMData.MCP || feaType == HMMData.LF0 ) )
         logger.info("Generation using Global Variance");
     
	 for (m=0; m<M; m++) {
	   calcWUWandWUM( m , debug);
	   ldlFactorization(debug);   /* LDL factorization                               */
	   forwardSubstitution();     /* forward substitution in Cholesky decomposition  */
	   backwardSubstitution(m);   /* backward substitution in Cholesky decomposition */
              
       if( htsData.getUseGV() && feaType == HMMData.MCP ) {
         logger.info("Optimization MCP feature: ("+ m + ")");    
         gvParmGen(m, htsData.getGVModelSet().getGVmeanMcp(), htsData.getGVModelSet().getGVcovInvMcp());  
       } else if ( htsData.getUseGV() && feaType == HMMData.LF0 ) {
           logger.info("Optimization LF0 feature: ("+ m + ")");    
           gvParmGen(m, htsData.getGVModelSet().getGVmeanLf0(), htsData.getGVModelSet().getGVcovInvLf0());           
       } else if ( false && feaType == HMMData.STR ) {
           logger.info("Optimization STR feature: ("+ m + ")");    
           gvParmGen(m, htsData.getGVModelSet().getGVmeanStr(), htsData.getGVModelSet().getGVcovInvStr());           
       } else if ( false && feaType == HMMData.MAG ) {
           logger.info("Optimization MAG feature: ("+ m + ")");    
           gvParmGen(m, htsData.getGVModelSet().getGVmeanMag(), htsData.getGVModelSet().getGVcovInvMag());           
       }
        
       
	 } 
	 if(debug) {
	   for(m=0; m<M; m++){
	     for(t=0; t<4; t++)
		   System.out.print("par[" + t + "][" + m + "]=" + par[t][m] + " ");
	     System.out.println();
	   }
	   System.out.println();
	 }
     
	 if(debug) {
	     if( htsData.getUseGV() && feaType == HMMData.MCP )
	         saveParam("/project/mary/marcela/gen-test/mcp-gv.bin");  
	     else if ( !htsData.getUseGV() && feaType == HMMData.MCP ) 
	         saveParam("/project/mary/marcela/gen-test/mcp.bin");

	     if( htsData.getUseGV() && feaType == HMMData.LF0 )
	         saveParam("/project/mary/marcela/gen-test/lf0-gv.bin");  
	     else if ( !htsData.getUseGV() && feaType == HMMData.LF0 ) 
	         saveParam("/project/mary/marcela/gen-test/lf0e.bin");
	 }
	 
  }  /* method mlpg */
  
  
  /*----------------- HTS parameter generation fuctions  -----------------------------*/
  
  /* Calc_WUW_and_WUM: calculate W'U^{-1}W and W'U^{-1}M      
  * W is size W[T][width] , width is width of dynamic window 
  * for the Cholesky decomposition:  A'Ax = A'b              
  * W'U^{-1}W C = W'U^{-1}M                                  
  *        A  C = B   where A = LL'                          
  *  Ly = B , solve for y using forward elimination          
  * L'C = y , solve for C using backward substitution        
  * So having A and B we can find the parameters C.          
  * U^{-1} = inverse covariance : inseq[][]                  */
  /*------ HTS parameter generation fuctions                  */
  /* Calc_WUW_and_WUM: calculate W'U^{-1}W and W'U^{-1}M      */
  /* W is size W[T][width] , width is width of dynamic window */
  /* for the Cholesky decomposition:  A'Ax = A'b              */
  /* W'U^{-1}W C = W'U^{-1}M                                  */
  /*        A  C = B   where A = LL'                          */
  /*  Ly = B , solve for y using forward elimination          */
  /* L'C = y , solve for C using backward substitution        */
  /* So having A and B we can find the parameters C.          */
  /* U^{-1} = inverse covariance : inseq[][]                  */
  private void calcWUWandWUM(int m, boolean debug) {
	int t, i, j, k,iorder;
	double WU;
	double val;
	
	for(t=0; t<nT; t++) {
	  /* initialise */
	  wum[t] = 0.0;
	  for(i=0; i<width; i++)
		wuw[t][i] = 0.0;
	  
	  /* calc WUW & WUM, U is already inverse  */
	    for(i=0; i<dw.getNum(); i++) {
	      if(debug)
	        System.out.println("WIN: " + i);
	      iorder = i*order+m;
	      for( j = dw.getWidth(i, WLEFT); j <= dw.getWidth(i, WRIGHT); j++) {
	    	 if(debug) 
	    	   System.out.println("  j=" + j + " t+j=" + (t+j) + " iorder=" + iorder + " coef["+i+"]["+(-j)+"]="+dw.getCoef(i,-j));
			 if( ( t+j>=0 ) && ( t+j<nT ) && ( dw.getCoef(i,-j)!=0.0 )  ) {
				 WU = dw.getCoef(i,-j) * ivseq[t+j][iorder];
				 if(debug)
				   System.out.println("  WU = coef[" + i +"][" + j + "] * ivseq[" + (t+j) + "][" + iorder + "]"); 
				 wum[t] += WU * mseq[t+j][iorder];
				 if(debug)
				   System.out.println("  WUM[" + t + "] += WU * mseq[" + t+j + "][" + iorder + "]   WU*mseq=" + WU * mseq[t+j][iorder]); 
				 
				 for(k=0; ( k<width ) && ( t+k<nT ); k++)
				   if( ( k-j<=dw.getWidth(i, 1) ) && ( dw.getCoef(i,(k-j)) != 0.0 ) ) {
				     wuw[t][k] += WU * dw.getCoef(i,(k-j));
				     val = WU * dw.getCoef(i,(k-j));
				     if(debug)
				       System.out.println("  WUW[" + t + "][" + k + "] += WU * coef[" + i + "][" + (k-j) + "]   WU*coef=" + val);
				   }
			  }
		  }		  
	    }  /* for i */
	    if(debug) {
	      System.out.println("------------\nWUM[" + t + "]=" + wum[t]); 
	      printWUW(t);
	      System.out.println("------------\n");
	    }
	    
	}  /* for t */
	  
  }
  
  
  /* ldlFactorization: Factorize W'*U^{-1}*W to L*D*L' (L: lower triangular, D: diagonal) */
  /* ldlFactorization: Factorize W'*U^{-1}*W to L*D*L' (L: lower triangular, D: diagonal) */
  private void ldlFactorization(boolean debug) {
	int t,i,j;
	for(t=0; t<nT; t++) {
		
	  if(debug){
	    System.out.println("WUW calculation:");
	    printWUW(t);
	  }
	  
	  /* I need i=1 for the delay in t, but the indexes i in WUW[t][i] go from 0 to 2 
	   * so wherever i is used as index i=i-1  (this is just to keep somehow the original 
	   * c implementation). */
	  for(i=1; (i<width) && (t-i>=0); i++)  
		wuw[t][0] -= wuw[t-i][i+1-1] * wuw[t-i][i+1-1] * wuw[t-i][0];
	  
	  for(i=2; i<=width; i++) {
	    for(j=1; (i+j<=width) && (t-j>=0); j++)
		  wuw[t][i-1] -= wuw[t-j][j+1-1] * wuw[t-j][i+j-1] * wuw[t-j][0];
	    wuw[t][i-1] /= wuw[t][0];
	 
	  }
	  if(debug) {
	    System.out.println("LDL factorization:");
	    printWUW(t);	
	    System.out.println();
	  }
	}
	
  }
  
  /* forward_Substitution */ 
  private void forwardSubstitution() {
	 int t, i;
	 
	 for(t=0; t<nT; t++) {
	   g[t] = wum[t];
	   for(i=1; (i<width) && (t-i>=0); i++)
		 g[t] -= wuw[t-i][i+1-1] * g[t-i];  /* i as index should be i-1 */
	   //System.out.println("  g[" + t + "]=" + g[t]);
	 }
	 
  }
  
  /* backward_Substitution */
  private void backwardSubstitution(int m) {
	 int t, i;
	 
	 for(t=(nT-1); t>=0; t--) {
	   par[t][m] = g[t] / wuw[t][0];
	   for(i=1; (i<width) && (t+i<nT); i++) {
		   par[t][m] -= wuw[t][i+1-1] * par[t+i][m]; /* i as index should be i-1 */
	   }
	   //System.out.println("  par[" + t + "]["+ m + "]=" + par[t][m]); 
	 }
	  
  }

  
  /*----------------- GV functions  -----------------------------*/
  
  private void gvParmGen(int m, double gvmean[], double gvcovInv[]){
      
    int t,iter;
    double step=stepInit;
    double obj=0.0, prev=0.0;
    double diag[] = new double[nT];
    double par_ori[] = new double[nT];
    mean=0.0;
    var=0.0;
    int numDown = 0;
    
    for(t=0; t<nT; t++){
      g[t] = 0.0;
      par_ori[t] = par[t][m];  
    }
    
    /* first convert c (c=par) according to GV pdf and use it as the initial value */
    convGV(m, gvmean, gvcovInv);
    
    /* recalculate R=WUW and r=WUM */
    calcWUWandWUM(m, false);
    
    /* iteratively optimize c */
    for (iter=1; iter<=maxGVIter; iter++) {
      /* calculate GV objective and its derivative with respect to c */
      obj = calcGradient(m, gvmean, gvcovInv);
    
      /* accelerate/decelerate step size */
      if(iter > 1) {
  
        /* objective function improved -> increase step size */
        if (obj > prev){
          step *= stepInc;
          //logger.info("+++ obj > prev iter=" + iter +"  obj=" + obj + "  > prev=" + prev);
          numDown = 0;
        }
        
        /* objective function degraded -> go back c and decrese step size */
        if (obj < prev) {
           for (t=0; t<nT; t++)  /* go back c=par to that at the previous iteration */
              par[t][m] -= step * diag[t];
           step *= stepDec;
           for (t=0; t<nT; t++)  /* gradient c */
              par[t][m] += step * diag[t];
           iter--;
           numDown++;
           //logger.info("--- obj < prev iter=" + iter +"  obj=" + obj + "  < prev=" + prev +"  numDown=" + numDown);
           if(numDown < 100)
            continue;
           else {
             logger.info("  ***Convergence problems....optimization stopped. Number of iterations: " + iter );
             break;
           }
        }
          
      } else
       logger.info("  First iteration:  GVobj=" + obj + " (HMMobj=" + HMMobj + "  GVobj=" + GVobj + ")");
      
      /* convergence check (Euclid norm, objective function) */
      if(norm < minEucNorm || (iter > 1 && Math.abs(obj-prev) < GVepsilon )){
        logger.info("  Number of iterations: [   " + iter + "   ] GVobj=" + obj + " (HMMobj=" + HMMobj + "  GVobj=" + GVobj + ")");
        if(iter > 1 )
            logger.info("  Converged (norm=" + norm + ", change=" + Math.abs(obj-prev) + ")");
        else
            logger.info("  Converged (norm=" + norm + ")");
        break;
      }
      
      /* steepest ascent and quasy Newton  c(i+1) = c(i) + alpha * grad(c(i)) */
      for(t=0; t<nT; t++){
        par[t][m] += step * g[t];
        diag[t] = g[t];
      }
      prev = obj;       
    }
    
    if( iter>maxGVIter ){
      logger.info("");  
      logger.info("  Number of iterations: " + maxGVIter + " GVobj=" + obj + " (HMMobj=" + HMMobj + "  GVobj=" + GVobj + ")");
      logger.info("  Optimization stopped by reaching max number of iterations = " + maxGVIter);
      logger.info("");

      /* If there it does not converge, the feature parameter is not optimized */
      for(t=0; t<nT; t++){
        par[t][m] = par_ori[t];  
      } 
      
    }
      
  }
  
  private double calcGradient(int m, double gvmean[], double gvcovInv[]){
   int t, i; 
   double vd, h;
   double w = 1.0 / (dw.getNum() * nT);
   
   /* recalculate GV of the current c = par */
   calcGV(m);   
   
   /* GV objective function and its derivative with respect to c */
   /* -1/2 * v(c)' U^-1 v(c) + v(c)' U^-1 mu + K  --> second part of eq (20) in Toda and Tokuda IEICE-2007 paper.*/
   GVobj = -0.5 * w2 * (var - gvmean[m]) * gvcovInv[m] * (var - gvmean[m]);
   vd = gvcovInv[m] * (var - gvmean[m]);
   
     
   /* calculate g = R*c = WUW*c*/
   for(t=0; t<nT; t++) {
     g[t] = wuw[t][0] * par[t][m];
     for(i=2; i<=width; i++){   /* width goes from 0 to 2  width=3 */
       if( t+i-1 < nT)
         g[t] += wuw[t][i-1] * par[t+i-1][m];      /* i as index should be i-1 */
       if( t-i+1 >= 0 )
         g[t] += wuw[t-i+1][i-1] * par[t-i+1][m];  /* i as index should be i-1 */
     }
     
   }
   
   
   for(t=0, HMMobj=0.0, norm=0.0; t<nT; t++) {
       
     HMMobj += -0.5 * w1 * w * par[t][m] * (g[t] - 2.0 * wum[t]); 
       
     /* case STEEPEST: do not use hessian */
     //h = 1.0;
     /* case NEWTON */
     /* only diagonal elements of Hessian matrix are used */
     h = -w1 * w * wuw[t][1-1] - w2 * 2.0 / (nT*nT) * ( ( nT-1) * vd + 2.0 * gvcovInv[m] * (par[t][m] - mean) * (par[t][m] - mean) );
     
     h = -1.0/h;
       
     /* gradient vector */
     g[t] = h * ( w1 * w *(-g[t] + wum[t]) + w2 * (-2.0/nT * (par[t][m] - mean ) * vd ) ); 
     
     /*  Euclidian norm of gradient vector */  
     norm += g[t]*g[t];
       
   }
   
   
   norm = Math.sqrt(norm);
   //logger.info("HMMobj=" + HMMobj + "  GVobj=" + GVobj + "  norm=" + norm);
   
   return(HMMobj+GVobj);  
   
  }
  
  private void convGV(int m, double gvmean[], double gvcovInv[]){
    int t;
    double ratio; 
    /* calculate GV of c */
    calcGV(m);
       
    /* ratio between GV mean and variance of c */
    ratio = Math.sqrt(gvmean[m] / var);
   
    /* c'[t][d] = ratio * (c[t][d]-mean[d]) + mean[d]  eq. (34) in Toda and Tokuda IEICE-2007 paper. */
    
    for(t=0; t<nT; t++){
     par[t][m] = ratio * ( par[t][m]-mean ) + mean;
    }
      
  }
  
  private void calcGV(int m){
    int t, i;
    mean=0.0;
    var=0.0;
 
    /* mean */
    for(t=0; t<nT; t++)
      mean += par[t][m];
    mean = mean / nT;
      
    /* variance */  
    for(t=0; t<nT; t++)
      var += (par[t][m] - mean) * (par[t][m] - mean);
    var = var / nT;
      
  }
  
  /* Save generated parameters in a binary file */
  public void saveParam(String fileName){
    int t, m;
    try{  
      DataOutputStream data_out = new DataOutputStream (new FileOutputStream (fileName));
      
      for(t=0; t<nT; t++)
        for (m=0; m<order; m++)
          data_out.writeFloat((float)par[t][m]);
      
      data_out.close();
      
      System.out.println("saveParam in file: " + fileName);
      
    } catch (IOException e) {
       System.out.println ("IO exception = " + e );
    }
      
      
  }
  

} /* class PStream */
