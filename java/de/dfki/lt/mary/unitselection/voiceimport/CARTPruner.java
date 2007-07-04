/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.unitselection.voiceimport;


import java.io.File;
import java.io.IOException;
import java.util.*;

import de.dfki.lt.mary.gizmos.CARTAnalyzer;

public class CARTPruner extends VoiceImportComponent
{
    protected File unprunedCart;
    protected File prunedCart;
    protected DatabaseLayout db = null;
    protected String logfile;
    protected int percent = 0;
    
    protected CARTAnalyzer ca;
    public final String CARTFILE = "CARTPruner.cartFile";
    public final String PRUNEDCARTFILE = "CARTPruner.prunedCartFile";
    public final String UNITFILE = "CARTPruner.unitFile";
    public final String WAVETIMELINE = "CARTPruner.waveFile";
    public final String UNITFEATUREFILE = "CARTPruner.unitFeatureFile";
   
    public String getName(){
        return "CARTPruner";
    }
    
  
     public void initialiseComp()
    {    
        logfile = db.getProp(db.TEMPDIR)+"prunedCart.log";
    }
    
      public SortedMap getDefaultProps(DatabaseLayout db){  
          this.db = db;
       if (props == null){
           props = new TreeMap();
           String filedir = db.getProp(db.FILEDIR);
           String maryext = db.getProp(db.MARYEXT);
           props.put(CARTFILE,filedir
                                +"cart"+maryext);
           props.put(PRUNEDCARTFILE,filedir
                        +"prunedCart"+maryext);
           props.put(UNITFILE, filedir
                        +"halfphoneUnits"+maryext);
           props.put(WAVETIMELINE, filedir
                        +"timeline_waveforms"+maryext);
           props.put(UNITFEATUREFILE, filedir
                        +"halfphoneFeatures"+maryext);           
       } 
       return props;
      }
      
      protected void setupHelp(){
          props2Help = new TreeMap();
          props2Help.put(CARTFILE,"file containing the preselection CART");
          props2Help.put(PRUNEDCARTFILE,"file containing the pruned preselection CART. Will be created by this module");
          props2Help.put(UNITFILE, "file containing all halfphone units");
          props2Help.put(WAVETIMELINE, "file containing all wave files");
          props2Help.put(UNITFEATUREFILE, "file containing all halfphone units and their target cost features"); 
      }
    
    public boolean compute() throws IOException
    {
        
        System.out.println("CART Pruner started.");

        prunedCart = new File(getProp(PRUNEDCARTFILE));
        boolean cutAbove1000 = true;
        boolean cutNorm = true;
        boolean cutSilence = true;
        boolean cutLong = true;
        
        ca = new CARTAnalyzer(getProp(UNITFILE),getProp(WAVETIMELINE),
                getProp(UNITFEATUREFILE),getProp(CARTFILE));
        try {
            
            ca.analyzeAutomatic(logfile, prunedCart.getPath(), cutAbove1000, cutNorm, cutSilence, cutLong);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

 
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        if (ca == null) return 0;
        return ca.percent();
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException
    {
        CARTPruner cartPruner = new CARTPruner();
        DatabaseLayout db = new DatabaseLayout(cartPruner);
        cartPruner.compute();
    }


}
