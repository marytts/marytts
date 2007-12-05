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
 * Windows used to calculate dynamic features, delta and delta-delta.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSDWin {
	  
	  public static final int WLEFT = 0;
	  public static final int WRIGHT = 1;	
		  
	  private int num;          /* number of static + deltas, number of window files */
	  // private String fn;      /* delta window coefficient file */
	  private int width[][];    /* width [0..num-1][0(left) 1(right)] */
	  private double coef[][];  /* coefficient [0..num-1][length[0]..length[1]] */
	  private int maxw[];       /* max width [0(left) 1(right)] */
	  //private int max_L;      /* max {maxw[0], maxw[1]} */
	  private int offset;


	  public HTSDWin() {
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

	  
	  public int getWidth(int i, int j){ return width[i][j]; }
	  public int getNum() { return num; }
	   
	  /* The coefficients are stored using all positive indexes, in oder to simmulate
	   * negative indexes of the window an offset is applied.
	   * For example if the window goes from -3 to 3, the corresponding indexes are: 
	   * -3 -2 -1 0 1 2 3 
	   *  0  1  2 3 4 5 6  
	   *  so the corresponding index is obtain adding abs(maxw[LEFT]) to the */
	  public double getCoef(int i, int j){
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