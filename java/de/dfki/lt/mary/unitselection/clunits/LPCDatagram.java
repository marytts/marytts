package de.dfki.lt.mary.unitselection.clunits;

import java.io.*;
import org.apache.log4j.Logger;

/**
 * 
 * A LPCDatagram is a LPC-Frame consisting of Coefficients and Residuals
 * 
 * @author Anna
 * @date 31.07.2006
 */
public class LPCDatagram extends Datagram {
    
    private short[] coeffs;
    private byte[] residuals;
    private int numResiduals;
    private Logger logger;
    
    public LPCDatagram(RandomAccessFile raf, int coeffLength) throws IOException{
        super();
        logger = Logger.getLogger(this.getClass());
        int sizeInBytes = raf.readInt();
        numResiduals = (int)raf.readLong();
        logger.debug("Size in bytes "+sizeInBytes+" numRes "+numResiduals);
        coeffs = new short[coeffLength];
        for (int i=0;i<coeffLength;i++){
            coeffs[i] = raf.readShort();
            //logger.debug("Coeff "+i+" "+coeffs[i]);
        }
        residuals = new byte[numResiduals];
        for (int i=0; i<numResiduals;i++){
            residuals[i] = (byte)raf.read();
            //logger.debug("Res "+i+" "+residuals[i]);
        }
    }
    
    public LPCDatagram(RandomAccessFile raf, int coeffLength,int numRes) throws IOException{
        super();
        logger = Logger.getLogger(this.getClass());
        numResiduals = numRes;
        logger.debug("numRes "+numResiduals);
        coeffs = new short[coeffLength];
        for (int i=0;i<coeffLength;i++){
            coeffs[i] = raf.readShort();
            //logger.debug("Coeff "+i+" "+coeffs[i]);
        }
        residuals = new byte[numResiduals];
        for (int i=0; i<numResiduals;i++){
            residuals[i] = (byte)raf.read();
            //logger.debug("Res "+i+" "+residuals[i]);
        }
    }
    
    public short[] getCoefficients(){
        return coeffs;
    }
    
    public byte[] getResiduals(){
        return residuals;
    }
    
    public int getNumResiduals(){
        return numResiduals;
    }
    
}