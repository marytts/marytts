/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.unitselection;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.voiceimport.MaryHeader;

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
        /* Load the Mary header */
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
