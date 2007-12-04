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

//import de.dfki.lt.mary.recsessionmgr.gui.AdminWindow;
import java.io.File;

/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
 */
public class Prompt {
    
    // ______________________________________________________________________
    // Instance fields
    private String basename;          // Basename used in filenames for this prompt
    private String promptText;        // Prompt text to display to speaker
    protected Synthesis synthesized;  // Synthesized version of the prompt - not needed here?
    protected Recording recorded;     // Recorded version(s) of the prompt
        
    // ______________________________________________________________________
    // Class fields
    
    // ______________________________________________________________________
    // Instance methods
    
    /** Gets the basename for the prompt
     *  @return The basename for the current prompt
     */
    public String getBasename() { return basename; }
    
    /** Gets the prompt text for the prompt
     *  @return The prompt text for the current prompt
     */
    public String getPromptText() { return promptText; }
    
    /** Sets the prompt text for the prompt
     *  @param The prompt text for the current prompt
     */
    public void setPromptText(String text) {
        this.promptText = text;
    }

    /**
     * Get the recording object associated to this prompt.
     * @return
     */
    public Recording getRecording() { return recorded; }
    
    /** Gets the the number of recordings for the current prompt
     *  @return The number of recordings for the current prompt
     */
    public int getRecCount() { return recorded.getFileCount(); }

    public Synthesis getSynthesis() {
        return synthesized;
    }
        
    // ______________________________________________________________________
    // Class methods
    
    // ______________________________________________________________________
    // Constructors
    
    /** Creates a new instance of Prompt
     *  @param promptBasename The basename for the prompt (e.g., spike0003)
     *  @param voicePath The file path for the voice (e.g., path for Spike)
     */    
    public Prompt(String passedBasename, File recFolderPath, File synthFolderPath) {

        this.basename = passedBasename;
        this.recorded = new Recording(recFolderPath, this.basename);
        this.synthesized = new Synthesis(synthFolderPath, this.basename);
        
    }
    
}
