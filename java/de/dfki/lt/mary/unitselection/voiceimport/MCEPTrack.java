package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.*;

/**
 * MCEP-Track represents the MFCCs of an audio file
 */
public class MCEPTrack extends Track{
    public String filename;
    public int numFrames;
    public int numChannels;
    public int sampleRate;
    public float min;
    public float range;   
    private Frame[] frames;
    private int start;
    

    public MCEPTrack(String filename, 
            		float min, 
            		float range, 
            		Frame[] frames,
            		int start)    
    	throws IOException {
        this.frames = frames;
        this.start = start;
        this.min = min;
        this.range = range;
        readMCEP(filename);
    }
    
   
    private void readMCEP(String filename) throws IOException {
        this.filename = filename;
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(filename)));

        /* Read MCEP meta info
         */
        String line = reader.readLine();
        while (!line.equals("EST_Header_End")) {
            line = reader.readLine();
            if (line.startsWith("NumFrames")) {
                numFrames = Integer.parseInt(line.substring(10));
            } else if (line.startsWith("NumChannels")) {
                
                /* With MCEP, the first channel is the energy.  We
                 * drop this because it is not used to select units.
                 */
                //Question (Anna): Do we want to drop the energy????
                numChannels = Integer.parseInt(line.substring(12));
            }
        }

        /* Read each of the frames.
         */
        //frames = new Frame[numFrames];
        for (int i = start; i < numFrames+start; i++) {
            line = reader.readLine();
            StringTokenizer tokenizer = new StringTokenizer(line);
            float pitchmarkTime = Float.parseFloat(tokenizer.nextToken());
            tokenizer.nextToken(); /* no clue what 1 means in the file */
            int[] mcepParameters = new int[numChannels];
            for (int j = 0; j < numChannels ; j++) {
                float mcepParameter = Float.parseFloat(tokenizer.nextToken());

                /* With MCEP, the first channel is the energy.  We
                 * drop this because it is not used to select units.
                 
                if (j == 0) {
                    continue;
                } else {*/
                    /* Normalize the parameter to 0 - 65535.
                     */
                    mcepParameters[j] = (int)
                        (65535.0f * (mcepParameter - min) / range);
                //}
            }
            
            frames[i] = new Frame(pitchmarkTime, mcepParameters);
        }
        
        reader.close();
    }

    /**
     * Returns all the frames for this file.
     */
    public Frame getFrames(int i) {
        return frames[i];
    }
    
    public String getFilename(){
        return null;
    }
    
    /**
     * Finds the relative index of the frame closest to the given time (in samples)
     * @param time the time in seconds
     */
    public  int findClosestFrame(long time){
        return -1;
    }
    
    /**
     * Gets the over-all index of a frame in the big audio file
     * @param index the relative index of a frame in this track
     * @return the over-all index
     */
    public int getOverAllIndex(int index){
        return -1;
    }
    
    /**
     * Gets the start time of a frame counted from beginning
     * of big audio file 
     * @param index the relative index of a frame
     * @return the start time
     */
    public long getTime(int index){
        return -1;
    }
    
    /**
     * Gets the frame size of frame at the
     * given relative index
     * @param index the relative index of the frame
     * @return the frame size
     */
    public int getFrameSize(int index){
        return -1;
    }
    
    /**
     * Gets the number of samples of all frames
     * from start index to end index
     * @param start start index
     * @param end end index
     * @return number of samples
     */
    public int getNumSamples(int start, int end){
        return -1;
    }
    
    /**
     * Return the index of the last frame
     * @return relative index of last frame
     */
    public int getLastFrame(){
        return -1;
    }
    
    /**
     * Dump binary
     */
    public void dumpBinary(DataOutputStream out){}
    
    public int getDuration(int start, int end){
        return -1;
    }
    
    
}
