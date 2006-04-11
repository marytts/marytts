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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.util.*;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitDatabase;


/**
 * Representation of a unit from a unit database. This gives access to
 * everything that is known about a given unit, including all sorts of 
 * features and the actual audio data.
 * @author Marc Schr&ouml;der
 *
 */
public class ClusterUnit extends Unit
{
    
    
    private UnitOriginInfo origin;
    private long valsStart;
    private MappedByteBuffer bb = null;
    private RandomAccessFile raf = null;
    private boolean canReadInVals = false;
    
    protected int instanceNumber; // identifies the instances of a given type
    
    /**
     * Read in a ClusterUnit from the given ByteBuffer
     * @param bb the ByteBuffer
     * @param database the database containing this unit
     * @param name the name of this unit
     * @throws IOException
     */
    public ClusterUnit(MappedByteBuffer bb, 
            		   ClusterUnitDatabase database, 
            		   String name,
            		   boolean readFeatures) throws IOException{
        super(database, name);
        this.type = bb.getInt();
        this.phone = bb.getInt();
        this.start = bb.getInt();
        this.end = bb.getInt();
        /**
        if (database.getUnitSize() == UnitDatabase.HALFPHONE &&
                name.endsWith("left")){
            end = end - ((start-end)/2);
        } 
        if (database.getUnitSize() == UnitDatabase.HALFPHONE &&
            name.endsWith("right")){
            start = start + ((end-start)/2);
        } **/
        this.prev = bb.getInt();
        this.next = bb.getInt();
        if (readFeatures){
            //remember the current position 
            int valSizeInBytes = bb.getInt();
            valsStart = bb.position();
            this.bb = bb;
            if (valSizeInBytes != 0){
                canReadInVals = true;
                // skip the values
                bb.position((int)valsStart+valSizeInBytes);
            }
        } else {
            values = null;
        }
    }
    
    /**
     * Read in a ClusterUnit from the given RandomAccessFile
     * @param raf the RandomAccessFile
     * @param database the database containing this unit
     * @param name the name of this unit
     * @throws IOException
     */
    public ClusterUnit(RandomAccessFile raf, 
            		   ClusterUnitDatabase database, 
            		   String name,
            		   boolean readFeatures) throws IOException{
        super(database, name);
        this.type = raf.readInt();
       this.phone = raf.readInt();
       this.start = raf.readInt();
       this.end = raf.readInt();
       this.prev = raf.readInt();
       this.next = raf.readInt();
       if (readFeatures){
           //remember current position
           int valSizeInBytes = raf.readInt();
           valsStart = raf.getFilePointer();
           this.raf = raf;
           if (valSizeInBytes != 0){
               canReadInVals = true;
               // skip the values
               raf.skipBytes(valSizeInBytes);
           }
       } else {
           values = null;
       }
    }   
    
    /**
     * Build a new ClusterUnit with the given parameters.
     * This constructor is only used for the conversion from FreeTTS
     * text format to Mary binary format
     */
    public ClusterUnit(int type, int phone, int start, 
            		   int end, int prev, int next){
        super(null, "");
        this.type = type;
       this.phone = phone;
       this.start = start;
       this.end = end;
       this.prev = prev;
       this.next = next;
    }
    
    /**
	 * Dumps this unit to the given output stream
	 * in Mary binary format
	 * @param os the output stream
	 *
	 * @throws IOException if an error occurs.
	 */
	public boolean dumpBinary(DataOutputStream os, boolean dumpFeatures) throws IOException {
	    os.writeInt(type);
	    os.writeInt(phone);
	    os.writeInt(start);
	    os.writeInt(end);
	    os.writeInt(prev);
	    os.writeInt(next);
	    if (dumpFeatures){
	        if (values != null){
	            //calculate the size of values in bytes
	            int valSizeInBytes = 4;
	            for (int i = 0; i<values.size();i++){
	                valSizeInBytes += 2 + 2*((String)values.get(i)).length();
	            }
	            os.writeInt(valSizeInBytes);
	            os.writeInt(values.size());
	            for (int i = 0; i<values.size();i++){
	                String nextVal = (String)values.get(i);
	                os.writeShort((short)nextVal.length());
	                os.writeChars(nextVal);
	                //System.out.println(start+": Value: "+nextValue);
	            } 
	            return true;
	        } else {
	            os.writeInt(0);
	            return false;
	        }
	    }
	    return true;
	}
    
	public String getValueForFeature(int index)
    {
	    //if you already have the values, return the right one
        if (haveValues){
            if (index < values.size()){
                return (String) values.get(index);
            } else {
                return "0";
            }
        } else { // if you dont have any values
            if (bb != null && canReadInVals){ 
                // read in the values from byteBuffer
                bb.position((int) valsStart);
                int numberOfVals = bb.getInt();
                values = new ArrayList();
                for (int i=0;i<numberOfVals;i++){
                    int valsize = bb.getShort();
                    //System.out.print(valsize);
                    char[] charBufferFeat = new char[valsize];
                    for (int j = 0; j < valsize; j++) {
                        charBufferFeat[j] = bb.getChar();
                    }
                    values.add(new String(charBufferFeat, 0, valsize));
                    //System.out.println(" Value: "+value);
                }
                haveValues = true;
                return (String) values.get(index);
            } else {
                if (raf != null && canReadInVals){ 
                    // read in the values from RandomAccessFile
                    try{
                        raf.seek(valsStart);
                        int numberOfVals = raf.readInt();
                        values = new ArrayList();
                        for (int i=0;i<numberOfVals;i++){
                            int valsize = raf.readShort();
                            char[] charBufferVal = new char[valsize];
                            for (int k = 0; k < valsize; k++) {
                                charBufferVal[k] = raf.readChar();
                            }
                            values.add(new String(charBufferVal, 0, valsize));
                         }
                        haveValues = true;
                        return (String) values.get(index);
                    }catch (IOException e){
                        throw new Error ("IOException: Can not read values; "
                                +e.getMessage());
                    }
                } else {
                    return null;
                }
            }
        }
    }

	public boolean hasValues()
    {
	    if (haveValues || canReadInVals){
	        return true;
	    } else {
	        return false;
	    }
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
    
    public int getType(){
        return type;
    }
    
    public int durationInSamples()
    {
        return ((ClusterUnitDatabase)database).getAudioFrames().getUnitSize(start, end);
    }
    
    public String toString()
    {
        return name+" "+instanceNumber+" (frames "+start+" - "+end+")";
    }
}
