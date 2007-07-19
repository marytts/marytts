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
import java.io.File;
import java.util.*;

import de.dfki.lt.mary.unitselection.MCepDatagram;

/**
 * The mcepTimelineMaker class takes a database root directory and a list of basenames,
 * and converts the related wav files into a mcep timeline in Mary format.
 * 
 * @author sacha
 */
public class MCepTimelineMaker extends VoiceImportComponent
{ 

    protected DatabaseLayout db = null;
    protected int percent = 0;
    protected String mcepExt = ".mcep";
    
    public final String MCEPDIR = "MCepTimelineMaker.mcepDir";
    public final String MCEPTIMELINE = "MCepTimelineMaker.mcepTimeline";
    
    public String getName(){
        return "MCepTimelineMaker";
    }
    
    
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
        if (props == null){
            props = new TreeMap();
            props.put(MCEPDIR, db.getProp(db.ROOTDIR)
                    +"mcep"
                    +System.getProperty("file.separator"));
            props.put(MCEPTIMELINE, db.getProp(db.FILEDIR)
                    +"timeline_mcep"+db.getProp(db.MARYEXT));  
        }
        return props;
    }
    
    protected void setupHelp(){         
        props2Help = new TreeMap();
        props2Help.put(MCEPDIR, "directory containing the mcep files");
        props2Help.put(MCEPTIMELINE,"file containing all mcep files. Will be created by this module");  
    }
    
    /**
     *  Reads and concatenates a list of mcep EST tracks into one single timeline file.
     *
     */
    public boolean compute()
    {
        System.out.println("---- Importing Mel Cepstrum coefficients\n\n");
        System.out.println("Base directory: " + db.getProp(db.ROOTDIR) + "\n");
        
        /* Export the basename list into an array of strings */
        String[] baseNameArray = bnl.getListAsArray();
        
        /* Prepare the output directory for the timelines if it does not exist */
        File timelineDir = new File(db.getProp(db.FILEDIR));
        
        
        try{
            /* 1) Determine the reference sampling rate as being the sample rate of the first encountered
             *    wav file */
            WavReader wav = new WavReader(db.getProp(db.WAVDIR) 
                    + baseNameArray[0] + db.getProp(db.WAVEXT));
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
            mcepFile = new ESTTrackReader(getProp(MCEPDIR) 
                    + baseNameArray[0] + mcepExt);
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
                percent = 50*i/baseNameArray.length;
                /* - open+load */
                // System.out.println( baseNameArray[i] );
                mcepFile = new ESTTrackReader(getProp(MCEPDIR) 
                        + baseNameArray[i] + mcepExt);
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
            System.out.println( "Will create the mcep timeline in file [" 
                    + getProp(MCEPTIMELINE) + "]." );
            
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
            TimelineWriter mcepTimeline = new TimelineWriter( getProp(MCEPTIMELINE), processingHeader, globSampleRate, 0.01 );
            
            
            /* 4) Write the datagrams and feed the index */
            
            long totalTime = 0l;
            
            /* For each EST track file: */
            for ( int i = 0; i < baseNameArray.length; i++ ) {
                percent = 50+50*i/baseNameArray.length;
                /* - open+load */
                System.out.println( baseNameArray[i] );
                mcepFile = new ESTTrackReader( getProp(MCEPDIR) 
                        + baseNameArray[i] + mcepExt);
                wav = new WavReader(db.getProp(db.WAVDIR) 
                        + baseNameArray[i] + db.getProp(db.WAVEXT));
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
