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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * Common utility to write a standard Mary header
 * 
 * @author sacha
 *
 */
public class MaryHeader 
{ 
    /* Global constants */
    private final static int MAGIC = 0x4d41525;
    private final static int VERSION = 1; /* TODO: this constant should be somehow automatically
                                            updated by the build process */
    
    /* List of authorized file type identifier constants */
    public final static int UNKNOWN = 0;
    public final static int CARTS = 1;
    public final static int UNITS = 2;
    public final static int TARGETFEATS = 3;
    public final static int JOINFEATS = 4;
    public final static int TIMELINE = 5;
    
    /* Private fields */
    private int magic = MAGIC;
    private int version = VERSION;
    private int type = UNKNOWN;
    public long byteSize = 12l; // 3 int with 4 bytes each.

    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * Plain constructor
     * 
     * @param newType The standard type of the Mary file, to be chosen among:
     * MaryHeader.CARTS, MaryHeader.UNITS, MaryHeader.TARGETFEATS, MaryHeader.JOINFEATS, MaryHeader.TIMELINE.
     * 
     * @throws IOException if the input type is unknown.
     */
    public MaryHeader( int newType ) throws IOException {
        if ( (newType > TIMELINE) || (newType < UNKNOWN) ) {
            throw new IOException( "Unauthorized Mary file type [" + type + "]." );
        }
        type = newType;    
    }
    
    /**
     * File constructor
     * 
     * @param raf a random access file to read the header from.
     * 
     * @throws IOException if the input type is unknown.
     */
    public MaryHeader( RandomAccessFile raf ) throws IOException {
        byteSize = this.read( raf );
        if ( !isMaryHeader() ) { throw new IOException( "Ill-formed Mary header!" ); }
    }
    
    /*****************/
    /* OTHER METHODS */
    /*****************/
    
    /** Static Mary header writer
     * 
     * @param raf The random access file to write to
     * 
     * @return the number of written bytes.
     * 
     * @throws IOException if the file type is unknown.
     * 
     * @author sacha
     */
    public long write( RandomAccessFile raf ) throws IOException {
        
        long nBytes = 0;
        
        if ( !this.hasLegalType() ) {
            throw new IOException( "Unknown Mary file type [" + type + "]." );
        }
        
        raf.writeInt( magic );   nBytes += 4;
        raf.writeInt( version ); nBytes += 4;
        raf.writeInt( type );    nBytes += 4;
        
        return( nBytes );
    }
    
    
    /** Static Mary header writer
     * 
     * @param raf The random access file to read from.
     * 
     * @return the number of read bytes.
     * 
     * @throws IOException (forwarded from the random access file read operations)
     * 
     * @author sacha
     */
    public long read( RandomAccessFile raf ) throws IOException {
        
        long nBytes = 0;
        
        magic = raf.readInt();   nBytes += 4;
        version = raf.readInt(); nBytes += 4;
        type = raf.readInt();    nBytes += 4;
        
        return( nBytes );
    }
    
    /* Accessors */
    public int getMagic() { return(magic); }
    public int getVersion() { return(version); }
    public int getType() { return(type); }

    /* Checkers */
    public boolean hasLegalMagic() { return( magic == MAGIC ); }
    public boolean hasCurrentVersion() { return( version == VERSION ); }
    public boolean hasLegalType() { return( (type > TIMELINE) || (type <= UNKNOWN) ); }
    public boolean isMaryHeader() { return( hasLegalMagic() && hasLegalType() ); }

}
