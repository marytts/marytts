
package de.dfki.lt.mary.htsengine;

/**
 * Windows used to calculate dynamic features, delta and delta-delta.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class DWin {
	  
	  public static final int WLEFT = 0;
	  public static final int WRIGHT = 1;	
		  
	  private int num;          /* number of static + deltas, number of window files */
	 // private String fn;      /* delta window coefficient file */
	  private int width[][];    /* width [0..num-1][0(left) 1(right)] */
	  private double coef[][];  /* coefficient [0..num-1][length[0]..length[1]] */
	  private int maxw[];       /* max width [0(left) 1(right)] */
	  //private int max_L;      /* max {maxw[0], maxw[1]} */
	  private int offset;


	  public DWin() {
		num = 3;                /* 3 windows: 1 static window, 1 delta window, 1 delta-delta window */
		                        /* also this is the number of window passed as parameters, ex:      */ 
		                        /* mcp.win1, mcp.win2 and mcp.win3 */
		width = new int[num][2];   
		/* The 3 windows from WLEFT to WRIGHT */
		width[0][WLEFT]=0;   width[0][WRIGHT]=0;
		width[1][WLEFT]=-1;  width[1][WRIGHT]=1;
		width[2][WLEFT]=-1;  width[2][WRIGHT]=1;
		
		coef = new double[3][3]; 
		/* win1: size=1  val=            1.0         -> static window      */
		/* win2: size=3  val=   -0.5     0.0    0.5  -> delta window       */
		/* win3: size=3  val=    1.0    -2.0    1.0  -> delta-delta window */ 
		coef[0][0] =  1.0;  coef[0][1] =  0.0;  coef[0][2] = 0.0;
		coef[1][0] = -0.5;  coef[1][1] =  0.0;  coef[1][2] = 0.5;
		coef[2][0] =  1.0;  coef[2][1] = -2.0;  coef[2][2] = 1.0;
		/* NOTE 2: in the C version, and using pointers, the coef matrix is used
		   in the following way: 
		                             pst->dw.coef[0][0]= 1.0
		   pst->dw.coef[1][-1]=-0.5  pst->dw.coef[1][0]= 0.0  pst->dw.coef[1][1]=0.5
		   pst->dw.coef[2][-1]= 1.0  pst->dw.coef[2][0]=-2.0  pst->dw.coef[2][1]=1.0
		*/

		maxw = new int[2];
		maxw[WLEFT]=-1; maxw[WRIGHT]=1;
		//max_L = 1;
		if( maxw[WLEFT] < 0 )
		  offset = Math.abs(maxw[WLEFT]);
		else
		  offset = 0;
		
	  }

	  
	  public int get_width(int i, int j){ return width[i][j]; }
	  public int get_num() { return num; }
	   
	  /* The coefficients are stored using all positive indexes, in oder to simmulate
	   * negative indexes of the window an offset is applied.
	   * For example if the window goes from -3 to 3, the corresponding indexes are: 
	   * -3 -2 -1 0 1 2 3 
	   *  0  1  2 3 4 5 6  
	   *  so the corresponding index is obtain adding abs(maxw[LEFT]) to the */
	  public double coef(int i, int j){
		 if(i==0) {
		   if( j < 0 ) {
			 System.err.println("Dwin: error reading window coeficients, window 1 has just coef[0][0]");
		     return 0.0;
		   } else
		     return coef[i][j];
		 }
		 else {
		   j = j + offset;      
		   return coef[i][j];
		 }
	  }

	}