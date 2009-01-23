/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.unitselection.data;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.MaryHeader;


public class FeatureFileReader
{
    protected MaryHeader hdr;
    protected FeatureDefinition featureDefinition;
    protected FeatureVector[] featureVectors;
    
    
    public static FeatureFileReader getFeatureFileReader(String fileName) throws IOException
    {
        int fileType = MaryHeader.peekFileType(fileName);
        if (fileType == MaryHeader.UNITFEATS)
            return new FeatureFileReader(fileName);
        else if (fileType == MaryHeader.HALFPHONE_UNITFEATS)
            return new HalfPhoneFeatureFileReader(fileName);
        throw new IOException("File "+fileName+": Type "+fileType+" is not a known unit feature file type");
    }
    
    
    /**
     * Empty constructor; need to call load() separately when using this.
     * @see load(String)
     */
    public FeatureFileReader()
    {
    }
    
    public FeatureFileReader( String fileName ) throws IOException
    {
        load(fileName);
    }
    
    public void load(String fileName) throws IOException
    {
        /* Open the file */
        DataInputStream dis = null;
        dis = new DataInputStream( new BufferedInputStream( new FileInputStream( fileName ) ) );
        
        boolean loadSuccess = this.load(dis);
        
        if (!loadSuccess)
            throw new IOException( "File [" + fileName + "] is not a valid Mary format file." );
        
        /*// --> load(dis)
        hdr = new MaryHeader( dis );
        if ( !hdr.isMaryHeader() ) {
            throw new IOException( "File [" + fileName + "] is not a valid Mary format file." );
        }
        if ( hdr.getType() != MaryHeader.UNITFEATS 
                && hdr.getType() != MaryHeader.HALFPHONE_UNITFEATS) {
            throw new IOException( "File [" + fileName + "] is not a valid Mary Features file." );
        }
        featureDefinition = new FeatureDefinition(dis);
        int numberOfUnits = dis.readInt();
        featureVectors = new FeatureVector[numberOfUnits];
        for (int i=0; i<numberOfUnits; i++) {
            featureVectors[i] = featureDefinition.readFeatureVector(i,dis);
        }
        */
    }
    
    public boolean load(DataInputStream dis) throws IOException{
        /* Load the Mary header */
        hdr = new MaryHeader( dis );
        if ( !hdr.isMaryHeader() ) {
            return false;
        }
        if ( hdr.getType() != MaryHeader.UNITFEATS 
                && hdr.getType() != MaryHeader.HALFPHONE_UNITFEATS) {
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
     * Get the unit feature vector for the given unit index number. 
     * @param unitIndex the absolute index number of a unit in the database
     * @return the corresponding feature vector
     */
    public FeatureVector getFeatureVector(int unitIndex)
    {
        return featureVectors[unitIndex];
    }
    
    /**
     * Return a shallow copy of the array of feature vectors.
     * @return a new array containing the internal feature vectors
     */
    public FeatureVector[] getCopyOfFeatureVectors()
    {
       return (FeatureVector[]) featureVectors.clone(); 
    }

    /**
     * Return the internal array of feature vectors.
     * @return the internal array of feature vectors.
     */
    public FeatureVector[] getFeatureVectors()
    {
       return featureVectors; 
    }

    /**
     * Get the unit feature vector for the given unit. 
     * @param unit a unit in the database
     * @return the corresponding feature vector
     */
    public FeatureVector getFeatureVector(Unit unit)
    {
        return featureVectors[unit.getIndex()];
    }

    public FeatureDefinition getFeatureDefinition()
    {
        return featureDefinition;
    }
    
    public int getNumberOfUnits() {
        return( featureVectors.length );
    }
}

