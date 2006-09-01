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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Vector;
import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * The BasenameList class produces and stores an alphabetically-sorted
 * array of basenames issued from the .wav files present in a given directory.
 * 
 * @author sacha
 *
 */
public class BasenameList 
{ 
    private Vector bList = null;
    private String fromDir = null;
    private static final int DEFAULT_INCREMENT = 128;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * This constructor lists the .wav files from directory dir,
     * and initializes an an array with their of alphabetically
     * sorted basenames.
     * 
     * @param dir The name of the directory to list the .wav files from.
     * 
     */
    public BasenameList( String dir ) {
        fromDir = dir;
        /* Turn the directory name into a file, to allow for checking and listing */
        File wavDir = new File( dir );
        /* Check if the directory exists */
        if ( !wavDir.exists() ) {
            throw new RuntimeException( "Directory [" + dir + "] does not exist. Can't find the .wav files." );
        }
        /* List the .wav files */
        File[] wavFiles = wavDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".wav");
            }
        });
        
        /* Sort the file names alphabetically */
        Arrays.sort( wavFiles );
        
        /* Extract the basenames and store them in a vector of strings */
        bList = new Vector( wavFiles.length, DEFAULT_INCREMENT );
        String str = null;
        for ( int i = 0; i < wavFiles.length; i++ ) {
            str = wavFiles[i].getName().substring( 0, wavFiles[i].getName().length() - 4 );
            bList.add( str );
        }
    }
    
    /**
     * This constructor loads the basename list from a random access file.
     * 
     * @param raf the RandomAccessFile to read from.
     */
    public BasenameList( RandomAccessFile raf ) throws IOException {
        load( raf );
    }
    
    /*****************/
    /* I/O METHODS   */
    /*****************/

    /**
     * Write the basenameList to a RandomAccessFile
     */
    public long dump( RandomAccessFile raf ) throws IOException {
        long nBytes = 0l;
        String str = null;
        raf.writeUTF(fromDir);        nBytes += fromDir.length()*2 + 2;
        raf.writeInt( bList.size() ); nBytes += 4;
        for ( int i = 0; i < bList.size(); i++ ) {
            str = (String)(bList.elementAt(i));
            raf.writeUTF( str ); nBytes += str.length()*2 + 2;
        }
        return( nBytes );
    }
    
    /**
     * Read the basenameList from a RandomAccessFile
     */
    public long load( RandomAccessFile raf ) throws IOException {
        long nBytes = 0l;
        fromDir = raf.readUTF();          nBytes += fromDir.length()*2 + 2;
        int numBasenames = raf.readInt(); nBytes += 4;
        String str = null;
        bList = new Vector( numBasenames, DEFAULT_INCREMENT );
        for ( int i = 0; i < numBasenames; i++ ) {
            str= raf.readUTF();
            bList.add( str );
            nBytes += str.length()*2 + 2;
        }
        return( nBytes );
    }
    
    /*****************/
    /* OTHER METHODS */
    /*****************/

    /**
     * An accessor for the list of basenames, returned as an array of strings
     */
    public String[] getListAsArray() {
        String[] ret = new String[bList.size()];
        bList.toArray( ret );
        return( (String[])( ret ) );
    }
    
    /**
     * Another accessor for the list of basenames, returned as a vector of strings
     */
    public Vector getListAsVector() {
        return( bList );
    }
    
    /**
     * An accessor for the list's length
     */
    public int getLength() {
        return( bList.size() );
    }
    
   /**
     * An accessor for the original directory
     */
    public String getDir() {
        return( fromDir );
    }
    
}