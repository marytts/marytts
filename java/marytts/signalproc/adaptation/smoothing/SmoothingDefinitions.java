/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.signalproc.adaptation.smoothing;

/**
 * @author Oytun T&uumlrk
 *
 */
public class SmoothingDefinitions {
    //Smoothing status
    public static final int NONE= 0;
    public static final int ESTIMATING_SMOOTHED_VOCAL_TRACT = 1; //No output generation except a smoothed vocal tract spectrum
    public static final int TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT = 2; //No mapping/transformation but use pre-computed values
    //
    
    //Smoothing methods: These are active when ESTIMATING_SMOOTHED_VOCAL_TRACT
    public static final int NO_SMOOTHING = 0; //Do nothing about smoothing
    public static final int TRANSFORMATION_FILTER_SMOOTHING = 1; //Store target/source filters to smooth them
    public static final int OUTPUT_LSFCONTOUR_SMOOTHING = 2; //Store target(output) lsf contours to smooth them
    public static final int OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING = 3; //Store target(output) vocal tract spectra to smooth them
    //
    
    public static final int DEFAULT_NUM_NEIGHBOURS = 5;
}
