/* Recording Session Manager
 * @version 1.0
 * 
 * Instructions.java
 *
 * Created on March 16, 2007, 5:43 PM
 *
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
