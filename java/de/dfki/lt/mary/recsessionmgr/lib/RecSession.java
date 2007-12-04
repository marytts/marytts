/* Recording Session Manager
 * @version 1.0
 * 
 * RecSession.java
 *
 * Created on March 15, 2007, 5:02 PM
 *
 */

package de.dfki.lt.mary.recsessionmgr.lib;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.dfki.lt.mary.recsessionmgr.gui.AdminWindow;

/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
 */
public class RecSession {
    
    // ______________________________________________________________________
    // Instance fields
       
    private Prompt[] promptArray;           // Set of prompts
    private Instructions instructions;      // Set of instructions for the prompt set
    
    // ______________________________________________________________________
    // Class fields
    
    // ______________________________________________________________________
    // Instance methods
        
    /** Gets a set of text instructions associated with a prompt set
     *  @return The instructions for the prompt set (as Instructions object)
     */
    public Instructions getInstructions() {       
       return instructions;    
    }
    
    /** Gets the array of prompts for the current recording session
     *  @return The array of prompts for the current recording session
     */
    public Prompt[] getPromptArray() {       
       return promptArray;    
    }
    
    // ______________________________________________________________________
    // Class methods
          
    // ______________________________________________________________________
    // Constructors
    
    /** Creates a new instance of RecSession */
    public RecSession(AdminWindow adminWindow) throws FileNotFoundException, IOException {
        
        // Create a new prompt set object
        PromptSet sessionPrompts = new PromptSet(adminWindow);
        
        this.promptArray = sessionPrompts.promptArray;
        this.instructions = sessionPrompts.instructions;
    }
    
}
