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

import marytts.util.MaryUtils;
import java.util.Arrays;

import org.apache.log4j.Logger;


/**
 * Data type and procedures used in parameter generation.
 * Contains means and variances of a particular model, 
 * mcep pdfs for a particular phone for example.
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
  
  /** type of features it contains */
  public final HMMData.FeatureType feaType;
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
  /** convergence factor for GV iteration */
  private final static double GVepsilon  = 1.0E-4;  //1.0E-4;  
  /** minimum Euclid norm of a gradient vector */ 
  private final static double minEucNorm = 1.0E-2;  //1.0E-2;  
  /** initial step size */
  private final static double stepInit   = 0.1;
  /** step size deceralation factor */
  private final static double stepDec    = 0.5;     
  /** step size acceleration factor */
  private final static double stepInc    = 1.2;     
  /** weight for HMM output prob. */
  private final static double w1         = 1.0;     
  /** weight for GV output prob. */
  private final static double w2         = 1.0;     
  /** ~log(0) */
  private final static double lzero      = (-1.0e+10);  
  private double norm       = 0.0; 
  private double GVobj      = 0.0;
  private double HMMobj     = 0.0;
  private double gvmean[];
  private double gvcovInv[];
  /** GV flag sequence, to consider or not the frame in gv */
  private boolean gvSwitch[];          
  /** this will be the number of frames for which gv can be calculated */
  private int gvLength;                

 
  private Logger logger = MaryUtils.getLogger("PStream");
  
  /* Constructor */
  public HTSPStream(int vector_size, int utt_length, HMMData.FeatureType fea_type, int maxIterationsGV) throws Exception {
	/* In the c code for each PStream there is an InitDwin() and an InitPStream() */ 
	/* - InitDwin reads the window files passed as parameters for example: mcp.win1, mcp.win2, mcp.win3 */
	/*   for the moment the dynamic window is the same for all MCP, LF0, STR and MAG  */
	/*   The initialisation of the dynamic window is done with the constructor. */
	dw = new HTSDWin();
    feaType = fea_type; 
    vSize = vector_size;
    order = vector_size / dw.getNum(); 
    nT = utt_length;
    maxGVIter = maxIterationsGV;
    width = 3;            /* hard-coded to 3, in the c code is:  pst->width = pst->dw.max_L*2+1;  */
                          /* pst->dw.max_L is hard-code to 1, for all windows                     */
    par = new double[nT][order];
    
    /* ___________________________Matrices initialisation___________________ */
	mseq = new double[nT][vSize];
	ivseq = new double[nT][vSize];
	g = new double[nT];
	wuw = new double[nT][width];
	wum = new double[nT];   
	
	/* GV Switch sequence initialisation */
	gvSwitch = new boolean[nT];
	for(int i=0; i<nT; i++)
	  gvSwitch[i] = true;  
	gvLength = nT;  /* at initialisation, all the frames can be used for gv */
    
  }

  public void setVsize(int val){ vSize=val; }
  public int getVsize(){ return vSize; }
  
  public void setOrder(int val){ order=val; }
  public int getOrder(){ return order; }
  
  public void setPar(int i, int j, double val){ par[i][j] = val; }
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
  
  public void setGvMeanVar(double[] mean, double[] ivar){
      gvmean = mean;
      gvcovInv = ivar;
  }
  
  public void setGvSwitch(int i, boolean bv){
    if(bv == false)
      gvLength--;
    gvSwitch[i] = bv;
  }
  
  private void printWUW(int t){
	for(int i=0; i<width; i++)
	  System.out.print("WUW[" + t + "][" + i + "]=" + wuw[t][i] + "  ");
	System.out.println(""); 
  }
  
  
  /* mlpg: generate sequence of speech parameter vector maximizing its output probability for 
   * given pdf sequence */
  public void mlpg(HMMData htsData, boolean useGV) {
	 int m;
	 int M = order;
	 boolean debug=false;
  
     if(htsData.getUseContextDependentGV())
       logger.info("Context-dependent global variance optimization: gvLength = "+ gvLength );
     else
       logger.info("Global variance optimization");
     
	 for (m=0; m<M; m++) {
	   calcWUWandWUM( m , debug);
	   ldlFactorization(debug);   /* LDL factorization                               */
	   forwardSubstitution();     /* forward substitution in Cholesky decomposition  */
	   backwardSubstitution(m);   /* backward substitution in Cholesky decomposition */
	          

       /* Global variance optimisation for MCP and LF0 */
       if( useGV && gvLength>0) {           
        if(htsData.getGvMethodGradient())
          gvParmGenGradient(m, debug);      // this is the previous method we have in MARY, using the Gradient as in the Paper of Toda et. al. IEICE 2007
                                           // if using this method the variances have to be inverse (see note in GVModel set: case NEWTON in gv optimization)
                                           // this method seems to give a better result
        else
          gvParmGenDerivative(m, debug);  // this is the method in the hts_engine 1.04 the variances are not inverse   

         
       }
	 }  
  }  /* method mlpg */
  
  
  /*----------------- HTS parameter generation fuctions  -----------------------------*/
  
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
	      iorder = i*order+m;
	      for( j = dw.getWidth(i, WLEFT); j <= dw.getWidth(i, WRIGHT); j++) {

	          if( ( t+j>=0 ) && ( t+j<nT ) && ( dw.getCoef(i,-j)!=0.0 )  ) {
	             
	             //System.out.format("coef[%d,%d]=%f  ivseq[%d][%d]=%f  ", i, -j, dw.getCoef(i,-j),t+j,iorder, ivseq[t+j][iorder]); 
	              
				 WU = dw.getCoef(i,-j) * ivseq[t+j][iorder];
				 
				 wum[t] += WU * mseq[t+j][iorder];
				 
				 //System.out.format("wu=%f wum[%d]=%f iorder=%d\n", WU, t, wum[t], iorder);
				 
				 for(k=0; ( k<width ) && ( t+k<nT ); k++)
				   if( ( k-j<=dw.getWidth(i, 1) ) && ( dw.getCoef(i,(k-j)) != 0.0 ) ) {
				     wuw[t][k] += WU * dw.getCoef(i,(k-j));
				     val = WU * dw.getCoef(i,(k-j));
				   }
			  }
		  }		  
	    }  /* for i */	    
	}  /* for t */
	if(debug){ 
	for(t=0; t<nT; t++) {
	  System.out.format("t=%d wum=%f  wuw:", t, wum[t]); 
      for(k=0; k<wuw[t].length; k++)
        System.out.format("%f ", wuw[t][k]);
      System.out.format("\n");
	}
	System.out.format("\n");
	}
  }
  
  
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
	 /*
	 for(t=0; t<nT; t++)
	   System.out.format("%f ", g[t]);
	 System.out.println(); */
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
  private void gvParmGenDerivative(int m, boolean debug){    
    int t,iter;
    double step = stepInit;
    double prev = -lzero;
    double obj=0.0;
    double diag[] = new double[nT];
    double par_ori[] = new double[nT];
    mean=0.0;
    var=0.0;
    int numDown = 0;
    
    /* make a copy in case there is problems during optimisation */
    for(t=0; t<nT; t++){
      g[t] = 0.0;
      par_ori[t] = par[t][m];  
    }
       
    /* first convert c (c=par) according to GV pdf and use it as the initial value */
    convGV(m);
    
    /* recalculate R=WUW and r=WUM */
    calcWUWandWUM(m, false);
    
    /* iteratively optimize c */
    for (iter=1; iter<=maxGVIter; iter++) {
      /* calculate GV objective and its derivative with respect to c */
      obj = calcDerivative(m);   
      
      /* objective function improved -> increase step size */
      if (obj > prev)
        step *= stepDec;
               
      /* objective function degraded -> go back c and decrese step size */
      if (obj < prev) 
         step *= stepInc;
        
      /* steepest ascent and quasy Newton  c(i+1) = c(i) + alpha * grad(c(i)) */
      for(t=0; t<nT; t++)
        par[t][m] += step * g[t];
      
      //System.out.format("iter=%d  prev=%f  obj=%f \n", iter, prev, obj);
      prev = obj;
    }
    logger.info("Derivative GV optimization for feature: ("+ m + ")  number of iterations=" + (iter-1) );

 }

  
  private void gvParmGenGradient(int m, boolean debug){    
      int t,iter;
      double step=stepInit;
      double obj=0.0, prev=0.0;
      double diag[] = new double[nT];
      double par_ori[] = new double[nT];
      mean=0.0;
      var=0.0;
      int numDown = 0;
      int totalNumIter = 0;
      int firstIter = 0;
      
      /* make a copy in case there is problems during optimisation */
      for(t=0; t<nT; t++){
        g[t] = 0.0;
        par_ori[t] = par[t][m];  
      }
              
      /* first convert c (c=par) according to GV pdf and use it as the initial value */
      convGV(m);
      
      /* recalculate R=WUW and r=WUM */
      calcWUWandWUM(m, false);
      
      /* iteratively optimize c */
      for (iter=1; iter<=maxGVIter; iter++) {
        /* calculate GV objective and its derivative with respect to c */
        obj = calcGradient(m);   
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
        } else {
         if(debug)   
           logger.info("  First iteration:  GVobj=" + obj + " (HMMobj=" + HMMobj + "  GVobj=" + GVobj + ")");
        }     
        /* convergence check (Euclid norm, objective function) */
        if(norm < minEucNorm || (iter > 1 && Math.abs(obj-prev) < GVepsilon )){
          if(debug)  
            logger.info("  Number of iterations: [   " + iter + "   ] GVobj=" + obj + " (HMMobj=" + HMMobj + "  GVobj=" + GVobj + ")");
          totalNumIter++; //gv.incTotalNumIter(iter);
          if(m==0)
            firstIter = iter;//gv.setFirstIter(iter);
          if(debug){
            if(iter > 1 )  
              logger.info("  Converged (norm=" + norm + ", change=" + Math.abs(obj-prev) + ")");
            else            
              logger.info("  Converged (norm=" + norm + ")");
          }
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
        logger.info("   optimization stopped by reaching max number of iterations (no global variance applied)");

        /* If there it does not converge, the feature parameter is not optimized */
        for(t=0; t<nT; t++){
          par[t][m] = par_ori[t];  
        }      
      }
      totalNumIter = iter; 
      
      logger.info("Gradient GV optimization for feature: ("+ m + ")  number of iterations=" + totalNumIter);
    }
 
  
  private double calcGradient(int m){
   int t, i,k; 
   double vd;
   double h, aux;
   double w = 1.0 / (dw.getNum() * nT);
   
   /* recalculate GV of the current c = par */
   calcGV(m);   
   
   /* GV objective function and its derivative with respect to c */
   /* -1/2 * v(c)' U^-1 v(c) + v(c)' U^-1 mu + K  --> second part of eq (20) in Toda and Tokuda IEICE-2007 paper.*/
   GVobj =  -0.5 * w2 * (var - gvmean[m]) * gvcovInv[m] * (var - gvmean[m]);
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
     h = ( ( nT-1) * vd + 2.0 * gvcovInv[m] * (par[t][m] - mean) * (par[t][m] - mean) );
     h = -w1 * w * wuw[t][1-1] - w2 * 2.0 / (nT*nT) * h;
     
     h = -1.0/h;
       
     /* gradient vector */
     if(gvSwitch[t]) {
       aux = (par[t][m] - mean ) * vd;        
       g[t] = h * ( w1 * w *(-g[t] + wum[t]) + w2 * -2.0/nT * aux );
     } else 
       g[t] = h * ( w1 * w *(-g[t] + wum[t]) );  
     
     /*  Euclidian norm of gradient vector */  
     norm += g[t]*g[t];
       
   }
     
   norm = Math.sqrt(norm);
   //logger.info("HMMobj=" + HMMobj + "  GVobj=" + GVobj + "  norm=" + norm);
   
   return(HMMobj+GVobj);  
   
  }

  private double calcDerivative(int m){
      int t, i,k; 
      double vd;
      double h, aux;
      double w = 1.0 / (dw.getNum() * nT);
      
      /* recalculate GV of the current c = par */
      calcGV(m);   
      
      /* GV objective function and its derivative with respect to c */
      /* -1/2 * v(c)' U^-1 v(c) + v(c)' U^-1 mu + K  --> second part of eq (20) in Toda and Tokuda IEICE-2007 paper.*/
      GVobj =  -0.5 * w2 * var * gvcovInv[m] * (var - 2.0 * gvmean[m]);
      vd = -2.0 * gvcovInv[m] * (var - gvmean[m])/nT;
      //System.out.format("GVobj=%f  vd=%f \n", GVobj, vd);  
      
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
         
      for(t=0, HMMobj=0.0; t<nT; t++) {
          
        HMMobj += w1 * w * par[t][m] * (wum[t] - 0.5 * g[t]); 
 
        h = -w1 * w * wuw[t][1-1] - w2 * 2.0 / (nT*nT) * ( (nT-1) * gvcovInv[m] * (var - gvmean[m]) + 2.0 * gvcovInv[m] * (par[t][m] - mean) * (par[t][m] - mean) ); 
  
        //System.out.format("HMMobj=%f  h=%f \n", HMMobj, h);
        /* gradient vector */
        if(gvSwitch[t]) {
          g[t] = 1.0 / h * ( w1 * w *(-g[t] + wum[t]) + w2 * vd * (par[t][m] - mean) );  

        } else 
          g[t] = 1.0 / h * ( w1 * w *(-g[t] + wum[t]) );  
                  
      }
              
      return(-(HMMobj+GVobj));  
      
     }
  
  
  private void convGV(int m){
    int t, k;
    double ratio, mixmean; 
    /* calculate GV of c */
    calcGV(m);
       
    ratio = Math.sqrt(gvmean[m] / var);
    //System.out.format("    mean=%f vari=%f ratio=%f \n", mean, var, ratio);
   
    /* c'[t][d] = ratio * (c[t][d]-mean[d]) + mean[d]  eq. (34) in Toda and Tokuda IEICE-2007 paper. */  
    for(t=0; t<nT; t++){
     if( gvSwitch[t] )
       par[t][m] = ratio * ( par[t][m]-mean ) + mean;
    }
      
  }
  
  private void calcGV(int m){
    int t, i;
    mean=0.0;
    var=0.0;
 
    /* mean */
    for(t=0; t<nT; t++)
      if(gvSwitch[t]){
        mean += par[t][m];
        //System.out.format("(%d)%f ", t, par[t][m]);
      }    
    mean = mean / gvLength;
    //System.out.format("  --- gvlength=%d  mean=%f\n", gvLength, mean);
      
    /* variance */  
    for(t=0; t<nT; t++)
      if(gvSwitch[t]){
        var += (par[t][m] - mean) * (par[t][m] - mean);
        //System.out.format("(%d)%f ", t, var);
      }
    //System.out.format("\n");
    var = var / gvLength;
      
  }
  
 
  

} /* class PStream */
