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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.*;

import de.dfki.lt.mary.unitselection.UnitFileReader;

public class PhoneUnitfileWriter extends VoiceImportComponent
{
    protected File maryDir;
    protected String unitFileName;
    protected File unitlabelDir;
    protected int samplingRate;
    protected String pauseSymbol;
    protected PhoneLabelFeatureAligner aligner;
    
    protected String unitlabelExt = ".lab";
    protected String corrPmExt = ".pm.corrected";

    protected DatabaseLayout db = null;
    protected int percent = 0;
    
    public String LABELDIR = "PhoneUnitfileWriter.labelDir";
    public String UNITFILE = "PhoneUnitfileWriter.unitFile";
    public String CORRPMDIR = "PhoneUnitfileWriter.corrPmDir";
    
    public String getName(){
        return "PhoneUnitfileWriter";
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
        aligner = new PhoneLabelFeatureAligner();
        db.initialiseComponent(aligner);        
    }
    
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           String rootDir = db.getProp(db.ROOTDIR);
           props.put(LABELDIR, rootDir
                   +"phonelab"
                   +System.getProperty("file.separator"));
           props.put(UNITFILE, db.getProp(db.FILEDIR)
                   +"phoneUnits"+db.getProp(db.MARYEXT));           
           props.put(CORRPMDIR, rootDir
                   +"pm"
                   +System.getProperty("file.separator"));
       }
       return props;
    }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(LABELDIR, "directory containing the phone labels");
        props2Help.put(UNITFILE, "file containing all phone units. Will be created by this module");           
        props2Help.put(CORRPMDIR, "directory containing the corrected pitchmarks");
    }
    
    public boolean compute() throws IOException
    {
        
        System.out.println("Unitfile writer started.");
        System.out.println("Verifying that unit feature and label files are perfectly aligned...");
        
        if (!aligner.compute()) throw new IllegalStateException("Database is NOT perfectly aligned. Cannot create unit file.");
        System.out.println("OK, alignment verified.");
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(unitFileName)));
        long posNumUnits = new MaryHeader(MaryHeader.UNITS).writeTo(out);
        out.writeInt(-1); // number of units; needs to be corrected later.
        out.writeInt(samplingRate);
        
        // Loop over all utterances
        int index = 0; // the unique index number of units in the unit file
        long globalStart = 0l; // time, given as sample position with samplingRate
        long localStart = 0l; // time, given as sample position with samplingRate
        long totalNbrSamples = 0l;
        long localNbrSamples = 0l;
        long localNbrSamplesFromPM = 0l;
        ESTTrackReader pmFile = null;
        for (int i=0; i<bnl.getLength(); i++) {
            percent = 100*i/bnl.getLength();
            /* Open the relevant pitchmark file */
            pmFile = new ESTTrackReader(getProp(CORRPMDIR)
                    + bnl.getName(i) +corrPmExt);
            // Output the utterance start marker: "null" unit
            out.writeLong( globalStart ); out.writeInt(-1);
            index++;
            // Open the label file and reset the local time pointer
            BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(unitlabelDir, bnl.getName(i) + unitlabelExt)), "UTF-8"));
            String line;
            localNbrSamples = 0l;
            localStart = 0l;
            // Skip label file header
            while ((line = labels.readLine()) != null) {
                if (line.startsWith("#")) break; // line starting with "#" marks end of header
            }
            // Now read the actual units
            while ((line = labels.readLine()) != null) {
                // Get the line and the double value in first position
                line = line.trim();
                if (line.equals("")) continue; // ignore empty lines
                String[] parts = line.split( "\\s", 2 );
                double endTime = Double.valueOf(parts[0]).doubleValue();
                /* Relocate the label-specific end time on the nearest pitchmark */
                endTime = (double)( pmFile.getClosestTime( endTime ) );
                long end = (long)( endTime * (double)(samplingRate) );
                // Output the unit
                int duration = (int) (end - localStart);
                out.writeLong( globalStart + localStart ); out.writeInt(duration);
                // System.out.println( "Unit [" + index + "] starts at [" + localStart + "] and has duration [" + duration + "]." );
                // Update various pointers
                localStart = end;
                index++;
                localNbrSamples += duration;
                totalNbrSamples += duration;
            }
            // Output the utterance end marker: "null" unit
            out.writeLong( globalStart + localStart ); out.writeInt(-1);
            index++;
            /* Locate the global start of the next file:
             * this corrects the discrpancy between the duration of the label file and the duration
             * of the pitchmark file (which is considered as the authority). */
            localNbrSamplesFromPM = (long)( (double)( pmFile.getTimeSpan() ) * (double)(samplingRate) );
            globalStart += localNbrSamplesFromPM;
            /* Clean the house */
            labels.close();
            System.out.println( "    " + bnl.getName(i) + " (" + index + ") (This file has [" + localNbrSamples
                    + "] samples from .lab, rectified to [" + localNbrSamplesFromPM
                    + "] from the pitchmarks, diff [" + (localNbrSamplesFromPM - localNbrSamples) + "], cumul [" + globalStart + "])" );
            if ( (localNbrSamplesFromPM - localNbrSamples) < 0 ) System.out.println( "BORK BORK BORK: .lab file longer than pitchmarks !" );
        }
        out.close();
        // Now index is the number of units. Set this in the file:
        RandomAccessFile raf = new RandomAccessFile(unitFileName, "rw");
        raf.seek(posNumUnits);
        raf.writeInt(index);
        raf.close();
        System.out.println("Number of processed units: " + index );

        
        UnitFileReader tester = new UnitFileReader(unitFileName);
        int unitsOnDisk = tester.getNumberOfUnits();
        if (unitsOnDisk == index) {
            System.out.println("Can read right number of units");
            return true;
        } else {
            System.out.println("Read wrong number of units: "+unitsOnDisk);
            return false;
        }
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

    public static void main(String[] args) throws IOException
    {
        PhoneUnitfileWriter ufw = new PhoneUnitfileWriter();
        new DatabaseLayout(ufw); 
        ufw.compute();
    }

}
