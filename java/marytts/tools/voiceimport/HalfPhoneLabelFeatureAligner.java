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
public class HalfPhoneLabelFeatureAligner extends PhoneLabelFeatureAligner {
    
    public String getName(){
        return "HalfPhoneLabelFeatureAligner";
    }
    
    public HalfPhoneLabelFeatureAligner(){
        featsExt = ".hpfeats";
        labExt = ".hplab";
        FEATUREDIR = "HalfPhoneLabelFeatureAligner.featureDir";
        LABELDIR = "HalfPhoneLabelFeatureAligner.labelDir";
    }
    
    @Override
    public void initialiseComp()
    throws Exception
    {
        this.featureComputer = new HalfPhoneUnitFeatureComputer();
        db.initialiseComponent(featureComputer); 
        
        this.pauseSymbol = System.getProperty("pause.symbol", "_");
        File unitfeatureDir = new File(getProp(FEATUREDIR));
        if (!unitfeatureDir.exists()){
            System.out.print(FEATUREDIR+" "+getProp(FEATUREDIR)
                    +" does not exist; ");
            if (!unitfeatureDir.mkdir()){
                throw new Error("Could not create FEATUREDIR");
            }
            System.out.print("Created successfully.\n");
        }
        File unitlabelDir = new File(getProp(LABELDIR));
        if (!unitlabelDir.exists()){
            System.out.print(LABELDIR+" "+getProp(LABELDIR)
                    +" does not exist; ");
            if (!unitlabelDir.mkdir()){
                throw new Error("Could not create LABELDIR");
            }
            System.out.print("Created successfully.\n");
        }  
    }
    
    public SortedMap<String,String> getDefaultProps(DatabaseLayout theDb){
        this.db = theDb;
       if (props == null){
           props = new TreeMap<String, String>();
           props.put(FEATUREDIR, db.getProp(db.ROOTDIR)
                        +"halfphonefeatures"
                        +System.getProperty("file.separator"));
           props.put(LABELDIR, db.getProp(db.ROOTDIR)
                        +"halfphonelab"
                        +System.getProperty("file.separator"));
       }
       return props;
    }

    protected void setupHelp(){
        props2Help = new TreeMap<String, String>();
        props2Help.put(FEATUREDIR, "directory containing the phone features.");
        props2Help.put(LABELDIR, "directory containing the phone labels");
    }
    

}

