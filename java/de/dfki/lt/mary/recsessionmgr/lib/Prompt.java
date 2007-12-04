/* Recording Session Manager
 * @version 1.0
 * 
 * Prompt.java
 *
 * Created on March 16, 2007, 5:26 PM
 *
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
