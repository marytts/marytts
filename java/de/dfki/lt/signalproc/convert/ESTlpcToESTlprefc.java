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
package de.dfki.lt.signalproc.convert;

import de.dfki.lt.signalproc.analysis.ReflectionCoefficients;
import de.dfki.lt.mary.unitselection.voiceimport.ESTTrackReader;
import de.dfki.lt.mary.unitselection.voiceimport.ESTTrackWriter;
import java.io.IOException;

public class ESTlpcToESTlprefc {

    /**
     * Internally does the conversion between LPCs and reflection coefficients.
     *
     */
    private static float[][] convertData( float[][] lpc ) {
        
        int nLPC = lpc[0].length;
        int nK = nLPC - 1;
        double[] a = new double[nLPC];
        a[0] = 1.0; // Set a[0] to 1.0 once and for all
        double[] k = new double[nK];
        float[][] lprefc = new float[lpc.length][nK];
        
        // For each LPC vector:
        for ( int i = 0; i < lpc.length; i++ ) {
            // Cast the LPC coeffs from float to double
            // Note: a[0] has been permanently set to one in the above.
            for ( int j = 1; j < nLPC; j++ ) {
                a[j] = (double)( lpc[i][j] );
            }
            // Do the conversion
            k = ReflectionCoefficients.lpc2lprefc( a );
            // Cast the reflection coefficients back to floats
            for ( int j = 0; j < nK; j++ ) {
                lprefc[i][j] = (float)( k[j] );
            }
        }
        return( lprefc );
    }
    
    /**
     * A method to convert between two files, from LPCs to reflection coeffs in EST format.
     * 
     * @param inFileName The name of the input file.
     * @param outFileName The name of the output file.
     * 
     * @throws IOException
     */
    public static void convert( String inFileName, String outFileName ) throws IOException {
        // Load the input file
        ESTTrackReader etr = new ESTTrackReader( inFileName );
        // Convert
        float[][] lprefc = convertData( etr.getFrames() );
        // Output the lpcc
        ESTTrackWriter etw = new ESTTrackWriter( etr.getTimes(), lprefc, "lprefc" );
        etw.doWriteAndClose( outFileName, etr.isBinary(), etr.isBigEndian() );
        
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        // Usage: ESTlpcToESTlprefc inFileName outFileName
        convert( args[0], args[1] );
    }

}
