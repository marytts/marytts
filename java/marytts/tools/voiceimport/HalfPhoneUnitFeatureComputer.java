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

package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.util.io.FileUtils;

/**
 * @author marc
 *
 */
public class HalfPhoneUnitFeatureComputer extends PhoneUnitFeatureComputer 
{
    public static final String[] HALFPHONE_FEATURES = new String[] {
        "halfphone_lr",
        "halfphone_unitname"
    };
    public static final String HALFPHONE_UNITNAME = "halfphone_unitname";
    
    public String getName(){
        return "HalfPhoneUnitFeatureComputer";
    }
    
    public HalfPhoneUnitFeatureComputer(){        
        featsExt = ".hpfeats";
        FEATUREDIR = "HalfPhoneUnitFeatureComputer.featureDir";
        ALLOPHONES = "HalfPhoneUnitFeatureComputer.allophonesDir";
        FEATURELIST = "HalfPhoneUnitFeatureComputer.featureFile";
        MARYSERVERHOST = "HalfPhoneUnitFeatureComputer.maryServerHost";
        MARYSERVERPORT = "HalfPhoneUnitFeatureComputer.maryServerPort";   
    }
    
    @Override
    public void initialiseComp()
    throws Exception
    {       
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
        File featureFile = new File(getProp(FEATURELIST));
        if (!featureFile.exists()) {
            throw new Error("No feature file: '"+getProp(FEATURELIST)+"'");
        }
        System.out.println("Loading features from file "+getProp(FEATURELIST));
        try {
            featureList = FileUtils.getFileAsString(featureFile, "UTF-8");
            featureList = featureList.replaceAll("\\s+", " ");
            // Make sure specific halfphone features are included:
            for (String f : HALFPHONE_FEATURES) {
                if (!featureList.contains(f)) {
                    featureList = f + " " + featureList;
                }
            }
            if (!featureList.startsWith(HALFPHONE_UNITNAME)) {
                // HALFPHONE_UNITNAME must be the first one in the list
                featureList = featureList.replaceFirst("\\s+"+HALFPHONE_UNITNAME+"\\s+", " ");
                featureList = HALFPHONE_UNITNAME + " " + featureList;

            }
        } catch (IOException e) {
            throw new IOException("Cannot read feature list", e);
        }
        maryInputType  = "ALLOPHONES";
        maryOutputType = "HALFPHONE_TARGETFEATURES";
    }
    
      public SortedMap<String,String> getDefaultProps(DatabaseLayout theDb){
        this.db = theDb;
       if (props == null){
           props = new TreeMap<String, String>();
           props.put(FEATUREDIR, db.getProp(db.ROOTDIR)
                        +"halfphonefeatures"
                        +System.getProperty("file.separator"));
           props.put(ALLOPHONES, db.getProp(db.ROOTDIR)
                   +"allophones"
                   +System.getProperty("file.separator"));
           props.put(FEATURELIST,
                   db.getProp(db.CONFIGDIR) + "features.txt");
           props.put(MARYSERVERHOST,"localhost");
           props.put(MARYSERVERPORT,"59125");
       } 
       return props;
      }
      
      protected void setupHelp(){
         props2Help = new TreeMap<String, String>();
         props2Help.put(FEATUREDIR, "directory containing the halfphone features." 
                 +"Will be created if it does not exist.");
         props2Help.put(ALLOPHONES, "Directory of corrected Allophones files.");
         props2Help.put(MARYSERVERHOST,"the host were the Mary server is running, default: \"localhost\"");
         props2Help.put(MARYSERVERPORT,"the port were the Mary server is listening, default: \"59125\"");
     }
}
