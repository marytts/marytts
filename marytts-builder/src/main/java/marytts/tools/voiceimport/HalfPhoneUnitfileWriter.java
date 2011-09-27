/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.voiceimport;

import java.io.File;
import java.util.SortedMap;
import java.util.TreeMap;

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
        LABELDIR = "HalfPhoneUnitfileWriter.labelDir";
        UNITFILE = "HalfPhoneUnitfileWriter.unitFile";
    }
    
    @Override
    protected void initialiseComp()
    throws Exception
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
        }
        return props;
    }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(LABELDIR, "directory containing the halfphone labels");
        props2Help.put(UNITFILE, "file containing all halfphone units. Will be created by this module");           
    }
    

}

