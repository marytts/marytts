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
public class Node {

	private int idx;         /* index of this node */
	private int pdf;         /* index of pdf for this node  ( leaf node only ) */
	
	private Node yes;        /* link to child node (yes) */
	private Node no;         /* link to child node (no)  */
	private Node next;       /* link to next node  */  
	
	private Question quest;  /* question applied at this node */

//	public Node(){
//		idx = -1;
//		pdf = -1;
//		yes = null;
//		no = null;
//		next = null;
//		quest = null;
//	}
	
	public void set_idx(int var){ idx = var; }
	public int get_idx(){ return idx; }
	
	public void set_pdf( int var){ pdf = var; }
	public int get_pdf(){ return pdf; }

	public void insert_next(){ next = new Node(); }
	public void set_next(Node nnode){ next = nnode; }
	public Node get_next(){ return next; }
	
	public void setQuestion(Question q){ quest = q; }
	public Question getQuestion(){ return quest; }
	
	public void insert_no(){ no = new Node(); }
	public Node get_no(){ return no; }
	
	public void insert_yes(){ yes = new Node(); }
	public Node get_yes(){ return yes; }
	
	
	public void PrintNode(){
		System.out.println("Printing node: ");
		System.out.println("  idx=" + idx);
		System.out.println("  pdf=" + pdf);
		if ( quest != null )
		  quest.printQuestion();
		if ( no != null )
		  System.out.println("  Node no=" + no + "  idx=" + no.get_idx());
		else
		  System.out.println("  Node no=" + no);
		if ( yes != null )
		  System.out.println("  Node yes=" + yes + "  idx=" + yes.get_idx());
		else
		  System.out.println("  Node yes=" + yes);
		System.out.println("  Node next=" + next);
		System.out.println();
	}
	
}
