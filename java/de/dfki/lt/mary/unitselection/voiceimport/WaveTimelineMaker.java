/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;

import de.dfki.lt.mary.unitselection.Datagram;

/**
 * The WaveTimelineMaker class takes a database root directory and a list of basenames,
 * and split the waveforms as datagrams to be stored in a timeline in Mary format.
 * 
 * @author sacha
 */
public class WaveTimelineMaker extends VoiceImportComponent
{ 

    protected DatabaseLayout db = null;
    protected int percent = 0;
    protected String corrPmExt = ".pm.corrected";
    public final String CORRPMDIR = "WaveTimelineMaker.corrPmDir";
    public final String WAVETIMELINE = "WaveTimelineMaker.waveTimeline";
    
     public final String getName(){
        return "WaveTimelineMaker";
    }    
    
    public SortedMap getDefaultProps(DatabaseLayout db){
       this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(CORRPMDIR, db.getProp(db.ROOTDIR)
           				+"pm"
           				+System.getProperty("file.separator"));
           props.put(WAVETIMELINE, db.getProp(db.FILEDIR)
                        +"timeline_waveforms"+db.getProp(db.MARYEXT));
       }
       return props;
   }
    
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(CORRPMDIR,"directory containing the corrected pitchmarks");
        props2Help.put(WAVETIMELINE,"file containing all wave files. Will be created by this module");
    }
    
    
    /**
     *  Reads and concatenates a list of waveforms into one single timeline file.
     *
     */
    public boolean compute()
    {
        System.out.println("---- Making a pitch synchronous waveform timeline\n\n");
        
        /* Export the basename list into an array of strings */
        String[] baseNameArray = bnl.getListAsArray();
        System.out.println("Processing [" + baseNameArray.length + "] utterances.\n");
        
       
        
        try{
            /* 1) Determine the reference sampling rate as being the sample rate of the first encountered
             *    wav file */
            WavReader wav = new WavReader( db.getProp(db.WAVDIR) 
                    + baseNameArray[0] + db.getProp(db.WAVEXT));
            int globSampleRate = wav.getSampleRate();
            System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz." );
            
            System.out.println("---- Folding the wav files according to the pitchmarks..." );
            
            /* 2) Open the destination timeline file */
            
            /* Make the file name */
            String waveTimelineName = getProp(WAVETIMELINE);
            System.out.println( "Will create the waveform timeline in file [" + waveTimelineName + "]." );
            
            /* Processing header: */
            String processingHeader = "\n";
            
            /* Instantiate the TimelineWriter: */
            TimelineWriter waveTimeline = new TimelineWriter( waveTimelineName, processingHeader, globSampleRate, 0.01 );
            
            /* 3) Write the datagrams and feed the index */
            
            float totalDuration = 0.0f;  // Accumulator for the total timeline duration
            long totalTime = 0l;
            int numDatagrams = 0;
            
            /* For each EST track file: */
            ESTTrackReader pmFile = null;
            for ( int i = 0; i < baseNameArray.length; i++ ) {
                percent = 100*i/baseNameArray.length;

                /* - open+load */
                System.out.println( baseNameArray[i] );
                pmFile = new ESTTrackReader( getProp(CORRPMDIR)
                        		+ baseNameArray[i] + corrPmExt);
                totalDuration += pmFile.getTimeSpan();
                wav = new WavReader( db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT) );
                short[] wave = wav.getSamples();
                /* - Reset the frame locations in the local file */
                int frameStart = 0;
                int frameEnd = 0;
                int duration = 0;
                long localTime = 0l;
                /* - For each frame in the WAV file: */
                for ( int f = 0; f < pmFile.getNumFrames(); f++ ) {
                    
                    /* Locate the corresponding segment in the wave file */
                    frameStart = frameEnd;
                    frameEnd = (int)( (double)pmFile.getTime( f ) * (double)(globSampleRate) );
                    assert frameEnd <= wave.length : "Frame ends after end of wave data: " + frameEnd + " > " + wave.length;
                    duration = frameEnd - frameStart;
                    ByteArrayOutputStream buff =  new ByteArrayOutputStream(2*duration);
                    DataOutputStream subWave = new DataOutputStream( buff );
                    for (int k = 0; k < duration; k++) {
                        subWave.writeShort( wave[frameStart+k] );
                    }
                    
                    /* Feed the datagram to the timeline */
                    waveTimeline.feed( new Datagram(duration, buff.toByteArray()), globSampleRate );
                    totalTime += duration;
                    localTime += duration;
                    numDatagrams++;
                }
                // System.out.println( baseNameArray[i] + " -> pm file says [" + localTime + "] samples, wav file says ["+ wav.getNumSamples() + "] samples." );
            }
            
            System.out.println("---- Done." );
            
            /* 7) Print some stats and close the file */
            System.out.println( "---- Waveform timeline result:");
            System.out.println( "Number of files scanned: " + baseNameArray.length );
            System.out.println( "Total speech duration: [" + totalTime + "] samples / [" + ((float)(totalTime) / (float)(globSampleRate)) + "] seconds." );
            System.out.println( "(Speech duration approximated from EST Track float times: [" + totalDuration + "] seconds.)" );
            System.out.println( "Number of frames: [" + numDatagrams + "]." );
            System.out.println( "Size of the index: [" + waveTimeline.idx.getNumIdx() + "] ("
                    + (waveTimeline.idx.getNumIdx() * 16) + " bytes, i.e. "
                    + ( (double)(waveTimeline.idx.getNumIdx()) * 16.0 / 1048576.0) + " megs)." );
            System.out.println( "---- Waveform timeline done.");
            
            waveTimeline.close();
        }
        catch ( SecurityException e ) {
            System.err.println( "Error: you don't have write access to the target database directory." );
            e.printStackTrace();
            return false;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            return false;
        }
        
        return( true );
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
