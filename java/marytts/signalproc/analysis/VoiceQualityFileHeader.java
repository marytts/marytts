package marytts.signalproc.analysis;

import java.io.DataOutput;
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
  
  @Override
  public void writeHeader(DataOutput ler) throws IOException
  {   
      super.writeHeader(ler);
  }

}
