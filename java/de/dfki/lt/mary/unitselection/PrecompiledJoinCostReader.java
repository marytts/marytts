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
package de.dfki.lt.mary.unitselection;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.dfki.lt.mary.unitselection.voiceimport.MaryHeader;

/**
 * Loads a precompiled join cost file and provides access to the join cost.
 * 
 */
public class PrecompiledJoinCostReader implements JoinCostFunction
{

    private MaryHeader hdr = null;
    
    // keys = Integers representing left unit index;
    // values = maps containing
    //          - keys = Integers representing right unit index;
    //          - values = Floats representing the jost of joining them.
    protected Map left;   

    /**
     * Empty constructor; need to call load() separately.
     * @see #load(String)
     */
    public PrecompiledJoinCostReader()
    {
    }
    
    /**
     * Create a precompiled join cost file reader from the given file
     * @param fileName the file to read
     * @throws IOException if a problem occurs while reading
     */
    public PrecompiledJoinCostReader( String fileName ) throws IOException 
    {
        load(fileName, null, null, 0);
    }
    
    /**
     * Load the given precompiled join cost file
     * @param fileName the file to read
     * @param dummy not used, just used to fulfil the join cost function interface
     * @param dummy2 not used, just used to fulfil the join cost function interface
     * @throws IOException if a problem occurs while reading
     */
    public void load(String fileName, String dummy, String dummy2, float dummy3) throws IOException
    {
        /* Open the file */
        DataInputStream dis = null;
        try {
            dis = new DataInputStream( new BufferedInputStream( new FileInputStream( fileName ) ) );
        }
        catch ( FileNotFoundException e ) {
            throw new RuntimeException( "File [" + fileName + "] was not found." );
        }
        try {
            /* Load the Mary header */
            hdr = new MaryHeader( dis );
            if ( !hdr.isMaryHeader() ) {
                throw new IOException( "File [" + fileName + "] is not a valid Mary format file." );
            }
            if ( hdr.getType() != MaryHeader.PRECOMPUTED_JOINCOSTS ) {
                throw new RuntimeException( "File [" + fileName + "] is not a valid Mary precompiled join costs file." );
            }
            /* Read the number of units */
            int numberOfLeftUnits = dis.readInt();
            if ( numberOfLeftUnits < 0 ) {
                throw new RuntimeException( "File [" + fileName + "] has a negative number of units. Aborting." );
            }
            
            left = new HashMap();
            /* Read the start times and durations */
            for ( int i = 0; i < numberOfLeftUnits; i++ ) {
                int leftIndex = dis.readInt();
                int numberOfRightUnits = dis.readInt();
                Map right = new HashMap();
                left.put(new Integer(leftIndex), right);
                for (int j=0; j<numberOfRightUnits; j++) {
                    int rightIndex = dis.readInt();
                    float cost = dis.readFloat();
                    right.put(new Integer(rightIndex), new Float(cost));
                }
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Reading the Mary header from file [" + fileName + "] failed.", e );
        }
    }
    
    /**
     * Return the (precomputed) cost of joining the two given units;
     * if there is no precomputed cost, return Double.POSITIVE_INFINITY.
     */
    public double cost(Unit uleft, Unit uright)
    {
        Integer leftIndex = new Integer(uleft.getIndex());
        Map rightUnitsMap = (Map)left.get(leftIndex);
        if (rightUnitsMap == null) return Double.POSITIVE_INFINITY;
        Integer rightIndex = new Integer(uright.getIndex());
        Float cost = (Float) rightUnitsMap.get(rightIndex);
        if (cost == null) return Double.POSITIVE_INFINITY;
        return cost.doubleValue();
    }
    
}
