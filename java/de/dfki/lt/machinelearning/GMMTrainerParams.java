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

import java.io.IOException;

import de.dfki.lt.signalproc.util.MaryRandomAccessFile;

/**
 * @author oytun.turk
 *
 */
public class GMMTrainerParams {
    public static final int EM_TOTAL_COMPONENTS_DEFAULT = 1;
    public static final boolean EM_IS_DIAGONAL_COVARIANCE_DEFAULT = true;
    public static final int EM_MIN_ITERATIONS_DEFAULT = 20;
    public static final int EM_MAX_ITERATIONS_DEFAULT = 200;
    public static final boolean EM_IS_UPDATE_COVARIANCES_DEFAULT = true;
    public static final double EM_TINY_LOGLIKELIHOOD_CHANGE_PERCENT_DEFAULT = 0.0001;
    public static final double EM_MIN_COVARIANCE_ALLOWED_DEFAULT = 1e-4;
    public static final boolean EM_USE_NATIVE_C_LIB_TRAINER_DEFAULT = false;
    
    public int totalComponents;
    public boolean isDiagonalCovariance; 
    public int kmeansMaxIterations;
    public double kmeansMinClusterChangePercent;
    public int kmeansMinSamplesInOneCluster;
    public int emMinIterations;
    public int emMaxIterations;
    public boolean isUpdateCovariances;
    public double tinyLogLikelihoodChange;
    public double minCovarianceAllowed;
    public boolean useNativeCLibTrainer;
    
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
        tinyLogLikelihoodChange = EM_TINY_LOGLIKELIHOOD_CHANGE_PERCENT_DEFAULT;
        minCovarianceAllowed = EM_MIN_COVARIANCE_ALLOWED_DEFAULT;
        useNativeCLibTrainer = EM_USE_NATIVE_C_LIB_TRAINER_DEFAULT;
    }
    
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
        tinyLogLikelihoodChange = existing.tinyLogLikelihoodChange;
        minCovarianceAllowed = existing.minCovarianceAllowed;
        useNativeCLibTrainer = existing.useNativeCLibTrainer;
    }
    
    public GMMTrainerParams(MaryRandomAccessFile stream)
    {
        read(stream);
    }
    
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
                stream.writeDouble(tinyLogLikelihoodChange);
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
                tinyLogLikelihoodChange = stream.readDouble();
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
