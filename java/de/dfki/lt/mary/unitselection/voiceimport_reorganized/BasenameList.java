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

import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

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
    private String fromExt = null;
    private static final int DEFAULT_INCREMENT = 128;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * This constructor lists the .wav files from directory dir,
     * and initializes an an array with their of alphabetically
     * sorted basenames.
     * 
     * @param dir The name of the directory to list the files from.
     * @param extension The extension of the files to list.
     * 
     */
    public BasenameList( String dirName, final String extension ) {
        fromDir = dirName;
        fromExt = extension;
        /* Turn the directory name into a file, to allow for checking and listing */
        File dir = new File( dirName );
        /* Check if the directory exists */
        if ( !dir.exists() ) {
            throw new RuntimeException( "Directory [" + dirName + "] does not exist. Can't find the [" + extension + "] files." );
        }
        /* List the .extension files */
        File[] selectedFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith( extension );
            }
        });
        
        /* Sort the file names alphabetically */
        Arrays.sort( selectedFiles );
        
        /* Extract the basenames and store them in a vector of strings */
        bList = new Vector( selectedFiles.length, DEFAULT_INCREMENT );
        String str = null;
        for ( int i = 0; i < selectedFiles.length; i++ ) {
            str = selectedFiles[i].getName().substring( 0, selectedFiles[i].getName().length() - 4 );
            bList.add( str );
        }
    }
    
    /**
     * This constructor loads the basename list from a random access file.
     * 
     * @param fileName The file to read from.
     */
    public BasenameList( String fileName ) throws IOException {
        load( fileName );
    }
    
    /*****************/
    /* I/O METHODS   */
    /*****************/

    /**
     * Write the basenameList to a file.
     */
    public void write( String fileName ) throws IOException {
        PrintWriter pw = new PrintWriter( new OutputStreamWriter( new FileOutputStream( fileName ), "UTF-8" ), true );
        if ( fromDir != null ) {
            pw.println( "FROM: " + fromDir + "*" + fromExt );
        }
        String str = null;
        for ( int i = 0; i < bList.size(); i++ ) {
            str = (String)(bList.elementAt(i));
            pw.println( str );
        }
    }
    
    /**
     * Read the basenameList from a file
     */
    public void load( String fileName ) throws IOException {
        /* Open the file */
        BufferedReader bfr = new BufferedReader( new InputStreamReader( new FileInputStream("fileName"), "UTF-8" ) );
        /* Make the vector */
        if ( bList == null ) bList = new Vector( DEFAULT_INCREMENT, DEFAULT_INCREMENT );
        /* Check if the first line contains the origin information (directory+ext) */
        String line = bfr.readLine();
        if ( line.indexOf("FROM: ") != -1 ) {
            line = line.substring( 6 );
            String[] parts = new String[2];
            parts = line.split( "\\*", 2 );
            fromDir = parts[0];
            fromExt = parts[1];
        }
        else if ( !(line.matches("^\\s*$")) ) bList.add( line );
        /* Add the lines to the vector, ignoring the blank ones. */
        while ( (line = bfr.readLine()) != null ) {
            if ( !(line.matches("^\\s*$")) ) bList.add( line );
        }
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
     * An accessor for the original directory. Returns null if the original
     * directory is undefined.
     */
    public String getDir() {
        return( fromDir );
    }
    
    /**
     * An accessor for the original extension. Returns null if the original
     * extension is undefined.
     */
    public String getExt() {
        return( fromExt );
    }
}