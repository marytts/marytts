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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * File reader for a wave (RIFF) waveform
 */
public class WavReader {
    
    private int numSamples;
    private int sampleRate;
    private short[] samples;

    // Only really used in loading of data.
    private int headerSize;
    private int numBytes;
    private int numChannels = 1;  // Only support mono

    static final short RIFF_FORMAT_PCM = 0x0001;

    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /** Constructor from an already open DataInputStream
     *
     * @param dis DataInputStream to read the wav data from
     *
     */
    public WavReader( DataInputStream dis ) {
        loadHeaderAndData( dis );
    }
    
    /** Constructor from a file name
    *
    * @param fileName the name of the file to read the wav data from
    *
    */
    public WavReader( String fileName ) {
        try {
            /* Open the file */
            FileInputStream fis = new FileInputStream( fileName );
            /* Stick the file to a DataInputStream to allow easy reading of primitive classes (numbers) */
            DataInputStream dis = new DataInputStream( fis );
            /* Parse the header and load the data */
            loadHeaderAndData( dis );
            /* Close the file */
            fis.close();
        }
        catch ( FileNotFoundException e ) {
            throw new Error("WAV file [" + fileName + "] was not found." );
        }
        catch ( SecurityException e ) {
            throw new Error("You do not have read access to the file [" + fileName + "]." );
        }
        catch ( IOException e ) {
            throw new Error("IO Exception caught when closing file [" + fileName + "]: " + e.getMessage() );
        }
    }
    
    
    /*****************/
    /* OTHER METHODS */
    /*****************/
    
    /**
     * Read in a wave from a riff format
     *
     * @param dis DataInputStream to read data from
     */
    private void loadHeaderAndData( DataInputStream dis ) {
        
        try {
            loadHeader(dis);
            if (dis.skipBytes(headerSize - 16) != (headerSize - 16)) {
                throw new Error("Unexpected error parsing wave file.");
            }

            // Bunch of potential random headers
            while (true) {
                String s = new String(General.readChars(dis, 4));

                if (s.equals("data")) {
                    numSamples = General.readInt(dis, false) / 2;
                    break;
                } else if (s.equals("fact")) {
                    int i = General.readInt(dis, false);
                    if (dis.skipBytes(i) != i) {
                        throw new Error("Unexpected error parsing wave file.");
                    }
                } else {
                    throw new Error("Unsupported wave header chunk type " + s);
                }
            }

            int dataLength = numSamples * numChannels;
            samples = new short[numSamples];

            for (int i = 0; i < dataLength; i++) {
                samples[i] = General.readShort(dis, false);
            }

        } catch (IOException ioe) {
            throw new Error("IO error while parsing wave" + ioe.getMessage());
        }
        
    }

    /**
     * load a RIFF header
     *
     * @param dis DataInputStream to read from
     *
     * @throws IOException on ill-formatted input
     */
    private void loadHeader(DataInputStream dis) throws IOException {
        
        if (!checkChars(dis, "RIFF")) {
            throw new Error("Invalid wave file format.");
        }
        numBytes = General.readInt(dis,false);
        if (!checkChars(dis, "WAVEfmt ")) {
            throw new Error("Invalid wave file format.");
        }

        headerSize = General.readInt(dis, false);

        if (General.readShort(dis, false) != RIFF_FORMAT_PCM) {
            throw new Error("Invalid wave file format.");
        }

        if (General.readShort(dis, false) != 1) {
            throw new Error("Only mono wave files supported.");
        }
        
        sampleRate = General.readInt(dis, false);
        General.readInt(dis, false);
        General.readShort(dis, false);
        General.readShort(dis, false);
        
    }

    
    /**
     * Make sure that a string of characters appear next in the file
     *
     * @param dis DataInputStream to read in
     * @param chars a String containing the ascii characters you
     *          want the <code>dis</code> to contain.
     *
     * @return <code>true</code> if <code>chars</code> appears next
     *          in <code>dis</code>, else <code>false</code>
     * @throws on ill-formatted input (end of file, for example)
     */
    private boolean checkChars(DataInputStream dis, String chars)
            throws IOException {
        char[] carray = chars.toCharArray();
        for (int i = 0; i < carray.length; i++) {
            if ((char) dis.readByte() != carray[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the sample rate for this wave
     *
     * @return sample rate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Get the number of samples for this wave
     *
     * @return number of samples
     */
    public int getNumSamples() {
        return numSamples;
    }

    /* Get the sample data of this wave
     *
     * @return samples
     */
    public short[] getSamples() {
        return samples;
    }
}


