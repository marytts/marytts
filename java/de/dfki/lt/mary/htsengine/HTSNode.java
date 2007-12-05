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
 * Node of a tree.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSNode {

	private int idx;         /* index of this node */
	private int pdf;         /* index of pdf for this node  ( leaf node only ) */
	
	private HTSNode yes;        /* link to child node (yes) */
	private HTSNode no;         /* link to child node (no)  */
	private HTSNode next;       /* link to next node  */  
	
	private HTSQuestion quest;  /* question applied at this node */

	
	public void setIdx(int var){ idx = var; }
	public int getIdx(){ return idx; }
	
	public void setPdf( int var){ pdf = var; }
	public int getPdf(){ return pdf; }

	public void insertNext(){ next = new HTSNode(); }
	public void setNext(HTSNode nnode){ next = nnode; }
	public HTSNode getNext(){ return next; }
	
	public void setQuestion(HTSQuestion q){ quest = q; }
	public HTSQuestion getQuestion(){ return quest; }
	
	public void insertNo(){ no = new HTSNode(); }
	public HTSNode getNo(){ return no; }
	
	public void insertYes(){ yes = new HTSNode(); }
	public HTSNode getYes(){ return yes; }
	
	
	public void printNode(){
		System.out.println("Printing node: ");
		System.out.println("  idx=" + idx);
		System.out.println("  pdf=" + pdf);
		if ( quest != null )
		  quest.printQuestion();
		if ( no != null )
		  System.out.println("  Node no=" + no + "  idx=" + no.getIdx());
		else
		  System.out.println("  Node no=" + no);
		if ( yes != null )
		  System.out.println("  Node yes=" + yes + "  idx=" + yes.getIdx());
		else
		  System.out.println("  Node yes=" + yes);
		System.out.println("  Node next=" + next);
		System.out.println();
	}
	
}
