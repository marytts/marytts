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
package marytts.machinelearning;

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;

/**
 * Wrapper class for GMM training parameters
 * 
 * @author Oytun T&uuml;rk
 */
public class GMMTrainerParams {

	// A set of default values for GMM training parameters
	public static final int EM_TOTAL_COMPONENTS_DEFAULT = 1;
	public static final boolean EM_IS_DIAGONAL_COVARIANCE_DEFAULT = true;
	public static final int EM_MIN_ITERATIONS_DEFAULT = 500;
	public static final int EM_MAX_ITERATIONS_DEFAULT = 2000;
	public static final boolean EM_IS_UPDATE_COVARIANCES_DEFAULT = true;
	public static final double EM_TINY_LOGLIKELIHOOD_CHANGE_PERCENT_DEFAULT = 0.0001;
	public static final double EM_MIN_COVARIANCE_ALLOWED_DEFAULT = 1e-4;
	public static final boolean EM_USE_NATIVE_C_LIB_TRAINER_DEFAULT = false;
	//

	public int totalComponents; // Total number of Gaussians in the GMM
	public boolean isDiagonalCovariance; // Estimate diagonal covariance matrices?
											// Full-covariance training is likely to result in ill-conditioned training due to
											// insufficient training data
	public int kmeansMaxIterations; // Minimum number of K-Means iterations to initialize the GMM
	public double kmeansMinClusterChangePercent; // Maximum number of K-Means iterations to initialize the GMM
	public int kmeansMinSamplesInOneCluster; // Minimum number of observations in one cluster while initializing the GMM with
												// K-Means
	public int emMinIterations; // Minimum number of EM iterations for which the algorithm will not quit
								// even when the total likelihood does not change much with additional iterations
	public int emMaxIterations; // Maximum number of EM iterations for which the algorithm will quit
								// even when total likelihood has not settled yet
	public boolean isUpdateCovariances; // Update covariance matrices in EM iterations?
	public double tinyLogLikelihoodChangePercent; // Threshold to compare percent decrease in total log-likelihood to stop
													// iterations automatically
	public double minCovarianceAllowed; // Minimum covariance value allowed - should be a small positive number to avoid
										// ill-conditioned training
	public boolean useNativeCLibTrainer; // Use native C library trainer (Windows OS only)

	// Default constructor
	public GMMTrainerParams() {
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

	// Constructor using an existing parameter set
	public GMMTrainerParams(GMMTrainerParams existing) {
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

	// Constructor that reads GMM training parameters from a binary file stream
	public GMMTrainerParams(MaryRandomAccessFile stream) {
		read(stream);
	}

	// Function to write GMM training parameters to a binary file stream
	public void write(MaryRandomAccessFile stream) {
		if (stream != null) {
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

	// Function that reads GMM training parameters from a binary file stream
	public void read(MaryRandomAccessFile stream) {
		if (stream != null) {
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
