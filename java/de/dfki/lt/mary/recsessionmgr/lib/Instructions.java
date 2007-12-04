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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.dfki.lt.mary.recsessionmgr.debug.Test;

/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
 */
public class Instructions {
    
    // ______________________________________________________________________
    // Instance fields
    
    private String text = "";    // Set of instructions to display to the speaker
    // PRI4 Instructions could later be a rich text object with formatting (boldface, underline, etc.)
    
    // ______________________________________________________________________
    // Class fields
    
    // ______________________________________________________________________
    // Instance methods
    
    /** Gets text instructions to display to the speaker
     *  @return The instructions as text
     */
    public String getText() {
        return text;
    }
   
    /** Loads instructions from a file
     *  @return The instructions as text
     */
    private String loadInstructions(File instructionsFolder) throws FileNotFoundException, IOException {
        
        String instructions = "";
        String instructionsFilePath = instructionsFolder.getPath() + "/instructions.txt";
        File instructionsFile = new File(instructionsFilePath);
        
        if (!instructionsFile.exists()) return "";
        
        // Get instructions from file
        try {
            
            // Open file input stream
            FileInputStream fileStream = new FileInputStream(instructionsFile);
        
            // Now read in data from the stream
            int size = (int)instructionsFile.length();
            byte[] byteSize = new byte[size];
            fileStream.read(byteSize);
            instructions = new String(byteSize, "UTF-8");
                        
            // Close file input stream
            fileStream.close();
        
        } 
        
        catch (Exception ex) {
        
            if (Test.isDebug) {
                ex.printStackTrace();
            }
            
        }
        
        return instructions;
                        
    }
    
    
    // ______________________________________________________________________
    // Class methods
    
   
    
    // ______________________________________________________________________
    // Constructors
    
    /** Creates a new instance of Instructions */
    public Instructions(File filepath) throws FileNotFoundException, IOException {
        text = loadInstructions(filepath);
    }
    
}
