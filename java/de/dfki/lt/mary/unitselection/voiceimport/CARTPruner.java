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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import de.dfki.lt.mary.gizmos.CARTAnalyzer;
import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

public class CARTPruner implements VoiceImportComponent
{
    protected File unprunedCart;
    protected File prunedCart;
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    protected int percent = 0;
    
    protected CARTAnalyzer ca;
    
    public CARTPruner( DatabaseLayout setdb, BasenameList setbnl )
    {
        this.db = setdb;
        this.bnl = setbnl;
    }
    
    /**
     * Set some global variables; sub-classes may want to override.
     *
     */
    protected void init()
    {
        unprunedCart = new File(db.cartFileName());
        if (!unprunedCart.exists()) throw new IllegalStateException("Unpruned CART file "+unprunedCart.getAbsolutePath()+" does not exist");
        if (!unprunedCart.getAbsolutePath().equals(new File("./mary_files/cart.mry").getAbsolutePath()))
            throw new IllegalStateException("Path to unpruned CART is currently hard coded to ./mary_files/cart.mry -- cannot use "+unprunedCart.getAbsolutePath());
    }
    
    public boolean compute() throws IOException
    {
        init();
        System.out.println("CART Pruner started.");

        prunedCart = new File("./mary_files/prunedcart");
        boolean cutAbove1000 = true;
        boolean cutNorm = true;
        boolean cutSilence = true;
        boolean cutLong = true;
        
        ca = new CARTAnalyzer();
        try {
            ca.analyzeAutomatic("cartpruner.log", prunedCart.getPath(), cutAbove1000, cutNorm, cutSilence, cutLong);
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
        //build database layout
        DatabaseLayout db = DatabaseImportMain.getDatabaseLayout();
        BasenameList bnl = DatabaseImportMain.getBasenameList(db);
        new CARTPruner(db, bnl).compute();
    }


}
