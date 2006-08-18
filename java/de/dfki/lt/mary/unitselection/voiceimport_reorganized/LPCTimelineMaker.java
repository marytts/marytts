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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.RandomAccessFile;

import de.dfki.lt.mary.unitselection.voiceimport_reorganized.General;
import de.dfki.lt.mary.unitselection.voiceimport_reorganized.MaryHeader;
import de.dfki.lt.mary.unitselection.voiceimport_reorganized.DatabaseLayout;
import de.dfki.lt.mary.unitselection.voiceimport_reorganized.TimelineReader;

/**
 * This class takes a database root directory and a list of basenames,
 * and converts the related wav files into a LPC timeline in Mary format.
 * 
 * @author sacha
 */
public class LPCTimelineMaker 
{ 
    /**
     *  Reads and concatenates a list of LPC EST tracks into one single timeline file.
     *
     */
    public static void run( DatabaseLayout db, String[] baseNameArray, boolean recompute )
    {
        System.out.println("---- Importing LPC coefficients\n\n");
        System.out.println("Base directory: " + db.baseName() + "\n");
        
        try{
            /******************/
            /* LPC conversion */
            /******************/
            
            /* 1) Determine the reference sampling rate as being the sample rate of the first encountered
             *    wav file */
            WavReader wav = new WavReader( db.wavDirName() + baseNameArray[0] + ".wav" );
            int globSampleRate = wav.getSampleRate();
            System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz." );
            
            /* 2) Scan all the EST LPC Track files for LPC min, LPC max and total LPC-timeline duration */
            
            System.out.println("---- Scanning for LPC min and LPC max..." );
            
            ESTTrackReader lpcFile;    // Structure that holds the LPC track data
            float[] current;             // local [min,max] vector for the current LPC track file
            float lpcMin, lpcMax, lpcRange;       // Global min/max/range values for the LPC coefficients
            float totalDuration = 0.0f;  // Accumulator for the total timeline duration
            long numDatagrams = 0l; // Total number of LPC datagrams in the timeline file
            int numLPC = 0;              // Number of LPC channels, assumed from the first LPC file
            
            /* Initialize with the first file: */
            /* - open and load */
            // System.out.println( baseNameArray[0] );
            lpcFile = new ESTTrackReader( db.lpcDirName() + baseNameArray[0] + ".lpc" );
            /* - get the min and the max */
            current = lpcFile.getMinMaxNo1st();
            lpcMin = current[0];
            lpcMax = current[1];
            /* - accumulate the file duration */
            totalDuration += lpcFile.getTimeSpan();
            /* - accumulate the number of datagrams: */
            numDatagrams += lpcFile.getNumFrames();
            /* - get the number of LPC channels: */
            numLPC = lpcFile.getNumChannels() - 1; // -1 => ignore the energy channel.
            System.out.println("Assuming that the number of LPC coefficients is: [" + numLPC + "] coefficients." );
            
            /* Then, browse the remaining files: */
            for ( int i = 1; i < baseNameArray.length; i++ ) {
                /* - open+load */
                // System.out.println( baseNameArray[i] );
                lpcFile = new ESTTrackReader( db.lpcDirName() + baseNameArray[i] + ".lpc" );
                /* - get min and max */
                current = lpcFile.getMinMaxNo1st();
                if ( current[0] < lpcMin ) { lpcMin = current[0]; }
                if ( current[1] > lpcMax ) { lpcMax = current[1]; }
                /* - accumulate and approximate of the total speech duration (to build the index) */
                totalDuration += lpcFile.getTimeSpan();
                /* - accumulate the number of datagrams: */
                numDatagrams += lpcFile.getNumFrames();
            }
            lpcRange = lpcMax - lpcMin;
            /* NOTE: accumulating the total LPC timeline duration (which is necessary for dimensioning the index)
             * from the LPC track times is slightly more imprecise than accumulating durations from the residuals,
             * but it avoids another loop through on-disk files. */
            
            System.out.println("LPCMin   = " + lpcMin );
            System.out.println("LPCMax   = " + lpcMax );
            System.out.println("LPCRange = " + lpcRange );
            
            System.out.println("---- Done." );
            
            System.out.println("---- Filtering the EST LPC tracks..." );
            
            /* 3) Prepare the index */
            int numIdx = (int)java.lang.Math.floor( totalDuration / TimelineReader.DEFAULTIDXINTERVAL );
            int idxInterval = (int)java.lang.Math.floor( TimelineReader.DEFAULTIDXINTERVAL * (float)(globSampleRate) );
            long[] begin = new long[numIdx];
            long[] offset = new long[numIdx];
            
            /* 4) Open the destination timeline file, output the header and reserve space for the index */
            String lpcTimelineName = db.timelineDirName() + "/timeline_lpc_res.bin";
            System.out.println( "Will create the LPC timeline in file [" + lpcTimelineName + "]." );
            RandomAccessFile timeLineRaf = new RandomAccessFile( lpcTimelineName, "rw" );
            long byteNow = 0; // Counter for the file position
            
            /* Mary header */
            MaryHeader hdr = new MaryHeader( MaryHeader.TIMELINE );
            byteNow += hdr.write( timeLineRaf );
            
            /* Text header for processing parameters */
            String audioInfo = "AudioType LPC Channels "+ numLPC +" LPCMin "+ lpcMin +" LPCRange "+ lpcRange;
            timeLineRaf.writeShort( audioInfo.length() * 2 ); // Note: this string size is in bytes and Unicode chars take 2 bytes.
            byteNow += 2;
            timeLineRaf.writeUTF( audioInfo ); byteNow += (audioInfo.length() * 2);
            /* Data header */
            timeLineRaf.writeInt( globSampleRate ); byteNow += 4;
            timeLineRaf.writeLong( numDatagrams );  byteNow += 8;
            timeLineRaf.writeByte( TimelineReader.VARIABLE ); byteNow += 1;
            
            /* Write a blank index */
            long IDXPOSITION = byteNow;
            timeLineRaf.writeInt( numIdx );      // 4 bytes
            timeLineRaf.writeInt( idxInterval ); // 4 bytes
            for(int i = 0; i < numIdx; i++ ) {
                timeLineRaf.writeLong( begin[i] );
                timeLineRaf.writeLong( offset[i] );
            }                                    // numIdx * (8+8) bytes
            long IDXSIZE = 8 + numIdx*16 ; byteNow += IDXSIZE;
            
            /* 5) Write the datagrams and feed the index */
            
            long totalTime = 0l;
            long timeNow = 0l;
            int idxNow = 0;
            long nextIdxLimit = idxInterval;
            int datagramSize = 0;
            
            /* For each EST track file: */
            for ( int i = 0; i < baseNameArray.length; i++ ) {
                /* - open+load */
                System.out.println( baseNameArray[i] + "\r" );
                lpcFile = new ESTTrackReader( db.lpcDirName() + "/" + baseNameArray[i] + ".lpc" );
                wav = new WavReader( db.wavDirName() + "/" + baseNameArray[i] + ".wav" );
                /* - Reset the frame locations in the local file */
                int frameStart = 0;
                int frameEnd = 0;
                int frameSize = 0;
                /* - For each frame in the LPC file: */
                for ( int f = 0; f < lpcFile.getNumFrames(); f++ ) {
                    
                    /* Locate the corresponding segment in the wave file */
                    frameStart = frameEnd;
                    frameEnd = (int)( lpcFile.getTime( f ) * (float)(globSampleRate) );
                    frameSize = frameEnd - frameStart;
                    
                    /* Update the index: */
                    /* if the current time passes the current index time, */
                    timeNow = totalTime + frameEnd;
                    if ( timeNow > nextIdxLimit ) {
                        /* then register the current time and file position */
                        begin[idxNow]  = byteNow;
                        offset[idxNow] = timeNow;
                        /* and move to the next index field. */
                        idxNow++;
                        nextIdxLimit += idxInterval;
                    }

                    /* Start outputing the datagram  */
                    datagramSize = numLPC*2 + (frameSize+numLPC);
                    timeLineRaf.writeInt( datagramSize ); byteNow += 4;
                    timeLineRaf.writeLong( frameSize );   byteNow += 8;
                    
                    /* Quantize and output the LPC coeffs: */
                    short[] quantizedFrame = General.quantize( lpcFile.getFrame( f ), lpcMin, lpcRange );
                    float[] unQuantizedFrame = General.unQuantize( quantizedFrame, lpcMin, lpcRange );
                    for ( int k = 0; k < numLPC; k++ ) {
                        timeLineRaf.writeShort( quantizedFrame[k+1] ); byteNow += 2; // k+1 => ignore the energy channel
                    }
                    /* Note: for inverse filtering (below), we will use the un-quantized values
                     *       of the LPC coefficients, so that the quantization noise is registered
                     *       into the residual (for better reconstruction of the waveform from
                     *       quantized coeffs). */
                    
                    /* PERFORM THE INVERSE FILTERING with quantized LPCs, and output the residual */
                    double r;
                    short[] wave = wav.getSamples();
                    for (int k = 0; k < (frameSize-numLPC); k++) {
                        // try {
                        r = (double)( wave[frameStart + k] );
                        /* } catch ( ArrayIndexOutOfBoundsException e ) {
                            System.out.println( "ARGH: " + (frameStart + numLPC + k) );
                            System.out.println( "Wlen: " + wave.length );
                            System.out.println( "FrameEnd: " + frameEnd );
                            System.out.println( "FrameSize: " + frameSize );
                            return;
                        } */
                        for (int j = 0; j < numLPC; j++) {
                            // try {
                            r -= unQuantizedFrame[j] * ((double) wave[frameStart + (numLPC - 1) + (k - j)]);
                            /* } catch ( ArrayIndexOutOfBoundsException e ) {
                                System.out.println( "ARGH: " + (frameStart + numLPC + k) );
                                System.out.println( "Wlen: " + wave.length );
                                return;
                            } */
                        }
                        timeLineRaf.writeByte( General.shortToUlaw((short) r) ); byteNow += 1;
                    }
                    
                    /* Update the byte position and the number of output datagrams: */
                    numDatagrams++;
                    
                }
                /* - Update the global time cursor by adding the position of the last frame */
                totalTime += frameEnd;
                
            }
            
            System.out.println("---- Done." );
            
            /* 6) Come back and write the correct index */
            timeLineRaf.seek( IDXPOSITION );
            for(int i = 0; i < numIdx; i++ ) {
                timeLineRaf.writeLong( begin[i] );
                timeLineRaf.writeLong( offset[i] );
            }
            
            /* 7) Close the file and print some stats */
            timeLineRaf.close();
            
            System.out.println( "---- LPC timeline result:");
            System.out.println( "Number of files scanned: " + baseNameArray.length );
            System.out.println( "Total speech duration: [" + totalTime + "] frames / [" + ((float)(totalTime) / (float)(globSampleRate)) + "] seconds." );
            System.out.println( "(Speech duration approximated from EST Track float times: [" + totalDuration + "] seconds.)" );
            System.out.println( "Number of frames: [" + numDatagrams + "]." );
            System.out.println( "Size of the index: [" + numIdx + "]." );
            System.out.println( "---- LPC timeline done.");
            
        }
        catch ( SecurityException e ) {
            System.err.println( "Error: you don't have write access to the target database directory." );
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
        
    }

}
