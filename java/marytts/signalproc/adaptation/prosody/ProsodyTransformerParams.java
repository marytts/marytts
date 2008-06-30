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

package marytts.signalproc.adaptation.prosody;

import marytts.signalproc.adaptation.BaselineParams;

/**
 * @author oytun.turk
 *
 */
public class ProsodyTransformerParams extends BaselineParams {
    
    ////PITCH
    public int pitchTransformationMethod;
    public static final int USE_ONLY_PSCALES = -1;
    public static final int NO_TRANSFORMATION = 0;    
    
    //Global transformations
    public static final int GLOBAL_MEAN = 1;
    public static final int GLOBAL_STDDEV = 2;
    public static final int GLOBAL_RANGE = 3;
    public static final int GLOBAL_SLOPE = 4;
    public static final int GLOBAL_INTERCEPT = 5;
    public static final int GLOBAL_MEAN_STDDEV = 6;
    public static final int GLOBAL_MEAN_SLOPE = 7;
    public static final int GLOBAL_INTERCEPT_STDDEV = 8;
    public static final int GLOBAL_INTERCEPT_SLOPE = 9;
    //
    
    //Local transformations (i.e. sentence level, based on median of N-best sentence matches in the training database)
    public static final int SENTENCE_MEAN = 21;
    public static final int SENTENCE_STDDEV = 22;
    public static final int SENTENCE_RANGE = 23;
    public static final int SENTENCE_SLOPE = 24;
    public static final int SENTENCE_INTERCEPT = 25;
    public static final int SENTENCE_MEAN_STDDEV = 26;
    public static final int SENTENCE_MEAN_SLOPE = 27;
    public static final int SENTENCE_INTERCEPT_STDDEV = 28;
    public static final int SENTENCE_INTERCEPT_SLOPE = 29;
    //
    
    //These are for GLOBAL_XXX cases of pitchTransformationMethod only
    public boolean isUseInputMean; //For GLOBAL tfms: Estimate mean from input f0s? Otherwise from codebook
    public boolean isUseInputStdDev; //For GLOBAL tfms: Estimate std. dev. from input f0s? Otherwise from codebook
    public boolean isUseInputRange; //For GLOBAL tfms: Estimate range from input f0s? Otherwise from codebook
    public boolean isUseInputIntercept; //For GLOBAL tfms: Estimate intercept from input f0s? Otherwise from codebook
    public boolean isUseInputSlope; //For GLOBAL tfms: Estimate slope from input f0s? Otherwise from codebook
    ////

    public int pitchStatisticsType;
    
    public ProsodyTransformerParams()
    {
        pitchTransformationMethod = NO_TRANSFORMATION;
  
        isUseInputMean = false;
        isUseInputStdDev = false;
        isUseInputRange = false;
        isUseInputIntercept = false;
        isUseInputSlope = false;
        
        pitchStatisticsType = PitchStatistics.DEFAULT_STATISTICS;
    }
    
    public ProsodyTransformerParams(ProsodyTransformerParams existing)
    {
        pitchTransformationMethod = existing.pitchTransformationMethod;
  
        isUseInputMean = existing.isUseInputMean;
        isUseInputStdDev = existing.isUseInputStdDev;
        isUseInputRange = existing.isUseInputRange;
        isUseInputIntercept = existing.isUseInputIntercept;
        isUseInputSlope = existing.isUseInputSlope;
        
        pitchStatisticsType = existing.pitchStatisticsType;
    }
}
