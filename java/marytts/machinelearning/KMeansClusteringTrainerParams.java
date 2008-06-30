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

package marytts.machinelearning;

/**
 * @author oytun.turk
 *
 */
public class KMeansClusteringTrainerParams {
    
    //A set of default values for K-Means  training parameters
    public static final int KMEANS_MAX_ITERATIONS_DEFAULT = 200;
    public static final double KMEANS_MIN_CLUSTER_CHANGE_PERCENT_DEFAULT = 0.0001;
    public static final boolean KMEANS_IS_DIAGONAL_COVARIANCE_DEFAULT = true;
    public static final int KMEANS_MIN_SAMPLES_IN_ONE_CLUSTER_DEFAULT = 10;
    private static final double KMEANS_MIN_COVARIANCE_ALLOWED_DEFAULT = 1e-5;
    //
    
    public int numClusters;  //Number of clusters to be trained
    public int maxIterations; //Maximum iterations to stop K-means training
    public double minClusterChangePercent; //Minimum percent change in cluster assignments to stop K-Means iterations
    public boolean isDiagonalOutputCovariance; //Estimate diagonal cluster covariances finally?
    public int minSamplesInOneCluster; //Minimum number of observations allowed in one cluster
    public double minCovarianceAllowed; //Minimum covariance value allowed for final cluster covariance matrices
    public double[] globalVariances; //Global variance vector of whole data
    
    //Default constructor
    public KMeansClusteringTrainerParams()
    {
        numClusters = 0;
        maxIterations = KMEANS_MAX_ITERATIONS_DEFAULT;
        minClusterChangePercent = KMEANS_MIN_CLUSTER_CHANGE_PERCENT_DEFAULT;
        isDiagonalOutputCovariance = KMEANS_IS_DIAGONAL_COVARIANCE_DEFAULT;
        minSamplesInOneCluster = KMEANS_MIN_SAMPLES_IN_ONE_CLUSTER_DEFAULT;
        minCovarianceAllowed = KMEANS_MIN_COVARIANCE_ALLOWED_DEFAULT;
        globalVariances = null;
    }
    
    //Constructor using GMM training parameters
    public KMeansClusteringTrainerParams(GMMTrainerParams gmmParams)
    {
        numClusters = gmmParams.totalComponents;
        maxIterations = gmmParams.kmeansMaxIterations;
        minClusterChangePercent = gmmParams.kmeansMinClusterChangePercent;
        isDiagonalOutputCovariance = gmmParams.isDiagonalCovariance;
        minSamplesInOneCluster = gmmParams.kmeansMinSamplesInOneCluster;
        minCovarianceAllowed = gmmParams.minCovarianceAllowed;
        globalVariances = null;
    }
    
    //Constructor using an existing parameter set
    public KMeansClusteringTrainerParams(KMeansClusteringTrainerParams existing)
    {
        numClusters = existing.numClusters;
        maxIterations = existing.maxIterations;
        minClusterChangePercent = existing.minClusterChangePercent;
        isDiagonalOutputCovariance = existing.isDiagonalOutputCovariance;
        minSamplesInOneCluster = existing.minSamplesInOneCluster;

        setGlobalVariances(existing.globalVariances);
    }
    
    //Set global variance values
    public void setGlobalVariances(double[] globalVariancesIn)
    {
        if (globalVariancesIn!=null)
        {
            if (globalVariances==null || globalVariancesIn.length!=globalVariances.length)
                globalVariances = new double[globalVariancesIn.length];
            
            System.arraycopy(globalVariancesIn, 0, globalVariances, 0, globalVariancesIn.length);
        }
    }
}
