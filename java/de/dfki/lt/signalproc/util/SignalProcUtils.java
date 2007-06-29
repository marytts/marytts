package de.dfki.lt.signalproc.util;

public class SignalProcUtils {
    
    public static int getLPOrder(int fs){
        int P = (int)(fs/1000.0f+2);
        
        if (P%2==1)
            P+=1;
        
        return P;
    }
    
    public static int getDFTSize(int fs){
        int dftSize;
        
        if (fs<8000)
            dftSize = 128;
        else if (fs<16000)
            dftSize = 256;
        else if (fs<22050)
            dftSize = 512;
        else if (fs<32000)
            dftSize = 1024;
        else if (fs<44100)
            dftSize = 2048;
        else
            dftSize = 4096;
        
        return dftSize;
    }
    
    public static int halfSpectrumSize(int fftSize)
    {
        return (int)(Math.floor(fftSize/2.0+1.5));
    }
    
    public static double getAverageSampleEnergy(double [] x)
    {
        double avgSampEn = 0.0;
        
        for (int i=0; i<x.length; i++)
            avgSampEn += x[i]*x[i];
        
        avgSampEn = Math.sqrt(avgSampEn);
        avgSampEn /= x.length;
        
        return avgSampEn;
    }
}
