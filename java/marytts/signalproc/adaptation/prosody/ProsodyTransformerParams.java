/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.signalproc.adaptation.prosody;

import marytts.signalproc.adaptation.BaselineParams;

/**
 * @author Oytun T&uumlrk
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

