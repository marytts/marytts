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
import java.util.Properties;

import de.dfki.lt.mary.unitselection.MCepDatagram;

/**
 * The mcepTimelineMaker class takes a database root directory and a list of basenames,
 * and converts the related wav files into a mcep timeline in Mary format.
 * 
 * @author sacha
 */
public class MCepTimelineMaker implements VoiceImportComponent
{ 

    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    protected int percent = 0;
    
    public MCepTimelineMaker( DatabaseLayout setdb, BasenameList setbnl ) {
        this.db = setdb;
        this.bnl = setbnl;
    }
    
    /**
     *  Reads and concatenates a list of mcep EST tracks into one single timeline file.
     *
     */
    public boolean compute()
    {
        System.out.println("---- Importing Mel Cepstrum coefficients\n\n");
        System.out.println("Base directory: " + db.rootDirName() + "\n");
        
        /* Export the basename list into an array of strings */
        String[] baseNameArray = bnl.getListAsArray();
        
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
            
            /* 2) Scan all the EST Mel Cepstrum Track files for min, max and total timeline duration */
            
            System.out.println("---- Scanning for MCep min and MCep max..." );
            
            ESTTrackReader mcepFile;    // Structure that holds the mcep track data
            float[] current;             // local [min,max] vector for the current mcep track file
            float mcepMin, mcepMax, mcepRange;       // Global min/max/range values for the mcep coefficients
            float totalDuration = 0.0f;  // Accumulator for the total timeline duration
            long numDatagrams = 0l; // Total number of mcep datagrams in the timeline file
            int numMCep = 0;              // Number of mcep channels, assumed from the first mcep file
            
            /* Initialize with the first file: */
            /* - open and load */
            // System.out.println( baseNameArray[0] );
            mcepFile = new ESTTrackReader( db.melcepDirName() + baseNameArray[0] + db.melcepExt() );
            /* - get the min and the max */
            current = mcepFile.getMinMax();
            mcepMin = current[0];
            mcepMax = current[1];
            /* - accumulate the file duration */
            totalDuration += mcepFile.getTimeSpan();
            /* - accumulate the number of datagrams: */
            numDatagrams += mcepFile.getNumFrames();
            /* - get the number of mcep channels: */
            numMCep = mcepFile.getNumChannels();
            System.out.println("Assuming that the number of Mel Cepstrum coefficients is: [" + numMCep + "] coefficients." );
            
            /* Then, browse the remaining files: */
            for ( int i = 1; i < baseNameArray.length; i++ ) {
                percent = 100*i/baseNameArray.length;
                /* - open+load */
                // System.out.println( baseNameArray[i] );
                mcepFile = new ESTTrackReader( db.melcepDirName() + baseNameArray[i] + db.melcepExt() );
                /* - get min and max */
                current = mcepFile.getMinMax();
                if ( current[0] < mcepMin ) { mcepMin = current[0]; }
                if ( current[1] > mcepMax ) { mcepMax = current[1]; }
                /* - accumulate and approximate of the total speech duration (to build the index) */
                totalDuration += mcepFile.getTimeSpan();
                /* - accumulate the number of datagrams: */
                numDatagrams += mcepFile.getNumFrames();
            }
            mcepRange = mcepMax - mcepMin;
            /* NOTE: accumulating the total mcep timeline duration (which is necessary for dimensioning the index)
             * from the mcep track times is slightly more imprecise than accumulating durations from the residuals,
             * but it avoids another loop through on-disk files. */
            
            System.out.println("mcepMin   = " + mcepMin );
            System.out.println("mcepMax   = " + mcepMax );
            System.out.println("mcepRange = " + mcepRange );
            
            System.out.println("---- Done." );
            
            System.out.println("---- Translating the EST MCep tracks..." );
            
            /* 3) Open the destination timeline file */
            
            /* Make the file name */
            String mcepTimelineName = db.melcepTimelineFileName();
            System.out.println( "Will create the mcep timeline in file [" + mcepTimelineName + "]." );
            
            /* An example of processing header: */
            Properties props = new Properties();
            String hdrCmdLine = "\n$ESTDIR/bin/sig2fv "
                + "-window_type hamming -factor 2.5 -otype est_binary -coefs melcep -melcep_order 12 -fbank_order 24 -shift 0.01 -preemph 0.97 "
                + "-pm PITCHMARKFILE.pm -o melcepDir/mcepFile.mcep WAVDIR/WAVFILE.wav\n";
            props.setProperty("command", hdrCmdLine);
            props.setProperty("mcep.order", String.valueOf(numMCep));
            props.setProperty("mcep.min", String.valueOf(mcepMin));
            props.setProperty("mcep.range", String.valueOf(mcepRange));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            props.store(baos, null);
            String processingHeader = new String(baos.toString("latin1"));
            
            
            /* Instantiate the TimelineWriter: */
            TimelineWriter mcepTimeline = new TimelineWriter( mcepTimelineName, processingHeader, globSampleRate, 0.01 );
            
            
            /* 4) Write the datagrams and feed the index */
            
            long totalTime = 0l;
            
            /* For each EST track file: */
            for ( int i = 0; i < baseNameArray.length; i++ ) {
                /* - open+load */
                System.out.println( baseNameArray[i] );
                mcepFile = new ESTTrackReader( db.melcepDirName() + baseNameArray[i] + db.melcepExt() );
                wav = new WavReader( db.wavDirName() + baseNameArray[i] + db.wavExt() );
                /* - For each frame in the mcep file: */
                int frameStart = 0;
                int frameEnd = 0;
                int duration = 0;
                long localTime = 0l;
                for ( int f = 0; f < mcepFile.getNumFrames(); f++ ) {
                    /* Get the datagram duration */
                    frameStart = frameEnd;
                    frameEnd = (int)( (double)mcepFile.getTime( f ) * (double)(globSampleRate) );
                    duration = frameEnd - frameStart;
                    /* NOTE: quantization is no more performed below, code&comments kept for archiving. */
                    /* Quantize the mcep coeffs: */
                    // short[] quantizedFrame = General.quantize( mcepFile.getFrame( f ), mcepMin, mcepRange );
                    /* Make a datagram from the quantized mcep coefficients: */
                    /* for ( int k = 0; k < quantizedFrame.length; k++ ) {
                        datagramContents.writeShort( quantizedFrame[k] );
                    } */
                    /* Feed the datagram to the timeline */
                    mcepTimeline.feed( new MCepDatagram( duration, mcepFile.getFrame( f ) ) , globSampleRate );
                    totalTime += duration;
                    localTime += duration;
                }
                // System.out.println( baseNameArray[i] + " -> mcep file says [" + localTime + "] samples, wav file says ["+ wav.getNumSamples() + "] samples." );
                
            }
            
            System.out.println("---- Done." );
            
            /* 7) Print some stats and close the file */
            System.out.println( "---- mcep timeline result:");
            System.out.println( "Number of files scanned: " + baseNameArray.length );
            System.out.println( "Total speech duration: [" + totalTime + "] samples / [" + ((double)(totalTime) / (double)(globSampleRate)) + "] seconds." );
            System.out.println( "(Speech duration approximated from EST Track float times: [" + totalDuration + "] seconds.)" );
            System.out.println( "Number of frames: [" + numDatagrams + "]." );
            System.out.println( "Size of the index: [" + mcepTimeline.idx.getNumIdx() + "] ("
                    + (mcepTimeline.idx.getNumIdx() * 16) + " bytes, i.e. "
                    + ( (double)(mcepTimeline.idx.getNumIdx()) * 16.0 / 1048576.0) + " megs)." );
            System.out.println( "---- mcep timeline done.");
            
            mcepTimeline.close();
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
