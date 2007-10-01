package de.dfki.lt.mary.htsengine;


import java.util.Vector;

/**
 * list of Model objects for current utterance.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class UttModel {

  private int nModel;        /* # of models for current utterance       */
  private int nState;        /* # of HMM states for current utterance   */ 
  private int totalframe;    /* # of frames for current utterance       */
  private int lf0frame;      /* # of frames that are voiced or non-zero */
  private Vector ModelList;  /* This will be a list of Model objects for current utterance */

  
  public UttModel() {
	nModel = 0;
	nState = 0;
	totalframe = 0;
	lf0frame = 0;
	ModelList = new Vector();
  }
  
  public void set_nModel(int val){ nModel = val; }
  public int get_nModel(){ return nModel; }
  
  public void set_nState(int val){ nState = val; }
  public int get_nState(){ return nState; }
  
  public void set_totalframe(int val){ totalframe = val; }
  public int get_totalframe(){ return totalframe; }
  
  public void set_lf0frame(int val){ lf0frame = val; }
  public int get_lf0frame(){ return lf0frame; }
  
  public void addUttModel(Model new_model){ ModelList.addElement(new_model); }
  public Model getUttModel(int i){ return (Model) ModelList.elementAt(i); 	}
  public int getNumUttModel(){ return ModelList.size(); }
  
}
