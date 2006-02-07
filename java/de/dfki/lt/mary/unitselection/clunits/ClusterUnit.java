/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package de.dfki.lt.mary.unitselection.clunits;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import de.dfki.lt.mary.unitselection.Unit;


/**
 * Representation of a unit from a unit database. This gives access to
 * everything that is known about a given unit, including all sorts of 
 * features and the actual audio data.
 * @author Marc Schr&ouml;der
 *
 */
public class ClusterUnit extends Unit
{
    public static final int PHONE = 1;
    public static final int DIPHONE = 2;
    public static final int HALFPHONE = 3;
    public static final int PHRASE = 100;
    
    public int type;
    public int phone;
    public int start;
    public int end;
    public int prev;
    public int next;
    
    private UnitOriginInfo origin;
    
    protected int instanceNumber; // identifies the instances of a given type
    
    /**
     * Read in a ClusterUnit from the given ByteBuffer
     * @param bb the ByteBuffer
     * @param database the database containing this unit
     * @param name the name of this unit
     * @throws IOException
     */
    public ClusterUnit(MappedByteBuffer bb, ClusterUnitDatabase database, String name) throws IOException{
        super(database, name);
        this.type = bb.getInt();
        this.phone = bb.getInt();
        this.start = bb.getInt();
        this.end = bb.getInt();
        this.prev = bb.getInt();
        this.next = bb.getInt();
    }
    
    /**
     * Read in a ClusterUnit from the given RandomAccessFile
     * @param raf the RandomAccessFile
     * @param database the database containing this unit
     * @param name the name of this unit
     * @throws IOException
     */
    public ClusterUnit(RandomAccessFile raf, ClusterUnitDatabase database, String name) throws IOException{
        super(database, name);
        this.type = raf.readInt();
       this.phone = raf.readInt();
       this.start = raf.readInt();
       this.end = raf.readInt();
       this.prev = raf.readInt();
       this.next = raf.readInt();
    }   
    
    public void setInstanceNumber(int instanceNumber)
    {
        this.instanceNumber = instanceNumber;
    }
    
    public int getInstanceNumber()
    {
        return instanceNumber;
    }
    
    /**
     * The number of frames that belong to this unit.
     * @return the number of frames.
     */
    public int getNumberOfFrames()
    {
        return end-start;
    }
    
    /**
     * Return the audio frame with the given index number. 
     * @param frameNumber index number of the required frame, ranging from 0 to
     * getNumberOfFrames()-1.
     * @return a Frame object representing an audio frame.
     * @throws IllegalArgumentException if a unit out of range is requested.
     */
    public Frame getAudioFrame(int frameNumber)
    {
        if (frameNumber < 0 || frameNumber > end-start) throw new IllegalArgumentException("Unit has "+(end-start)+" frames, requested no. "+frameNumber);
        return database.getAudioFrames().getFrame(start+frameNumber);
    }
    
    /**
     * Return the join cost feature vector belonging to the given frame number. 
     * @param frameNumber index number of the required frame, ranging from 0 to
     * getNumberOfFrames()-1.
     * @return a Frame object representing a join cost feature vector.
     * @throws IllegalArgumentException if a unit out of range is requested.
     */
    public Frame getJoinCostFeatureVector(int frameNumber)
    {
        if (frameNumber < 0 || frameNumber > end-start) throw new IllegalArgumentException("Unit has "+(end-start)+" frames, requested no. "+frameNumber);
        return database.getJoinCostFeatureVectors().getFrame(start+frameNumber);
    }

    /**
     * Get the unit that in the original recordings follows this one.
     * @return the next unit, or null if this unit is final.
     */
    public Unit getNext()
    {
        if (next == ClusterUnitDatabase.CLUNIT_NONE) return null;
        return ((ClusterUnitDatabase)database).getUnit(next);
    }

    /**
     * Get the unit that in the original recordings precedes this one.
     * @return the previous unit, or null if this unit is initial.
     */
    public Unit getPrevious()
    {
        if (prev == ClusterUnitDatabase.CLUNIT_NONE) return null;
        return ((ClusterUnitDatabase)database).getUnit(prev);
    }
    
    /**
     * Sets the origin info of this unit
     * @param origin the origin info
     */
    public void setOriginInfo(UnitOriginInfo origin){
        this.origin = origin;
    }
    
    /**
     * Gets the origin info of this unit
     * @return the origin info
     */
    public UnitOriginInfo getOriginInfo(){
        return origin;
    }
    
    public String toString()
    {
        return name+" "+instanceNumber+" (frames "+start+" - "+end+")";
    }
}
