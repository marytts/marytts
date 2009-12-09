package marytts.nonverbal;

import java.io.DataInputStream;
import java.io.IOException;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.data.MaryHeader;

public class BackchannelFeatureFileReader extends marytts.unitselection.data.FeatureFileReader {

    public BackchannelFeatureFileReader( String fileName ) throws IOException
    {
        load(fileName);
    }
    
    public static BackchannelFeatureFileReader getFeatureFileReader(String fileName) throws IOException
    {
        int fileType = MaryHeader.peekFileType(fileName);
        
        if (fileType == MaryHeader.LISTENERFEATS)
            return new BackchannelFeatureFileReader(fileName);
        
        throw new IOException("File "+fileName+": Type "+fileType+" is not a known unit feature file type");
    }
    
    
    public boolean load(DataInputStream dis) throws IOException{
        /* Load the Mary header */
        hdr = new MaryHeader( dis );
        if ( !hdr.isMaryHeader() ) {
            return false;
        }
        if ( hdr.getType() != MaryHeader.LISTENERFEATS ) {
            return false;
        }
        featureDefinition = new FeatureDefinition(dis);
        int numberOfUnits = dis.readInt();
        featureVectors = new FeatureVector[numberOfUnits];
        for (int i=0; i<numberOfUnits; i++) {
            featureVectors[i] = featureDefinition.readFeatureVector(i,dis);
        }
        
        return true;

    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
