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
package de.dfki.lt.mary.gizmos;

import de.dfki.lt.mary.unitselection.voiceimport.ESTTrackReader;

import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;

public class ExportLPCSpectrogram {

    /**
     * Computes a LPC spectrum from a LPC vector over a set of normalized frequencies.
     * 
     * @param a The array of LPC coefficients, where a[0] == 1.
     * @param omega The array of normalized frequencies, with values between 0.0d and PI.
     * @return An array of spectrum magnitude values, in the log domain, one value
     * for each normalized frequency.
     */
    private static double[] lpcToLogMagSpectrum(double[] a, double[] omega) {
        
        double[] spec = new double[omega.length];
        
        double zRe = 0.0d;
        double zIm = 0.0d;
        double specRe = 0.0d;
        double specIm = 0.0d;
        
        double mag = 0.0d;
        
        int order = a.length - 1;
        
        /* for ( int i = 1; i < a.length; i++ ) {
            a[i] = a[i] / a[0];
        } */
        a[0] = 1.0d; // In the EST format, a[0] is the gain term.
        
        /* For each frequency */
        for ( int i = 0; i < omega.length; i++ ) {

            /* Make exponential of normalized frequency */
            zRe = Math.cos( omega[i] );
            zIm = - Math.sin( omega[i] );

            /* Evaluate polynom recursively */
            specRe = a[order];
            specIm = 0.0d; //was cmplxspec
            for ( int j=1; j<= order; j++ ) {
                /* cmplxSpec = a[order-j] + cmplxSpec * z; */
                specRe = a[order-j] + specRe * zRe - specIm * zIm;
                specIm = /* 0.0  + */ specRe * zIm + specIm * zRe;
            }

            /* Invert result */
            mag = specRe*specRe + specIm*specIm;
            specRe =   specRe / mag;
            specIm = - specIm / mag;

            /* return log(mag(spectrum)) */
            spec[i] = Math.log( Math.sqrt( specRe*specRe + specIm*specIm ) );
            //spec[i] = Math.sqrt( specRe*specRe + specIm*specIm );
        }
        
        return( spec );
    }
    
    /**
     * Writes out a spectrogram matrix from an EST LPC file.
     * 
     * @param lpcFile The name of the EST LPC file.
     * @param spectroFile The name of the output spetrogram file.
     */
    private static void lpcToSpectrogram( String lpcFileName, String spectroFileName ) throws FileNotFoundException, IOException {
        
        /* Make a vector of normalized frequencies */
        final int NUMSPEC = 1024;
        double[] omega = new double[NUMSPEC];
        double step = Math.PI / (NUMSPEC-1);
        omega[0] = 0.0d;
        for ( int i = 1; i < (NUMSPEC-1); i++ ) {
            omega[i] = (double)(i) * step;
        }
        omega[NUMSPEC-1] = Math.PI;

        /* Open the input and output files */
        ESTTrackReader lpcFile = new ESTTrackReader( lpcFileName );
        DataOutputStream dos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( spectroFileName ) ) );
        
        /* Allocate the LPC frame vector and the spectrogram matrix */
        int order = lpcFile.getNumChannels();
        double[] lpcFrame = new double[order];
        int numFrames = lpcFile.getNumFrames();
        double[][] spectrogram = new double[numFrames][NUMSPEC];
        
        /* Write out the matrix dimensions */
        dos.writeInt( numFrames );
        dos.writeInt( NUMSPEC );
        
        /* Compute and write the spectrogram matrix */
        for ( int i = 0; i < numFrames; i++ ) {
            /* Cast the LPC values from float to double */
            for ( int k = 0; k < order; k++ ) {
                lpcFrame[k] = (double)( lpcFile.getFrameEntry( i, k ) );
            }
            /* Compute the log mag spectrum */
            spectrogram[i] = lpcToLogMagSpectrum( lpcFrame, omega );
            /* Write it out */
            for ( int k = 0; k < NUMSPEC; k++ ) {
                dos.writeDouble( spectrogram[i][k] );
            }
        }
        
        /* Clean the house */
        dos.close();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
        /* Hack the file names */
        final String file1 = args[0];
        final String file2 = args[1];
        
        String bname1 = new File( file1 ).getName();
        String bname2 = new File( file2 ).getName();

        System.out.println( "File [" + file1 + "] => spectro [spectro_" + bname1 + "]" );
        lpcToSpectrogram( file1, "spectro_" + bname1 );
        System.out.println( "File [" + file2 + "] => spectro [spectro_" + bname2 + "]" );
        lpcToSpectrogram( file2, "spectro_" + bname2 );
    }

}
