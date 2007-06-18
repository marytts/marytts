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
public class HalfPhoneUnitFeatureComputer extends PhoneUnitFeatureComputer 
{

   
    public String getName(){
        return "halfPhoneUnitFeatureComputer";
    }
    
    public HalfPhoneUnitFeatureComputer(){
        FEATUREDIR = "halfPhoneUnitFeatureComputer.featureDir";
        FEATUREEXT = "halfPhoneUnitFeatureComputer.featureExt";
        MARYSERVERHOST = "halfPhoneUnitFeatureComputer.maryServerHost";
        MARYSERVERPORT = "halfPhoneUnitFeatureComputer.maryServerPort";    
    }
    
     public void initialise( BasenameList setbnl, SortedMap newProps )
    {
        this.bnl = setbnl;
        this.props = newProps;        
        locale = db.getProp(db.LOCALE);        
        mary = null; // initialised only if needed   
        unitfeatureDir = new File(getProp(FEATUREDIR));
        if (!unitfeatureDir.exists()){
            System.out.print(FEATUREDIR+" "+getProp(FEATUREDIR)
                    +" does not exist; ");
            if (!unitfeatureDir.mkdir()){
                throw new Error("Could not create FEATUREDIR");
            }
            System.out.print("Created successfully.\n");
        }    
        featsExt = getProp(FEATUREEXT);
        maryInputType = "RAWMARYXML";
        if (locale.equals("de")) maryOutputType = "HALFPHONE_TARGETFEATURES_DE";
        else if (locale.equals("en")) maryOutputType = "HALFPHONE_TARGETFEATURES_EN";
        else throw new IllegalArgumentException("Unsupported locale: "+locale);
    }
    
      public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(FEATUREDIR, db.getProp(db.ROOTDIR)
                        +"halfphonefeatures"
                        +System.getProperty("file.separator"));
           props.put(FEATUREEXT,".hpfeats");
           props.put(MARYSERVERHOST,"localhost");
           props.put(MARYSERVERPORT,"59125");
       } 
       return props;
      }
}
