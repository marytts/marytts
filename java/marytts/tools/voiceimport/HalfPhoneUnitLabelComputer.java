/**
 * Copyright 2000-2009 DFKI GmbH.
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


import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Compute unit labels from phone labels.
 * @author schroed
 *
 */
public class HalfPhoneUnitLabelComputer extends PhoneUnitLabelComputer
{    
    public String getName(){
        return "HalfPhoneUnitLabelComputer";      
    }
    
    public HalfPhoneUnitLabelComputer(){
        unitlabelExt = ".hplab";
        LABELDIR = "HalfPhoneUnitLabelComputer.labelDir";
    } 
    
     public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(LABELDIR, db.getProp(db.ROOTDIR)
                        +"halfphonelab"
                        +System.getProperty("file.separator"));
       }
       return props;
    }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(LABELDIR,"directory containing the halfphone labels." 
                +"Will be created if it does not exist.");
    } 
     
    protected String[] toUnitLabels(String[] phoneLabels)
    {
        // We will create exactly two half phones for every phone:
        String[] halfPhoneLabels = new String[2*phoneLabels.length];
        float startTime = 0;
        int unitIndex = 0;
        for (int i=0; i<phoneLabels.length; i++) {
            unitIndex++;
            StringTokenizer st = new StringTokenizer(phoneLabels[i]);
            String endTimeString = st.nextToken();
            String dummyNumber = st.nextToken();
            String phone = st.nextToken();
            assert !st.hasMoreTokens();
            float endTime = Float.parseFloat(endTimeString);
            float duration = endTime - startTime;
            assert duration > 0 : "Duration is not > 0 for phone "+i+" ("+phone+")";
            float midTime = startTime + duration/2;
            String leftUnitLine = midTime + " " + unitIndex + " " + phone + "_L";
            unitIndex++;
            String rightUnitLine = endTime + " " + unitIndex + " " + phone + "_R";
            halfPhoneLabels[2*i] = leftUnitLine;
            halfPhoneLabels[2*i+1] = rightUnitLine;
            startTime = endTime;
        }
        return halfPhoneLabels;
    }
}

