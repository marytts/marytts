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

package de.dfki.lt.machinelearning;

/**
 * @author oytun.turk
 *
 */
public class GMMTrainerParams {
    public static final int TOTAL_COMPONENTS_DEFAULT = 1;
    public static final boolean IS_DIAGONAL_COVARIANCE_DEFAULT = true;
    public static final int MINIMUM_ITERATIONS_DEFAULT = 20;
    public static final int MAXIMUM_ITERATIONS_DEFAULT = 200;
    public static final boolean IS_UPDATE_COVARIANCES_DEFAULT = true;
    public static final double TINY_LOGLIKELIHOOD_CHANGE_DEFAULT = 0.01;
    public static final double MINIMUM_COVARIANCE_ALLOWED_DEFAULT = 1e-4;
    
    public int totalComponents;
    public boolean isDiagonalCovariance; 
    public int minimumIterations;
    public int maximumIterations;
    public boolean isUpdateCovariances;
    public double tinyLogLikelihoodChange;
    public double minimumCovarianceAllowed;
    
    public GMMTrainerParams()
    {
        totalComponents = TOTAL_COMPONENTS_DEFAULT;
        isDiagonalCovariance = IS_DIAGONAL_COVARIANCE_DEFAULT; 
        minimumIterations = MINIMUM_ITERATIONS_DEFAULT;
        maximumIterations = MAXIMUM_ITERATIONS_DEFAULT;
        isUpdateCovariances = IS_UPDATE_COVARIANCES_DEFAULT;
        tinyLogLikelihoodChange = TINY_LOGLIKELIHOOD_CHANGE_DEFAULT;
        minimumCovarianceAllowed = MINIMUM_COVARIANCE_ALLOWED_DEFAULT;
    }
    
    public GMMTrainerParams(GMMTrainerParams existing)
    {
        totalComponents = existing.totalComponents;
        isDiagonalCovariance = existing.isDiagonalCovariance; 
        minimumIterations = existing.minimumIterations;
        maximumIterations = existing.maximumIterations;
        isUpdateCovariances = existing.isUpdateCovariances;
        tinyLogLikelihoodChange = existing.tinyLogLikelihoodChange;
        minimumCovarianceAllowed = existing.minimumCovarianceAllowed;
    }

}
