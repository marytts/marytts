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

public class ESTlprefcToESTlpc {

    /**
     * Internally does the conversion between reflection coefficients and LPCs.
     *
     */
    private static float[][] convertData( float[][] lprefc ) {
        
        int lpcOrder = lprefc[0].length;
        double[] k = new double[lpcOrder];
        double[] a = new double[lpcOrder+1];
        float[][] lpc = new float[lprefc.length][lpcOrder+1];
        
        // For each LPC vector:
        for ( int i = 0; i < lprefc.length; i++ ) {
            // Cast the LPCC coeffs from float to double
            for ( int j = 0; j < lpcOrder; j++ ) {
                k[j] = (double)( lprefc[i][j] );
            }
            // Do the conversion
            a = ReflectionCoefficients.lprefc2lpc( k );
            // Default a[0] to 1.0
            lpc[i][0] = 1.0f;
            // Cast the LPCs back to floats
            for ( int j = 1; j <= lpcOrder; j++ ) {
                lpc[i][j] = (float)( a[j] );
            }
        }
        return( lpc );
    }
    
    /**
     * A method to convert between two files, from LPCs to reflection coefficients in EST format.
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
        float[][] lpc = convertData( etr.getFrames() );
        // Output the lpcc
        ESTTrackWriter etw = new ESTTrackWriter( etr.getTimes(), lpc, "lpc" );
        etw.doWriteAndClose( outFileName, etr.isBinary(), etr.isBigEndian() );
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        // Usage: ESTlpccToESTlpc inFileName outFileName
        convert( args[0], args[1] );
    }

}
