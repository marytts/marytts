package marytts.signalproc.analysis;

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;

public class VoiceQualityFileHeader extends FeatureFileHeader {
  
  public VoiceQualityFileHeader()
  {
      super();
  }
  
  public VoiceQualityFileHeader(VoiceQualityFileHeader existingHeader)
  {
      super(existingHeader);
  }
  
  public VoiceQualityFileHeader(String vqFile)
  {
      super(vqFile);
  }
  
  public boolean isIdenticalAnalysisParams(VoiceQualityFileHeader hdr)
  {
      return super.isIdenticalAnalysisParams(hdr);
  }
  
  public void readHeader(MaryRandomAccessFile ler, boolean bLeaveStreamOpen) throws IOException
  {
      super.readHeader(ler, true);
      
      if (ler!=null)
      {
          if (!bLeaveStreamOpen)
          {
              ler.close();
              ler = null;
          }
      }
  }
  
  public void writeHeader(MaryRandomAccessFile ler) throws IOException
  {   
      super.writeHeader(ler);
  }

}
