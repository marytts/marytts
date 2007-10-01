package de.dfki.lt.mary.htsengine;

import java.util.Vector;


/**
 * Decision trees.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class Tree {

	private int state;  /* state position of this tree */
	private Node root;  /* root node of this decision tree */
    private Node leaf;  /* leaf nodes of this decision tree */
    private Tree next;  /* link to next tree */
    
    /* i am not sure if this is needed??? what is it used for??? */
    private static Vector pattern;  /* pattern list for this tree */
    
    /* Every time a new Tree is created a new vector of patterns is created */
	public Tree(){
		state = 0;
		root = null;
		leaf = null;
		next = null;
		pattern = new Vector();
	}
	public void insert_next(){ next = new Tree(); }
	public Tree get_next(){ return next; }
	
	public void set_state(int var){ state = var; }
	public int get_state(){ return state; }
	
	public void set_root(Node rnode){ root = rnode; }
	public Node get_root(){ return root; }

	public void set_leaf(Node lnode){ leaf = lnode; }
	public Node get_leaf(){ return leaf; }
	
}
