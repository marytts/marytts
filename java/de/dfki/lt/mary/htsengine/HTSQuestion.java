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

import java.util.Vector;


/** The question class contains a name of the question and its patterns.  
* The vector (or linked list) pat will contain the list of               
* patterns in a question, for example for the question QS L-vc-          
* in file: /project/mary/marcela/HTS-mix/voices/qst001/ver1/tree-mcp.inf 
* QS L-vc- { "*^d-*","*^n-*","*^p-*","*^s-*","*^t-*","*^z-*" }           
*   qName = L-vc-     
*   pat[0] = *^d-*    
*   pat[1] = *^n-*    
*   ...               
*   pat[5] = *^z-*    
* Vector will contain a list of patterns which are Strings.   
* 
* Java port and extension of HTS engine version 2.0
* Extension: mixed excitation
* @author Marcela Charfuelan
*/
public class HTSQuestion {

	private String qName; 
	private Vector<String> pattern;
	private HTSQuestion next;
	
	/* Every time a new Question is created a new vector of patterns is created */
	public HTSQuestion(){
		qName = null;
		next = null;
		pattern = new Vector<String>();
	}
	
	public void setQuestionName(String var){ qName = var; }
	public String getQuestionName(){ return qName; }
	
	public void addPattern(String pat){ pattern.addElement(pat); }
	public String getPattern(int i){ return (String) pattern.elementAt(i); 	}
	public int getNumPatterns(){ return pattern.size(); }
	
	public void printQuestion(){
	  int i;
	  System.out.println("    qName: " + qName);
	  System.out.print("    patterns: ");
	  for(i=0; i<pattern.size(); i++)
		System.out.print(pattern.elementAt(i) + " ");
	  System.out.println();
	}
	
	public void insertNext(){ next = new HTSQuestion(); }
	public HTSQuestion getNext(){ return next; }
	
}
