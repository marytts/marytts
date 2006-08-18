package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.DataOutputStream;

import java.util.*;

/**
 * STSTrack represents the LPC-Data of an audio file
 */
public class STSTrack extends Track{
    private String filename;
    public int numFrames;
    public int numChannels;
    public int sampleRate;
    public float min;
    public float range;   
    public STSFrame[] frames;
    public int overAllTime; //over-all time of track (in samples)
    public long startTime; //start time of track in big audio file (in samples)
    private long[] times2Frames; //contains accumulated time of each frame (in samoles)
    public int startIndex; //index of track start in big audio file
    private long[] byteSize; //contains accumulated size of each frame in bytes

    public STSTrack(String filename, 
            		long startTime, 
            		STSFrame[] stsData,
            		long[] times2Frames, 
            		long[] byteSize, 
            		int overAllTime,
            		float sampleRate,
            		int numChannels){
        this.filename = filename;
        this.startTime = startTime;
        this.frames = stsData;
        this.times2Frames = times2Frames;
        this.byteSize = byteSize;
        this.overAllTime = overAllTime;
        this.sampleRate = (int)sampleRate;
        numFrames = frames.length;
        this.numChannels = numChannels;
    }
    
    

    public STSTrack(String filename, float min, 
            		float range, long startTime,
            		int startIndex)    
        throws IOException {
        this.startTime = startTime;
        this.min = min;
        this.range = range;
        this.startIndex = startIndex;
        readSTS(filename);
    }
    
    private void readSTS(String filename) throws IOException {
        this.filename = filename;
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(filename)));

        /* Read STS meta info
         */
        String line = reader.readLine();
        StringTokenizer tokenizer = new StringTokenizer(line);
        numFrames = Integer.parseInt(tokenizer.nextToken());
        numChannels = Integer.parseInt(tokenizer.nextToken());
        sampleRate = Integer.parseInt(tokenizer.nextToken());
        min = Float.parseFloat(tokenizer.nextToken());
        range = Float.parseFloat(tokenizer.nextToken());
        
        /* Read in the STS frame data from the file.
         */
        byteSize = new long[numFrames];
        long bytes = 0;
        overAllTime = 0;
        frames = new STSFrame[numFrames];
        for (int i = 0; i < numFrames; i++) {
            STSFrame frame = new STSFrame(numChannels, reader, startIndex);
            frames[i] = frame;
            overAllTime += frame.getNumSamples();
            times2Frames[i] = overAllTime+startTime;
            bytes += 12 + frame.getSize();
            byteSize[i] = bytes;
            startIndex++;
        }
        
        System.out.println("Over-all Time of track "+filename+" is "
                +overAllTime+" samples");
        
        reader.close();
    }

    public int getDuration(int start, int end){
        int duration = 0;
        for (int i=start;i<=end;i++){
            duration += frames[i].getNumSamples();
        }
        return duration;
    }
   
    public String getFilename(){
        return filename;
    }

    /**
     * Returns all the frames for this file.
     */
    public Frame[] getFrames() {
        return frames;
    }
    
    /**
     * Returns the number of bytes from beginning of track
     * up to a certain frame
     * @param index index of frame; -1 means byte size of whole track
     * @return number of bytes
     */
    public long getByteSize(int index){
        if (index == -1){
            return byteSize[numFrames-1];
        } else {
            return byteSize[index];
        }
    }
    
    /**
     * Gets the start time of a frame counted from beginning
     * of big audio file 
     * @param index the relative index of a frame
     * @return the start time
     */
    public long getTime(int index){
        if (index < 0 || index > times2Frames.length-1){
            return -1;
        } else {
        return times2Frames[index];}
    }
    
    /**
     * Gets the start time of the last frame
     * @return start time of last frame
     */
    public long getLastTime(){
        return times2Frames[times2Frames.length-1];
    }
    
    /**
     * Return the index of the last frame
     * @return relative index of last frame
     */
    public int getLastFrame(){
        return frames.length - 1;
    }
    
    /**
     * Gets the over-all index of a frame in the big audio file
     * @param index the relative index of a frame in this track
     * @return the over-all index
     */
   public int getOverAllIndex(int index){
       return frames[index].index;
   }
    
    /**
     * Finds the relative index of the frame closest to the given time (in samples)
     * @param time the time in seconds
     */
    public int findClosestFrame(long time) {
        int index = 0;
        for (int i=0; i<times2Frames.length; i++){
            long nextTime = times2Frames[i];
               //System.out.println("Comparison : "+nextTime+" > "+time);
            if (nextTime>=time){
                break;
            } else {
                index ++;
            }
        }
        if (index==times2Frames.length){
            return -1; //return error value
        } else {
            return index;
        }
    }

    /**
     * Gets the number of samples of all frames
     * from start index to end index
     * @param start start index
     * @param end end index
     * @return number of samples
     */
    public int getNumSamples(int start, int end){
        int result = 0;
        System.out.println("Getting num samples from index "
                	+start+" to "+end);
        for (int i=start;i<end+1;i++){
            result += frames[i].getNumSamples();
            System.out.print(result+" ");
        }
        return result;
    }
    
    /**
     * Gets the frame size of frame at the
     * given relative index
     * @param index the relative index of the frame
     * @return the frame size
     */
    public int getFrameSize(int index){
        return frames[index].getNumSamples();
    }
    
    /**
     * Dump binary
     */
    public void dumpBinary(DataOutputStream out) {
        for (int i = 0; i < frames.length; i++) {
            frames[i].dumpBinary(out);
        }
    }
    
    
}
