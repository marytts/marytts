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

import java.util.Arrays;

import de.dfki.lt.signalproc.util.DistanceComputer;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class KMeansClusteringTrainer {
    
    private static final double TINY = 1.0e-3;
    private static final double MIN_CHANGE_PERCENT = 0.1; //minimum change in cluster assignments in percent of whole data size
    private static final double TINY_CLUSTER_PERCENT = 1e-3;
    private static final double MINIMUM_VARIANCE = 1e-5;
    
    public static final int MAXIMUM_ITERATIONS_DEFAULT = 200;
    public static final double MIN_CLUSTER_PERCENT_DEFAULT = 0.1;
    public static final boolean IS_DIAGONAL_COVARIANCE_DEFAULT = true;
    
    public Cluster[] clusters;
    public int[] totalObservationsInClusters;
    public int[] clusterIndices;
    
    public void cluster(double[][] x, int numClusters)
    {   
        cluster(x, numClusters, MAXIMUM_ITERATIONS_DEFAULT);
    }
    
    public void cluster(double[][] x, int numClusters, int maximumIterations)
    {   
        cluster(x, numClusters, maximumIterations, MIN_CLUSTER_PERCENT_DEFAULT);
    }
    
    public void cluster(double[][] x, int numClusters, int maximumIterations, double minClusterPercent)
    {   
        cluster(x, numClusters, maximumIterations, minClusterPercent, IS_DIAGONAL_COVARIANCE_DEFAULT);
    }
    
    public void cluster(double[][] x, int numClusters, int maximumIterations, double minClusterPercent, boolean isDiagonalOutputCovariance)
    {
        double[] globalVariances = new double[x[0].length];
        Arrays.fill(globalVariances, 1.0);
        
        cluster(x, numClusters, maximumIterations, minClusterPercent, isDiagonalOutputCovariance, globalVariances);
    }
    
    //K-Means clustering algorithm
    // numClusters: Desired number of clusters
    // minClusterPercent: Minimum cluster size in percent of whole data size
    public void cluster(double[][] x, int numClusters, int maximumIterations, double minClusterPercent, boolean isDiagonalOutputCovariance, double[] globalVariances)
    {   
        int observations = x.length;
        int dimension = x[0].length;
        
        int c, k, k2, d, t, iter, i, totChanged;
        int ind = -1;
        boolean bCont;
        double rnd, tmpDist;
        double minDist = Double.MIN_VALUE;
        
        double[][] m_new = new double[numClusters][];
        for (k=0; k<numClusters; k++)
            m_new[k] = new double[dimension];
        
        int[][] b = new int[observations][];
        for (t=0; t<observations; t++)
            b[t] = new int[numClusters];
        
        int[][] b_old = new int[observations][];
        for (t=0; t<observations; t++)
            b_old[t] = new int[numClusters];
        
        int[] prev_totals = new int[numClusters];
        double changedPerc;

        double[] mAll = new double[dimension];
        
        clusters = new Cluster[numClusters];
        for (k=0; k<numClusters; k++)
            clusters[k] = new Cluster(dimension, isDiagonalOutputCovariance);
        
        for (k=1; k<=numClusters; k++)
        {
            for (d=1; d<=dimension; d++)
                clusters[k-1].meanVector[d-1] = 0.0;
            
            for (t=1; t<=observations; t++)
                b[t-1][k-1] = 0;
        }
        
        //Select initial cluster centers
        mAll = MathUtils.mean(x, true);
        
        k = 1;
        double[] dists = new double[observations];
        double[] tmp = new double[numClusters+1];
        double maxD = Double.MAX_VALUE;
        int maxInd = -1;
        
        while(k<=numClusters)
        {
            for (t=1; t<=observations; t++)
            {
                if (k>1)
                {
                    for (i=1; i<=k-1; i++)
                        tmp[i-1] = DistanceComputer.getNormalizedEuclideanDistance(clusters[i-1].meanVector, x[t-1], globalVariances);
                    
                    tmp[k-1] = DistanceComputer.getNormalizedEuclideanDistance(mAll, x[t-1], globalVariances);
                    dists[t-1] = MathUtils.mean(tmp, 0, k-1);
                }
                else
                    dists[t-1] = DistanceComputer.getNormalizedEuclideanDistance(mAll, x[t-1], globalVariances);
            }

            for (t=1; t<=observations; t++)
            {
                if (t==1 || dists[t-1]>maxD)
                {
                    maxD = dists[t-1];
                    maxInd = t;
                }
            }
                
            for (d=0; d<dimension; d++)
                clusters[k-1].meanVector[d] = x[maxInd-1][d];
        
            System.out.println("Cluster center " + String.valueOf(k) + " initialized...");
            k++;
        }
        //

        int[] tinyClusterInds = new int[numClusters];
        int numTinyClusters = 0;
        double[] tmps = new double[numClusters];
        int[] inds = new int[numClusters];
        totalObservationsInClusters = new int[numClusters];
        clusterIndices = new int[observations];

        iter = 0;
        bCont = true;
        while(bCont)
        {
            for (t=1; t<=observations; t++) //Overall observations
            {
                for (i=1; i<=numClusters; i++) //Overall classes
                {
                    tmpDist = DistanceComputer.getNormalizedEuclideanDistance(clusters[i-1].meanVector, x[t-1], globalVariances);
                    b[t-1][i-1] = 0;
                    if (i==1 || tmpDist<minDist)
                    {
                        minDist = tmpDist;
                        ind = i;  
                    }
                }
                for (i=1; i<=numClusters; i++) //Overall classes
                {
                    if (i==ind)
                        b[t-1][i-1] = 1;  
                }
            }
            
            //Update means
            for (i=1; i<=numClusters; i++)
            {
                totalObservationsInClusters[i-1] = 0;
                tinyClusterInds[i-1] = 0;
            }
            
            c=1;
            for (i=1; i<=numClusters; i++)
            {
                for (d=1; d<=dimension; d++)
                    m_new[i-1][d-1]=0.0f;
                
                for (t=1; t<=observations; t++)
                {
                    if (b[t-1][i-1]==1)
                    {
                        for (d=1; d<=dimension; d++)
                            m_new[i-1][d-1] = m_new[i-1][d-1] + x[t-1][d-1];
                        
                        clusterIndices[t-1] = i-1; // zero-based
                        (totalObservationsInClusters[i-1])++;
                    }
                }
                
                //Do something if totalObservationsInClusters[i-1] is less than some value 
                // (i.e. there are too few observations for the cluster)
                if ((double)totalObservationsInClusters[i-1]/observations*100.0<minClusterPercent)
                {
                    tinyClusterInds[c-1] = i;
                    numTinyClusters++;
                    c++;
                }
            }
            //
            
            c=0;
            for (i=0; i<totalObservationsInClusters.length; i++)
                tmps[i] = totalObservationsInClusters[i];
            
            inds = MathUtils.quickSort(tmps, 0,  numClusters-1); 
            for (i=1; i<=numClusters; i++)
            {
                if (((double)totalObservationsInClusters[i-1]/observations*100)>=minClusterPercent)
                {
                    for (d=1; d<=dimension; d++)
                        clusters[i-1].meanVector[d-1] = m_new[i-1][d-1]/totalObservationsInClusters[i-1];
                }
                else
                {
                    for (d=1; d<=dimension; d++)
                    {
                        rnd = Math.random()*Math.abs(clusters[inds[numClusters-c-1]].meanVector[d-1])*0.01;
                        clusters[i-1].meanVector[d-1] = clusters[inds[numClusters-c-1]].meanVector[d-1] + rnd;
                    }
                    c++;
                }
            }
            
            for (i=1; i<=numClusters; i++)
                prev_totals[i-1] = totalObservationsInClusters[i-1];
            
            iter++;
            totChanged = 0;
            if (iter>1)
            {
                if (iter>=maximumIterations)
                    bCont=false;    
                
                for (t=1; t<=observations; t++)
                {
                    for (i=1; i<=numClusters; i++)
                    {
                        if (b_old[t-1][i-1] != b[t-1][i-1])
                        {
                            totChanged++;
                            break; //Count each difference once
                        }
                    }
                }
                
                changedPerc = (double)totChanged/observations*100.0;
                if  (changedPerc < MIN_CHANGE_PERCENT) //stop if number of clusters changed is less than %MIN_CHANGE_PERCENT of total observation
                    bCont = false;
                
                System.out.println("Iteration: " + String.valueOf(iter) + " with " + String.valueOf(changedPerc) + " percent of cluster assignments updated");
            }
            else
                System.out.println("Iteration: " + String.valueOf(iter) + " K-means initialized");
            
            for (t=1; t<=observations; t++)
            {
                for (k2=1; k2<=numClusters; k2++)
                    b_old[t-1][k2-1] = b[t-1][k2-1];
            }
        }

        //Finally, calculate the cluster covariances
        if (isDiagonalOutputCovariance)
        {
            for (i=0; i<numClusters; i++)
            {
                Arrays.fill(clusters[i].covMatrix[0], 0.0);
                for (t=0; t<observations; t++)
                {
                    if (clusterIndices[t]==i)
                    {
                        for (d=0; d<dimension; d++)
                            clusters[i].covMatrix[0][d] += (x[t][d]-clusters[i].meanVector[d])*(x[t][d]-clusters[i].meanVector[d]);
                    }
                }
                
                for (d=0; d<dimension; d++)
                {
                    if (totalObservationsInClusters[i]>=10)
                        clusters[i].covMatrix[0][d] /= (totalObservationsInClusters[i]-1);
                    else if (totalObservationsInClusters[i]>=5)
                        clusters[i].covMatrix[0][d] /= totalObservationsInClusters[i];
                    else
                        clusters[i].covMatrix[0][d] = 1.0;
                    
                    clusters[i].covMatrix[0][d] = Math.max(MINIMUM_VARIANCE, clusters[i].covMatrix[0][d]);
                }
            }
        }
        else
        {
            //We need matrix inversion here!
        }
       
    }
}
