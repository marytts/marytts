package marytts.vocalizations;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.data.MaryHeader;

public class VocalizationFeatureFileReader extends marytts.unitselection.data.FeatureFileReader {

    public VocalizationFeatureFileReader( String fileName ) throws IOException
    {
        load(fileName);
    }
    
    public static VocalizationFeatureFileReader getFeatureFileReader(String fileName) throws IOException
    {
        int fileType = MaryHeader.peekFileType(fileName);
        
        if (fileType == MaryHeader.LISTENERFEATS)
            return new VocalizationFeatureFileReader(fileName);
        
        throw new IOException("File "+fileName+": Type "+fileType+" is not a known unit feature file type");
    }
    
    @Override
    protected void loadFromStream(String fileName) throws IOException {
        /* Open the file */
        DataInputStream dis = null;
        dis = new DataInputStream( new BufferedInputStream( new FileInputStream( fileName ) ) );
        
        /* Load the Mary header */
        hdr = new MaryHeader( dis );
        if ( !hdr.isMaryHeader() ) {
            throw new IOException( "File [" + fileName + "] is not a valid Mary format file." );
        }
        if ( hdr.getType() != MaryHeader.LISTENERFEATS ) {
            throw new IOException( "File [" + fileName + "] is not a valid Mary listener feature file." );
        }
        featureDefinition = new FeatureDefinition(dis);
        int numberOfUnits = dis.readInt();
        featureVectors = new FeatureVector[numberOfUnits];
        for (int i=0; i<numberOfUnits; i++) {
            featureVectors[i] = featureDefinition.readFeatureVector(i,dis);
        }
    }

    @Override
    protected void loadFromByteBuffer(String fileName) throws IOException {
        /* Open the file */
        /* Open the file */
        FileInputStream fis = new FileInputStream(fileName);
        FileChannel fc = fis.getChannel();
        ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        fis.close();
        
        /* Load the Mary header */
        hdr = new MaryHeader(bb);
        if ( !hdr.isMaryHeader() ) {
            throw new IOException( "File [" + fileName + "] is not a valid Mary format file." );
        }
        if ( hdr.getType() != MaryHeader.LISTENERFEATS ) {
            throw new IOException( "File [" + fileName + "] is not a valid Mary listener feature file." );
        }
        featureDefinition = new FeatureDefinition(bb);
        int numberOfUnits = bb.getInt();
        featureVectors = new FeatureVector[numberOfUnits];
        for (int i=0; i<numberOfUnits; i++) {
            featureVectors[i] = featureDefinition.readFeatureVector(i, bb);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
