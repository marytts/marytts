
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
