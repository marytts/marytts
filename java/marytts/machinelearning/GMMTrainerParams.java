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

import java.io.IOException;

import marytts.util.MaryRandomAccessFile;


/**
 * @author oytun.turk
 *
 */
public class GMMTrainerParams {
    
    //A set of default values for GMM  training parameters
    public static final int EM_TOTAL_COMPONENTS_DEFAULT = 1;
    public static final boolean EM_IS_DIAGONAL_COVARIANCE_DEFAULT = true;
    public static final int EM_MIN_ITERATIONS_DEFAULT = 500;
    public static final int EM_MAX_ITERATIONS_DEFAULT = 2000;
    public static final boolean EM_IS_UPDATE_COVARIANCES_DEFAULT = true;
    public static final double EM_TINY_LOGLIKELIHOOD_CHANGE_PERCENT_DEFAULT = 0.0001;
    public static final double EM_MIN_COVARIANCE_ALLOWED_DEFAULT = 1e-4;
    public static final boolean EM_USE_NATIVE_C_LIB_TRAINER_DEFAULT = false;
    //
    
    public int totalComponents; //Total number of Gaussians in the GMM
    public boolean isDiagonalCovariance; //Estimate diagonal covariance matrices? 
                                         //  Full-covariance training is likely to result in ill-conditioned training due to insufficient training data
    public int kmeansMaxIterations; //Minimum number of K-Means iterations to initialize the GMM
    public double kmeansMinClusterChangePercent; //Maximum number of K-Means iterations to initialize the GMM
    public int kmeansMinSamplesInOneCluster; //Minimum number of observations in one cluster while initializing the GMM with K-Means
    public int emMinIterations;  //Minimum number of EM iterations for which the algorithm will not quit
                                 //  even when the total likelihood does not change much with additional iterations
    public int emMaxIterations;  //Maximum number of EM iterations for which the algorithm will quit 
                                 //  even when total likelihood has not settled yet
    public boolean isUpdateCovariances; //Update covariance matrices in EM iterations?
    public double tinyLogLikelihoodChangePercent; //Threshold to compare percent decrease in total log-likelihood to stop iterations automatically
    public double minCovarianceAllowed; //Minimum covariance value allowed - should be a small positive number to avoid ill-conditioned training
    public boolean useNativeCLibTrainer; //Use native C library trainer (Windows OS only)
    
    //Default constructor
    public GMMTrainerParams()
    {
        totalComponents = EM_TOTAL_COMPONENTS_DEFAULT;
        isDiagonalCovariance = EM_IS_DIAGONAL_COVARIANCE_DEFAULT; 
        kmeansMaxIterations = KMeansClusteringTrainerParams.KMEANS_MAX_ITERATIONS_DEFAULT;
        kmeansMinClusterChangePercent = KMeansClusteringTrainerParams.KMEANS_MIN_CLUSTER_CHANGE_PERCENT_DEFAULT;
        kmeansMinSamplesInOneCluster = KMeansClusteringTrainerParams.KMEANS_MIN_SAMPLES_IN_ONE_CLUSTER_DEFAULT;
        emMinIterations = EM_MIN_ITERATIONS_DEFAULT;
        emMaxIterations = EM_MAX_ITERATIONS_DEFAULT;
        isUpdateCovariances = EM_IS_UPDATE_COVARIANCES_DEFAULT;
        tinyLogLikelihoodChangePercent = EM_TINY_LOGLIKELIHOOD_CHANGE_PERCENT_DEFAULT;
        minCovarianceAllowed = EM_MIN_COVARIANCE_ALLOWED_DEFAULT;
        useNativeCLibTrainer = EM_USE_NATIVE_C_LIB_TRAINER_DEFAULT;
    }
    
    //Constructor using an existing parameter set
    public GMMTrainerParams(GMMTrainerParams existing)
    {
        totalComponents = existing.totalComponents;
        isDiagonalCovariance = existing.isDiagonalCovariance; 
        kmeansMaxIterations = existing.kmeansMaxIterations;
        kmeansMinClusterChangePercent = existing.kmeansMinClusterChangePercent;
        kmeansMinSamplesInOneCluster = existing.kmeansMinSamplesInOneCluster;
        emMinIterations = existing.emMinIterations;
        emMaxIterations = existing.emMaxIterations;
        isUpdateCovariances = existing.isUpdateCovariances;
        tinyLogLikelihoodChangePercent = existing.tinyLogLikelihoodChangePercent;
        minCovarianceAllowed = existing.minCovarianceAllowed;
        useNativeCLibTrainer = existing.useNativeCLibTrainer;
    }
    
    //Constructor that reads GMM training parameters from a binary file stream
    public GMMTrainerParams(MaryRandomAccessFile stream)
    {
        read(stream);
    }
    
    //Function to write GMM training parameters to a binary file stream
    public void write(MaryRandomAccessFile stream)
    {
        if (stream!=null)
        {
            try {
                stream.writeInt(totalComponents);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                stream.writeBoolean(isDiagonalCovariance);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            try {
                stream.writeInt(kmeansMaxIterations);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            try {
                stream.writeDouble(kmeansMinClusterChangePercent);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            try {
                stream.writeInt(kmeansMinSamplesInOneCluster);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }            
            try {
                stream.writeInt(emMinIterations);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                stream.writeInt(emMaxIterations);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                stream.writeBoolean(isUpdateCovariances);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                stream.writeDouble(tinyLogLikelihoodChangePercent);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                 stream.writeDouble(minCovarianceAllowed);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                stream.writeBoolean(useNativeCLibTrainer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    //Function that reads GMM training parameters from a binary file stream 
    public void read(MaryRandomAccessFile stream)
    {
        if (stream!=null)
        {
            try {
                totalComponents = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                isDiagonalCovariance = stream.readBoolean();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            
            try {
                kmeansMaxIterations = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            
            try {
                kmeansMinClusterChangePercent = stream.readDouble();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            
            try {
                kmeansMinSamplesInOneCluster = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            
            try {
                emMinIterations = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                emMaxIterations = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                isUpdateCovariances = stream.readBoolean();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           
            try {
                tinyLogLikelihoodChangePercent = stream.readDouble();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                minCovarianceAllowed = stream.readDouble();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                useNativeCLibTrainer = stream.readBoolean();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
