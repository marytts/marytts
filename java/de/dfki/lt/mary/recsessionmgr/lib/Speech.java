/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.mary.recsessionmgr.lib;

import java.io.File;

import javax.sound.sampled.SourceDataLine;

import de.dfki.lt.mary.recsessionmgr.debug.Test;
import de.dfki.lt.signalproc.util.AudioPlayer;

/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
 */
public class Speech {
    
    // ______________________________________________________________________
    // Instance fields
    
    int duration;    // Duration of most recent speech file
    int fileCount;   // Number of files
    File filePath;   // Path and filename of speech file(s)
    String basename; // Basename for the associated speech file(s)
    
    // ______________________________________________________________________
    // Class fields
    private static AudioPlayer audioPlayer;
    
    // ______________________________________________________________________
    // Instance methods
    
    /** Determines how many recordings a prompt has
     *  @param wavPath The file path to the wav directory of recordings
     *  @param basename The basename for the currently selected prompt
     *  @return The number of files
     */
    public void updateFileCount() {
        // Note: This method doesn't increment the file count; it only asks the Speech object to initiate
        //       an update. Therefore public access should be okay.
        
        File[] fileList = this.filePath.listFiles();   // List of files in wav folder
        int fileListLength = fileList.length;     // Number of files in wav folder
        
        int count = 0;

        if (fileList != null) {            
            for (int i = 0; i < fileListLength; i++) {
                File wavFile = fileList[i];
                
                // Only consider files, not folders
                if (wavFile.isFile()) {
                                        
                    // If filename contains basename
                    if (wavFile.getName().indexOf(this.basename) != -1) {
                       count++;
                    }
                    
                } // wavFile
            } // i
        }  // fileList

        // TEST CODE
        Test.output("Files for " + this.basename + ": " + count);
                
        this.fileCount = count;
    } 
    
    /** Gets duration of the speech file
     *  @return The duration of the speech file (in milliseconds)
     */
    public int getDuration() {
        return duration;
    }
    
    /** Gets the file path for the speech file
     *  @return The file path for the current speech file
     */
    public File getFilePath() {
        return filePath;
    }

    public void setFilePath(File newPath) {
        this.filePath = newPath;
    }
    
    public String getBasename() {
        return basename;
    }

    public void setBasename(String name) {
        this.basename = name;
    }
    
    public File getFile() {
        return new File(filePath, basename + ".wav");
    }
    
    /**
     * Get the number of files in filePath containing basename in their file name.
     * @return
     */
    public int getFileCount() {
        return fileCount;
    }
    
    /** Plays a sound file once via the indicated sourcedataline.
     * The method blocks until the playing has completed. */
    public static void play(String soundFilePathString, SourceDataLine line, int outputMode)
    {
        play(new File(soundFilePathString), line, outputMode);
    }
    
    /** Plays a sound file once via the indicated sourcedataline.
     * The method blocks until the playing has completed. */
    public static void play(File soundFile, SourceDataLine line, int outputMode)
    {
        try {
            audioPlayer = new AudioPlayer(soundFile, line, null, outputMode);
            audioPlayer.start();
            audioPlayer.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void stopPlaying() 
    {
        if (audioPlayer != null) audioPlayer.cancel();
    }
    
    // ______________________________________________________________________
    // Class methods
    
    // ______________________________________________________________________
    // Constructors
    
    /** Creates a new instance of Speech given a file path
     *  @param filePath The file path containing the sound files (i.e., the wav or wav_synth directory path)
     */
    public Speech(File passedFilePath, String passedBasename) {
        
        this.basename = passedBasename;
        this.filePath = passedFilePath;
        if (!this.filePath.isDirectory()) {
            System.err.println("Creating directory: "+this.filePath);
            boolean success = this.filePath.mkdir();
            if (!success) {
                throw new RuntimeException("could not create directory '"+this.filePath+"'");
            }
        }
        updateFileCount();
        
        Test.output("Speech object has " + this.fileCount + " file(s).");  // TESTCODE
        
    }
    
}
