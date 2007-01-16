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
 * EST Track file reader
 * 
 * This class interpretes a DataInputStream as an EST_Track file:
 * it loads the whole track data in memory, and provides access methods
 * to reach each individual frame or each individual track value.
 *
 */
public class ESTTrackReader {
    
    private int numFrames;
    private int numChannels;
    private float[] times;
    private float[][] frames;
    private boolean isBigEndian = false;
    private boolean isBinary = false;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /** Constructor from an already open DataInputStream
     *
     * @param dis DataInputStream to read the EST_Track from
     *
     */
    public ESTTrackReader( DataInputStream dis ) {
        loadHeaderAndData( dis );
    }
    
    /** Constructor from a file name
    *
    * @param fileName the name of the file to read the EST_Track from
    *
    */
    public ESTTrackReader( String fileName ) {
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
            throw new Error("EST track file [" + fileName + "] was not found." );
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
     * Parse the EST header and load the track data
     *
     * @param dis DataInputStream to read from
     *
     * @throws IOException on ill-formatted input
     */
     private void loadHeaderAndData( DataInputStream dis ) {
         try {
             if (!General.readWord(dis).equals("EST_File") ||
                     !General.readWord(dis).equals("Track")) {
                 throw new Error("The given data input stream is not an EST Track file.");
             }

             // Read Header
             String token = General.readWord(dis);
             while (!token.equals("EST_Header_End")) {
                 if (token.equals("DataType")) {
                     if (General.readWord(dis).equals("binary")) {
                         isBinary = true;
                     } else {
                         isBinary = false;
                     }
                 } else if (token.equals("ByteOrder")) {
                     if (General.readWord(dis).equals("10")) {
                         isBigEndian = true;
                     } else {
                         isBigEndian = false;
                     }
                 } else if (token.equals("NumFrames")) {
                     numFrames = Integer.parseInt(General.readWord(dis));
                 } else if (token.equals("NumChannels")) {
                     numChannels = Integer.parseInt(General.readWord(dis));
                 }
                 // Ignore all other content in header

                 token = General.readWord(dis);
             }

             /* Prepare the storage for the data... */
             times = new float[numFrames];
             frames = new float[numFrames][numChannels];
             /* ... then load it. */
             if (isBinary) {
                 loadBinaryData(dis);
             } else {
                 loadTextData(dis);
             }
         }
         
         /* Verify if everything went OK during the reading of the DataInputStream */
         catch (IOException ioe) {
             throw new Error("IO Exception while parsing EST Track file: " + ioe.getMessage());
         }
         
     } /* End of loadHeaderAndData() */
    
    
    /**
     * load the data section of the file as ascii text
     *
     * @param dis DataInputStream to read from
     *
     * @throws IOException on ill-formatted input
     */
    private void loadTextData(DataInputStream dis) throws IOException {
        for (int f=0; f < numFrames; f++) {
            times[f] = Float.parseFloat(General.readWord(dis));
            General.readWord(dis);  // can be only 1
            for (int c=0; c < numChannels; c++) {
                    frames[f][c] = Float.parseFloat(General.readWord(dis));
            }
        }
    }

    /**
     * load the data section of the file as ascii text
     *
     * @param dis DataInputStream to read from
     *
     * @throws IOException on ill-formatted input
     */
    private void loadBinaryData(DataInputStream dis)
            throws IOException {
        for (int f=0; f < numFrames; f++) {
            times[f] = General.readFloat(dis, isBigEndian);

            // Ignore the 'breaks' field
            General.readFloat(dis, isBigEndian);

            for (int c=0; c < numChannels; c++) {
                frames[f][c] = General.readFloat(dis, isBigEndian);
            }
        }
    }

    /**
     * Get the number of frames in this track
     *
     * @return number of frames in this track
     */
    public int getNumFrames() {
        return numFrames;
    }

    /**
     * Get the number of channels in this track
     *
     * @return number of channels in this track
     */
    public int getNumChannels() {
        return numChannels;
    }

    /**
     * Get the times associated with this track
     *
     * @return an array of times associated with this track
     */
    public float[] getTimes() {
        return times;
    }

    /**
     * Get the frames associated with this track
     *
     * @return an array of frames associated with this track
     */
    public float[][] getFrames() {
        return frames;
    }

    /**
     * Get an individual time associated with this track
     *
     * @param index index of time to get
     *
     * @return time value at given index
     */
    public float getTime(int index) {
        return times[index];
    }
    
    /**
     * Returns the endianness of the file
     *
     * @return false if little endian (Intel), true if big endian (PowerPC, SPARC)
     */
    public boolean isBigEndian() {
        return isBigEndian;
    }
    
    /**
     * Returns the mode of the file (ascii or binary)
     *
     * @return false if ascii text, true if binary
     */
    public boolean isBinary() {
        return isBinary;
    }
    
    /**
     * Internal time pointer for the getClosestTime(double) method.
     */
    private int timeIdx = 0;
    /**
     * Get the frame time which is closest to a certain time specification.
     * Time specifications falling after the last frame return the position of the last frame.
     * 
     * @param seconds A time point, in seconds.
     * @return
     */
    public float getClosestTime( double seconds ) {
        /* Obvious conditions */
        if ( seconds < 0.0 ) return( 0.0f );
        if ( seconds > getTimeSpan() ) return( getTimeSpan() );
        /* If the internal pointer is after the requested time, rewind */
        if ( getTime(timeIdx) > seconds ) timeIdx = 0;
        float t1 = 0.0f;
        float t2 = getTime(timeIdx);
        /* Advance the internal time pointer until the requested time is crossed */
        while( (t2 < seconds) && (timeIdx < numFrames) ) {
            timeIdx++;
            t1 = t2;
            t2 = getTime(timeIdx);
        }
        /* Find and return the closest time */
        return( (seconds - t1) < (t2 - seconds) ? t1 : t2 );
    }

    /**
     * Get the time associated with the last frame
     *
     * @return time value of the last frame
     */
    public float getTimeSpan() {
        return times[numFrames-1];
    }

    /**
     * Get an individual frame
     *
     * @param i index of frame
     *
     * @return the frame
     */
    public float[] getFrame(int i) {
        return frames[i];
    }

    /**
     * Get an individual frame entry
     *
     * @param i index of frame
     * @param j index into frame
     *
     * @return the frame entry in frame <code>i</code> at index
     *          <code>j</code>
     */
    public float getFrameEntry(int i, int j) {
        return frames[i][j];
    }
    
    /**
     * Get the max and the min over the whole file
     *
     * @return a vector of 2 float values in the order [min,max]. 
     * 
     * @author Sacha K.
     */
    public float[] getMinMax() {
        /* Initialize */
        float min = getFrameEntry(0,0);
        float max = min;
        float val;
        /* Browse the values */
        for (int f=0; f < numFrames; f++) {
            for (int c=0; c < numChannels; c++) {
                val = frames[f][c];
                if ( val < min ) { min = val; };
                if ( val > max ) { max = val; };
            }
        }
        /* Return the result in a float vector */
        float[] result = new float[2]; // Resulting vector, returned as [min,max,range]
        result[0] = min;
        result[1] = max;
        return result;
    }
    
    /**
     * Get the max and the min over the whole file,
     * excluding the first column (which can be the energy in the EST LPCs)
     *
     * @return a vector of 2 float values in the order [min,max]. 
     * 
     * @author Sacha K.
     */
    public float[] getMinMaxNo1st() {
        /* Initialize */
        float min = getFrameEntry(0,1);
        float max = min;
        float val;
        /* Browse the values */
        for (int f=0; f < numFrames; f++) {
            for (int c=1; c < numChannels; c++) {
                val = frames[f][c];
                if ( val < min ) { min = val; };
                if ( val > max ) { max = val; };
            }
        }
        /* Return the result in a float vector */
        float[] result = new float[2]; // Resulting vector, returned as [min,max,range]
        result[0] = min;
        result[1] = max;
        return result;
    }
}


