/**
 * Copyright 2007 DFKI GmbH.
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
import java.util.*;

/**
 * @author marc
 *
 */
public class HalfPhoneUnitfileWriter extends PhoneUnitfileWriter
{

   
    public String getName(){
        return "HalfPhoneUnitfileWriter";
    }
    
    public HalfPhoneUnitfileWriter(){
        unitlabelExt = ".hplab";
        corrPmExt = ".pm.corrected";
        LABELDIR = "HalfPhoneUnitfileWriter.labelDir";
        UNITFILE = "HalfPhoneUnitfileWriter.unitFile";
        CORRPMDIR = "HalfPhoneUnitfileWriter.corrPmDir";
    }
    
    public void initialiseComp()
    {
        maryDir = new File(db.getProp(db.FILEDIR));
        
        samplingRate = Integer.parseInt(db.getProp(db.SAMPLINGRATE));
        pauseSymbol = System.getProperty("pause.symbol", "pau");
    
        unitFileName = getProp(UNITFILE);
        unitlabelDir = new File(getProp(LABELDIR));
        if (!unitlabelDir.exists()){
            System.out.print(LABELDIR+" "+getProp(LABELDIR)
                    +" does not exist; ");
            if (!unitlabelDir.mkdir()){
                throw new Error("Could not create LABELDIR");
            }
            System.out.print("Created successfully.\n");
        } 
        aligner = new HalfPhoneLabelFeatureAligner();
        db.initialiseComponent(aligner);        
    }
    
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
        if (props == null){
            props = new TreeMap();
            String rootDir = db.getProp(db.ROOTDIR);
            props.put(LABELDIR, rootDir
                    +"halfphonelab"
                    +System.getProperty("file.separator"));
            props.put(UNITFILE, db.getProp(db.FILEDIR)
                    +"halfphoneUnits"+db.getProp(db.MARYEXT));           
            props.put(CORRPMDIR, rootDir
                    +"pm"                   
                    +System.getProperty("file.separator"));
        }
        return props;
    }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(LABELDIR, "directory containing the halfphone labels");
        props2Help.put(UNITFILE, "file containing all halfphone units. Will be created by this module");           
        props2Help.put(CORRPMDIR, "directory containing the corrected pitchmarks");
    }
    

}
