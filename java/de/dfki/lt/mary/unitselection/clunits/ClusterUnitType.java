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
import java.nio.ByteBuffer;

import de.dfki.lt.mary.unitselection.Unit;

/**
 * Represents a unit type in the system
 */
public class ClusterUnitType {
    private ClusterUnitDatabase database;
    private String name;
    private int start;
    private int count;

    /**
     * Constructs a UnitType from the given parameters
     *
     * @param name the name of the type
     * @param start the starting index for this type
     * @param count the number of elements for this type
     */
    ClusterUnitType(ClusterUnitDatabase database, String name, int start, int count) {
	this.name = name;
	this.start = start;
	this.count = count;
    }

    /**
     * Creates a unit type by reading it from the given byte buffer.
     *
     * @param bb source of the UnitType  data
     *
     * @throws IOException if an IO error occurs
     */
    ClusterUnitType(ByteBuffer bb, ClusterUnitDatabase database) throws IOException {
        this.database = database;
        int size = bb.getShort();
    	char[] charBuffer = new char[size];
    	for (int i = 0; i < size; i++) {
    	    charBuffer[i] = bb.getChar();
    	}
    	this.name = new String(charBuffer, 0, size);
        this.start = bb.getInt();
        this.count = bb.getInt();
    }
    
    /**
     * Creates a unit type by reading it from the given Random Access File
     *
     * @param raf source of the UnitType  data
     *
     * @throws IOException if an IO error occurs
     */
    ClusterUnitType(RandomAccessFile raf, ClusterUnitDatabase database) throws IOException {
    	this.database = database;
        int size = raf.readShort();
    	char[] charBuffer = new char[size];
    	for (int i = 0; i < size; i++) {
    	    charBuffer[i] = raf.readChar();
    	}
    	this.name = new String(charBuffer, 0, size);
    	this.start = raf.readInt();
    	this.count = raf.readInt();
    }

    /**
     * Constructs a UnitType from the given parameters
     * This constructor is only used for the conversion from FreeTTS
     * text format to Mary binary format
     */
    ClusterUnitType(String name, int start, int count) {
	this.name = name;
	this.start = start;
	this.count = count;
    }
    
    /**
     * Gets the name for this unit type
     * 
     * @return the name for the type
     */
    public String getName() {
        return name;
    }

    /**
     * Get a unit of this type by its instance number
     * @param instance the instance number
     * @return the requested unit
     * @throws IllegalArgumentException if the instance number is out of range 
     */
    public Unit getInstance(int instance)
    {
        if (instance<0||instance>=count) throw new IllegalArgumentException("Unit type "+name+" has "+count+" instances, cannot get instance "+instance);
        return database.getUnit(start+instance);
    }
    
    /**
     * Gets the start index for this type
     *
     * @return the start index
     */
    public int getStart() {
	return start;
    }
    
    /**
     * Gets the count for this type
     *
     * @return the  count for this type
     */
    public int getCount() {
	return count;
    }

    /**
     * Dumps this unit to the given output stream.
     *
     * @param os the output stream
     *
     * @throws IOException if an error occurs.
     */
    public void dumpBinary(DataOutputStream os) throws IOException {
        os.writeShort((short) name.length());
    	for (int i = 0; i < name.length(); i++) {
    	    os.writeChar(name.charAt(i));
    	}
    	os.writeInt(start);
    	os.writeInt(count);
    }
}
