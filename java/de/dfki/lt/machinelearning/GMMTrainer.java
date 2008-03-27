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

import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class GMMTrainer {

    public double[] logLikelihoods;

    public GMMTrainer()
    {
        logLikelihoods = null;
    }
    
    public GMM train(double[][] x, GMMTrainerParams gmmParams)
    {
        return train(x, 
                     gmmParams.totalComponents, 
                     gmmParams.isDiagonalCovariance, 
                     gmmParams.minimumIterations,
                     gmmParams.maximumIterations, 
                     gmmParams.isUpdateCovariances, 
                     gmmParams.tinyLogLikelihoodChange,
                     gmmParams.minimumCovarianceAllowed);
    }

    public GMM train(double[][] x, 
            int totalComponents)
    {
        return train(x, 
                totalComponents, 
                GMMTrainerParams.IS_DIAGONAL_COVARIANCE_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                GMMTrainerParams.MINIMUM_ITERATIONS_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int minimumIterations)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                minimumIterations, 
                GMMTrainerParams.MAXIMUM_ITERATIONS_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int minimumIterations,
            int maximumIterations)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                minimumIterations, 
                maximumIterations, 
                GMMTrainerParams.IS_UPDATE_COVARIANCES_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int minimumIterations,
            int maximumIterations, 
            boolean isUpdateCovariances)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                minimumIterations, 
                maximumIterations, 
                isUpdateCovariances, 
                GMMTrainerParams.TINY_LOGLIKELIHOOD_CHANGE_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int minimumIterations,
            int maximumIterations, 
            boolean isUpdateCovariances, 
            double tinyLogLikelihoodChange)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                minimumIterations, 
                maximumIterations, 
                isUpdateCovariances, 
                tinyLogLikelihoodChange,
                GMMTrainerParams.MINIMUM_COVARIANCE_ALLOWED_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int minimumIterations,
            int maximumIterations, 
            boolean isUpdateCovariances, 
            double tinyLogLikelihoodChange,
            double minimumCovarianceAllowed)
    {
        GMM gmm = null;
        if (x!=null && totalComponents>0)
        {
            int featureDimension = x[0].length;
            int i;
            for (i=1; i<x.length; i++)
                assert x[i].length==featureDimension;

            //Initialize components with KMeans clustering
            KMeansClusteringTrainer kmeansClusterer = new KMeansClusteringTrainer();
            kmeansClusterer.cluster(x, totalComponents, 
                    KMeansClusteringTrainer.MAXIMUM_ITERATIONS_DEFAULT, 
                    KMeansClusteringTrainer.MIN_CLUSTER_PERCENT_DEFAULT,
                    isDiagonalCovariance);

            //Create initial GMM according to KMeans clustering results
            GMM initialGmm = new GMM(kmeansClusterer);

            //Update model parameters with Expectation-Maximization
            gmm = expectationMaximization(x, 
                    initialGmm, 
                    minimumIterations, 
                    maximumIterations, 
                    isUpdateCovariances, 
                    tinyLogLikelihoodChange,
                    minimumCovarianceAllowed);
        }

        return gmm;
    }

    // x: data matrix (each row is another observation)
    // Model: initial mixture model
    //
    // Automatically stop iterating if alphas do not change much
    // Now changed!!! Check the change in group means and stop if less than threshold!
    // Added is_update_variances as an argument to the main
    public GMM expectationMaximization(double[][] x, 
            GMM initialGmm, 
            int minimumIterations,
            int maximumIterations, 
            boolean isUpdateCovariances,
            double tinyLogLikelihoodChange,
            double minimumCovarianceAllowed)
    {
        int i, j,k;
        int totalObservations = x.length;

        GMM gmm = new GMM(initialGmm);

        for (i=0; i<totalObservations; i++)
            assert x[i].length == gmm.featureDimension;

        int numIterations = 1;

        double error = 0.0;
        double prevErr;

        for (k=0; k<gmm.totalComponents; k++)
            gmm.weights[k] = 1.0f/gmm.totalComponents;

        boolean bContinue = true;

        double[] zDenum = new double[totalObservations];
        double P_xj_tetak;

        double[][] zNum = new double[totalObservations][gmm.totalComponents];
        double[][] z = new double[totalObservations][gmm.totalComponents];

        double[] num1 = new double[gmm.featureDimension];
        double[] tmpMean = new double[gmm.featureDimension];

        double[][] num2 = new double[gmm.featureDimension][gmm.featureDimension];

        double tmpSum;
        double mean_diff;
        double denum;
        double diffk; 
        int d1, d2;
        logLikelihoods = new double[maximumIterations];

        while(bContinue)
        {
            //Expectation step
            // Find zjk's at time (s+1) using alphak's at time (s)
            for (j=0; j<totalObservations; j++)
            {
                zDenum[j] = 0.0f;
                for (k=0; k<gmm.totalComponents; k++)
                {
                    //P(xj|teta_k)
                    if (gmm.isDiagonalCovariance)
                        P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector, gmm.components[k].getCovMatrixDiagonal(), gmm.components[k].getConstantTerm()); 
                    else
                        P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector, gmm.components[k].getInvCovMatrix(), gmm.components[k].getConstantTerm());

                    zNum[j][k] = gmm.weights[k] * P_xj_tetak;
                    zDenum[j] = zDenum[j] + zNum[j][k];
                }
            }

            //Find zjk's at time (s+1)
            for (j=0; j<totalObservations; j++)
            {
                for (k=0; k<gmm.totalComponents; k++)
                    z[j][k] = zNum[j][k]/zDenum[j];
            }

            //Now update alphak's to find their values at time (s+1)
            for (k=0; k<gmm.totalComponents; k++)
            {
                tmpSum = 0.0;
                for (j=0; j<totalObservations; j++)
                    tmpSum += z[j][k];

                gmm.weights[k] = tmpSum/totalObservations;
            }

            //Maximization step
            // Find the model parameters at time (s+1) using zjk's at time (s+1)
            mean_diff=0.0;
            for (k=0; k<gmm.totalComponents; k++)
            {
                for (d1=0; d1<gmm.featureDimension; d1++)
                {
                    num1[d1] = 0.0f;
                    for (d2=0; d2<gmm.featureDimension; d2++)
                        num2[d1][d2] = 0.0f;
                }

                denum=0.0;

                for (j=0; j<totalObservations; j++)
                {
                    for (d1=0; d1<gmm.featureDimension; d1++)
                        num1[d1] += x[j][d1]*z[j][k];

                    denum += z[j][k];

                    for (d1=0; d1<gmm.featureDimension; d1++)
                    {
                        for (d2=0; d2<gmm.featureDimension; d2++)
                            num2[d1][d2] += z[j][k]*(x[j][d1]-gmm.components[k].meanVector[d1])*(x[j][d2]-gmm.components[k].meanVector[d2]);
                    }
                }

                for (d1=0; d1<gmm.featureDimension; d1++)
                    tmpMean[d1] = num1[d1] / denum;

                diffk = 0.0f;
                for (d1=0; d1<gmm.featureDimension; d1++)
                    diffk += (tmpMean[d1]-gmm.components[k].meanVector[d1])*(tmpMean[d1]-gmm.components[k].meanVector[d1]);
                diffk = Math.sqrt(diffk);
                mean_diff += diffk;

                for (d1=0; d1<gmm.featureDimension; d1++)
                    gmm.components[k].meanVector[d1] = tmpMean[d1];

                if (isUpdateCovariances)
                {
                    if (gmm.isDiagonalCovariance)
                    {
                        for (d1=0; d1<gmm.featureDimension; d1++)
                            gmm.components[k].covMatrix[0][d1] = Math.max(num2[d1][d1]/denum, minimumCovarianceAllowed);
                    }
                    else
                    {
                        for (d1=0; d1<gmm.featureDimension; d1++)
                        {
                            for (d2=0; d2<gmm.featureDimension; d2++)
                                gmm.components[k].covMatrix[d1][d2] = Math.max(num2[d1][d2]/denum, minimumCovarianceAllowed);
                        }
                    }

                    gmm.components[k].setDerivedValues();
                }
            }

            if (numIterations == 1)
                error = mean_diff;
            else
            {
                prevErr = error;
                error = mean_diff;
            }

            logLikelihoods[numIterations-1] = 0.0;
            for (j=0; j<totalObservations; j++)
            {
                double tmp=0.0;
                for (k=0; k<gmm.totalComponents; k++)
                {
                    if (gmm.isDiagonalCovariance)
                        P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector, gmm.components[k].getCovMatrixDiagonal(), gmm.components[k].getConstantTerm()); 
                    else
                        P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector, gmm.components[k].getInvCovMatrix(), gmm.components[k].getConstantTerm()); 

                    tmp += gmm.weights[k]*P_xj_tetak;
                }
                
                logLikelihoods[numIterations-1] += Math.log(tmp);
            }

            System.out.println("Iteration no: " + String.valueOf(numIterations) + " with error " + String.valueOf(error) + " log-likelihood=" + String.valueOf(logLikelihoods[numIterations-1]));

            if (numIterations+1>maximumIterations)
                break;

            /*
            if (mean_diff<TINY_DIFF)
                 break;

            if (numIterations>minimumIterations && prevErr-error<TINY_DIFF)
                break;
            */

            if (numIterations>minimumIterations && logLikelihoods[numIterations-1]-logLikelihoods[numIterations-2]<Math.abs(logLikelihoods[numIterations-1]/100*tinyLogLikelihoodChange))
                break;

            numIterations++;
        }

        double[] tmpLogLikelihoods = new double[numIterations-1];
        System.arraycopy(logLikelihoods, 0, tmpLogLikelihoods, 0, numIterations-1);
        logLikelihoods = new double[numIterations-1];
        System.arraycopy(tmpLogLikelihoods, 0, logLikelihoods, 0, numIterations-1);   

        return gmm;
    }
    
    public static void main(String[] args)
    {
        int numClusters = 20;
        int numSamplesInClusters = 100;
        double variance = 1.0; //Setting the variance too small, i.e. 0.08 results in ill-conditioned training - requires a log-domain implementation
        ClusteredDataGenerator c = new ClusteredDataGenerator(numClusters, numSamplesInClusters, variance);
        int vectorDim = 25;
        double[][] x = new double[c.data.length][vectorDim];
        
        for (int i=0; i<c.data.length; i++)
        {
            for (int j=0; j<vectorDim; j++)
                x[i][j] = c.data[i];
        }
        
        double[] m = MathUtils.mean(x);
        double[] v = MathUtils.variance(x, m);
        System.out.println(String.valueOf(m[0]) + " " + String.valueOf(v[0]));
        
        GMMTrainer g = new GMMTrainer();
        GMM gmm = g.train(x, numClusters, true, 200);
        
        for (int i=0; i<gmm.totalComponents; i++)
            System.out.println("Gaussian #" + String.valueOf(i+1) + " mean=" + String.valueOf(gmm.components[i].meanVector[0]) + " variance=" + String.valueOf(gmm.components[i].covMatrix[0][0]));
    }
}
