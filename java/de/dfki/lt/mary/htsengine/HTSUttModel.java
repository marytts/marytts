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

/**
 * list of Model objects for current utterance.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSUttModel {

  private int numModel;             /* # of models for current utterance       */
  private int numState;             /* # of HMM states for current utterance   */ 
  private int totalFrame;           /* # of frames for current utterance       */
  private int lf0Frame;             /* # of frames that are voiced or non-zero */
  private Vector<HTSModel> modelList;  /* This will be a list of Model objects for current utterance */
  private String realisedAcoustParams;  /* list of phonemes and actual realised durations for each one */
  
  public HTSUttModel() {
	numModel = 0;
	numState = 0;
	totalFrame = 0;
	lf0Frame = 0;
	modelList = new Vector<HTSModel>();
    realisedAcoustParams = "";
  }
  
  public void setNumModel(int val){ numModel = val; }
  public int getNumModel(){ return numModel; }
  
  public void setNumState(int val){ numState = val; }
  public int getNumState(){ return numState; }
  
  public void setTotalFrame(int val){ totalFrame = val; }
  public int getTotalFrame(){ return totalFrame; }
  
  public void setLf0Frame(int val){ lf0Frame = val; }
  public int getLf0Frame(){ return lf0Frame; }
  
  public void addUttModel(HTSModel newModel){ modelList.addElement(newModel); }
  public HTSModel getUttModel(int i){ return (HTSModel) modelList.elementAt(i); 	}
  public int getNumUttModel(){ return modelList.size(); }
  
  public void setRealisedAcoustParams(String str){ realisedAcoustParams = str;}
  public String getRealisedAcoustParams(){ return realisedAcoustParams; }
  public void concatRealisedAcoustParams(String str){
      realisedAcoustParams = realisedAcoustParams + str;
  }
  
}
