package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.DataOutputStream;

/**
 * Track represents audio data of a file
 */
public abstract class Track {
    
    public abstract String getFilename();
    
    /**
     * Finds the relative index of the frame closest to the given time (in samples)
     * @param time the time in seconds
     */
    public  abstract int findClosestFrame(long time);
    
    /**
     * Gets the over-all index of a frame in the big audio file
     * @param index the relative index of a frame in this track
     * @return the over-all index
     */
    public abstract int getOverAllIndex(int index);
    
    /**
     * Gets the start time of a frame counted from beginning
     * of big audio file 
     * @param index the relative index of a frame
     * @return the start time
     */
    public abstract long getTime(int index);
    
    /**
     * Gets the frame size of frame at the
     * given relative index
     * @param index the relative index of the frame
     * @return the frame size
     */
    public abstract int getFrameSize(int index);
    
    /**
     * Gets the number of samples of all frames
     * from start index to end index
     * @param start start index
     * @param end end index
     * @return number of samples
     */
    public abstract int getNumSamples(int start, int end);
    
    /**
     * Return the index of the last frame
     * @return relative index of last frame
     */
    public abstract int getLastFrame();
    
    /**
     * Dump binary
     */
    public abstract void dumpBinary(DataOutputStream out);
    
    public abstract int getDuration(int start, int end);
    
}