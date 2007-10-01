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
public class Question {

	private String qName; 
	private Vector pattern;
	private Question next;
	
	/* Every time a new Question is created a new vector of patterns is created */
	public Question(){
		qName = null;
		next = null;
		pattern = new Vector();
	}
	
	public void set_qName(String var){ qName = var; }
	public String get_qName(){ return qName; }
	
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
	
	public void insert_next(){ next = new Question(); }
	public Question get_next(){ return next; }
	
}
