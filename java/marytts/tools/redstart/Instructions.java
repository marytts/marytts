/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.redstart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


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

