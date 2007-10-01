package de.dfki.lt.mary.htsengine;


/**
 * Data type and procedures used in parameter generation.
 * Contains means and variances of a particular model, 
 * mcep pdfs for a particular phoneme for example.
 * It also contains auxiliar matrices used in maximum likelihood 
 * parameter generation.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class PStream {
	
  public static final int WLEFT = 0;
  public static final int WRIGHT = 1;	
	  
  private int vSize;       /* vector size of observation vector (include static and dynamic features) */
  private int order;       /* vector size of static features */
  private int T;           /* length, number of frames in utterance */
  private int width;       /* width of dynamic window */
  
  private double par[][];  /* output parameter vector, the size of this parameter is par[T][vSize] */ 
  
  /* ____________________Matrices for parameter generation____________________ */
  private double mseq[][];   /* sequence of mean vector */
  private double ivseq[][];  /* sequence of inversed variance vector */
  private double g[];        /* for forward substitution */
  private double WUW[][];    /* W' U^-1 W  */
  private double WUM[];      /* W' U^-1 mu */
  
  /* ____________________Dynamic window ____________________ */
  private DWin dw;         
 // private int num;          /* number of static + deltas, number of window files */
  // private String fn;     /* delta window coefficient file */
 // private int dw_width[][]; /* width [0..num-1][0(left) 1(right)] */
 // private double coef[][];  /* coefficient [0..num-1][length[0]..length[1]] */
 // private int maxw[];       /* max width [0(left) 1(right)] */
 // private int max_L;        /* max {maxw[0], maxw[1]} */
 
  
  /* Constructor */
  public PStream(int vector_size, int utt_length) {
	/* in the c code for each PStream there is an InitDwin() and an InitPStream() */ 
	/* - InitDwin reads the window files passed as parameters for example: mcp.win1, mcp.win2, mcp.win3 */
	/*   for the moment the dynamic window is the same for all MCP, LF0, STR and MAG  */
	/*   The initialisation of the dynamic window is done with the constructor. */
	/* - InitPstream does the same as it is done here with the SMatrices constructor. */
	dw = new DWin();
    vSize = vector_size;
    order = vector_size / dw.get_num(); 
    T = utt_length;
    width = 3;            /* hard-coded to 3, in the c code is:  pst->width = pst->dw.max_L*2+1;  */
                          /* pst->dw.max_L is hard-code to 1, for all windows                     */
    par = new double[T][order];
   // sm = new SMatrices(T, vSize, width, order);
    
    /* ___________________________Matrices initialisation___________________ */
	mseq = new double[T][vSize];
	ivseq = new double[T][vSize];
	g = new double[T];
	WUW = new double[T][width];
	WUM = new double[T];
	
  }

  public void set_order(int val){ order=val; }
  public int get_order(){ return order; }
  
  public double get_par(int i, int j){ return par[i][j]; }
  public int get_T(){ return T; }
  
  public void set_mseq(int i, int j, double val){ mseq[i][j]=val; }
  public double get_mseq(int i, int j){ return mseq[i][j]; }
  
  public void set_ivseq(int i, int j, double val){ ivseq[i][j]=val; }
  public double get_ivseq(int i, int j){ return ivseq[i][j]; }
  
  public void set_g(int i, double val){ g[i]=val; }
  public double get_g(int i){ return g[i]; }
  
  public void set_WUW(int i, int j, double val){ WUW[i][j]=val; }
  public double get_WUW(int i, int j){ return WUW[i][j]; }
  
  public void set_WUM(int i, double val){ WUM[i]=val; }
  public double get_WUM(int i){ return WUM[i]; }
  
  public int get_dw_width(int i, int j){ return dw.get_width(i,j); }
  
  private void printWUW(int t){
	for(int i=0; i<width; i++)
	  System.out.print("WUW[" + t + "][" + i + "]=" + WUW[t][i] + "  ");
	System.out.println(""); 
  }
  
  /* mlpg: generate sequence of speech parameter vector maximizing its output probability for 
   * given pdf sequence */
  public void mlpg() {
	 int m,t;
	 int M = order;
	 boolean debug=false;

	 for (m=0; m<M; m++) {
	   Calc_WUW_and_WUM( m , debug);
	   LDL_Factorization(debug);       /* LDL factorization                               */
	   Forward_Substitution();    /* forward substitution in Cholesky decomposition  */
	   Backward_Substitution(m);  /* backward substitution in Cholesky decomposition */
	 } 
	 if(debug) {
	   for(m=0; m<M; m++){
	     for(t=0; t<4; t++)
		   System.out.print("par[" + t + "][" + m + "]=" + par[t][m] + " ");
	     System.out.println();
	   }
	   System.out.println();
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
  private void Calc_WUW_and_WUM(int m, boolean debug) {
	int t, i, j, k,iorder;
	double WU;
	double val;
	
	for(t=0; t<T; t++) {
	  /* initialise */
	  WUM[t] = 0.0;
	  for(i=0; i<width; i++)
		WUW[t][i] = 0.0;
	  
	  /* calc WUW & WUM, U is already inverse  */
	    for(i=0; i<dw.get_num(); i++) {
	      if(debug)
	        System.out.println("WIN: " + i);
	      iorder = i*order+m;
	      for( j = dw.get_width(i, WLEFT); j <= dw.get_width(i, WRIGHT); j++) {
	    	 if(debug) 
	    	   System.out.println("  j=" + j + " t+j=" + (t+j) + " iorder=" + iorder + " coef["+i+"]["+(-j)+"]="+dw.coef(i,-j));
			 if( ( t+j>=0 ) && ( t+j<T ) && ( dw.coef(i,-j)!=0.0 )  ) {
				 WU = dw.coef(i,-j) * ivseq[t+j][iorder];
				 if(debug)
				   System.out.println("  WU = coef[" + i +"][" + j + "] * ivseq[" + (t+j) + "][" + iorder + "]"); 
				 WUM[t] += WU * mseq[t+j][iorder];
				 if(debug)
				   System.out.println("  WUM[" + t + "] += WU * mseq[" + t+j + "][" + iorder + "]   WU*mseq=" + WU * mseq[t+j][iorder]); 
				 
				 for(k=0; ( k<width ) && ( t+k<T ); k++)
				   if( ( k-j<=dw.get_width(i, 1) ) && ( dw.coef(i,(k-j)) != 0.0 ) ) {
				     WUW[t][k] += WU * dw.coef(i,(k-j));
				     val = WU * dw.coef(i,(k-j));
				     if(debug)
				       System.out.println("  WUW[" + t + "][" + k + "] += WU * coef[" + i + "][" + (k-j) + "]   WU*coef=" + val);
				   }
			  }
		  }		  
	    }  /* for i */
	    if(debug) {
	      System.out.println("------------\nWUM[" + t + "]=" + WUM[t]); 
	      printWUW(t);
	      System.out.println("------------\n");
	    }
	    
	}  /* for t */
	  
  }
  
  
  /* LDL_Factorization: Factorize W'*U^{-1}*W to L*D*L' (L: lower triangular, D: diagonal) */
  /* LDL_Factorization: Factorize W'*U^{-1}*W to L*D*L' (L: lower triangular, D: diagonal) */
  private void LDL_Factorization(boolean debug) {
	int t,i,j;
	for(t=0; t<T; t++) {
		
	  if(debug){
	    System.out.println("WUW calculation:");
	    printWUW(t);
	  }
	  
	  /* I need i=1 for the delay in t, but the indexes i in WUW[t][i] go from 0 to 2 
	   * so wherever i is used as index i=i-1  (this is just to keep somehow the original 
	   * c implementation). */
	  for(i=1; (i<width) && (t-i>=0); i++)  
		WUW[t][0] -= WUW[t-i][i+1-1] * WUW[t-i][i+1-1] * WUW[t-i][0];
	  
	  for(i=2; i<=width; i++) {
	    for(j=1; (i+j<=width) && (t-j>=0); j++)
		  WUW[t][i-1] -= WUW[t-j][j+1-1] * WUW[t-j][i+j-1] * WUW[t-j][0];
	    WUW[t][i-1] /= WUW[t][0];
	 
	  }
	  if(debug) {
	    System.out.println("LDL factorization:");
	    printWUW(t);	
	    System.out.println();
	  }
	}
	
  }
  
  /* Forward_Substitution */
  
  private void Forward_Substitution() {
	 int t, i;
	 
	 for(t=0; t<T; t++) {
	   g[t] = WUM[t];
	   for(i=1; (i<width) && (t-i>=0); i++)
		 g[t] -= WUW[t-i][i+1-1] * g[t-i];  /* i as index should be i-1 */
	   //System.out.println("  g[" + t + "]=" + g[t]);
	 }
	 
  }
  
  /* Backward_Substitution */
  private void Backward_Substitution(int m) {
	 int t, i;
	 
	 for(t=(T-1); t>=0; t--) {
	   par[t][m] = g[t] / WUW[t][0];
	   for(i=1; (i<width) && (t+i<T); i++) {
		   par[t][m] -= WUW[t][i+1-1] * par[t+i][m]; /* i as index should be i-1 */
	   }
	   //System.out.println("  par[" + t + "]["+ m + "]=" + par[t][m]); 
	 }
	  
  }
  
  
  

} /* class PStream */
