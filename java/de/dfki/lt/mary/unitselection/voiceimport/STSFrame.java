package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.DataOutputStream;

import java.util.StringTokenizer;

	
/**
 * STS-Frame is an audio frame in LPC-Format
 */
public class STSFrame extends Frame {
    
    
    public byte[] residuals;
    
    public STSFrame(){
    }
    
    public STSFrame(int numChannels, BufferedReader reader, int index)
        throws IOException {
        this.index= index;
        String line = reader.readLine();
        pitchmarkTime = Float.parseFloat(line);

        StringTokenizer tokenizer = new StringTokenizer(line);
        for (int i = 0; i < numChannels; i++) {
            int svalue = Integer.parseInt(tokenizer.nextToken()) - 32768;

            if (svalue < -32768 || svalue > 32767) {
                throw new Error("data out of short range : " + svalue);
            }
            parameters[i] = (short) svalue;
        }

        line = reader.readLine();
        tokenizer = new StringTokenizer(line);
        // first residual is number of residuals
        numRes = Integer.parseInt(tokenizer.nextToken());
        
        residuals = new byte[numRes];
        for (int i = 0; i < residuals.length; i++) {
            int bvalue = Integer.parseInt(tokenizer.nextToken()) - 128;
            if (bvalue < -128 || bvalue > 127) {
                throw new Error("data out of byte range : " + bvalue);
            }
            residuals[i] = (byte) bvalue;
        }
       
    }
    
    /**
     * Gives size of the raw data of this frame in bytes
     * @return size in bytes
     */
    public int getSize(){
       return parameters.length*2+residuals.length;
    }

    public void dumpBinary(DataOutputStream out){
        try{
            out.writeInt(getSize());
            out.writeLong(numRes);
            //out.writeByte(parameters.length);
            for (int i = 0; i<parameters.length; i++){
                out.writeShort(parameters[i]);
            }
            out.write(residuals, 0, residuals.length); 
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error writing LPC data");
        }
    }
    
    public int getNumSamples(){
        return numRes;
        }
    
}
