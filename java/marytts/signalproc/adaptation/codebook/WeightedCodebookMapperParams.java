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

package marytts.signalproc.adaptation.codebook;

/**
 * 
 * @author oytun.turk
 * 
 * Wrapper for parameters of weighted codebook mapping procedure
 *
 */
public class WeightedCodebookMapperParams {
    public int numBestMatches; // Number of best matches in codebook
    
    public int distanceMeasure; // Distance measure for comparing source training and transformation features
    public static int LSF_INVERSE_HARMONIC_DISTANCE = 1;
    public static int LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC = 2;
    public static int LSF_EUCLIDEAN_DISTANCE = 3;
    public static int LSF_MAHALANOBIS_DISTANCE = 4;
    public static int LSF_ABSOLUTE_VALUE_DISTANCE = 5;
    public static int DEFAULT_DISTANCE_MEASURE = LSF_INVERSE_HARMONIC_DISTANCE;
    
    public double alphaForSymmetric; //Weighting factor for using weights of two lsf vectors in distance computation relatively.
                                     //The range is [0.0,1.0]
    public static double MIN_ALPHA_FOR_SYMMETRIC = 0.0;
    public static double MAX_ALPHA_FOR_SYMMETRIC = 1.0f;
    
    public int weightingMethod; // Method for weighting best codebook matches
    public static int EXPONENTIAL_HALF_WINDOW = 1;
    public static int TRIANGLE_HALF_WINDOW = 2;
    public static int DEFAULT_WEIGHTING_METHOD = EXPONENTIAL_HALF_WINDOW;
    
    public double weightingSteepness; // Steepness of weighting function in range [MIN_STEEPNESS, MAX_STEEPNESS]
    public static double MIN_STEEPNESS = 0.0; //All weights are equal
    public static double MAX_STEEPNESS = 10.0; //Best codebook weight is very large as compared to remaining
    public static double DEFAULT_WEIGHTING_STEEPNESS = 1.0;
    
    ////Mean and variance of a specific distance measure can be optionally kept in the follwoing
    // two parameters for z-normalization
    public double distanceMean; 
    public double distanceVariance;
    public static final double DEFAULT_DISTANCE_MEAN = 0.0;
    public static final double DEFAULT_DISTANCE_VARIANCE = 1.0;
    
    public double freqRange; //Frequency range to be considered around center freq when matching LSFs (note that center freq is estimated automatically as the middle of most closest LSFs)
    public static final double DEFAULT_FREQ_RANGE_FOR_LSF_MATCH = 5000.0;
    
    public int lpOrder;
    
    public WeightedCodebookMapperParams()
    {
        distanceMeasure = DEFAULT_DISTANCE_MEASURE;
        weightingMethod = DEFAULT_WEIGHTING_METHOD;
        weightingSteepness = DEFAULT_WEIGHTING_STEEPNESS;
        distanceMean = DEFAULT_DISTANCE_MEAN;
        distanceVariance = DEFAULT_DISTANCE_VARIANCE;
        freqRange = DEFAULT_FREQ_RANGE_FOR_LSF_MATCH;
    }
    
    public WeightedCodebookMapperParams(WeightedCodebookMapperParams w)
    {
        numBestMatches = w.numBestMatches;
        distanceMeasure = w.distanceMeasure;
        alphaForSymmetric = w.alphaForSymmetric;
        weightingMethod = w.weightingMethod;
        weightingSteepness = w.weightingSteepness;
        distanceMean = w.distanceMean; 
        distanceVariance = w.distanceVariance;
        lpOrder = w.lpOrder;
        freqRange = w.freqRange;
    }

}
