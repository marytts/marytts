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

import de.dfki.lt.signalproc.util.PraatTextfileDoubleDataSource;

public class PraatPitchmarker extends VoiceImportComponent
{
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;

    private int percent = 0;
    
    public final String COMMAND = "praatPitchmarker.command";
    public final String TEMPSCRIPT = "praatPitchmarker.tempScript";
    public final String MINPITCH = "praatPitchmarker.minPitch";
    public final String MAXPITCH = "praatPitchmarker.maxPitch";
    public final String CORRPMDIR = "praatPitchmarker.corrPmDir";
    public final String CORRPMEXT = "praatPitchmarker.corrPmExt";
    public final String PMDIR = "praatPitchmarker.pmDir";
    public final String PMEXT = "praatPitchmarker.pmExt";
    public final String POINTPEXT = "praatPitchmarker.pointPExt";
    
     public final String getName(){
        return "praatPitchmarker";
    }
    
    public void initialise( BasenameList setbnl, SortedMap newProps )
    {
         this.bnl = setbnl;
        this.props = newProps;
    }
    
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(COMMAND,"praat");
           props.put(TEMPSCRIPT,db.getProp(db.TEMPDIR)
                   +"script.praat");
           if (db.getProp(db.GENDER).equals("female")){
               props.put(MINPITCH,"100");
               props.put(MAXPITCH,"500");
           } else {
               props.put(MINPITCH,"75");
               props.put(MAXPITCH,"300");
           }
           String rootDir = db.getProp(db.ROOTDIR);
           props.put(CORRPMDIR, rootDir
                   +"pm"
                   +System.getProperty("file.separator"));
           props.put(CORRPMEXT, ".pm.corrected");
           props.put(PMDIR, rootDir
                   +"pm"
                   +System.getProperty("file.separator"));
           props.put(PMEXT, ".pm");
           props.put(POINTPEXT, ".PointProcess");
       }
       return props;
    }
    
    /**
     * Shift the pitchmarks to the closest peak.
     */
    private float[] shiftToClosestPeak( float[] pmIn, short[] w, int sampleRate ) {
        
        final int HORIZON = 32; // <= number of samples to seek before and after the pitchmark
        float[] pmOut = new float[pmIn.length];
        
        /* Browse the pitchmarks */
        int pm = 0;
        int pmwmax = w.length - 1;
        int TO = 0;
        int max = 0;
        for ( int pi = 0; pi < pmIn.length; pi++ ) {
            pm = (int)( pmIn[pi] * sampleRate );
            // If the pitchmark goes out of the waveform (this sometimes
            // happens with the last one due to rounding errors), just clip it.
            if ( pm > pmwmax ) {
                // If this was not the last pitchmark, there is a problem
                if ( pi < (pmIn.length-1)) {
                    throw new RuntimeException( "Some pitchmarks are located above the location of the last waveform sample !" );
                }
                // Else, if it was the last pitchmark, clip it:
                pmOut[pi] = (float) ((double)(pmwmax) / (double)(sampleRate));
            }
            // Else, if the pitchmark is in the waveform:
            else {
                /* Seek the max of the wav samples around the pitchmark */
                max = pm;
                // - Back:
                TO = (pm-HORIZON) < 0 ? 0 : (pm-HORIZON);
                for ( int i = pm-1; i >= TO; i-- ) {
                    if ( w[i] > w[max] ) max = i;
                }
                // - Forth:
                TO = (pm+HORIZON+1) > w.length ? w.length : (pm+HORIZON+1);
                for ( int i = pm+1; i < TO; i++ ) {
                    if ( w[i] >= w[max] ) max = i;
                }
                /* Translate the pitchmark */
                pmOut[pi] = (float) ( (double)(max) / (double)(sampleRate) );
            }
        }
        
        return pmOut;
    }
    
    /**
     * Shift the pitchmarks to the previous zero crossing.
     */
    private float[] shiftToPreviousZero( float[] pmIn, short[] w, int sampleRate ) {
        
        final int HORIZON = 32; // <= number of samples to seek before the pitchmark
        float[] pmOut = new float[pmIn.length];
        
        /* Browse the pitchmarks */
        int pm = 0;
        int TO = 0;
        int zero = 0;
        for ( int pi = 0; pi < pmIn.length; pi++ ) {
            pm = (int)( pmIn[pi] * sampleRate );
            /* If the initial pitchmark is on a zero, don't shift the pitchmark. */
            if ( w[pm] == 0 ) {
                pmOut[pi] = pmIn[pi];
                continue;
            }
            /* Else: */
            /* Seek the zero crossing preceding the pitchmark */
            TO = (pm-HORIZON) < 0 ? 0 : (pm-HORIZON);
            for ( zero = pm; ( zero > TO ) && ( (w[zero]*w[zero+1]) > 0 ); zero-- );
            /* If no zero crossing was found, don't move the pitchmark */
            if ( (zero == TO) && ( (w[zero]*w[zero+1]) > 0 )  ) {
                pmOut[pi] = pmIn[pi];
            }
            /* If a zero crossing was found, translate the pitchmark */
            else {
                pmOut[pi] = (float) ( (double)( (-w[zero]) <  w[zero+1] ?  zero : (zero+1) ) / (double)(sampleRate) );
            }
        }
        
        return pmOut;
    }
    
    /**
     * Adjust pitchmark position to the zero crossing preceding the closest peak.
     * @param basename basename of the corresponding wav file
     * @param pitchmarks the input pitchmarks
     * @return the adjusted pitchmarks
     */
    private float[] adjustPitchmarks( String basename, float[] pitchmarks ) throws IOException
    {
        /* Load the wav file */
        String fName = db.getProp(db.WAVDIR) + basename + db.getProp(db.WAVEXT);
        WavReader wf = new WavReader( fName );
        short[] w = wf.getSamples();
        float[] pmOut = null;
        try {
            /* Shift to the closest peak */
            pmOut = shiftToClosestPeak( pitchmarks, w, wf.getSampleRate() );
            /* Shift to the zero immediately preceding the closest peak */
            pmOut = shiftToPreviousZero( pmOut, w, wf.getSampleRate() );
        } catch ( RuntimeException e ) {
            throw new RuntimeException( "For utterance [" + basename + "]:" , e );
        }
        return pmOut;
    }
    
    private boolean praatPitchmarks(String basename) throws IOException
    {
        String wavFilename = db.getProp(db.WAVDIR) + basename + db.getProp(db.WAVEXT);
        String pointprocessFilename = getProp(PMDIR)+basename+getProp(POINTPEXT);
        String pmFilename = getProp(PMDIR)+basename+getProp(PMEXT);
        String correctedPmFilename = getProp(CORRPMDIR) + basename + getProp(CORRPMEXT);

        File script = new File(getProp(TEMPSCRIPT));
        if (script.exists()) script.delete();
        PrintWriter toScript = new PrintWriter(new FileWriter(script));
        toScript.println("Read from file... "+wavFilename);
        toScript.println("Rename... sound");
        // First, low-pass filter the speech signal to make it more robust against noise
        // (i.e., mixed noise+periodicity regions treated more likely as periodic)
        toScript.println("Filter (pass Hann band)... 0 1000 100");
        // Then determine pitch curve
        toScript.println("To Pitch... 0 "+getProp(MINPITCH)+" "+getProp(MAXPITCH));
        toScript.println("Rename... pitch");
        // Get some debug info:
        toScript.println("startTime = Get start time");
        toScript.println("endTime = Get end time");
        toScript.println("min_f0 = Get minimum... startTime endTime Hertz Parabolic");
        toScript.println("max_f0 = Get maximum... startTime endTime Hertz Parabolic");
        toScript.println("select Sound sound");
        toScript.println("plus Pitch pitch");
        // And convert to pitch marks
        toScript.println("To PointProcess (cc)");
        // Fill in 100 Hz pseudo-pitchmarks in unvoiced regions:
        toScript.println("Voice... 0.01 0.02000000001");
        toScript.println("Write to short text file... "+pointprocessFilename);
        toScript.println("printline "+basename+"   f0 range: 'min_f0:0' - 'max_f0:0' Hz");
        toScript.println("Quit");
        toScript.close();

        Process praat = Runtime.getRuntime().exec(getProp(COMMAND)+" "+getProp(TEMPSCRIPT));
        final BufferedReader fromPraat = new BufferedReader(new InputStreamReader(praat.getInputStream()));
        new Thread() {
            public void run() {
                String line;
                try {
                    while ((line = fromPraat.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {} // fail silently 
            }
        }.start();
        try {
            praat.waitFor();
        } catch (InterruptedException ie) {} // ignore
        
        // Now convert the praat format into EST pm format
        double[] pm = new PraatTextfileDoubleDataSource(new FileReader(pointprocessFilename)).getAllData();
        float[] pitchmarks = new float[pm.length];
        for (int i=0; i<pitchmarks.length; i++) pitchmarks[i] = (float) pm[i];
        new ESTTrackWriter(pitchmarks, null, "pitchmarks").doWriteAndClose(pmFilename, false, false);
        
        // And correct pitchmark locations
        pitchmarks = adjustPitchmarks(basename, pitchmarks);
        new ESTTrackWriter(pitchmarks, null, "pitchmarks").doWriteAndClose(correctedPmFilename, false, false);
        return true;
    }
    
    
    
    /**
     * The standard compute() method of the VoiceImportComponent interface.
     */
    public boolean compute() throws IOException {
        
        String[] baseNameArray = bnl.getListAsArray();
        System.out.println( "Computing pitchmarks for " + baseNameArray.length + " utterances." );

        /* Ensure the existence of the target pitchmark directory */
        File dir = new File(getProp(PMDIR));
        if (!dir.exists()) {
            System.out.println( "Creating the directory [" + getProp(PMDIR) + "]." );
            dir.mkdir();
        }
        /* Ensure the existence of the target directory for corrected pitchmarks */
        dir = new File(getProp(CORRPMDIR));
        if (!dir.exists()) { 
            System.out.println( "Creating the directory [" + getProp(CORRPMDIR) + "]." );
            dir.mkdir();
        }


        for ( int i = 0; i < baseNameArray.length; i++ ) {
            percent = 100*i/baseNameArray.length;
            praatPitchmarks(baseNameArray[i]);
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
