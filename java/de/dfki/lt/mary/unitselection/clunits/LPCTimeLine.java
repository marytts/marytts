package de.dfki.lt.mary.unitselection.clunits;

import java.io.*;
import java.util.*;
//import org.apache.log4j.Logger;

/**
 * 
 * A LPCTimeLine manages the access to audio data
 * encoded as LPC and stores the audio metadata
 * 
 * @author Anna
 * @date 31.07.2006
 */
public class LPCTimeLine extends TimeLine{
    
    private int channels;
    private float lpcMin;
    private float lpcRange;
    
    private int numIndices;
    private int intervall;
    
    private long[] bytePositions;
    private long[] timeDiffs;
    
    //private Logger logger;
    
    public LPCTimeLine(RandomAccessFile raf, StringTokenizer tok){
        super(raf);
        //logger = Logger.getLogger(this.getClass());
        //read header information
        tok.nextToken();
        channels = Integer.parseInt(tok.nextToken());
        tok.nextToken();
        lpcMin = Float.parseFloat(tok.nextToken());
        tok.nextToken();
        lpcRange = Float.parseFloat(tok.nextToken());
        
        try {
            //read indices
            numIndices = (int)raf.readLong();
            intervall = raf.readInt();
            bytePositions = new long[numIndices];
            timeDiffs = new long[numIndices];
            for (int i=0;i<numIndices;i++){
                bytePositions[i] = raf.readLong();
                timeDiffs[i] = raf.readLong();
                //System.out.println("Next position "+bytePositions[i]
                  //                +" next time diff "+timeDiffs[i]);
            }
        } catch  (IOException ioe){
            throw new Error("Error reading audio : "+ioe.getMessage());
        }
    }
    
    public static void main (String[] args){
        try{
        String filename = "/home/cl-home/sacha/workspace/openmary/lib/voices/cmu_us_bdl_arctic/audio.bin";
        RandomAccessFile raf = new RandomAccessFile(filename, "r");
        raf.readInt();
        raf.readInt(); 
        raf.readInt(); 
        //read the header
        String header = raf.readUTF();
        byte timeSpacing = (byte)raf.read();
        long numDatagrams = raf.readLong();
        int numIndices = (int)raf.readLong();
        int intervall = raf.readInt();
        long[] bytePositions = new long[numIndices];
        long[]timeDiffs = new long[numIndices];
        for (int i=0;i<numIndices;i++){
            bytePositions[i] = raf.readLong();
            timeDiffs[i] = raf.readLong();
            //System.out.println("Next position "+bytePositions[i]
              //                +" next time diff "+timeDiffs[i]);
        }
        
        int i=0;
        while (i<1){
          
            int sizeInBytes = raf.readInt();
            int numRes = (int)raf.readLong();
             LPCDatagram d = new LPCDatagram(raf,16,numRes);
            d.dump();
            i++;
        }  
        } catch  (IOException ioe){
            throw new Error("Error reading audio : "+ioe.getMessage());
        }
    }
    
    public Datagram[] getDatagrams(long startSample, int durationInSamples){
        
        List datagrams = new ArrayList();
        System.out.println("Collecting Datagrams, starting at "+startSample );
        //move RandomAccessFile to first datagram
        try{
            
        int numRes = goToUnitStart(startSample);
        //read in the datagrams
        System.out.println("Reading datagram "+0+" at position "+raf.getFilePointer());
        datagrams.add(new LPCDatagram(raf, channels, numRes));
        durationInSamples -= numRes;
        int numFrames = 1;
        while (durationInSamples > 0){
            System.out.println("Reading datagram "+numFrames+" at position "+raf.getFilePointer());
            int sizeInBytes = raf.readInt();
            numRes = (int)raf.readLong();
            datagrams.add(new LPCDatagram(raf,channels,numRes));
            durationInSamples -= numRes;
            numFrames++;
        }        
        Datagram[] result = new LPCDatagram[numFrames];
        datagrams.toArray(result);
        return result;
        } catch (IOException ioe){
            ioe.printStackTrace();
            throw new Error("IOException when reading audio file");
        }
    }
    
    private int goToUnitStart(long startSample) throws IOException{
        //jump to Position of nearest index
        int index = (int)startSample/intervall;
        int samplesToGo = (int)startSample%intervall; 
        System.out.println("Starting from index "+index+". Still "
                +samplesToGo+" samples to go.");
        if (index>numIndices+1){
            throw new Error("Start of unit out of index range: "+startSample);
        } 
        long bytePos = bytePositions[index];
        long timeDiff = timeDiffs[index];
        //System.out.println("Jumping to "+bytePos);
        raf.seek(bytePos);
        long currentPos = bytePos;
        int numRes = 0;
        //System.out.println("Next time diff "+timeDiff+". Still "
          //      +samplesToGo+" samples to go.");
        if (timeDiff>samplesToGo){
            System.out.println("Size of first datagram "+raf.readInt());
            numRes=(int) raf.readLong();
        } else {           
            samplesToGo-= timeDiff;
            while (samplesToGo>0){
                int sizeInBytes = raf.readInt();
               // System.out.println("Size of next Frame "+sizeInBytes+" bytes");
                numRes = (int)raf.readLong();
                //System.out.println("Next time diff "+numRes+". Still "
                  //      +samplesToGo+" samples to go.");
                if (numRes>samplesToGo){
                    samplesToGo =0;
                    System.out.println("Size of first datagram "+sizeInBytes);
                } else {
                    samplesToGo-=numRes;
                    currentPos+=sizeInBytes+12;
                    //System.out.println("Jumping to "+currentPos);
                    raf.seek(currentPos);
                    if (samplesToGo == 0){
                        System.out.println("numRes same as samplesToGo");
                        sizeInBytes = raf.readInt();
                        numRes = (int)raf.readLong();
                        System.out.println("Size of first datagram "+sizeInBytes);
                    } 
                   
                    
                }
            }
        }
        return numRes;
    }
    
    public int getNumChannels(){
        return channels;
    }
    
    public float getCoeffMin(){
        return lpcMin;
    }
    
    public float getCoeffRange(){
        return lpcRange;
    }
    
}