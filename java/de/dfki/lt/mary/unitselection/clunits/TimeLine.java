package de.dfki.lt.mary.unitselection.clunits;

import java.io.*;

/**
 * 
 * A TimeLine manages the access to audio data
 * and stores the audio metadata
 * 
 * @author Anna
 * @date 31.07.2006
 */
public abstract class TimeLine{

    protected RandomAccessFile raf;
    protected long numDatagrams;
    protected final byte REGULAR = 0;
    protected final byte VARIABLE = 1;
    protected byte timeSpacing;
    
    public TimeLine(RandomAccessFile raf){
        try {
            this.raf = raf;
            timeSpacing = (byte)raf.read();
            numDatagrams = raf.readLong();
        } catch (IOException ioe){
            throw new Error("Error reading audio : "+ioe.getMessage());
        }
    }
    
    public Datagram[] getDatagrams(long startSample, int duration){
        return null;
    }
    
}