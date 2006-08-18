package de.dfki.lt.mary.unitselection.clunits;

import java.io.*;
//import org.apache.log4j.Logger;

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
    //private Logger logger;
    
    public LPCDatagram(RandomAccessFile raf, int coeffLength) throws IOException{
        super();
        //logger = Logger.getLogger(this.getClass());
        int sizeInBytes = raf.readInt();
        numResiduals = (int)raf.readLong();
        //logger.debug("Size in bytes "+sizeInBytes+" numRes "+numResiduals);
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
    
    public LPCDatagram(RandomAccessFile raf, int numCoeffs,int numRes) throws IOException{
        super();
        //logger = Logger.getLogger(this.getClass());
        numResiduals = numRes;
        System.out.println("NumRes "+numResiduals );
       //logger.debug("numRes "+numResiduals);
        coeffs = new short[numCoeffs];
        for (int i=0;i<numCoeffs;i++){
            coeffs[i] = raf.readShort();
           //logger.debug("Coeff "+i+" "+coeffs[i]);
           System.out.println("Coeff "+i+" "+ (coeffs[i] + 32768) );
        }
        residuals = new byte[numResiduals];
        for (int i=0; i<numResiduals;i++){
            residuals[i] = (byte)raf.readUnsignedByte();
            //System.out.println( raf.read() );
            //logger.debug("Res "+i+" "+residuals[i]);
            System.out.println("Res "+i+" "+(residuals[i]+128));
        }
    }
    
    public void dump(){
        System.out.print(numResiduals+" ");
        for (int i=0;i<coeffs.length;i++){
            System.out.print(coeffs[i] +" ");
            //logger.debug("Coeff "+i+" "+coeffs[i]);
        }
        residuals = new byte[numResiduals];
        for (int i=0; i<numResiduals;i++){
            System.out.print(residuals[i] + " ");
            //logger.debug("Res "+i+" "+residuals[i]);
        }
        System.out.println();
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