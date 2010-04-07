package marytts.signalproc.analysis;

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;

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
  
  public VoiceQuality(int numVqParams, int numFrames)
  {
     params = new VoiceQualityFileHeader();    
     vq = new double[numVqParams][numFrames];
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
      
      if (stream!=null && params.numfrm>0 && params.dimension>0)
      {
          vqs = new double[params.numfrm][];
          
          for (int i=0; i<vqs.length; i++)
              vqs[i] = stream.readDouble(params.dimension);
          
          stream.close();
      }
      
      return vqs;
  }  
  
  
}
