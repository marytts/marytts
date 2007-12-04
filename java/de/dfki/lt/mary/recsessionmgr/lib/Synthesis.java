/* Recording Session Manager
 * @version 1.0
 * 
 * Synthesis.java
 *
 * Created on March 16, 2007, 5:43 PM
 *
 */

package de.dfki.lt.mary.recsessionmgr.lib;

import java.io.File;

/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
 */
public class Synthesis extends Speech {
    
    // ______________________________________________________________________
    // Instance fields
    
    // ______________________________________________________________________
    // Class fields
    
    // ______________________________________________________________________
    // Instance methods
    
    /** Determines duration of the synthesized sound file
     *  @return The duration (in milliseconds)
     */
    public int getDuration() {
        
        // PRI1 Determine duration of synthesis object
        // Hardcoded value for development and testing
        int synthDuration = 4000;  // Duration of sound file (in ms)
        
        // Code to determine duration (in ms) of synthesized sound file
        
        return synthDuration; // in ms
        
    }
    
    // play() method inherited from Speech object, so not needed here
    
    // ______________________________________________________________________
    // Class methods
    
    // ______________________________________________________________________
    // Constructors
    
    /** Creates a new instance of Synthesis */
    public Synthesis(File filePath, String basename) {
        super(filePath, basename);
    }
    
}
