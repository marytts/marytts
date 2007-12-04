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
