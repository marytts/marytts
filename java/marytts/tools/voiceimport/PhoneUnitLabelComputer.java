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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import marytts.modules.phonemiser.AllophoneSet;

/**
 * Compute unit labels from phone labels.
 * @author schroed
 *
 */
public class PhoneUnitLabelComputer extends VoiceImportComponent
{
    protected File phonelabelDir;
    protected File unitlabelDir;
    protected String pauseSymbol;
    
    protected String unitlabelExt = ".lab";
    
    protected DatabaseLayout db = null;
    protected int percent = 0;
    protected int basenameIndex;
    
    public String LABELDIR = "PhoneUnitLabelComputer.labelDir";
    
    public String getName(){
        return "PhoneUnitLabelComputer";
    }
    
     public void initialiseComp()
    {
         try {
            pauseSymbol = AllophoneSet.getAllophoneSet(db.getProp(db.ALLOPHONESET)).getSilence().name();
        } catch (Exception e) {
            System.err.println("Cannot get pause symbol from allophone set -- will assume default '_'");
            pauseSymbol = "_";
        }

        this.unitlabelDir = new File(getProp(LABELDIR));
        if (!unitlabelDir.exists()){
            System.out.print(LABELDIR+" "+getProp(LABELDIR)
                    +" does not exist; ");
            if (!unitlabelDir.mkdir()){
                throw new Error("Could not create LABELDIR");
            }
            System.out.print("Created successfully.\n");
        }  
    }
    
     public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(LABELDIR, db.getProp(db.ROOTDIR)
                        +"phonelab"
                        +System.getProperty("file.separator"));
       }
       return props;
    }
     
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(LABELDIR,"directory containing the phone labels." 
                +"Will be created if it does not exist.");
    } 
    
    public boolean compute() throws IOException
    {
        
        phonelabelDir = new File(db.getProp(db.LABDIR));
        if (!phonelabelDir.exists()) throw new IOException("No such directory: "+ phonelabelDir);
        
        System.out.println( "Computing unit labels for " 
                + basenameList.getLength() + " files." );
        System.out.println( "From phonetic label files: " 
                + db.getProp(db.LABDIR) + "*" + db.getProp(db.LABEXT));
        System.out.println( "To       unit label files: " 
                + getProp(LABELDIR) + "*" + unitlabelExt );
        for (basenameIndex=0; basenameIndex<basenameList.getLength(); basenameIndex++) {
            percent = 100*basenameIndex/basenameList.getLength();
            computePhoneLabel(basenameList.getName(basenameIndex));
        }
        System.out.println("Finished computing unit labels");
        return true;
    }
    
    public void computePhoneLabel(String baseName) throws IOException{
        File labFile = 
            new File( db.getProp(db.LABDIR) 
                    + baseName + db.getProp(db.LABEXT) );
        if ( !labFile.exists() ) {
            System.out.println( "Utterance [" + baseName + "] does not have a phonetic label file." );
            System.out.println( "Removing this utterance from the base utterance list." );
            basenameList.remove( baseName );
            basenameIndex--;
        }
        else {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream( labFile ), "UTF-8"));
            String labelFile = getProp(LABELDIR)+ baseName + unitlabelExt;
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(labelFile)), "UTF-8"));
            // Merge adjacent pauses into one: In a sequence of pauses,
            // only remember the last one.
            String pauseLine = null;
            String line;
            List header = new ArrayList();
            boolean readingHeader = true;
            List phoneLabels = new ArrayList();
            while ((line = in.readLine())!= null) {
                if (readingHeader) {
                    if (line.trim().equals("#")) {
                        // found end of header
                        readingHeader = false;
                    }
                    header.add(line);                        
                } else {
                    // Not reading header
                    // Verify if this is a pause unit
                    String phone = getPhone(line);
                    if (pauseSymbol.equals(phone)) {
                        pauseLine = line; // remember only the latest pause line
                    } else { // a non-pause symbol
                        if (pauseLine != null) {
                            phoneLabels.add(pauseLine);
                            pauseLine = null;
                        }
                        phoneLabels.add(line);
                    }
                }
            }
            if (pauseLine != null) {
                phoneLabels.add(pauseLine);
            }
            String[] phoneLabelLines = (String[]) phoneLabels.toArray(new String[0]);
            String[] unitLabelLines = toUnitLabels(phoneLabelLines);
            out.println("format: end time, unit index, phone");
            for (int h=0; h<header.size(); h++) {                
                out.println(header.get(h));
            }
            for (int u=0; u<unitLabelLines.length; u++) {
                out.println(unitLabelLines[u]);
            }
            out.flush();
            out.close();
            in.close();
            System.out.println( "    " + baseName );
        }
    }
    
    /**/
    private String getPhone(String line)
    {
        StringTokenizer st = new StringTokenizer(line.trim());
        // The third token in each line is the label
        if (st.hasMoreTokens()) st.nextToken();
        if (st.hasMoreTokens()) st.nextToken();
        if (st.hasMoreTokens()) return st.nextToken();
        return null;
    }
    
    /**
     * Convert phone labels to unit labels. This base implementation
     * returns the phone labels; subclasses may want to override that
     * behaviour.
     * @param phoneLabels the phone labels, one phone per line, with each
     * line containing three fields: 1. the end time of the current phone,
     * in seconds, since the beginning of the file; 2. a number to be ignored;
     * 3. the phone symbol.
     * @return an array of lines, in the same format as the phoneLabels input
     * array, but with unit symbols instead of phone symbols. The
     * number in the middle now denotes the unit index. This array may
     * or may not have the same number of lines as phoneLabels.
     */
    protected String[] toUnitLabels(String[] phoneLabels)
    {
        String[] unitLabels = new String[phoneLabels.length];
        int unitIndex = 0;
        for (int i=0;i<phoneLabels.length;i++){
            String line = phoneLabels[i];
            unitIndex++;
            StringTokenizer st = new StringTokenizer(line.trim());
            //first token is time
            String time = st.nextToken();
            //next token is some number, throw away
            st.nextToken();
            //next token is phone
            String phone = st.nextToken();
            unitLabels[i] = time+" "+unitIndex+" "+phone;
        }
        return unitLabels; 
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }

}

