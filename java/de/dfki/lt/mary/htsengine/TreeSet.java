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

import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * Tree set containing trees and questions lists for DUR, logF0, MCP, STR and MAG
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class TreeSet {
    
	private int nTrees[];      /* # of trees for DUR, logF0, MCP, STR and MAG */
	private Question qhead[];  /* question lists for DUR, logF0, MCP, STR and MAG */
	private Question qtail[];
	private Tree thead[];      /* tree lists for DUR, logF0, MCP, STR and MAG */
	private Tree ttail[];
	
	/** Constructor 
	* TreeSet is initialised with the information in ModelSet,      
	* basically we need to know the structure of the feature vector 
	* then we know how many trees and lists of questions we have,   
	* there is a tree and a list of questions per each feature */
	public TreeSet(int num_mtype){
	   nTrees = new int[num_mtype];
	   qhead = new Question[num_mtype];
	   qtail = new Question[num_mtype];
	   thead = new Tree[num_mtype];
	   ttail = new Tree[num_mtype];
	}

	/**
	 * This function returns the head tree of this type
	 * @param type: one of hts_data.DUR, hts_data.LF0, hts_data.MCP, hts_data.STR or hts_data.MAG
	 * @return a Tree object.
	 */
	public Tree getTreeHead(int type){ return thead[type]; }

	/**
	 * This function returns the tail tree of this type
	 * @param type: one of hts_data.DUR, hts_data.LF0, hts_data.MCP, hts_data.STR or hts_data.MAG
	 * @return a Tree object.
	 */
	public Tree getTreeTail(int type){ return ttail[type]; }
	
    
    /** This function loads all the trees and questions for the files 
     * Tree*File in hts_data. */
    public void LoadTreeSet(HMMData hts_data) {
        
        _LoadTreeSet(hts_data.TreeDurFile(), hts_data.DUR);        
        _LoadTreeSet(hts_data.TreeLf0File(), hts_data.LF0);        
        _LoadTreeSet(hts_data.TreeMcpFile(), hts_data.MCP);        
        _LoadTreeSet(hts_data.TreeStrFile(), hts_data.STR);        
        _LoadTreeSet(hts_data.TreeMagFile(), hts_data.MAG);
    }
    
    
    /** This function loads the trees and questions for a particular tree type 
     */
	private void _LoadTreeSet(String fileName, int type) {
		
	  Question q = new Question(); /* so: qName=null; next=null; pattern=new Vector(); */
	  qhead[type] = q;
	  qtail[type] = null;
	  
	  Tree t = new Tree(); /* so state=0; root=null; leaf=null; next=null; pattern=new Vector(); */
	  thead[type] = t;     /* first tree corresponds to state 2, next tree to state 3, ..., until state 6 */
	  ttail[type] = null;
	  nTrees[type] = 0;
	  
	  Scanner s = null;
	  String line, aux;
	  	  
	  try {   
		/* read lines of tree-*.inf fileName */ 
		s = new Scanner(new BufferedReader(new FileReader(fileName))).useDelimiter("\n"); 
		
		//System.out.println("LoadTreeSet reading: " + fileName + " tree type: " + type);
        System.out.println("LoadTreeSet reading: " + fileName);
		
		while(s.hasNext()) {
		  line = s.next();
		  
		  if (line.indexOf("QS") >= 0 ) {				
		    //System.out.println("QUESTION: " + line );
		    LoadQuestions(line.substring(3),q,false);  /* after 3 char so without QS */
		    
		    q.insert_next();  /* create new next Question (set of questions) */
		    q = q.get_next();
		    qtail[type] = q;  
		    
		  }
		  else if(line.indexOf("{*}") >= 0 ){  /* this is the indicator of a new state-tree */
			aux = line.substring(line.indexOf("[")+1, line.indexOf("]")); 
			t.set_state(Integer.parseInt(aux));
			//System.out.println("Loading tree type=" + type + " TREE STATE: " +  t.get_state() );
			
			LoadTree(s, t, type, false);   /* load one tree per state */
			t.insert_next(); /* create new next Tree */
			t = t.get_next();
			ttail[type] = t;
			
			/* increment number of trees for this type */
			nTrees[type]++;
			
		  }
		 
		} /* while */  
	    
		s.close();
		
		/* check that the tree was correctly loaded */
		if( qhead[type] == null || nTrees[type] == 0 ) {
         System.err.println("LoadTreeSet: error loading trees from " + fileName);  
            
//		   if(type == hts_data.DUR)
//			 System.err.println("LoadTreeSet: no trees for duration are loaded.");
//		   else if(type == hts_data.LF0)
//		     System.err.println("LoadTreeSet: no trees for log f0 are loaded.");
//		   else if(type == hts_data.MCP)
//			 System.err.println("LoadTreeSet: no trees for mel-cepstrum are loaded.");
//		   else if(type == hts_data.STR)
//			 System.err.println("LoadTreeSet: no trees for strengths are loaded.");
//		   else  /* if(type == hts_data.MAG) */
//			 System.err.println("LoadTreeSet: no trees for Fourier magnitudes are loaded.");
			
		}
		
	    
	  } catch (FileNotFoundException e) {
	       System.err.println("FileNotFoundException: " + e.getMessage());
	  }
		
	} /* method LoadTreeSet */

	
	/** Load questions from file, it receives a line from the tree-*.inf file 
	 * each line corresponds to a question name and one or more patterns.  
	 * parameter debug is used for printing detailed information.          */
	private void LoadQuestions(String line, Question q, boolean debug) {
		Scanner s = new Scanner(line);
		String aux, sub_aux;
		String pats[];
		int i; 
		
		while (s.hasNext()) {
			aux = s.next();
			
			if( aux.length() > 1 ){  /* discard { or } */
			  if(aux.indexOf(",") > 0){
				/* here i need to split the patterns separated by , and remove " from each pattern */
				/*  "d^*","n^*","p^*","s^*","t^*","z^*"  */
				pats = aux.split(",");
				for(i=0; i<pats.length; i++) {
					q.addPattern(pats[i].substring(1, pats[i].lastIndexOf("\"")));
		        }
			  }
			  else if(aux.indexOf("\"") >= 0 ){
				/* here i need to remove " at the beginning and at the end: ex. "_^*" --> _^*  */  				
				sub_aux = aux.substring(1, aux.lastIndexOf("\""));
  			    q.addPattern(sub_aux);
			  }
			  else		
				q.set_qName(aux);
			}		
		}  
		s.close();
		
		/* print this question */
		if(debug) {
		  System.out.println("  qName   : " + q.get_qName());
		  System.out.print  ("  patterns: ");
		  for(i=0; i<q.getNumPatterns(); i++) {
			 System.out.print(q.getPattern(i) + "  ");
	      }
		  System.out.println();
		}
	} /* method load Questions */
	
	
	
	/** Load a tree per state
	 * @param s: text scanner of the whole tree-*.inf file
	 * @param t: tree to be filled
	 * @param type: corresponds to one of DUR, logF0, MCP, STR and MAG
	 * @param debug: when true print out detailled information
	 */
	private void LoadTree(Scanner s, Tree t, int type, boolean debug){
	  Scanner sline;
	  String aux,buf;
	  Node node = new Node();
	  t.set_root(node);
	  t.set_leaf(node);
	  int iaux;
	  Question qaux;
	  
	  //System.out.println("root node = " + node + "  t.leaf = " + t.get_leaf() + "  t.root = " + t.get_root());
	  
	  aux = s.next();   /* next line for this state tree must be { */
	  if(aux.indexOf("{") >= 0 ) {
	    while ( s.hasNext() ){  /* last line for this state tree must be } */
	      aux = s.next();
	      //System.out.println("next=" + aux);
	      if(aux.indexOf("}") < 0 ) {
	        /* then parse this line, it contains 4 fields */
	        /* 1: node index #  2: Question name 3: NO # node 4: YES # node */
		    sline = new Scanner(aux);
		  
		    /* 1:  gets index node and looks for the node whose idx = buf */
		    buf = sline.next();
		    //System.out.println("\nNode to find: " + buf + " starting in leaf node: " + t.get_leaf());
		    if(buf.startsWith("-"))
		  	  node = FindNode(t.get_leaf(),Integer.parseInt(buf.substring(1)),debug);  
		    else
		      node = FindNode(t.get_leaf(),Integer.parseInt(buf),debug);
		  
		    if(node == null)
			  System.err.println("LoadTree: Node not found, index = " +  buf); 
		    else {
		      //System.out.println("node found = " + node);
		      /* 2: gets question name and looks for the question whose qName = buf */
		      buf = sline.next();
		      //System.out.println("Question to find: " + buf);
		      qaux = FindQuestion(type,buf);
		      node.setQuestion(qaux);
		  
		      /* create nodes for NO and YES */
		      node.insert_no();
		      node.insert_yes();
		  
		      /* NO index */
		      buf = sline.next();
		      //System.out.print("  NO:" + buf + "   ");
		      if(buf.startsWith("-")) {
		 	    iaux = Integer.parseInt(buf.substring(1));
			    node.get_no().set_idx(iaux);
			    //System.out.println("No IDX=" + iaux);
		      } else {  /*convert name of node to node index number */
			    iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_")+1, buf.length()-1));
			    node.get_no().set_pdf(iaux);
			    //System.out.println("No PDF=" + iaux); 
		      }
		  	  
		      node.get_no().set_next(t.get_leaf());
		      t.set_leaf(node.get_no());
		  
		  	  
		      /* YES index */
		      buf = sline.next();
		      //System.out.print("  YES: " + buf + "   ");
		      if(buf.startsWith("-")) {
			    iaux = Integer.parseInt(buf.substring(1));
			    node.get_yes().set_idx(iaux);
			    //System.out.println("Yes IDX=" + iaux);
		      } else {  /*convert name of node to node index number */
			    iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_")+1, buf.length()-1));
			    node.get_yes().set_pdf(iaux);
			    //System.out.println("Yes PDF=" + iaux); 
		      }
		      node.get_yes().set_next(t.get_leaf());
		      t.set_leaf(node.get_yes());
		  
		      if(debug)
		        node.PrintNode();
		      //node.get_no().PrintNode();
		      //node.get_yes().PrintNode();
		    }  /* if node not null */
		  
	        sline.close();
	        sline=null;
	        
	     }else { /* if "}", so last line for this tree */
		      //System.out.println("*** next=" + aux);
		      return;
		   }
	   } /* while there is another line */
	  }  /* if not "{" */
	  	
	} /* method LoadTree() */

	
	private Node FindNode(Node node, int num, boolean debug){
	  if(debug)
        System.out.print("Finding Node : " + num + "  "  );
	  while(node != null){
		 if(debug)
		   System.out.print("node->idx=" + node.get_idx() + "  ");
		 if(node.get_idx() == num){
			if(debug) 
			  System.out.println(); 
			return node;
		 }
		 else
			 node = node.get_next();
	  }
	  return null;
		
	} /* method FindNode */
	
	private Question FindQuestion(int type, String qname){
		Question q;
		//System.out.println("qhead[type]=" + qhead[type] + "  "  + "qtail[type]=" + qtail[type]);
		for(q = qhead[type]; q != qtail[type]; q = q.get_next() ) {
		 //System.out.println("q : " + q + "  qname=" + qname + "  q.get_qName= " + q.get_qName());
		 if(qname.equals(q.get_qName()) ) {
		   //q.printQuestion();
		   break;
		 }
		 
		}
		if(q == qtail[type])
		  System.err.println("FindQuestion: cannot find question %s." + qname );
		
		
		return q;
		
	} /* method FindQuestion */
	
	
	public int SearchTree(String name, Node root_node, boolean debug){
	   	 
		Node aux_node = root_node;
		
		while (aux_node != null ){
			
			if( QMatch(name, aux_node.getQuestion(),debug)) {
				if(aux_node.get_yes().get_pdf() > 0 )
					return aux_node.get_yes().get_pdf();
				aux_node = aux_node.get_yes();
				if(debug)
				  System.out.println("  QMatch=1 node->YES->idx=" + aux_node.get_idx());
			} else {
				if(aux_node.get_no().get_pdf() > 0)
				  return(aux_node.get_no().get_pdf());
				aux_node = aux_node.get_no();
				if(debug)
				  System.out.println("  QMatch=0 node->NO->idx=" + aux_node.get_idx());
			}
			
		}
		return -1;
		
	} /* method SearchTree */
	
	private boolean QMatch(String str, Question q, boolean debug) {
		int i;
		String pat;
	    
		for(i=0; i<q.getNumPatterns(); i++) {
		   pat = q.getPattern(i);		   
		   if(pat.startsWith("*"))
			 pat = pat.substring(1);
		   if(pat.endsWith("*"))
			 pat = pat.substring(0, pat.length()-1);  
		   if( str.contains(pat) ){
			  if(debug) {  
			   q.printQuestion();
               System.out.println("    pattern matched: " + pat);
			  }
              return true;
		   }
		}
	    return false;
		
	}
	

} /* class TreeSet */



