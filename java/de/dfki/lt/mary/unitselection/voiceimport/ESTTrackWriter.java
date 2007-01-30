/**
 * Copyright 2004-2006 DFKI GmbH.
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

import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class ESTTrackWriter {

    private float[]   times = null;
    private float[][] frames = null;
    private String feaType = "";
    
    /**
     * Plain constructor.
     * 
     * @param setTimes The vector of frame locations
     * @param setFrames The frames -- can be null if only times are to be written
     * @param setFeaType A string indicating the feature type, for header info (e.g., "LPCC")
     */
    public ESTTrackWriter( float[] setTimes, float[][] setFrames, String setFeaType ) {
        this.times = setTimes;
        this.frames = setFrames;
        this.feaType = setFeaType;
    }
    
    /**
     * Triggers the writing of the file to the disk.
     * 
     * @param fName The name of the file to write to.
     * @param isBinary true for a binary write, or false for an ascii text write.
     * @param isBigEndian true for a big endian write (PowerPC or SPARC), or false for a little endian write (Intel).
     * 
     * @throws IOException
     */
    public void doWriteAndClose( String fName, boolean isBinary, boolean isBigEndian ) throws IOException {
        
        // Open the file for writing
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( fName ) ) );
        }
        catch ( FileNotFoundException e ) {
            throw new RuntimeException( "Can't open file [" + fName + "] for writing.", e );
        }
        // Output the header
        dos.writeBytes(
                "EST_File Track\n" );
        if ( isBinary ) {
            dos.writeBytes( "DataType binary\n");
            if ( isBigEndian ) dos.writeBytes( "ByteOrder 10\n");
            else               dos.writeBytes( "ByteOrder 01\n");
        } else {
            dos.writeBytes( "DataType ascii\n");
        }
        
        int numChannels;
        if (frames != null && frames.length > 0 && frames[0] != null)
            numChannels = frames[0].length;
        else
            numChannels = 0; // e.g., for pitchmarks
        dos.writeBytes(
                "NumFrames "   + times.length + "\n" +
                "NumChannels " + numChannels + "\n" +
                "NumAuxChannels 0\n" +
                "EqualSpace 0\n" +
                "BreaksPresent true\n" +
                "CommentChar ;\n" );
        String K;
        for ( int k = 0; k < numChannels; k++ ) {
            K = new Integer(k).toString();
            dos.writeBytes( "Channel_" + K + " " + feaType + "_" + K + "\n" );
        }
        dos.writeBytes( "EST_Header_End\n" );
        // Output the data:
        // - in binary mode
        if ( isBinary ) {
            for ( int i = 0; i < times.length; i++ ) {
                General.writeFloat( dos, isBigEndian, times[i] );
                General.writeFloat( dos, isBigEndian, 1.0f );
                for ( int k = 0; k < numChannels; k++ ) {
                    General.writeFloat( dos, isBigEndian, frames[i][k] );
                }
            }
        }
        // - in ASCII mode
        else {
            for ( int i = 0; i < times.length; i++ ) {
                dos.writeBytes( new Float(times[i]).toString() );
                dos.writeBytes( "\t1\t" );
                if (numChannels > 0) {
                    dos.writeBytes( new Float(frames[i][0]).toString() );
                    for ( int k = 1; k < frames[0].length; k++ ) {
                        dos.writeBytes( " " + new Float(frames[i][k]).toString() );
                    }
                }
                dos.writeBytes( "\n" );                    
            }
        }
        // Flush and close
        dos.flush();
        dos.close();
        
    }

}
