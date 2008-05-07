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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtils;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
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
                     gmmParams.kmeansMaximumIterations,
                     gmmParams.kmeansMinClusterChangePercent,
                     gmmParams.emMinimumIterations,
                     gmmParams.emMaximumIterations, 
                     gmmParams.isUpdateCovariances, 
                     gmmParams.tinyLogLikelihoodChange,
                     gmmParams.minimumCovarianceAllowed,
                     gmmParams.useNativeCLibTrainer);
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
                     KMeansClusteringTrainer.KMEANS_MAXIMUM_ITERATIONS_DEFAULT);
    }
    
    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance,
            int kmeansMaximumIterations)
    {
        return train(x, 
                     totalComponents, 
                     isDiagonalCovariance,
                     kmeansMaximumIterations,
                     KMeansClusteringTrainer.KMEANS_MIN_CLUSTER_CHANGE_PERCENT_DEFAULT);
    }
    
    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance,
            int kmeansMaximumIterations,
            double kmeansMinClusterChangePercent)
    {
        return train(x, 
                     totalComponents, 
                     isDiagonalCovariance,
                     kmeansMaximumIterations,
                     kmeansMinClusterChangePercent,
                     GMMTrainerParams.EM_MINIMUM_ITERATIONS_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance,
            int kmeansMaximumIterations,
            double kmeansMinClusterChangePercent,
            int emMinimumIterations)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                emMinimumIterations, 
                GMMTrainerParams.EM_MAXIMUM_ITERATIONS_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int kmeansMaximumIterations,
            double kmeansMinClusterChangePercent,
            int emMinimumIterations,
            int emMaximumIterations)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                kmeansMaximumIterations,
                kmeansMinClusterChangePercent,
                emMinimumIterations, 
                emMaximumIterations, 
                GMMTrainerParams.IS_UPDATE_COVARIANCES_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int kmeansMaximumIterations,
            double kmeansMinClusterChangePercent,
            int emMinimumIterations,
            int emMaximumIterations, 
            boolean isUpdateCovariances)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                kmeansMaximumIterations,
                kmeansMinClusterChangePercent,
                emMinimumIterations, 
                emMaximumIterations, 
                isUpdateCovariances, 
                GMMTrainerParams.TINY_LOGLIKELIHOOD_CHANGE_PERCENT_DEFAULT);
    }

    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int kmeansMaximumIterations,
            double kmeansMinClusterChangePercent,
            int emMinimumIterations,
            int emMaximumIterations, 
            boolean isUpdateCovariances, 
            double tinyLogLikelihoodChangePercent)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                kmeansMaximumIterations,
                kmeansMinClusterChangePercent,
                emMinimumIterations, 
                emMaximumIterations, 
                isUpdateCovariances, 
                tinyLogLikelihoodChangePercent,
                GMMTrainerParams.MINIMUM_COVARIANCE_ALLOWED_DEFAULT);
    }
    
    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int kmeansMaximumIterations,
            double kmeansMinClusterChangePercent,
            int emMinimumIterations,
            int emMaximumIterations, 
            boolean isUpdateCovariances, 
            double tinyLogLikelihoodChangePercent,
            double minimumCovarianceAllowed)
    {
        return train(x, 
                totalComponents, 
                isDiagonalCovariance, 
                kmeansMaximumIterations,
                kmeansMinClusterChangePercent,
                emMinimumIterations,
                emMaximumIterations, 
                isUpdateCovariances, 
                tinyLogLikelihoodChangePercent,
                minimumCovarianceAllowed,
                GMMTrainerParams.USE_NATIVE_C_LIB_TRAINER_DEFAULT);
    }
    
    public GMM train(double[][] x, 
            int totalComponents, 
            boolean isDiagonalCovariance, 
            int kmeansMaximumIterations,
            double kmeansMinClusterChangePercent,
            int emMinimumIterations,
            int emMaximumIterations, 
            boolean isUpdateCovariances, 
            double tinyLogLikelihoodChangePercent,
            double minimumCovarianceAllowed,
            boolean useNativeCLibTrainer)
    {
        long startTime, endTime;
        
        /*
        //For testing Java and native C versions with identical data
        String dataFile0 = "d:\\gmmTester2.dat";
        DoubleData d0 = null;
        if (FileUtils.exists(dataFile0))
        {
            d0 = new DoubleData(dataFile0);
            x = new double[d0.numVectors][];
            for (int i=0; i<d0.numVectors; i++)
            {
                x[i] = new double[d0.dimension];
                System.arraycopy(d0.vectors[i], 0, x[i], 0, d0.dimension);
            } 
        }
        else
        {
            d0 = new DoubleData(x);
            d0.write(dataFile0);
        }
        */
        
        startTime = System.currentTimeMillis();
        
        GMM gmm = null;
        if (x!=null && totalComponents>0)
        {
            if (!MaryUtils.isWindows())
                useNativeCLibTrainer = false;
            
            if (!useNativeCLibTrainer) //Java training
            {
                int featureDimension = x[0].length;
                int i;
                for (i=1; i<x.length; i++)
                    assert x[i].length==featureDimension;

                //Initialize components with KMeans clustering
                KMeansClusteringTrainer kmeansClusterer = new KMeansClusteringTrainer();
                kmeansClusterer.cluster(x, totalComponents, 
                        kmeansMaximumIterations, 
                        kmeansMinClusterChangePercent,
                        isDiagonalCovariance);

                //Create initial GMM according to KMeans clustering results
                GMM initialGmm = new GMM(kmeansClusterer);

                //Update model parameters with Expectation-Maximization
                gmm = expectationMaximization(x, 
                        initialGmm, 
                        emMinimumIterations, 
                        emMaximumIterations, 
                        isUpdateCovariances, 
                        tinyLogLikelihoodChangePercent,
                        minimumCovarianceAllowed);
            }
            else //native C Library training (only available for Windows)
            {
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("gmm", ".dat");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                if (tempFile!=null)
                {
                    String strIsBigEndian = "1";
                    String dataFile = tempFile.getPath();
                    DoubleData d = new DoubleData(x);
                    d.write(dataFile);
                    
                    String gmmFile = StringUtils.modifyExtension(dataFile, ".gmm");
                    String strCommand = "GMMTrainer.exe " +
                                        "\"" + dataFile + "\" " +
                                        "\"" + gmmFile + "\" " +
                                        String.valueOf(totalComponents) + " " +
                                        strIsBigEndian + " " +
                                        String.valueOf(isDiagonalCovariance==true ? 1 : 0) + " " +
                                        String.valueOf(kmeansMaximumIterations) + " " + 
                                        String.valueOf(kmeansMinClusterChangePercent) + " " +
                                        String.valueOf(emMinimumIterations) + " " +
                                        String.valueOf(emMaximumIterations) + " " +
                                        String.valueOf(isUpdateCovariances==true ? 1 : 0) + " " +
                                        String.valueOf(tinyLogLikelihoodChangePercent) + " " +
                                        String.valueOf(minimumCovarianceAllowed);

                    int exitVal = MaryUtils.shellExecute(strCommand, true);
                    
                    if (exitVal == 0) 
                    {
                        System.out.println("GMM training with native C library done...");
                        gmm = new GMM(gmmFile);
                        FileUtils.delete(gmmFile); 
                    }
                    else
                        System.out.println("Error executing native C library with exit code " + exitVal);

                    FileUtils.delete(dataFile);
                }
            }
        }
        
        endTime = System.currentTimeMillis();
        System.out.println("GMM training took " + String.valueOf((endTime-startTime)/1000.0) + " seconds...");

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
            int emMinimumIterations,
            int emMaximumIterations, 
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
        logLikelihoods = new double[emMaximumIterations];

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

            System.out.println("For " + String.valueOf(gmm.totalComponents) + " mixes - EM iteration no: " + String.valueOf(numIterations) + " with error " + String.valueOf(error) + " log-likelihood=" + String.valueOf(logLikelihoods[numIterations-1]));

            if (numIterations+1>emMaximumIterations)
                break;

            /*
            if (mean_diff<TINY_DIFF)
                 break;

            if (numIterations>emMinimumIterations && prevErr-error<TINY_DIFF)
                break;
            */

            if (numIterations>emMinimumIterations && logLikelihoods[numIterations-1]-logLikelihoods[numIterations-2]<Math.abs(logLikelihoods[numIterations-1]/100*tinyLogLikelihoodChange))
                break;

            numIterations++;
        }

        double[] tmpLogLikelihoods = new double[numIterations-1];
        System.arraycopy(logLikelihoods, 0, tmpLogLikelihoods, 0, numIterations-1);
        logLikelihoods = new double[numIterations-1];
        System.arraycopy(tmpLogLikelihoods, 0, logLikelihoods, 0, numIterations-1);   
        
        System.out.println("GMM training completed...");

        return gmm;
    }
    
    public static void testEndianFileIO() throws IOException
    {
        boolean b1 = true;
        char c1 = 'c';
        short s1 = 111;
        int i1 = 222;
        double d1 = 33.3;
        float f1 = 44.4f;
        long l1 = 555;
        
        String javaFile = "d:\\endianJava.tmp";
        MaryRandomAccessFile fp = new MaryRandomAccessFile(javaFile, "rw");
        if (fp!=null)
        {
            fp.writeBooleanEndian(b1);
            fp.writeCharEndian(c1);
            fp.writeShortEndian(s1);
            fp.writeIntEndian(i1);
            fp.writeDoubleEndian(d1);
            fp.writeFloatEndian(f1);
            fp.writeLongEndian(l1);

            fp.close();
        }

        boolean b2;
        char c2;
        short s2;
        int i2;
        double d2;
        float f2;
        long l2;

        String cFile = "d:\\endianC.tmp";
        
        if (FileUtils.exists(cFile))
        {
            MaryRandomAccessFile fp2 = new MaryRandomAccessFile(cFile, "r");
            if (fp2!=null)
            {
                b2 = fp2.readBooleanEndian();
                c2 = fp2.readCharEndian();
                s2 = fp2.readShortEndian();
                i2 = fp2.readIntEndian();
                d2 = fp2.readDoubleEndian();
                f2 = fp2.readFloatEndian();
                l2 = fp2.readLongEndian();

                fp2.close();

                if (b1!=b2)
                    System.out.println("Error in bool!\n");
                if (c1!=c2)
                    System.out.println("Error in char!\n");
                if (s1!=s2)
                    System.out.println("Error in short!\n");
                if (i1!=i2)
                    System.out.println("Error in int!\n");
                if (d1!=d2)
                    System.out.println("Error in double!\n");
                if (f1!=f2)
                    System.out.println("Error in float!\n");
                if (l1!=l2)
                    System.out.println("Error in long!\n");
            }
            else
                System.out.println("C generated file cannot be opened...\n");
        }
        else
            System.out.println("C generated file not found...\n");
    }
    
    public static void main(String[] args)
    {
        int numClusters = 16;
        int numSamplesInClusters = 1000;
        double[] variances = {2.0};
        int vectorDim = 1;
        ClusteredDataGenerator[] c = new ClusteredDataGenerator[vectorDim];
        int i, j, n;
        int totalVectors = 0;
        for (i=0; i<vectorDim; i++)
        {
            if (i<variances.length)
                c[i] = new ClusteredDataGenerator(numClusters, numSamplesInClusters, 10.0*(i+1), variances[i]);
            else
                c[i] = new ClusteredDataGenerator(numClusters, numSamplesInClusters, 10.0*(i+1), variances[0]);
            
            totalVectors += c[i].data.length;
        }
        
        double[][] x = new double[totalVectors][vectorDim];
        int counter=0;
        for (n=0; n<c.length; n++)
        {
            for (i=0; i<c[n].data.length; i++)
            {
                for (j=0; j<vectorDim; j++)
                    x[counter][j] = c[n].data[i];
                
                counter++;
                if (counter>=totalVectors)
                    break;
            }
            
            if (counter>=totalVectors)
                break;
        }
        
        double[] m = MathUtils.mean(x);
        double[] v = MathUtils.variance(x, m);
        System.out.println(String.valueOf(m[0]) + " " + String.valueOf(v[0]));
        
        GMMTrainerParams gmmParams = new GMMTrainerParams();
        gmmParams.totalComponents = numClusters;
        gmmParams.isDiagonalCovariance = true; 
        gmmParams.kmeansMaximumIterations = 100;
        gmmParams.kmeansMinClusterChangePercent = 0.001;
        gmmParams.emMinimumIterations = 1000;
        gmmParams.emMaximumIterations = 2000; 
        gmmParams.isUpdateCovariances = true;
        gmmParams.tinyLogLikelihoodChange = 0.001;
        gmmParams.minimumCovarianceAllowed = 1e-5;
        gmmParams.useNativeCLibTrainer = true;
        
        GMMTrainer g = new GMMTrainer();
        GMM gmm = g.train(x, gmmParams);
        
        if (gmm!=null)
        {
            for (i=0; i<gmm.totalComponents; i++)
                System.out.println("Gaussian #" + String.valueOf(i+1) + " mean=" + String.valueOf(gmm.components[i].meanVector[0]) + " variance=" + String.valueOf(gmm.components[i].covMatrix[0][0]));
        }
        
        /*
        try {
            testEndianFileIO();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
    }
}
