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
import java.io.File;

import de.dfki.lt.mary.unitselection.Datagram;

/**
 * The WaveTimelineMaker class takes a database root directory and a list of basenames,
 * and split the waveforms as datagrams to be stored in a timeline in Mary format.
 * 
 * @author sacha
 */
public class WaveTimelineMaker implements VoiceImportComponent
{ 

    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    
    public WaveTimelineMaker( DatabaseLayout setdb, BasenameList setbnl ) {
        this.db = setdb;
        this.bnl = setbnl;
    }
    
    /**
     *  Reads and concatenates a list of waveforms into one single timeline file.
     *
     */
    public boolean compute()
    {
        System.out.println("---- Making a pitch synchronous waveform timeline\n\n");
        System.out.println("Base directory: " + db.rootDirName() + "\n");
        
        /* Export the basename list into an array of strings */
        String[] baseNameArray = bnl.getListAsArray();
        System.out.println("Processing [" + baseNameArray.length + "] utterances.\n");
        
        /* Prepare the output directory for the timelines if it does not exist */
        File timelineDir = new File( db.timelineDirName() );
        if ( !timelineDir.exists() ) {
            timelineDir.mkdir();
            System.out.println("Created output directory [" + db.timelineDirName() + "] to store the timelines." );
        }
        
        try{
            /* 1) Determine the reference sampling rate as being the sample rate of the first encountered
             *    wav file */
            WavReader wav = new WavReader( db.wavDirName() + baseNameArray[0] + db.wavExt() );
            int globSampleRate = wav.getSampleRate();
            System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz." );
            
            System.out.println("---- Folding the wav files according to the pitchmarks..." );
            
            /* 2) Open the destination timeline file */
            
            /* Make the file name */
            String waveTimelineName = db.waveTimelineFileName() ;
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
                /* - open+load */
                System.out.println( baseNameArray[i] );
                pmFile = new ESTTrackReader( db.correctedPitchmarksDirName() + "/" + baseNameArray[i] + db.correctedPitchmarksExt() );
                totalDuration += pmFile.getTimeSpan();
                wav = new WavReader( db.wavDirName() + baseNameArray[i] + db.wavExt() );
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
                    duration = frameEnd - frameStart;
                    ByteArrayOutputStream buff =  new ByteArrayOutputStream(2*duration);
                    DataOutputStream subWave = new DataOutputStream( buff );
                    for (int k = 0; k < duration; k++) {
                        subWave.writeShort( wave[frameStart+k] );
                    }
                    
                    /* Feed the datagram to the timeline */
                    waveTimeline.feed( new Datagram(duration,buff.toByteArray()) , globSampleRate );
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
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
        
        return( true );
    }

}
