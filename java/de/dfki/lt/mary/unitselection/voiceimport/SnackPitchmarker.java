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

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileWriter;

import de.dfki.lt.mary.unitselection.voiceimport.SphinxTrainer.StreamGobbler;
import de.dfki.lt.signalproc.util.PraatTextfileDoubleDataSource;

public class SnackPitchmarker extends VoiceImportComponent
{
    protected DatabaseLayout db = null;
    protected String pmExt = ".pm";
    protected String snackPmExt = ".pm.snack";
    protected String scriptFileName;
    
    private int percent = 0;
    
   
    public final String PMDIR = "SnackPitchmarker.pmDir";    
    public final String MINPITCH = "SnackPitchmarker.minPitch";
    public final String MAXPITCH = "SnackPitchmarker.maxPitch";

    protected void setupHelp()
    {
        if (props2Help ==null){
            props2Help = new TreeMap();
            props2Help.put(MINPITCH,"minimum value for the pitch (in Hz). Default: female 100, male 75");
            props2Help.put(MAXPITCH,"maximum value for the pitch (in Hz). Default: female 500, male 300");    
            props2Help.put(PMDIR, "directory containing the pitchmark files. Will be created if" 
                    +"it does not exist");
        }
        

    }
    
     public final String getName(){
        return "SnackPitchmarker";
    }
    
    public void initialiseComp()
    {
        scriptFileName = db.getProp(db.TEMPDIR)+"pm.tcl";
    }
    
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();       
           if (db.getProp(db.GENDER).equals("female")){
               props.put(MINPITCH,"100");
               props.put(MAXPITCH,"500");
           } else {
               props.put(MINPITCH,"75");
               props.put(MAXPITCH,"300");
           }
           props.put(PMDIR, db.getProp(db.ROOTDIR)
                   +"pm"
                   +System.getProperty("file.separator"));
       }
       return props;
    }
    
    
    
    /**
     * The standard compute() method of the VoiceImportComponent interface.
     */
    public boolean compute() throws Exception {
        
        String[] baseNameArray = bnl.getListAsArray();
        System.out.println( "Computing pitchmarks for " + baseNameArray.length + " utterances." );

        /* Ensure the existence of the target pitchmark directory */
        File dir = new File(getProp(PMDIR));
        if (!dir.exists()) {
            System.out.println( "Creating the directory [" + getProp(PMDIR) + "]." );
            dir.mkdir();
        }        

        //TODO: if we are going to use the script, 
        /**if (System.getProperty("TCLLIBPATH")==null){
            System.out.println("Can not run SnackPitchmarker; need property "
                    +"TCLLIBPATH to point to snack installation directory, "
                    +"for example, /home/cl-home/hunecke/anna/snack2.2/");
            return false;
        }**/
        
        /* execute snack */        
        for ( int i = 0; i < baseNameArray.length; i++ ) {
            percent = 100*i/baseNameArray.length;
            String infile = db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
            String outfile = getProp(PMDIR) + baseNameArray[i] + snackPmExt;
            System.out.println("Writing pm file to "+outfile);
            Process snack = Runtime.getRuntime().exec(scriptFileName+" "+infile+" "+outfile
                    +" "+getProp(MAXPITCH)+" "+getProp(MINPITCH));
        
             StreamGobbler errorGobbler = new 
             StreamGobbler(snack.getErrorStream(), "err");            
             
             //read from output stream
             StreamGobbler outputGobbler = new 
             StreamGobbler(snack.getInputStream(), "out");    
             
             
             //start reading from the streams
             errorGobbler.start();
             outputGobbler.start();
             
             //close everything down
             snack.waitFor();
             snack.exitValue();
             
             /* Now convert the snack format into EST pm format
              double[] pm = new PraatTextfileDoubleDataSource(new FileReader(pointprocessFilename)).getAllData();
            float[] pitchmarks = new float[pm.length];
            for (int i=0; i<pitchmarks.length; i++) pitchmarks[i] = (float) pm[i];
            new ESTTrackWriter(pitchmarks, null, "pitchmarks").doWriteAndClose(pmFilename, false, false);
            
            // And correct pitchmark locations
            pitchmarks = adjustPitchmarks(basename, pitchmarks);
            new ESTTrackWriter(pitchmarks, null, "pitchmarks").doWriteAndClose(correctedPmFilename, false, false);
            **/
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
        return percent;
    }

}
