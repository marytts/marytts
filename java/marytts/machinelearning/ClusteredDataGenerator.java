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

import java.util.Arrays;

import marytts.util.math.MathUtils;


/**
 * 
 * Generates clustered data for testing machine learning algorithms
 * 
 * @author Oytun T&uumlrk
 */
public class ClusteredDataGenerator {
    public static final int DEFAULT_NUM_SAMPLES_IN_CLUSTERS = 50;
    public static final int DEFAULT_NUM_CLUSTERS = 10;
    public static final double DEFAULT_INIT_MEAN = 10.0;
    public static final double DEFAULT_VARIANCE = 1.0;
    public double[] data;
    
    public ClusteredDataGenerator()
    {
        double[] clusterMeans = new double[DEFAULT_NUM_CLUSTERS];
        for (int i=0; i<DEFAULT_NUM_CLUSTERS; i++)
            clusterMeans[i] = (i+1)*10.0;
        init(clusterMeans);
    }
    
    public ClusteredDataGenerator(int numClusters, int numSamplesInClusters)
    {
        this(numClusters, numSamplesInClusters, DEFAULT_INIT_MEAN);
    }
    
    public ClusteredDataGenerator(int numClusters, int numSamplesInClusters, double initMean)
    {
        this(numClusters, numSamplesInClusters, initMean, DEFAULT_VARIANCE);
    }
    
    public ClusteredDataGenerator(int numClusters, int numSamplesInClusters, double initMean, double variance)
    {
        double[] clusterMeans = new double[numClusters];
        for (int i=0; i<numClusters; i++)
            clusterMeans[i] = (i+1)*initMean;
        
        init(clusterMeans, variance, numSamplesInClusters);
    }
    
    public ClusteredDataGenerator(double[] clusterMeans)
    {
        this(clusterMeans, DEFAULT_VARIANCE);
    }
    
    public ClusteredDataGenerator(double[] clusterMeans, double variance)
    {
        init(clusterMeans, variance);
    }
    
    public ClusteredDataGenerator(double[] clusterMeans, double[] variances)
    {
        init(clusterMeans, variances, DEFAULT_NUM_SAMPLES_IN_CLUSTERS);
    }
    
    public ClusteredDataGenerator(double[] clusterMeans, double[] variances, int numSamplesPerCluster)
    {
        init(clusterMeans, variances, numSamplesPerCluster);
    }
    
    public void init(double[] clusterMeans)
    {
        init(clusterMeans, DEFAULT_VARIANCE);
    }
    
    public void init(double[] clusterMeans, double variance)
    {
        init(clusterMeans, variance, DEFAULT_NUM_SAMPLES_IN_CLUSTERS);
    }
    
    public void init(double[] clusterMeans, double variance, int numClusters)
    {
        double[] variances = new double[clusterMeans.length];
        Arrays.fill(variances, variance);
    
        init(clusterMeans, variances, numClusters);
    }
    
    public void init(double[] clusterMeans, double[] variances, int numSamplesPerCluster)
    {
        data = new double[numSamplesPerCluster*clusterMeans.length];
        for (int i=0; i<clusterMeans.length; i++)
        {
            double[] tmp = MathUtils.random(numSamplesPerCluster);
            MathUtils.adjustMean(tmp, clusterMeans[i]);
            MathUtils.adjustVariance(tmp, variances[i]);
            System.arraycopy(tmp, 0, data, i*numSamplesPerCluster, numSamplesPerCluster);
            System.out.println("Target mean=" + String.valueOf(clusterMeans[i]) + " Target variance=" + String.valueOf(variances[i]) + " - Mean=" + String.valueOf(MathUtils.mean(tmp)) + " Variance=" + String.valueOf(MathUtils.variance(tmp)));
        }
        
        double m = MathUtils.mean(data);
        double v = MathUtils.variance(data, m);
        System.out.println(String.valueOf(m) + " " + String.valueOf(v));
    }
    
    public static void main(String[] args)
    {
        ClusteredDataGenerator c = new ClusteredDataGenerator();
    }
}
