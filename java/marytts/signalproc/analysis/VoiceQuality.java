package marytts.signalproc.analysis;

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;
import marytts.util.MaryUtils;

/**
 * A wrapper class for frame based voice quality parameters
 *
 * @author Marcela Charfuelan
 */
public class VoiceQuality {

  public double [][] vq;
  public VoiceQualityFileHeader params;
  
  public VoiceQuality()
  {
      this("");
  }
  
  public VoiceQuality(String vqFile)
  {
      readVqFile(vqFile);
  }
  
  /**
   * VoiceQuality object containing various vq mesures
   * @param numVqParams number of vq parameters per frame
   * @param Fs sampling rate
   * @param skipSize skip size in seconds
   * @param winSize window size in seconds
   */
  public VoiceQuality(int numVqParams, int Fs, float skipSize, float winSize)
  {
     params = new VoiceQualityFileHeader();   
     params.dimension = numVqParams;     
     params.samplingRate = 16000;
     params.skipsize = skipSize;  // in seconds
     params.winsize = winSize;  // in seconds
     
  }
  
  public void allocate(int numFramesVq, double[][] par){
    params.numfrm = numFramesVq;
    vq = new double[params.dimension][numFramesVq];
    for(int i=0; i<params.dimension; i++)
      for(int j=0; j<numFramesVq; j++)
        vq[i][j] = par[i][j];        
    
  }
  
  public void printPar(){
    System.out.println("Features Read:\nframe\tOQG\tGOG\tSKG\tRCG\tIC");
    for(int i=0; i<params.numfrm; i++)
      System.out.format("%d\t%.3f %.3f %.3f %.3f %.3f \n", 
          i+1, vq[0][i], vq[1][i], vq[2][i], vq[3][i], vq[4][i]); 
  }
  
  public void printMeanStd(){
    System.out.println("Mean +- Standard deviation:\n\tOQG\tGOG\tSKG\tRCG\tIC");
    System.out.format("mean: %.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", MaryUtils.mean(vq[0], true), MaryUtils.mean(vq[1], true),
        MaryUtils.mean(vq[2], true), MaryUtils.mean(vq[3], true),  MaryUtils.mean(vq[4], true));
    System.out.format("std : %.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", MaryUtils.stdDev(vq[0], true), MaryUtils.stdDev(vq[1], true),
        MaryUtils.stdDev(vq[2], true), MaryUtils.stdDev(vq[3], true),  MaryUtils.stdDev(vq[4], true));

  }
  
  public void readVqFile(String vqFile)
  {
      params = new VoiceQualityFileHeader();
      
      if (vqFile!="")
      {
          MaryRandomAccessFile stream = null;
          try {
              stream = params.readHeader(vqFile, true);
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }

          if (stream != null)
          {
              try {
                  vq = readVqs(stream, params);
              } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
              }
          }
      }
  }
  
  public void writeVqFile(String vqFile)
  {   
      if (vqFile!="")
      {
          MaryRandomAccessFile stream = null;
          try {
              stream = params.writeHeader(vqFile, true);
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }

          if (stream != null)
          {
              try {
                  writeVqs(stream, vq);
              } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
              }
          }
      }
  }
  
  
  public static void writeVqs(MaryRandomAccessFile stream, double[][] vqs) throws IOException
  {
      if (stream!=null && vqs!=null && vqs.length>0)
      {
          for (int i=0; i<vqs.length; i++)
              stream.writeDouble(vqs[i]);
          
          stream.close();
      }
  }
  
  
  public static double[][] readVqs(MaryRandomAccessFile stream, VoiceQualityFileHeader params) throws IOException
  {
      double[][] vqs = null;
      
      if (stream!=null && params.dimension>0 && params.dimension>0)
      {
          vqs = new double[params.dimension][];
          
          for (int i=0; i<vqs.length; i++)
              vqs[i] = stream.readDouble(params.numfrm);
          
          stream.close();
      }
      
      return vqs;
  }  
  
  
}
