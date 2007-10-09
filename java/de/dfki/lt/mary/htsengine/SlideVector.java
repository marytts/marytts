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
 * Slide Vector is a Vector that can be used with different offsets.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Christoph Endres, Marcela charfuelan.
 */
public class SlideVector {
	
   private double[] content;
   private int contentSize;
   
   /* offsets for vector c */ 
   private int cc;
   private int cinc;
   private int d1;
   
   /** Constructor *
   * The size of the vector is specified when creating a new SlideVector */
   public SlideVector(int vector_size, int mcep_order, int pade_order) {
	   
	   content    = new double[vector_size];
	   contentSize = vector_size;
	   
	   /* offset for vector c:
	    * vector_size = ( 3*(m+1) + 3*(pd+1) + pd*(m+2) ), m=mcep_vsize;
	    * vector_size = 623 with mcep_vsize=74
	    * c    =                              --> c[0]...  
	    * cc   = c+m+1                        --> c[75]... 
	    * cinc = cc+m+1   = (c+m+1)+m+1       --> c[150]... 
	    * d1   = cinc+m+1 = ((c+m+1)+m+1)+m+1 --> c[225]... */
	    cc   = 1*(mcep_order+1);
	    cinc = 2*(mcep_order+1);
	    d1   = 3*(mcep_order+1);
	   
   }
    
   public void clearContent(){
	 for(int i=0; i<contentSize; i++)
		 content[i]=0.0;  
   }
   /* General methods for getting content */
   private double getContent(int index, int offset) { return content[offset+index]; }
   /* this is the general method to get content[] without offset */
   public double getContent(int index) {return content[index]; }

   /* Particular methods for getting content */
   public double getCC(int index) { return this.getContent(index, cc); }

   public double getCINC(int index) { return this.getContent(index, cinc); }

   public double getD1(int index) { return this.getContent(index, d1); }
   
   
   /* General method for setting content */
   private void setContent(int index, int offset, double value) { content[offset+index] = value; }
   /* this is the general method to set content[] without offset*/
   public void setContent(int index, double value){ content[index] = value; }
   
   /* Particular methods for setting content */
   public void setCC(int index, double value) { this.setContent(index, cc, value); }
  
   public void setCINC(int index, double value) { this.setContent(index, cinc, value); }

   public void setD1(int index, double value) { this.setContent(index, d1, value); }
   
   
}  /* class SlideVector */

