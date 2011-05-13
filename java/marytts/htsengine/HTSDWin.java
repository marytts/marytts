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

/**
 * Windows used to calculate dynamic features, delta and delta-delta.
 * 
 * Java port and extension of HTS engine API version 1.04
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