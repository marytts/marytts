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

public class DatabaseLayout 
{ 
    /* Private fields with default values */
    private String baseName = ".";
    private String wavSubDir = "wav";
    private String lpcSubDir = "lpc";
    private String timelineSubDir = "timelines";
    private String pitchmarksSubDir = "pm";
    private String melcepSubDir = "mcep";
    
    private File baseDir = null;
    private File wavDir = null;
    private File lpcDir = null;
    private File timelineDir = null;
    private File pitchmarksDir = null;
    private File melcepDir = null;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * Constructor for a new database layout.
     * 
     * @param newBaseName the root directory of the database tree
     * @param newWavSubDir the name of the wav files sub-directory, e.g., "wav"
     * @param newLpcSubDir the name of the lpc files sub-directory, e.g., "lpc"
     * @param newTLSubDir the name of the timeline files sub-directory, e.g., "timelines"
     * @param newPitchmarkSubDir the name of the pitchmarks files sub-directory, e.g., "pm"
     * @param newMelcepSubDir the name of the Mel-cepstrum files sub-directory, e.g., "mcep"
     */
    public DatabaseLayout( String newBaseName, String newWavSubDir, String newLpcSubDir, String newTLSubDir,
            String newPitchmarksSubDir, String newMelcepSubDir ) {
        
        /* TODO: make this interface more flexible by using a name/value model
         * (through a hash map?), so that the order of the parameters becomes
         *  arbitrary, and so that some default values can be preserved. */
        
        baseName = newBaseName;
        wavSubDir = newWavSubDir;
        lpcSubDir = newLpcSubDir;
        timelineSubDir = newTLSubDir;
        pitchmarksSubDir = newPitchmarksSubDir;
        melcepSubDir = newMelcepSubDir;
        
        baseDir = new File(baseName);
        wavDir = new File( wavDirName() );
        lpcDir = new File( lpcDirName() );
        timelineDir = new File( timelineDirName() );
        pitchmarksDir = new File( pitchmarksDirName() );
        melcepDir = new File( melcepDirName() );
    }
    
    /*****************/
    /* OTHER METHODS */
    /*****************/
    
    /* Various accessors: */

    public String baseName() { return( baseName ); }
    public File baseDir()    { return( baseDir  ); }

    public String wavDirName() { return( baseName + "/" + wavSubDir + "/" ); }
    public File wavDir()       { return( wavDir ); }

    public String lpcDirName() { return( baseName + "/" + lpcSubDir + "/" ); }
    public File lpcDir()       { return( lpcDir ); }

    public String timelineDirName() { return( baseName + "/" + timelineSubDir + "/" ); }
    public File timelineDir()       { return( timelineDir ); }

    public String pitchmarksDirName() { return( baseName + "/" + pitchmarksSubDir + "/" ); }
    public File pitchmarksDir()       { return( pitchmarksDir ); }

    public String melcepDirName() { return( baseName + "/" + melcepSubDir + "/" ); }
    public File melcepDir()       { return( melcepDir ); }

}
