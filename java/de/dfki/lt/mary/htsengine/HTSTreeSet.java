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
import java.io.*;

import org.apache.log4j.Logger;

/**
 * Tree set containing trees and questions lists for DUR, logF0, MCP, STR and MAG
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSTreeSet {
    
	private int nTrees[];         /* # of trees for DUR, logF0, MCP, STR and MAG */
	private HTSQuestion qhead[];  /* question lists for DUR, logF0, MCP, STR and MAG */
	private HTSQuestion qtail[];
	private HTSTree thead[];      /* tree lists for DUR, logF0, MCP, STR and MAG */
	private HTSTree ttail[];
    
    private Logger logger = Logger.getLogger("TreeSet");
	
	/** Constructor 
	* TreeSet is initialised with the information in ModelSet,      
	* basically we need to know the structure of the feature vector 
	* then we know how many trees and lists of questions we have,   
	* there is a tree and a list of questions per each feature */
	public HTSTreeSet(int num_mtype){
	   nTrees = new int[num_mtype];
	   qhead = new HTSQuestion[num_mtype];
	   qtail = new HTSQuestion[num_mtype];
	   thead = new HTSTree[num_mtype];
	   ttail = new HTSTree[num_mtype];
	}

	/**
	 * This function returns the head tree of this type
	 * @param type: one of htsData.DUR, htsData.LF0, htsData.MCP, htsData.STR or htsData.MAG
	 * @return a Tree object.
	 */
	public HTSTree getTreeHead(int type){ return thead[type]; }

	/**
	 * This function returns the tail tree of this type
	 * @param type: one of htsData.DUR, htsData.LF0, htsData.MCP, htsData.STR or htsData.MAG
	 * @return a Tree object.
	 */
	public HTSTree getTreeTail(int type){ return ttail[type]; }
	
    
    
    
    /** This function loads all the trees and questions for the files 
     * Tree*File in htsData. */
    public void loadTreeSet(HMMData htsData) throws Exception {
       
     /* DUR, LF0 and MCP are required as minimum for generating voice */
        _loadTreeSet(htsData.getTreeDurFile(), HMMData.DUR);        
        _loadTreeSet(htsData.getTreeLf0File(), HMMData.LF0);        
        _loadTreeSet(htsData.getTreeMcpFile(), HMMData.MCP); 
        
     /* STR and MAG are optional for generating mixed excitation */ 
      if( htsData.getTreeStrFile() != null)
        _loadTreeSet(htsData.getTreeStrFile(), HMMData.STR);
      if( htsData.getTreeMagFile() != null)
        _loadTreeSet(htsData.getTreeMagFile(), HMMData.MAG);

    }
    
    
    /** This function loads the trees and questions for a particular tree type 
     */
	private void _loadTreeSet(String fileName, int type) throws Exception {
		
      HTSQuestion q = new HTSQuestion(); /* so: qName=null; next=null; pattern=new Vector(); */
	  qhead[type] = q;
	  qtail[type] = null;
	  
      HTSTree t = new HTSTree(); /* so state=0; root=null; leaf=null; next=null; pattern=new Vector(); */
	  thead[type] = t;     /* first tree corresponds to state 2, next tree to state 3, ..., until state 6 */
	  ttail[type] = null;
	  nTrees[type] = 0;
	  
	  Scanner s = null;
	  String line, aux;
	  	  
	  try {   
		/* read lines of tree-*.inf fileName */ 
		s = new Scanner(new BufferedInputStream(new FileInputStream(fileName))).useDelimiter("\n");
          
		//System.out.println("LoadTreeSet reading: " + fileName + " tree type: " + type);
        logger.info("LoadTreeSet reading: " + fileName);
		
		while(s.hasNext()) {
		  line = s.next();
		  
		  if (line.indexOf("QS") >= 0 ) {				
		    //System.out.println("QUESTION: " + line );
		    loadQuestions(line.substring(3),q,false);  /* after 3 char so without QS */
		    
		    q.insertNext();  /* create new next Question (set of questions) */
		    q = q.getNext();
		    qtail[type] = q;  
		    
		  }
		  else if(line.indexOf("{*}") >= 0 ){  /* this is the indicator of a new state-tree */
			aux = line.substring(line.indexOf("[")+1, line.indexOf("]")); 
			t.setState(Integer.parseInt(aux));
			//System.out.println("Loading tree type=" + type + " TREE STATE: " +  t.get_state() );
			
			loadTree(s, t, type, false);   /* load one tree per state */
			t.insertNext(); /* create new next Tree */
			t = t.getNext();
			ttail[type] = t;
			
			/* increment number of trees for this type */
			nTrees[type]++;
			
		  }
		 
		} /* while */  
        if (s != null)
		  s.close();
		
		/* check that the tree was correctly loaded */
		if( qhead[type] == null || nTrees[type] == 0 ) {
          logger.debug("LoadTreeSet: error no trees loaded from " + fileName);  
          throw new Exception("LoadTreeSet: error no trees loaded from  " + fileName);   
		}
		
	    
	  } catch (FileNotFoundException e) {
          logger.debug("FileNotFoundException: " + e.getMessage());
          throw new FileNotFoundException("LoadTreeSet: " + e.getMessage());
	  }
		
	} /* method loadTreeSet */

	
	/** Load questions from file, it receives a line from the tree-*.inf file 
	 * each line corresponds to a question name and one or more patterns.  
	 * parameter debug is used for printing detailed information.          */
	private void loadQuestions(String line, HTSQuestion q, boolean debug) {
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
				q.setQuestionName(aux);
			}		
		}  
		s.close();
		
		/* print this question */
		if(debug) {
		  System.out.println("  qName   : " + q.getQuestionName());
		  System.out.print  ("  patterns: ");
		  for(i=0; i<q.getNumPatterns(); i++) {
			 System.out.print(q.getPattern(i) + "  ");
	      }
		  System.out.println();
		}
	} /* method loadQuestions */
	
	
	
	/** Load a tree per state
	 * @param s: text scanner of the whole tree-*.inf file
	 * @param t: tree to be filled
	 * @param type: corresponds to one of DUR, logF0, MCP, STR and MAG
	 * @param debug: when true print out detailled information
	 */
	private void loadTree(Scanner s, HTSTree t, int type, boolean debug) throws Exception {
	  Scanner sline;
	  String aux,buf;
      HTSNode node = new HTSNode();
	  t.setRoot(node);
	  t.setLeaf(node);
	  int iaux;
      HTSQuestion qaux;
	  
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
		  	  node = findNode(t.getLeaf(),Integer.parseInt(buf.substring(1)),debug);  
		    else
		      node = findNode(t.getLeaf(),Integer.parseInt(buf),debug);
		  
		    if(node == null)
                throw new Exception("LoadTree: Node not found, index = " +  buf); 
		    else {
		      //System.out.println("node found = " + node);
		      /* 2: gets question name and looks for the question whose qName = buf */
		      buf = sline.next();
		      //System.out.println("Question to find: " + buf);
		      qaux = findQuestion(type,buf);
		      node.setQuestion(qaux);
		  
		      /* create nodes for NO and YES */
		      node.insertNo();
		      node.insertYes();
		  
		      /* NO index */
		      buf = sline.next();
		      //System.out.print("  NO:" + buf + "   ");
		      if(buf.startsWith("-")) {
		 	    iaux = Integer.parseInt(buf.substring(1));
			    node.getNo().setIdx(iaux);
			    //System.out.println("No IDX=" + iaux);
		      } else {  /*convert name of node to node index number */
			    iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_")+1, buf.length()-1));
			    node.getNo().setPdf(iaux);
			    //System.out.println("No PDF=" + iaux); 
		      }
		  	  
		      node.getNo().setNext(t.getLeaf());
		      t.setLeaf(node.getNo());
		  
		  	  
		      /* YES index */
		      buf = sline.next();
		      //System.out.print("  YES: " + buf + "   ");
		      if(buf.startsWith("-")) {
			    iaux = Integer.parseInt(buf.substring(1));
			    node.getYes().setIdx(iaux);
			    //System.out.println("Yes IDX=" + iaux);
		      } else {  /*convert name of node to node index number */
			    iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_")+1, buf.length()-1));
			    node.getYes().setPdf(iaux);
			    //System.out.println("Yes PDF=" + iaux); 
		      }
		      node.getYes().setNext(t.getLeaf());
		      t.setLeaf(node.getYes());
		  
		      if(debug)
		        node.printNode();
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
	  	
	} /* method loadTree() */

	
	private HTSNode findNode(HTSNode node, int num, boolean debug){
	  if(debug)
        System.out.print("Finding Node : " + num + "  "  );
	  while(node != null){
		 if(debug)
		   System.out.print("node->idx=" + node.getIdx() + "  ");
		 if(node.getIdx() == num){
			if(debug) 
			  System.out.println(); 
			return node;
		 }
		 else
			 node = node.getNext();
	  }
	  return null;
		
	} /* method findNode */
	
	private HTSQuestion findQuestion(int type, String qname) throws Exception {
        HTSQuestion q;
		//System.out.println("qhead[type]=" + qhead[type] + "  "  + "qtail[type]=" + qtail[type]);
		for(q = qhead[type]; q != qtail[type]; q = q.getNext() ) {
		 //System.out.println("q : " + q + "  qname=" + qname + "  q.get_qName= " + q.get_qName());
		 if(qname.equals(q.getQuestionName()) ) {
		   //q.printQuestion();
		   break;
		 }
		 
		}
		if(q == qtail[type])
            throw new Exception("FindQuestion: cannot find question %s." + qname );
		
		
		return q;
		
	} /* method findQuestion */
	
	
	public int searchTree(String name, HTSNode root_node, boolean debug){
	   	 
        HTSNode aux_node = root_node;
		
		while (aux_node != null ){
			
			if( questionMatch(name, aux_node.getQuestion(),debug)) {
				if(aux_node.getYes().getPdf() > 0 ){
                    if(debug)
                      System.out.println("  QMatch=1 node->YES->idx=" + aux_node.getIdx() + "  aux_node.getYes().getPdf()=" + aux_node.getYes().getPdf());
					return aux_node.getYes().getPdf();
                }
				aux_node = aux_node.getYes();
				
			} else {
				if(aux_node.getNo().getPdf() > 0){
                  if(debug)
                    System.out.println("  QMatch=0 node->NO->idx=" + aux_node.getIdx() + "  aux_node.getNo().getPdf()=" + aux_node.getNo().getPdf() );  
				  return(aux_node.getNo().getPdf());
                }
				aux_node = aux_node.getNo();
				
			}
			
		}
		return -1;
		
	} /* method searchTree */
	
	private boolean questionMatch(String str, HTSQuestion q, boolean debug) {
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
               System.out.println("    pattern matched: " + pat + "\n");
			  }
              return true;
		   }
		}
        
	    return false;
		
	}
	

} /* class TreeSet */



