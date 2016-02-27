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

import java.util.Arrays;

/**
 * 
 * Implements a cluster center that has a mean vector and a covariance matrix (and its inverse)
 * 
 * @author Oytun T&uuml;rk
 */
public class Cluster {
	public double[] meanVector;
	public double[][] covMatrix;
	public double[][] invCovMatrix; // This is not supported yet (requires matrix inversion)
	public boolean isDiagonalCovariance;

	public Cluster() {
		this(0, true);
	}

	public Cluster(int dimension, boolean isDiagonalCovariance) {
		allocate(dimension, isDiagonalCovariance);
	}

	public void allocate(int dimension, boolean isDiagonalCovarianceIn) {
		if (dimension > 0) {
			isDiagonalCovariance = isDiagonalCovarianceIn;
			meanVector = new double[dimension];
			Arrays.fill(meanVector, 0.0);

			if (isDiagonalCovariance) {
				covMatrix = new double[1][];
				covMatrix[0] = new double[dimension];
				Arrays.fill(covMatrix[0], 0.0);

				invCovMatrix = new double[1][];
				invCovMatrix[0] = new double[dimension];
				Arrays.fill(invCovMatrix[0], 0.0);
			} else {
				covMatrix = new double[dimension][];
				for (int i = 0; i < dimension; i++) {
					covMatrix[i] = new double[dimension];
					Arrays.fill(covMatrix[i], 0.0);
				}

				invCovMatrix = new double[dimension][];
				for (int i = 0; i < dimension; i++) {
					invCovMatrix[i] = new double[dimension];
					Arrays.fill(invCovMatrix[i], 0.0);
				}
			}
		} else {
			meanVector = null;
			covMatrix = null;
			invCovMatrix = null;
		}
	}

	public double[] getCovarianceDiagonal() {
		double[] diagonal = null;

		if (covMatrix != null && covMatrix[0] != null && covMatrix[0].length > 0) {
			diagonal = new double[covMatrix[0].length];
			if (isDiagonalCovariance)
				System.arraycopy(covMatrix[0], 0, diagonal, 0, covMatrix[0].length);
			else {
				for (int i = 0; i < covMatrix.length; i++)
					diagonal[i] = covMatrix[i][i];
			}
		}

		return diagonal;
	}
}
