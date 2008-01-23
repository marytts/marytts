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

package de.dfki.lt.mary.unitselection.adaptation;

/**
 * @author oytun.turk
 *
 * A general purpose class for computing various distance measures
 * Examples include Euclidean, Mahalanobis, distance to GMMs etc.
 * 
 **/

public class DistanceComputer {
    
    //Computes absolute value distance
    // dist = sum(|x(i)-y(i)|)
    public static double getAbsoluteValueDistance(double[] x, double[] y)
    {
        assert x.length==y.length;
        
        double dist = 0.0;
        for (int i=0; i<x.length; i++)
           dist += Math.abs(x[i]-y[i]);
        
        return dist;
    }
    
    //Computes the Euclidean distance metric
    // dist = sqrt( sum( (x[i]-y[i])^2 ) )
    public static double getEuclideanDistance(double[] x, double[] y)
    {
        assert x.length==y.length;
        
        double dist = 0.0;
        for (int i=0; i<x.length; i++)
           dist += (x[i]-y[i])*(x[i]-y[i]);

        dist = Math.sqrt(dist);
        
        return dist;
    }
    
    //Computes the normalized Euclidean distance metric
    // dist = sqrt( sum( (x[i]-y[i])^2 / var[i] ) )
    public static double getNormalizedEuclideanDistance(double[] x, double[] y, double[] variances)
    {
        assert x.length==y.length;
        assert x.length==variances.length;
        
        double dist = 0.0;
        for (int i=0; i<x.length; i++)
           dist += (x[i]-y[i])*(x[i]-y[i])/variances[i];

        dist = Math.sqrt(dist);
        
        return dist;
    }
    
    //Note that this requires an inverse covariance matrix
    //If you have a diagonal matrix only, then the Mahalanobis distance reduces to
    // Normalized Eucledian distance
    // dist = sqrt( sum( (x-meanX)' * (covarianceMatrix^-1) * (x-meanX) ) )
    public static double getMahalanobisDistance(double[] x, double[] mean, double[][] inverseCovarianceMatrix)
    {
        assert x.length==mean.length;
        assert x.length==inverseCovarianceMatrix.length;
        
        int i, j;
        double dist = 0.0;
        double tmpDist;
        
        for (i=0; i<x.length; i++)
        {
            tmpDist = 0.0;
            for (j=0; j<x.length; j++)
                tmpDist += (x[j]-mean[j])*inverseCovarianceMatrix[j][i];
            
            dist += tmpDist*(x[i]-mean[i]);
        }
        
        dist = Math.sqrt(dist); 
        
        return dist;
    }

}
