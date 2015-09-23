/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.util.math;

import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import marytts.util.string.StringUtils;

/**
 * @author Marc Schr&ouml;der, Oytun Tuerk
 * 
 * 
 *         An uninstantiable class, containing static utility methods in the Math domain.
 * 
 */
public class MathUtils {
	public static final double TINY_PROBABILITY = 1e-50;
	public static final double TINY_PROBABILITY_LOG = Math.log(TINY_PROBABILITY);
	public static final double TINY = 1e-50;
	public static final double TINY_LOG = Math.log(TINY);

	protected static final double PASCAL = 2E-5;
	protected static final double PASCALSQUARE = 4E-10;
	protected static final double LOG10 = Math.log(10);

	public static final double TWOPI = 2 * Math.PI;

	public static final int EQUALS = 0;
	public static final int GREATER_THAN = 1;
	public static final int GREATER_THAN_OR_EQUALS = 2;
	public static final int LESS_THAN = 3;
	public static final int LESS_THAN_OR_EQUALS = 4;
	public static final int NOT_EQUALS = 5;

	public static boolean isPowerOfTwo(int N) {
		final int maxBits = 32;
		int n = 2;
		for (int i = 2; i <= maxBits; i++) {
			if (n == N)
				return true;
			n <<= 1;
		}
		return false;
	}

	public static int closestPowerOfTwoAbove(int N) {
		return 1 << (int) Math.ceil(Math.log(N) / Math.log(2));
	}

	public static int findNextValleyLocation(double[] data, int startIndex) {
		for (int i = startIndex + 1; i < data.length; i++) {
			if (data[i - 1] < data[i])
				return i - 1;
		}
		return data.length - 1;
	}

	public static int findNextPeakLocation(double[] data, int startIndex) {
		for (int i = startIndex + 1; i < data.length; i++) {
			if (data[i - 1] > data[i])
				return i - 1;
		}
		return data.length - 1;
	}

	/**
	 * Find the maximum of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return the index number of the maximum element
	 */
	public static int findGlobalPeakLocation(double[] data) {
		double max = Double.NaN;
		int imax = -1;
		for (int i = 0; i < data.length; i++) {
			if (Double.isNaN(data[i]))
				continue;
			if (Double.isNaN(max) || data[i] > max) {
				max = data[i];
				imax = i;
			}
		}
		return imax;
	}

	/**
	 * Find the maximum of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return the index number of the maximum element
	 */
	public static int findGlobalPeakLocation(float[] data) {
		float max = Float.NaN;
		int imax = -1;
		for (int i = 0; i < data.length; i++) {
			if (Float.isNaN(data[i]))
				continue;
			if (Float.isNaN(max) || data[i] > max) {
				max = data[i];
				imax = i;
			}
		}
		return imax;
	}

	/**
	 * Find the minimum of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return the index number of the minimum element
	 */
	public static int findGlobalValleyLocation(double[] data) {
		double min = Double.NaN;
		int imin = -1;
		for (int i = 0; i < data.length; i++) {
			if (Double.isNaN(data[i]))
				continue;
			if (Double.isNaN(min) || data[i] < min) {
				min = data[i];
				imin = i;
			}
		}
		return imin;
	}

	/**
	 * Find the minimum of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return the index number of the minimum element
	 */
	public static int findGlobalValleyLocation(float[] data) {
		float min = Float.NaN;
		int imin = -1;
		for (int i = 0; i < data.length; i++) {
			if (Float.isNaN(data[i]))
				continue;
			if (Float.isNaN(min) || data[i] < min) {
				min = data[i];
				imin = i;
			}
		}
		return imin;
	}

	/**
	 * Build the sum of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return sum
	 */
	public static double sum(double[] data) {
		double sum = 0.0;
		for (int i = 0; i < data.length; i++) {
			if (Double.isNaN(data[i]))
				continue;
			sum += data[i];
		}
		return sum;
	}

	public static float sum(float[] data) {
		float sum = 0.0f;
		for (int i = 0; i < data.length; i++) {
			if (Float.isNaN(data[i]))
				continue;
			sum += data[i];
		}
		return sum;
	}

	public static int sum(int[] data) {
		int sum = 0;
		for (int i = 0; i < data.length; i++)
			sum += data[i];

		return sum;
	}

	public static double sumSquared(double[] data) {
		return sumSquared(data, 0.0);
	}

	// Computes sum_i=0^data.length-1 (data[i]+term)^2
	public static double sumSquared(double[] data, double term) {
		double sum = 0.0;
		for (int i = 0; i < data.length; i++) {
			if (Double.isNaN(data[i]))
				continue;
			sum += (data[i] + term) * (data[i] + term);
		}
		return sum;
	}

	public static double sumSquared(double[] data, int startInd, int endInd) {
		return sumSquared(data, startInd, endInd, 0.0);
	}

	// Computes sum_i=0^data.length-1 (data[i]+term)^2
	public static double sumSquared(double[] data, int startInd, int endInd, double term) {
		double sum = 0.0;
		for (int i = startInd; i <= endInd; i++)
			sum += (data[i] + term) * (data[i] + term);

		return sum;
	}

	/**
	 * Find the maximum of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return max
	 */
	public static double max(double[] data) {
		double max = Double.NaN;
		for (int i = 0; i < data.length; i++) {
			if (Double.isNaN(data[i]))
				continue;
			if (Double.isNaN(max) || data[i] > max)
				max = data[i];
		}
		return max;
	}

	public static int max(int[] data) {
		int max = data[0];
		for (int i = 1; i < data.length; i++) {
			if (data[i] > max)
				max = data[i];
		}
		return max;
	}

	/**
	 * Find the maximum of the absolute values of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return absMax of data, 0, data.length
	 */
	public static double absMax(double[] data) {
		return absMax(data, 0, data.length);
	}

	/**
	 * Find the maximum of the absolute values of all elements in the given subarray, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @param off
	 *            off
	 * @param len
	 *            len
	 * @return max
	 */
	public static double absMax(double[] data, int off, int len) {
		double max = Double.NaN;
		for (int i = off; i < off + len; i++) {
			if (Double.isNaN(data[i]))
				continue;
			double abs = Math.abs(data[i]);
			if (Double.isNaN(max) || abs > max)
				max = abs;
		}
		return max;
	}

	/**
	 * Find the minimum of all elements in the array, ignoring elements that are NaN.
	 * 
	 * @param data
	 *            data
	 * @return min
	 */
	public static double min(double[] data) {
		double min = Double.NaN;
		for (int i = 0; i < data.length; i++) {
			if (Double.isNaN(data[i]))
				continue;
			if (Double.isNaN(min) || data[i] < min)
				min = data[i];
		}
		return min;
	}

	public static int min(int[] data) {
		int min = data[0];
		for (int i = 1; i < data.length; i++) {
			if (data[i] < min)
				min = data[i];
		}
		return min;
	}

	public static double mean(double[] data) {
		return mean(data, 0, data.length - 1);
	}

	/**
	 * Compute the mean of all elements in the array. No missing values (NaN) are allowed.
	 * 
	 * @param data
	 *            data
	 * @param startIndex
	 *            start index
	 * @param endIndex
	 *            end index
	 * @throws IllegalArgumentException
	 *             if the array contains NaN values.
	 * @return mean
	 */
	public static double mean(double[] data, int startIndex, int endIndex) {
		double mean = 0;
		int total = 0;
		startIndex = Math.max(startIndex, 0);
		startIndex = Math.min(startIndex, data.length - 1);
		endIndex = Math.max(endIndex, 0);
		endIndex = Math.min(endIndex, data.length - 1);

		if (startIndex > endIndex)
			startIndex = endIndex;

		for (int i = startIndex; i <= endIndex; i++) {
			if (Double.isNaN(data[i]))
				throw new IllegalArgumentException("NaN not allowed in mean calculation");
			mean += data[i];
			total++;
		}
		mean /= total;
		return mean;
	}

	/**
	 * Compute the mean of all elements in the array with given indices. No missing values (NaN) are allowed.
	 * 
	 * @param data
	 *            data
	 * @param inds
	 *            inds
	 * @throws IllegalArgumentException
	 *             if the array contains NaN values.
	 * @return mean
	 */
	public static double mean(double[] data, int[] inds) {
		double mean = 0;
		for (int i = 0; i < inds.length; i++) {
			if (Double.isNaN(data[inds[i]]))
				throw new IllegalArgumentException("NaN not allowed in mean calculation");

			mean += data[inds[i]];
		}
		mean /= inds.length;
		return mean;
	}

	/**
	 * Compute the mean of all elements in the array. No missing values (NaN) are allowed.
	 * 
	 * @param data
	 *            data
	 * @param startIndex
	 *            start index
	 * @param endIndex
	 *            end index
	 * @throws IllegalArgumentException
	 *             if the array contains NaN values.
	 * @return mean
	 */
	public static float mean(float[] data, int startIndex, int endIndex) {
		float mean = 0;
		int total = 0;
		startIndex = Math.max(startIndex, 0);
		startIndex = Math.min(startIndex, data.length - 1);
		endIndex = Math.max(endIndex, 0);
		endIndex = Math.min(endIndex, data.length - 1);

		if (startIndex > endIndex)
			startIndex = endIndex;

		for (int i = startIndex; i <= endIndex; i++) {
			if (Float.isNaN(data[i]))
				throw new IllegalArgumentException("NaN not allowed in mean calculation");
			mean += data[i];
			total++;
		}
		mean /= total;
		return mean;
	}

	public static float mean(float[] data) {
		return mean(data, 0, data.length - 1);
	}

	/**
	 * Compute the mean of all elements in the array with given indices. No missing values (NaN) are allowed.
	 * 
	 * @param data
	 *            data
	 * @param inds
	 *            inds
	 * @throws IllegalArgumentException
	 *             if the array contains NaN values.
	 * @return mean
	 */
	public static float mean(float[] data, int[] inds) {
		float mean = 0;
		for (int i = 0; i < inds.length; i++) {
			if (Float.isNaN(data[inds[i]]))
				throw new IllegalArgumentException("NaN not allowed in mean calculation");

			mean += data[inds[i]];
		}
		mean /= inds.length;
		return mean;
	}

	/**
	 * Compute the mean of all elements in the array. this function can deal with NaNs
	 * 
	 * @param data
	 *            double[]
	 * @param opt
	 *            0: arithmetic mean, 1: geometric mean
	 * @return math.exp(mean)
	 */
	public static double mean(double[] data, int opt) {
		if (opt == 0) {
			int numData = 0;
			double mean = 0;
			for (int i = 0; i < data.length; i++) {
				if (!Double.isNaN(data[i])) {
					mean += data[i];
					numData++;
				}
			}
			mean /= numData;
			return mean;
		} else {
			int numData = 0;
			double mean = 0;
			for (int i = 0; i < data.length; i++) {
				if (!Double.isNaN(data[i])) {
					mean += Math.log(data[i]);
					numData++;
				}
			}
			mean = mean / numData;
			return Math.exp(mean);
		}

	}

	public static double standardDeviation(double[] data) {
		return standardDeviation(data, mean(data));
	}

	public static double standardDeviation(double[] data, double meanVal) {
		return standardDeviation(data, meanVal, 0, data.length - 1);
	}

	public static double standardDeviation(double[] data, double meanVal, int startIndex, int endIndex) {
		return Math.sqrt(variance(data, meanVal, startIndex, endIndex));
	}

	/**
	 * Compute the standard deviation of the given data, this function can deal with NaNs
	 * 
	 * @param data
	 *            double[]
	 * @param opt
	 *            0: normalizes with N-1, this provides the square root of best unbiased estimator of the variance, 1: normalizes
	 *            with N, this provides the square root of the second moment around the mean
	 * @return Math.sqrt(variance(data, opt))
	 */
	public static double standardDeviation(double[] data, int opt) {
		if (opt == 0)
			return Math.sqrt(variance(data, opt));
		else
			return Math.sqrt(variance(data, opt));
	}

	/**
	 * Compute the variance in the array. This function can deal with NaNs
	 * 
	 * @param data
	 *            double[]
	 * @param opt
	 *            0: normalizes with N-1, this provides the square root of best unbiased estimator of the variance, 1: normalizes
	 *            with N, this provides the square root of the second moment around the mean
	 * @return S / numData -1 if opt is 0, S / numData otherwise
	 */
	public static double variance(double[] data, int opt) {
		// Pseudocode from wikipedia, which cites Knuth:
		// n = 0
		// mean = 0
		// S = 0
		// foreach x in data:
		// n = n + 1
		// delta = x - mean
		// mean = mean + delta/n
		// S = S + delta*(x - mean) // This expression uses the new value of mean
		// end for
		// variance = S/(n - 1)
		double mean = 0;
		double S = 0;
		double numData = 0;
		for (int i = 0; i < data.length; i++) {
			if (!Double.isNaN(data[i])) {
				double delta = data[i] - mean;
				mean += delta / (numData + 1);
				S += delta * (data[i] - mean);
				numData++;
			}
		}
		if (opt == 0)
			return (S / (numData - 1));
		else
			return (S / numData);
	}

	public static double variance(double[] data) {
		return variance(data, mean(data));
	}

	public static float variance(float[] data) {
		return variance(data, mean(data));
	}

	public static double variance(double[] data, double meanVal) {
		return variance(data, meanVal, 0, data.length - 1);
	}

	public static float variance(float[] data, float meanVal) {
		return variance(data, meanVal, 0, data.length - 1);
	}

	public static float variance(float[] data, float meanVal, int startIndex, int endIndex) {
		double[] ddata = new double[data.length];
		for (int i = 0; i < data.length; i++)
			ddata[i] = data[i];

		return (float) variance(ddata, meanVal, startIndex, endIndex);
	}

	public static double variance(double[] data, double meanVal, int startIndex, int endIndex) {
		double var = 0.0;

		if (startIndex < 0)
			startIndex = 0;
		if (startIndex > data.length - 1)
			startIndex = data.length - 1;
		if (endIndex < startIndex)
			endIndex = startIndex;
		if (endIndex > data.length - 1)
			endIndex = data.length - 1;

		for (int i = startIndex; i <= endIndex; i++)
			var += (data[i] - meanVal) * (data[i] - meanVal);

		if (endIndex - startIndex > 1)
			var /= (endIndex - startIndex);

		return var;
	}

	public static double[] variance(double[][] x, double[] meanVector) {
		return variance(x, meanVector, true);
	}

	/**
	 * Returns the variance of rows or columns of matrix x
	 * 
	 * @param x
	 *            the matrix consisting of row vectors
	 * @param meanVector
	 *            the vector of mean values -- a column vector if row-wise variances are to be computed, or a row vector if
	 *            column-wise variances are to be calculated. param isAlongRows if true, compute the variance of x[0][0], x[1][0]
	 *            etc. given mean[0]; if false, compute the variances for the vectors x[0], x[1] etc. separately, given the
	 *            respective mean[0], mean[1] etc.
	 * @param isAlongRows
	 *            isAlongRows
	 * @return var
	 */
	public static double[] variance(double[][] x, double[] meanVector, boolean isAlongRows) {
		double[] var = null;

		if (x != null && x[0] != null && x[0].length > 0 && meanVector != null) {
			if (isAlongRows) {
				var = new double[x[0].length];
				int j, i;
				for (j = 0; j < x[0].length; j++) {
					for (i = 0; i < x.length; i++)
						var[j] += (x[i][j] - meanVector[j]) * (x[i][j] - meanVector[j]);

					var[j] /= (x.length - 1);
				}
			} else {
				var = new double[x.length];
				for (int i = 0; i < x.length; i++) {
					var[i] = variance(x[i], meanVector[i]);
				}
			}
		}

		return var;
	}

	public static double[] mean(double[][] x) {
		return mean(x, true);
	}

	public static double[] mean(double[][] x, boolean isAlongRows) {
		int[] indices = null;
		int i;

		if (isAlongRows) {
			indices = new int[x.length];
			for (i = 0; i < x.length; i++)
				indices[i] = i;
		} else {
			indices = new int[x[0].length];
			for (i = 0; i < x[0].length; i++)
				indices[i] = i;
		}

		return mean(x, isAlongRows, indices);
	}

	// If isAlongRows==true, the observations are row-by-row
	// if isAlongRows==false, they are column-by-column
	public static double[] mean(double[][] x, boolean isAlongRows, int[] indicesOfX) {
		double[] meanVector = null;
		int i, j;
		if (isAlongRows) {
			meanVector = new double[x[indicesOfX[0]].length];
			Arrays.fill(meanVector, 0.0);

			for (i = 0; i < indicesOfX.length; i++) {
				for (j = 0; j < x[indicesOfX[0]].length; j++)
					meanVector[j] += x[indicesOfX[i]][j];
			}

			for (j = 0; j < meanVector.length; j++)
				meanVector[j] /= indicesOfX.length;
		} else {
			meanVector = new double[x.length];
			Arrays.fill(meanVector, 0.0);

			for (i = 0; i < indicesOfX.length; i++) {
				for (j = 0; j < x.length; j++)
					meanVector[j] += x[j][indicesOfX[i]];
			}

			for (j = 0; j < meanVector.length; j++)
				meanVector[j] /= indicesOfX.length;
		}

		return meanVector;
	}

	// The observations are taken row by row
	public static double[][] covariance(double[][] x) {
		return covariance(x, true);
	}

	// The observations are taken row by row
	public static double[][] covariance(double[][] x, double[] meanVector) {
		return covariance(x, meanVector, true);
	}

	// If isAlongRows==true, the observations are row-by-row
	// if isAlongRows==false, they are column-by-column
	public static double[][] covariance(double[][] x, boolean isAlongRows) {
		double[] meanVector = mean(x, isAlongRows);

		return covariance(x, meanVector, isAlongRows);
	}

	public static double[][] covariance(double[][] x, double[] meanVector, boolean isAlongRows) {

		int[] indices = null;
		int i;

		if (isAlongRows) {
			indices = new int[x.length];
			for (i = 0; i < x.length; i++)
				indices[i] = i;
		} else {
			indices = new int[x[0].length];
			for (i = 0; i < x[0].length; i++)
				indices[i] = i;
		}

		return covariance(x, meanVector, isAlongRows, indices);
	}

	// If isAlongRows==true, the observations are row-by-row
	// if isAlongRows==false, they are column-by-column
	public static double[][] covariance(double[][] x, double[] meanVector, boolean isAlongRows, int[] indicesOfX) {
		int numObservations;
		int dimension;
		int i, j, p;
		double[][] cov = null;
		double[][] tmpMatrix = null;
		double[][] zeroMean = null;
		double[][] zeroMeanTranspoze = null;

		if (x != null && meanVector != null) {
			if (isAlongRows) {
				for (i = 0; i < indicesOfX.length; i++)
					assert meanVector.length == x[indicesOfX[i]].length;

				numObservations = indicesOfX.length;
				dimension = x[indicesOfX[0]].length;

				cov = new double[dimension][dimension];
				tmpMatrix = new double[dimension][dimension];
				zeroMean = new double[dimension][1];
				double[] tmpVector;

				for (i = 0; i < dimension; i++)
					Arrays.fill(cov[i], 0.0);

				for (i = 0; i < numObservations; i++) {
					tmpVector = subtract(x[indicesOfX[i]], meanVector);
					zeroMean = transpoze(tmpVector);
					zeroMeanTranspoze = transpoze(zeroMean);

					tmpMatrix = matrixProduct(zeroMean, zeroMeanTranspoze);
					cov = add(cov, tmpMatrix);
				}

				cov = divide(cov, numObservations - 1);
			} else {
				assert meanVector.length == x.length;
				numObservations = indicesOfX.length;

				for (i = 1; i < indicesOfX.length; i++)
					assert x[indicesOfX[i]].length == x[indicesOfX[0]].length;

				dimension = x.length;

				cov = transpoze(covariance(transpoze(x), meanVector, true, indicesOfX));
			}
		}

		return cov;
	}

	/***
	 * Sample correlation coefficient Ref: http://en.wikipedia.org/wiki/Correlation_and_dependence
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @return r
	 */
	public static double correlation(double[] x, double[] y) {

		if (x.length == y.length) {
			// mean
			double mx = MathUtils.mean(x);
			double my = MathUtils.mean(y);
			// standard deviation
			double sx = Math.sqrt(MathUtils.variance(x));
			double sy = Math.sqrt(MathUtils.variance(y));

			int n = x.length;
			double nval = 0.0;
			for (int i = 0; i < n; i++) {
				nval += (x[i] - mx) * (y[i] - my);
			}
			double r = nval / ((n - 1) * sx * sy);

			return r;
		} else
			throw new IllegalArgumentException("vectors of different size");
	}

	public static double[] diagonal(double[][] x) {
		double[] d = null;
		int dim = x.length;
		int i;
		for (i = 1; i < dim; i++)
			assert x[i].length == dim;

		if (x != null) {
			d = new double[dim];

			for (i = 0; i < x.length; i++)
				d[i] = x[i][i];
		}

		return d;
	}

	public static double[][] toDiagonalMatrix(double[] x) {
		double[][] m = null;

		if (x != null && x.length > 0) {
			m = new double[x.length][x.length];
			int i;
			for (i = 0; i < x.length; i++)
				Arrays.fill(m[i], 0.0);

			for (i = 0; i < x.length; i++)
				m[i][i] = x[i];
		}

		return m;
	}

	public static double[][] transpoze(double[] x) {
		double[][] y = new double[x.length][1];
		for (int i = 0; i < x.length; i++)
			y[i][0] = x[i];

		return y;
	}

	public static double[][] transpoze(double[][] x) {
		double[][] y = null;

		if (x != null) {
			int i, j;
			int rowSizex = x.length;
			int colSizex = x[0].length;
			for (i = 1; i < rowSizex; i++)
				assert x[i].length == colSizex;

			y = new double[colSizex][rowSizex];
			for (i = 0; i < rowSizex; i++) {
				for (j = 0; j < colSizex; j++)
					y[j][i] = x[i][j];
			}
		}

		return y;
	}

	public static ComplexNumber[][] transpoze(ComplexNumber[][] x) {
		ComplexNumber[][] y = null;

		if (x != null) {
			int i, j;
			int rowSizex = x.length;
			int colSizex = x[0].length;
			for (i = 1; i < rowSizex; i++)
				assert x[i].length == colSizex;

			y = new ComplexNumber[colSizex][rowSizex];
			for (i = 0; i < rowSizex; i++) {
				for (j = 0; j < colSizex; j++)
					y[j][i] = new ComplexNumber(x[i][j]);
			}
		}

		return y;
	}

	public static ComplexNumber[][] hermitianTranspoze(ComplexNumber[][] x) {
		ComplexNumber[][] y = null;

		if (x != null) {
			int i, j;
			int rowSizex = x.length;
			int colSizex = x[0].length;
			for (i = 1; i < rowSizex; i++)
				assert x[i].length == colSizex;

			y = new ComplexNumber[colSizex][rowSizex];
			for (i = 0; i < rowSizex; i++) {
				for (j = 0; j < colSizex; j++)
					y[j][i] = new ComplexNumber(x[i][j].real, -1.0 * x[i][j].imag);
			}
		}

		return y;
	}

	public static ComplexNumber[][] diagonalComplexMatrix(double[] diag) {
		ComplexNumber[][] x = null;
		int N = diag.length;
		if (N > 0) {
			x = new ComplexNumber[N][N];
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					if (i == j)
						x[i][j] = new ComplexNumber(diag[i], 0.0);
					else
						x[i][j] = new ComplexNumber(0.0, 0.0);
				}
			}
		}

		return x;
	}

	public static double[][] diagonalMatrix(double[] diag) {
		double[][] x = null;
		int N = diag.length;
		if (N > 0) {
			x = new double[N][N];
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					if (i == j)
						x[i][j] = diag[i];
					else
						x[i][j] = 0.0;
				}
			}
		}

		return x;
	}

	public static ComplexNumber ampPhase2ComplexNumber(double amp, double phaseInRadians) {
		return new ComplexNumber(amp * Math.cos(phaseInRadians), amp * Math.sin(phaseInRadians));
	}

	public static ComplexNumber[] polar2complex(double[] amps, float[] phasesInRadian) {
		if (amps.length != phasesInRadian.length) {
			throw new IllegalArgumentException("Arrays must have same length, but are " + amps.length + " vs. "
					+ phasesInRadian.length);
		}

		ComplexNumber[] comps = new ComplexNumber[amps.length];
		for (int i = 0; i < amps.length; i++)
			comps[i] = ampPhase2ComplexNumber(amps[i], phasesInRadian[i]);

		return comps;
	}

	public static ComplexNumber[] polar2complex(double[] amps, double[] phasesInRadian) {
		if (amps.length != phasesInRadian.length) {
			throw new IllegalArgumentException("Arrays must have same length, but are " + amps.length + " vs. "
					+ phasesInRadian.length);
		}

		ComplexNumber[] comps = new ComplexNumber[amps.length];
		for (int i = 0; i < amps.length; i++)
			comps[i] = ampPhase2ComplexNumber(amps[i], phasesInRadian[i]);

		return comps;
	}

	public static double[] add(double[] x, double[] y) {
		assert x.length == y.length;
		double[] z = new double[x.length];
		for (int i = 0; i < x.length; i++)
			z[i] = x[i] + y[i];

		return z;
	}

	public static double[] add(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] + b;
		}
		return c;
	}

	public static double[] subtract(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] - b[i];
		}
		return c;
	}

	public static double[] subtract(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] - b;
		}
		return c;
	}

	public static double[] multiply(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] * b[i];
		}
		return c;
	}

	public static float[] multiply(float[] a, float[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] * b[i];
		}
		return c;
	}

	public static double[] multiply(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] * b;
		}
		return c;
	}

	public static float[] multiply(float[] a, float b) {
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] * b;
		}
		return c;
	}

	/**
	 * Returns the multiplicative inverse (element-wise 1/x) of an array
	 * 
	 * @param a
	 *            array to invert
	 * @return a new array of the same size as <b>a</b>, in which each element is equal to the multiplicative inverse of the
	 *         corresponding element in <b>a</b>
	 * @throws IllegalArgumentException
	 *             if the array is null
	 */
	public static double[] invert(double[] a) throws IllegalArgumentException {
		if (a == null) {
			throw new IllegalArgumentException("Argument cannot be null");
		}
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = 1.0 / a[i];
		}
		return c;
	}

	/**
	 * @param a
	 *            a
	 * @return c
	 * @see #invert(double[])
	 */
	public static float[] invert(float[] a) {
		if (a == null) {
			throw new IllegalArgumentException("Argument cannot be null");
		}
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = 1.0f / a[i];
		}
		return c;
	}

	public static ComplexNumber[] multiplyComplex(ComplexNumber[] a, double b) {
		ComplexNumber[] c = new ComplexNumber[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = MathUtils.multiply(b, a[i]);
		}
		return c;
	}

	public static ComplexNumber complexConjugate(ComplexNumber x) {
		return new ComplexNumber(x.real, -1.0 * x.imag);
	}

	public static ComplexNumber complexConjugate(double xReal, double xImag) {
		return new ComplexNumber(xReal, -1.0 * xImag);
	}

	public static ComplexNumber addComplex(ComplexNumber x1, ComplexNumber x2) {
		return new ComplexNumber(x1.real + x2.real, x1.imag + x2.imag);
	}

	public static ComplexNumber addComplex(ComplexNumber x, double yReal, double yImag) {
		return new ComplexNumber(x.real + yReal, x.imag + yImag);
	}

	public static ComplexNumber addComplex(double yReal, double yImag, ComplexNumber x) {
		return new ComplexNumber(x.real + yReal, x.imag + yImag);
	}

	public static ComplexNumber addComplex(double xReal, double xImag, double yReal, double yImag) {
		return new ComplexNumber(xReal + yReal, xImag + yImag);
	}

	public static ComplexNumber subtractComplex(ComplexNumber x1, ComplexNumber x2) {
		return new ComplexNumber(x1.real - x2.real, x1.imag - x2.imag);
	}

	public static ComplexNumber subtractComplex(ComplexNumber x, double yReal, double yImag) {
		return new ComplexNumber(x.real - yReal, x.imag - yImag);
	}

	public static ComplexNumber subtractComplex(double yReal, double yImag, ComplexNumber x) {
		return new ComplexNumber(yReal - x.real, yImag - x.imag);
	}

	public static ComplexNumber subtractComplex(double xReal, double xImag, double yReal, double yImag) {
		return new ComplexNumber(xReal - yReal, xImag - yImag);
	}

	public static ComplexNumber multiplyComplex(ComplexNumber x1, ComplexNumber x2) {
		return new ComplexNumber(x1.real * x2.real - x1.imag * x2.imag, x1.real * x2.imag + x1.imag * x2.real);
	}

	public static ComplexNumber multiplyComplex(ComplexNumber x, double yReal, double yImag) {
		return new ComplexNumber(x.real * yReal - x.imag * yImag, x.real * yImag + x.imag * yReal);
	}

	public static ComplexNumber multiplyComplex(double yReal, double yImag, ComplexNumber x) {
		return new ComplexNumber(x.real * yReal - x.imag * yImag, x.real * yImag + x.imag * yReal);
	}

	public static ComplexNumber multiplyComplex(double xReal, double xImag, double yReal, double yImag) {
		return new ComplexNumber(xReal * yReal - xImag * yImag, xReal * yImag + xImag * yReal);
	}

	public static ComplexNumber multiply(double x1, ComplexNumber x2) {
		return new ComplexNumber(x1 * x2.real, x1 * x2.imag);
	}

	public static ComplexNumber divideComplex(ComplexNumber x, double yReal, double yImag) {
		double denum = magnitudeComplexSquared(yReal, yImag);

		return new ComplexNumber((x.real * yReal + x.imag * yImag) / denum, (x.imag * yReal - x.real * yImag) / denum);
	}

	public static ComplexNumber divideComplex(double yReal, double yImag, ComplexNumber x) {
		double denum = magnitudeComplexSquared(x.real, x.imag);

		return new ComplexNumber((yReal * x.real + yImag * x.imag) / denum, (yImag * x.real - yReal * x.imag) / denum);
	}

	public static ComplexNumber divideComplex(ComplexNumber x1, ComplexNumber x2) {
		double denum = magnitudeComplexSquared(x2.real, x2.imag);

		return new ComplexNumber((x1.real * x2.real + x1.imag * x2.imag) / denum, (x1.imag * x2.real - x1.real * x2.imag) / denum);
	}

	public static ComplexNumber divideComplex(double xReal, double xImag, double yReal, double yImag) {
		double denum = magnitudeComplexSquared(yReal, yImag);

		return new ComplexNumber((xReal * yReal + xImag * yImag) / denum, (xImag * yReal - xReal * yImag) / denum);
	}

	public static ComplexNumber divide(ComplexNumber x1, double x2) {
		return new ComplexNumber(x1.real / x2, x1.imag / x2);
	}

	public static ComplexNumber divide(double x1, ComplexNumber x2) {
		return divideComplex(x1, 0.0, x2);
	}

	public static double magnitudeComplexSquared(ComplexNumber x) {
		return x.real * x.real + x.imag * x.imag;
	}

	public static double magnitudeComplexSquared(double xReal, double xImag) {
		return xReal * xReal + xImag * xImag;
	}

	public static double magnitudeComplex(ComplexNumber x) {
		return Math.sqrt(magnitudeComplexSquared(x));
	}

	public static double[] magnitudeComplex(ComplexNumber[] xs) {
		double[] mags = new double[xs.length];

		for (int i = 0; i < xs.length; i++)
			mags[i] = magnitudeComplex(xs[i]);

		return mags;
	}

	public static double[] magnitudeComplex(ComplexArray x) {
		assert x.real.length == x.imag.length;
		double[] mags = new double[x.real.length];

		for (int i = 0; i < x.real.length; i++)
			mags[i] = magnitudeComplex(new ComplexNumber(x.real[i], x.imag[i]));

		return mags;
	}

	public static double magnitudeComplex(double xReal, double xImag) {
		return Math.sqrt(magnitudeComplexSquared(xReal, xImag));
	}

	public static double phaseInRadians(ComplexNumber x) {
		/*
		 * double modul = MathUtils.magnitudeComplex(x); // modulus double phase = Math.atan2(x.imag, x.real); // use atan2: theta
		 * ranges from [-pi,pi]
		 * 
		 * if (x.imag<0.0) // lower half plane (Im<0), needs shifting { phase += MathUtils.TWOPI; // shift by adding 2pi to lower
		 * half plane
		 * 
		 * // fix the discontinuity between phase = 0 and phase = 2pi if (x.real>0.0 && x.imag<0.0 && Math.abs(x.imag)<1e-10)
		 * phase = 0.0; }
		 * 
		 * return phase;
		 */

		return Math.atan2(x.imag, x.real);
	}

	public static float phaseInRadiansFloat(ComplexNumber x) {
		return (float) phaseInRadians(x);
	}

	public static double phaseInRadians(double xReal, double xImag) {
		return phaseInRadians(new ComplexNumber(xReal, xImag));
	}

	public static double[] phaseInRadians(ComplexNumber[] xs) {
		double[] phases = new double[xs.length];

		for (int i = 0; i < xs.length; i++)
			phases[i] = phaseInRadians(xs[i]);

		return phases;
	}

	public static float[] phaseInRadiansFloat(ComplexNumber[] xs) {
		float[] phases = new float[xs.length];

		for (int i = 0; i < xs.length; i++)
			phases[i] = phaseInRadiansFloat(xs[i]);

		return phases;
	}

	public static double[] phaseInRadians(ComplexArray x) {
		assert x.real.length == x.imag.length;

		double[] phases = new double[x.real.length];

		for (int i = 0; i < x.real.length; i++)
			phases[i] = phaseInRadians(x.real[i], x.imag[i]);

		return phases;
	}

	// Returns a+jb such that a+jb=r.exp(j.theta) where theta is in radians
	public static ComplexNumber complexNumber(double r, double theta) {
		return new ComplexNumber(r * Math.cos(theta), r * Math.sin(theta));
	}

	public static double[] divide(double[] a, double[] b) {
		if (a == null || b == null || a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] / b[i];
		}
		return c;
	}

	public static double[] divide(double[] a, double b) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] / b;
		}
		return c;
	}

	// Returns the summ of two matrices, i.e. x+y
	// x and y should be of same size
	public static double[][] add(double[][] x, double[][] y) {
		double[][] z = null;

		if (x != null && y != null) {
			int i, j;
			assert x.length == y.length;
			for (i = 0; i < x.length; i++) {
				assert x[i].length == x[0].length;
				assert x[i].length == y[i].length;
			}

			z = new double[x.length][x[0].length];

			for (i = 0; i < x.length; i++) {
				for (j = 0; j < x[i].length; j++)
					z[i][j] = x[i][j] + y[i][j];
			}
		}

		return z;
	}

	// Returns the difference of two matrices, i.e. x-y
	// x and y should be of same size
	public static double[][] subtract(double[][] x, double[][] y) {
		double[][] z = null;

		if (x != null && y != null) {
			int i, j;
			assert x.length == y.length;
			for (i = 0; i < x.length; i++) {
				assert x[i].length == x[0].length;
				assert x[i].length == y[i].length;
			}

			z = new double[x.length][x[0].length];

			for (i = 0; i < x.length; i++) {
				for (j = 0; j < x[i].length; j++)
					z[i][j] = x[i][j] - y[i][j];
			}
		}

		return z;
	}

	// Returns multiplication of matrix entries with a constant, i.e. ax
	// x and y should be of same size
	public static double[][] multiply(double a, double[][] x) {
		double[][] z = null;

		if (x != null) {
			int i, j;
			for (i = 1; i < x.length; i++)
				assert x[i].length == x[0].length;

			z = new double[x.length][x[0].length];

			for (i = 0; i < x.length; i++) {
				for (j = 0; j < x[i].length; j++)
					z[i][j] = a * x[i][j];
			}
		}

		return z;
	}

	// Returns the division of matrix entries with a constant, i.e. x/a
	// x and y should be of same size
	public static double[][] divide(double[][] x, double a) {
		return multiply(1.0 / a, x);
	}

	// Matrix of size NxM multiplied by an appropriate sized vector, i.e. Mx1, returns a vector of size Nx1
	public static double[] matrixProduct(double[][] x, double[] y) {
		double[][] y2 = new double[y.length][1];
		int i;
		for (i = 0; i < y.length; i++)
			y2[i][0] = y[i];

		y2 = matrixProduct(x, y2);

		double[] y3 = new double[y2.length];
		for (i = 0; i < y2.length; i++)
			y3[i] = y2[i][0];

		return y3;
	}

	public static double[] matrixProduct(double[][] x, float[] y) {
		double[][] y2 = new double[y.length][1];
		int i;
		for (i = 0; i < y.length; i++)
			y2[i][0] = y[i];

		y2 = matrixProduct(x, y2);

		double[] y3 = new double[y2.length];
		for (i = 0; i < y2.length; i++)
			y3[i] = y2[i][0];

		return y3;
	}

	public static ComplexNumber[] matrixProduct(ComplexNumber[][] x, ComplexNumber[] y) {
		ComplexNumber[][] y2 = new ComplexNumber[y.length][1];
		int i;
		for (i = 0; i < y.length; i++)
			y2[i][0] = new ComplexNumber(y[i]);

		y2 = matrixProduct(x, y2);

		ComplexNumber[] y3 = new ComplexNumber[y2.length];
		for (i = 0; i < y2.length; i++)
			y3[i] = new ComplexNumber(y2[i][0]);

		return y3;
	}

	public static ComplexNumber[] matrixProduct(ComplexNumber[][] x, double[] y) {
		ComplexNumber[][] y2 = new ComplexNumber[y.length][1];
		int i;
		for (i = 0; i < y.length; i++)
			y2[i][0] = new ComplexNumber(y[i], 0.0);

		y2 = matrixProduct(x, y2);

		ComplexNumber[] y3 = new ComplexNumber[y2.length];
		for (i = 0; i < y2.length; i++)
			y3[i] = new ComplexNumber(y2[i][0]);

		return y3;
	}

	// Vector of size N is multiplied with matrix of size NxM
	// Returns a matrix of size NxM
	public static double[][] matrixProduct(double[] x, double[][] y) {
		double[][] x2 = new double[x.length][1];
		int i;
		for (i = 0; i < x.length; i++)
			x2[i][0] = x[i];

		return matrixProduct(x2, y);
	}

	public static ComplexNumber[][] matrixProduct(ComplexNumber[] x, ComplexNumber[][] y) {
		ComplexNumber[][] x2 = new ComplexNumber[x.length][1];
		int i;
		for (i = 0; i < x.length; i++)
			x2[i][0] = new ComplexNumber(x[i]);

		return matrixProduct(x2, y);
	}

	// This is a "*" product --> should return a matrix provided that the sizes are appropriate
	public static double[][] matrixProduct(double[][] x, double[][] y) {
		double[][] z = null;

		if (x != null && y != null) {
			if (x.length == 1 && y.length == 1) // Special case -- diagonal matrix multiplication, returns a diagonal matrix
			{
				assert x[0].length == y[0].length;
				z = new double[1][x[0].length];
				for (int i = 0; i < x[0].length; i++)
					z[0][i] = x[0][i] * y[0][i];
			} else {
				int i, j, m;
				int rowSizex = x.length;
				int colSizex = x[0].length;
				int rowSizey = y.length;
				int colSizey = y[0].length;
				for (i = 1; i < x.length; i++)
					assert x[i].length == colSizex;
				for (i = 1; i < y.length; i++)
					assert y[i].length == colSizey;
				assert colSizex == rowSizey;

				z = new double[rowSizex][colSizey];
				double tmpSum;
				for (i = 0; i < rowSizex; i++) {
					for (j = 0; j < colSizey; j++) {
						tmpSum = 0.0;
						for (m = 0; m < x[i].length; m++)
							tmpSum += x[i][m] * y[m][j];

						z[i][j] = tmpSum;
					}
				}
			}
		}

		return z;
	}

	// This is a "*" product --> should return a matrix provided that the sizes are appropriate
	public static ComplexNumber[][] matrixProduct(ComplexNumber[][] x, ComplexNumber[][] y) {
		ComplexNumber[][] z = null;

		if (x != null && y != null) {
			if (x.length == 1 && y.length == 1) // Special case -- diagonal matrix multiplication, returns a diagonal matrix
			{
				assert x[0].length == y[0].length;
				z = new ComplexNumber[1][x[0].length];
				for (int i = 0; i < x[0].length; i++)
					z[0][i] = multiplyComplex(x[0][i], y[0][i]);
			} else {
				int i, j, m;
				int rowSizex = x.length;
				int colSizex = x[0].length;
				int rowSizey = y.length;
				int colSizey = y[0].length;
				for (i = 1; i < x.length; i++)
					assert x[i].length == colSizex;
				for (i = 1; i < y.length; i++)
					assert y[i].length == colSizey;
				assert colSizex == rowSizey;

				z = new ComplexNumber[rowSizex][colSizey];

				/**
				 * Marc Schröder, 3 July 2009: The following implementation used up about 93% of total processing time. Replacing
				 * it with a less elegant but more efficient implementation:
				 * 
				 * ComplexNumber tmpSum; for (i=0; i<rowSizex; i++) { for (j=0; j<colSizey; j++) { tmpSum = new ComplexNumber(0.0,
				 * 0.0); for (m=0; m<x[i].length; m++) tmpSum = addComplex(tmpSum, multiplyComplex(x[i][m],y[m][j]));
				 * 
				 * z[i][j] = new ComplexNumber(tmpSum); } }
				 */

				for (i = 0; i < rowSizex; i++) {
					for (j = 0; j < colSizey; j++) {
						float real = 0f, imag = 0f;
						for (m = 0; m < x[i].length; m++) {
							ComplexNumber x1 = x[i][m];
							ComplexNumber x2 = y[m][j];
							real += x1.real * x2.real - x1.imag * x2.imag;
							imag += x1.real * x2.imag + x1.imag * x2.real;
						}

						z[i][j] = new ComplexNumber(real, imag);
					}
				}
			}
		}

		return z;
	}

	// "x" product of two vectors
	public static double[][] vectorProduct(double[] x, boolean isColumnVectorX, double[] y, boolean isColumnVectorY) {
		double[][] xx = null;
		double[][] yy = null;
		int i;
		if (isColumnVectorX) {
			xx = new double[x.length][1];
			for (i = 0; i < x.length; i++)
				xx[i][0] = x[i];
		} else {
			xx = new double[1][x.length];
			System.arraycopy(x, 0, xx[0], 0, x.length);
		}

		if (isColumnVectorY) {
			yy = new double[y.length][1];
			for (i = 0; i < y.length; i++)
				yy[i][0] = y[i];
		} else {
			yy = new double[1][y.length];
			System.arraycopy(y, 0, yy[0], 0, y.length);
		}

		return matrixProduct(xx, yy);
	}

	public static double dotProduct(double[] x, double[] y) {
		assert x.length == y.length;

		double tmpSum = 0.0;
		for (int i = 0; i < x.length; i++)
			tmpSum += x[i] * y[i];

		return tmpSum;
	}

	public static double[][] dotProduct(double[][] x, double[][] y) {
		double[][] z = null;
		assert x.length == y.length;
		int numRows = x.length;
		int numCols = x[0].length;
		int i;
		for (i = 1; i < numRows; i++) {
			assert numCols == x[i].length;
			assert numCols == y[i].length;
		}

		if (x != null) {
			int j;
			z = new double[numRows][numCols];
			for (i = 0; i < numRows; i++) {
				for (j = 0; j < numCols; j++)
					z[i][j] = x[i][j] * y[i][j];
			}
		}

		return z;
	}

	/**
	 * Convert energy from linear scale to db SPL scale (comparing energies to the minimum audible energy, one Pascal squared).
	 * 
	 * @param energy
	 *            in time or frequency domain, on a linear energy scale
	 * @return energy on a db scale, or NaN if energy is less than or equal to 0.
	 */
	public static double dbSPL(double energy) {
		if (energy <= 0)
			return Double.NaN;
		else
			return 10 * log10(energy / PASCALSQUARE);
	}

	public static double[] dbSPL(double[] energies) {
		return multiply(log10(divide(energies, PASCALSQUARE)), 10);
	}

	/**
	 * Convert energy from linear scale to db scale.
	 * 
	 * @param energy
	 *            in time or frequency domain, on a linear energy scale
	 * @return energy on a db scale, or NaN if energy is less than or equal to 0.
	 */
	public static double db(double energy) {
		if (energy <= 1e-80)
			return -200.0;
		else
			return 10 * log10(energy);
	}

	public static double amp2db(double amp) {
		if (amp <= 1e-80)
			return -200.0;
		else
			return 20 * log10(amp);
	}

	public static double amp2neper(double amp) {
		if (amp <= 1e-80)
			return -200.0;
		else
			return Math.log(amp);
	}

	public static double[] db(double[] energies) {
		return multiply(log10(energies), 10);
	}

	public static double[] abs(ComplexArray c) {
		int len = Math.min(c.real.length, c.imag.length);

		return abs(c, 0, len - 1);
	}

	public static double[] abs(ComplexNumber[] x) {
		double[] absMags = null;

		if (x.length > 0) {
			absMags = new double[x.length];

			for (int i = 0; i < x.length; i++)
				absMags[i] = magnitudeComplex(x[i]);
		}

		return absMags;
	}

	public static double[] abs(ComplexArray c, int startInd, int endInd) {
		if (startInd < 0)
			startInd = 0;
		if (startInd > Math.min(c.real.length - 1, c.imag.length - 1))
			startInd = Math.min(c.real.length - 1, c.imag.length - 1);
		if (endInd < startInd)
			endInd = startInd;
		if (endInd > Math.min(c.real.length - 1, c.imag.length - 1))
			endInd = Math.min(c.real.length - 1, c.imag.length - 1);

		double[] absVals = new double[endInd - startInd + 1];
		for (int i = startInd; i <= endInd; i++)
			absVals[i - startInd] = Math.sqrt(c.real[i] * c.real[i] + c.imag[i] * c.imag[i]);

		return absVals;
	}

	public static double[] amp2db(double[] amps) {
		return multiply(log10(amps), 20);
	}

	public static double[] amp2neper(double[] amps) {
		double[] newAmps = new double[amps.length];
		for (int i = 0; i < amps.length; i++)
			newAmps[i] = amp2neper(amps[i]);

		return newAmps;
	}

	public static double[] dft2ampdb(ComplexArray c) {
		return dft2ampdb(c, 0, c.real.length - 1);
	}

	public static double[] dft2ampdb(ComplexArray c, int startInd, int endInd) {
		if (startInd < 0)
			startInd = 0;
		if (startInd > Math.min(c.real.length - 1, c.imag.length - 1))
			startInd = Math.min(c.real.length - 1, c.imag.length - 1);
		if (endInd < startInd)
			endInd = startInd;
		if (endInd > Math.min(c.real.length - 1, c.imag.length - 1))
			endInd = Math.min(c.real.length - 1, c.imag.length - 1);

		double[] dbs = new double[endInd - startInd + 1];
		for (int i = startInd; i <= endInd; i++)
			dbs[i - startInd] = amp2db(Math.sqrt(c.real[i] * c.real[i] + c.imag[i] * c.imag[i]));

		return dbs;
	}

	/**
	 * Convert energy from db scale to linear scale.
	 * 
	 * @param dbEnergy
	 *            in time or frequency domain, on a db energy scale
	 * @return energy on a linear scale.
	 */
	public static double db2linear(double dbEnergy) {
		if (Double.isNaN(dbEnergy))
			return 0.;
		else
			return exp10(dbEnergy / 10);
	}

	public static double[] db2linear(double[] dbEnergies) {
		return exp10(divide(dbEnergies, 10.0));
	}

	public static double[] linear2db(double[] linears) {
		return multiply(log10(linears), 10.0);
	}

	public static float db2amp(float dbAmplitude) {
		if (Float.isNaN(dbAmplitude))
			return 0.0f;
		else
			return (float) (Math.pow(10.0, dbAmplitude / 20));
	}

	public static double db2amp(double dbAmplitude) {
		if (Double.isNaN(dbAmplitude))
			return 0.;
		else
			return Math.pow(10.0, dbAmplitude / 20);
	}

	public static float[] db2amp(float[] dbAmplitudes) {
		float[] amps = new float[dbAmplitudes.length];
		for (int i = 0; i < dbAmplitudes.length; i++)
			amps[i] = db2amp(dbAmplitudes[i]);

		return amps;
	}

	public static double[] db2amp(double[] dbAmplitudes) {
		double[] amps = new double[dbAmplitudes.length];
		for (int i = 0; i < dbAmplitudes.length; i++)
			amps[i] = db2amp(dbAmplitudes[i]);

		return amps;
	}

	public static float radian2degrees(float rad) {
		return (float) ((rad / MathUtils.TWOPI) * 360.0f);
	}

	public static double radian2degrees(double rad) {
		return (rad / MathUtils.TWOPI) * 360.0;
	}

	public static float degrees2radian(float deg) {
		return (float) ((deg / 360.0) * MathUtils.TWOPI);
	}

	public static double degrees2radian(double deg) {
		return ((deg / 360.0) * MathUtils.TWOPI);
	}

	/**
	 * Build the sum of the squared difference of all elements with the same index numbers in the arrays. Any NaN values in either
	 * a or b are ignored in computing the error.
	 * 
	 * @param a
	 *            a
	 * @param b
	 *            a
	 * @return sum
	 */
	public static double sumSquaredError(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			double delta = a[i] - b[i];
			if (!Double.isNaN(delta)) {
				sum += delta * delta;
			}
		}
		return sum;
	}

	public static double log10(double x) {
		return Math.log(x) / LOG10;
	}

	public static double[] log(double[] a) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = Math.log(a[i]);
		}
		return c;
	}

	// A special log operation
	// The values smaller than or equal to minimumValue are set to fixedValue
	// The values greater than minimumValue are converted to log
	public static double[] log(double[] a, double minimumValue, double fixedValue) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			if (a[i] > minimumValue)
				c[i] = Math.log(a[i]);
			else
				c[i] = fixedValue;
		}
		return c;
	}

	public static double[] log10(double[] a) {
		double[] c = null;

		if (a != null) {
			c = new double[a.length];

			for (int i = 0; i < a.length; i++)
				c[i] = log10(a[i]);
		}

		return c;
	}

	public static double exp10(double x) {
		return Math.exp(LOG10 * x);
	}

	public static double[] exp(double[] a) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = Math.exp(a[i]);
		}
		return c;
	}

	public static double[] exp10(double[] a) {
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = exp10(a[i]);
		}
		return c;
	}

	public static float[] add(float[] a, float[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] + b[i];
		}
		return c;
	}

	public static float[] add(float[] a, float b) {
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] + b;
		}
		return c;
	}

	public static float[] subtract(float[] a, float[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] - b[i];
		}
		return c;
	}

	public static float[] subtract(float[] a, float b) {
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i] - b;
		}
		return c;
	}

	public static double euclidianLength(float[] a) {
		double len = 0.;
		for (int i = 0; i < a.length; i++) {
			len += a[i] * a[i];
		}
		return Math.sqrt(len);
	}

	public static double euclidianLength(double[] a) {
		double len = 0.;
		for (int i = 0; i < a.length; i++) {
			len += a[i] * a[i];
		}
		return Math.sqrt(len);
	}

	/**
	 * Convert a pair of arrays from cartesian (x, y) coordinates to polar (r, phi) coordinates. Phi will be in radians, i.e. a
	 * full circle is two pi.
	 * 
	 * @param x
	 *            as input, the x coordinate; as output, the r coordinate;
	 * @param y
	 *            as input, the y coordinate; as output, the phi coordinate.
	 */
	public static void toPolarCoordinates(double[] x, double[] y) {
		if (x.length != y.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		for (int i = 0; i < x.length; i++) {
			double r = Math.sqrt(x[i] * x[i] + y[i] * y[i]);
			double phi = Math.atan2(y[i], x[i]);
			x[i] = r;
			y[i] = phi;
		}
	}

	/**
	 * Convert a pair of arrays from polar (r, phi) coordinates to cartesian (x, y) coordinates. Phi is in radians, i.e. a whole
	 * circle is two pi.
	 * 
	 * @param r
	 *            as input, the r coordinate; as output, the x coordinate;
	 * @param phi
	 *            as input, the phi coordinate; as output, the y coordinate.
	 */
	public static void toCartesianCoordinates(double[] r, double[] phi) {
		if (r.length != phi.length) {
			throw new IllegalArgumentException("Arrays must be equal length");
		}
		for (int i = 0; i < r.length; i++) {
			double x = r[i] * Math.cos(phi[i]);
			double y = r[i] * Math.sin(phi[i]);
			r[i] = x;
			phi[i] = y;
		}
	}

	/**
	 * For a given angle in radians, return the equivalent angle in the range [-PI, PI].
	 * 
	 * @param angle
	 *            angle
	 * @return (angle + PI) % (-TWOPI) + PI
	 */
	public static double angleToDefaultAngle(double angle) {
		return (angle + Math.PI) % (-TWOPI) + Math.PI;
	}

	/**
	 * For each of an array of angles (in radians), return the equivalent angle in the range [-PI, PI].
	 * 
	 * @param angle
	 *            angle
	 * 
	 */
	public static void angleToDefaultAngle(double[] angle) {
		for (int i = 0; i < angle.length; i++) {
			angle[i] = angleToDefaultAngle(angle[i]);
		}
	}

	/**
	 * This is the Java source code for a Levinson Recursion. from http://www.nauticom.net/www/jdtaft/JavaLevinson.htm
	 * 
	 * @param r
	 *            contains the autocorrelation lags as input [r(0)...r(m)].
	 * @param m
	 *            m
	 * @return the array of whitening coefficients
	 */
	public static double[] levinson(double[] r, int m) {
		// The matrix l is unit lower triangular.
		// It's i-th row contains upon completion the i-th prediction error filter,
		// with the coefficients in reverse order. The vector e contains upon
		// completion the prediction errors.
		// The last section extracts the maximum length whitening filter
		// coefficients from matrix l.
		int i;
		int j;
		int k;
		double gap;
		double gamma;
		double e[] = new double[m + 1];
		double l[][] = new double[m + 1][m + 1];
		double[] coeffs = new double[m + 1];

		/* compute recursion */
		for (i = 0; i <= m; i++) {
			for (j = i + 1; j <= m; j++) {
				l[i][j] = 0.;
			}
		}
		l[0][0] = 1.;
		l[1][1] = 1.;
		l[1][0] = -r[1] / r[0];
		e[0] = r[0];
		e[1] = e[0] * (1. - l[1][0] * l[1][0]);
		for (i = 2; i <= m; i++) {
			gap = 0.;
			for (k = 0; k <= i - 1; k++) {
				gap += r[k + 1] * l[i - 1][k];
			}
			gamma = gap / e[i - 1];
			l[i][0] = -gamma;
			for (k = 1; k <= i - 1; k++) {
				l[i][k] = l[i - 1][k - 1] - gamma * l[i - 1][i - 1 - k];
			}
			l[i][i] = 1.;
			e[i] = e[i - 1] * (1. - gamma * gamma);
		}
		/* extract length-m whitening filter coefficients */
		coeffs[0] = 1.;
		for (i = 1; i <= m; i++) {
			coeffs[i] = l[m][m - i];
		}
		/*
		 * double sum = 0.; for (i = 0; i < m; i++) { sum += coeffs[i]; } for (i = 0; i < m; i++) { coeffs[i] = coeffs[i] / sum; }
		 */
		return coeffs;
	}

	// Modified(Generalized) Levinson recursion to solve the matrix equation R*h=c
	// where R is a complex-valued Toeplitz matrix
	//
	// r : Complex vector of length N containing the first row of the correlation matrix R
	// c : Complex vector containing the right handside of the equation
	public static ComplexNumber[] levinson(ComplexNumber[] r, ComplexNumber[] c) {
		assert r.length == c.length;

		int M = r.length; // Order of equations to be solved
		ComplexNumber[] a = new ComplexNumber[M]; // Temporary array for computations
		ComplexNumber[] b = new ComplexNumber[M]; // Temporary array for computations
		ComplexNumber[] h = new ComplexNumber[M]; // Output
		ComplexNumber alpha, beta, gamma, xk, q;
		int i;

		// Check for zero input
		if (r[0].real == 0.0 && r[0].imag == 0.0) {
			for (i = 1; i <= M; i++)
				h[i - 1] = new ComplexNumber(0.0, 0.0);

			return h;
		}

		// First order solution
		a[0] = new ComplexNumber(1.0, 0.0);
		beta = new ComplexNumber(r[1]);
		alpha = new ComplexNumber(r[0]);
		h[0] = MathUtils.divideComplex(c[0], r[0]);
		if (M == 1)
			return h;

		// Second order solution
		gamma = MathUtils.multiplyComplex(h[0], r[1]);
		xk = MathUtils.divideComplex(MathUtils.multiply(-1.0, beta), alpha);
		a[1] = new ComplexNumber(xk);
		alpha = MathUtils.addComplex(alpha, MathUtils.multiplyComplex(xk, MathUtils.complexConjugate(beta)));
		q = MathUtils.divideComplex((MathUtils.subtractComplex(c[1], gamma)), MathUtils.complexConjugate(alpha));
		h[0] = MathUtils.addComplex(h[0], MathUtils.multiplyComplex(q, MathUtils.complexConjugate(a[1])));
		h[1] = new ComplexNumber(q);
		if (M == 2)
			return h;

		// Recursion for orders >= 3
		beta = MathUtils.addComplex(r[2], MathUtils.multiplyComplex(a[1], r[1]));
		gamma = MathUtils.addComplex(MathUtils.multiplyComplex(h[0], r[2]), MathUtils.multiplyComplex(h[1], r[1]));
		int M1 = M - 1;

		for (int N = 2; N <= M1; N++) {
			xk = MathUtils.divideComplex(MathUtils.multiply(-1.0, beta), MathUtils.complexConjugate(alpha));

			for (i = 2; i <= N; i++)
				b[i - 1] = MathUtils
						.addComplex(a[i - 1], MathUtils.multiplyComplex(xk, MathUtils.complexConjugate(a[N + 1 - i])));

			for (i = 2; i <= N; i++)
				a[i - 1] = new ComplexNumber(b[i - 1]);

			a[N] = new ComplexNumber(xk);
			alpha = MathUtils.addComplex(alpha, MathUtils.multiplyComplex(xk, MathUtils.complexConjugate(beta)));
			q = MathUtils.divideComplex(MathUtils.subtractComplex(c[N], gamma), MathUtils.complexConjugate(alpha));
			h[0] = MathUtils.addComplex(h[0], MathUtils.multiplyComplex(q, MathUtils.complexConjugate(a[N])));
			for (i = 2; i <= N; i++)
				h[i - 1] = MathUtils.addComplex(h[i - 1], MathUtils.multiplyComplex(q, MathUtils.complexConjugate(a[N + 1 - i])));

			h[N] = new ComplexNumber(q);

			if (N == M1)
				return h;

			gamma = new ComplexNumber(0.0, 0.0);
			beta = new ComplexNumber(0.0, 0.0);

			for (i = 1; i <= N + 1; i++) {
				beta = MathUtils.addComplex(beta, MathUtils.multiplyComplex(a[i - 1], r[N - i + 2]));
				gamma = MathUtils.addComplex(gamma, MathUtils.multiplyComplex(h[i - 1], r[N - i + 2]));
			}
		}

		return h;
	}

	public static float[] interpolate(float[] x, int newLength) {
		if (x != null) {
			int i;
			double[] xDouble = new double[x.length];
			for (i = 0; i < x.length; i++)
				xDouble[i] = x[i];

			double[] yDouble = interpolate(xDouble, newLength);

			float[] y = new float[yDouble.length];
			for (i = 0; i < yDouble.length; i++)
				y[i] = (float) yDouble[i];

			return y;
		} else
			return null;
	}

	// Performs linear interpolation to increase or decrease the size of array x to newLength
	public static double[] interpolate(double[] x, int newLength) {
		double[] y = null;
		if (newLength > 0) {
			int N = x.length;
			if (N == 1) {
				y = new double[1];
				y[0] = x[0];
				return y;
			} else if (newLength == 1) {
				y = new double[1];
				int ind = (int) Math.floor(N * 0.5 + 0.5);
				ind = Math.max(1, ind);
				ind = Math.min(ind, N);
				y[0] = x[ind - 1];
				return y;
			} else {
				y = new double[newLength];
				int leftInd;
				double ratio = ((double) x.length) / newLength;
				for (int i = 0; i < newLength; i++) {
					leftInd = (int) Math.floor(i * ratio);
					if (leftInd < x.length - 1)
						y[i] = interpolatedSample(leftInd, i * ratio, leftInd + 1, x[leftInd], x[leftInd + 1]);
					else {
						if (leftInd > 0)
							y[i] = interpolatedSample(leftInd, i * ratio, leftInd + 1, x[leftInd], 2 * x[leftInd]
									- x[leftInd - 1]);
						else
							y[i] = x[leftInd];
					}
				}
			}
		}

		return y;
	}

	// Performs linear interpolation to increase or decrease the size of array x to newLength
	public static ComplexNumber[] interpolate(ComplexNumber[] x, int newLength) {
		ComplexNumber[] y = null;
		if (newLength > 0) {
			int N = x.length;
			if (N == 1) {
				y = new ComplexNumber[1];
				y[0] = new ComplexNumber(x[0]);
				return y;
			} else if (newLength == 1) {
				y = new ComplexNumber[1];
				int ind = (int) Math.floor(N * 0.5 + 0.5);
				ind = Math.max(1, ind);
				ind = Math.min(ind, N);
				y[0] = new ComplexNumber(x[ind - 1]);
				return y;
			} else {
				y = new ComplexNumber[newLength];
				int leftInd;
				double ratio = ((double) x.length) / newLength;
				double yReal, yImag;
				for (int i = 0; i < newLength; i++) {
					leftInd = (int) Math.floor(i * ratio);
					if (leftInd < x.length - 1) {
						yReal = interpolatedSample(leftInd, i * ratio, leftInd + 1, x[leftInd].real, x[leftInd + 1].real);
						yImag = interpolatedSample(leftInd, i * ratio, leftInd + 1, x[leftInd].imag, x[leftInd + 1].imag);
						y[i] = new ComplexNumber(yReal, yImag);
					} else {
						if (leftInd > 0) {
							yReal = interpolatedSample(leftInd, i * ratio, leftInd + 1, x[leftInd].real, 2 * x[leftInd].real
									- x[leftInd - 1].real);
							yImag = interpolatedSample(leftInd, i * ratio, leftInd + 1, x[leftInd].imag, 2 * x[leftInd].imag
									- x[leftInd - 1].imag);
							y[i] = new ComplexNumber(yReal, yImag);
						} else
							y[i] = new ComplexNumber(x[leftInd]);
					}
				}
			}
		}

		return y;
	}

	// Linear interpolation of values in xVals at indices xInds to give values at indices xInds2
	public static double[] interpolate(int[] xInds, double[] xVals, int[] xInds2) {
		double[] xVals2 = new double[xInds2.length];
		assert xInds.length == xVals.length;

		for (int i = 0; i < xInds2.length; i++) {
			int closestInd = MathUtils.findClosest(xInds, xInds2[i]);
			if (closestInd == xInds2[i])
				xVals2[i] = xVals[closestInd];
			else if (closestInd > xInds2[i]) {
				if (closestInd > 0)
					xVals2[i] = MathUtils.interpolatedSample(xInds[closestInd - 1], xInds2[i], xInds[closestInd],
							xVals[closestInd - 1], xVals[closestInd]);
				else {
					if (closestInd + 1 < xVals.length)
						xVals2[i] = MathUtils.interpolatedSample(xInds[closestInd] - 1, xInds2[i], xInds[closestInd], 2
								* xVals[closestInd] - xVals[closestInd + 1], xVals[closestInd]);
					else
						xVals2[i] = xVals[closestInd];
				}
			} else {
				if (closestInd + 1 < xVals.length)
					xVals2[i] = MathUtils.interpolatedSample(xInds[closestInd], xInds2[i], xInds[closestInd + 1],
							xVals[closestInd], xVals[closestInd + 1]);
				else {
					if (closestInd - 1 >= 0)
						xVals2[i] = MathUtils.interpolatedSample(xInds[closestInd], xInds2[i], xInds[closestInd] + 1,
								xVals[closestInd], 2 * xVals[closestInd] - xVals[closestInd - 1]);
					else
						xVals2[i] = xVals[closestInd];
				}
			}
		}

		return xVals2;
	}

	public static double interpolatedSample(double xStart, double xVal, double xEnd, double yStart, double yEnd) {
		return (xVal - xStart) * (yEnd - yStart) / (xEnd - xStart) + yStart;
	}

	public static int getMax(int[] x) {
		int maxx = x[0];
		for (int i = 1; i < x.length; i++) {
			if (x[i] > maxx)
				maxx = x[i];
		}

		return maxx;
	}

	public static int getMinIndex(int[] x) {
		return getMinIndex(x, 0);
	}

	public static int getMinIndex(int[] x, int startInd) {
		return getMinIndex(x, startInd, x.length - 1);
	}

	public static int getMinIndex(int[] x, int startInd, int endInd) {
		return getExtremaIndex(x, false, startInd, endInd);
	}

	public static int getMaxIndex(int[] x) {
		return getMaxIndex(x, 0);
	}

	public static int getMaxIndex(int[] x, int startInd) {
		return getMaxIndex(x, startInd, x.length - 1);
	}

	public static int getMaxIndex(int[] x, int startInd, int endInd) {
		return getExtremaIndex(x, true, startInd, endInd);
	}

	public static int getExtremaIndex(int[] x, boolean isMax) {
		return getExtremaIndex(x, isMax, 0);
	}

	public static int getExtremaIndex(int[] x, boolean isMax, int startInd) {
		return getExtremaIndex(x, isMax, startInd, x.length - 1);
	}

	public static int getExtremaIndex(int[] x, boolean isMax, int startInd, int endInd) {
		int extrema = x[0];
		int extremaInd = 0;
		if (startInd < 0)
			startInd = 0;
		if (endInd > x.length - 1)
			endInd = x.length - 1;
		if (startInd > endInd)
			startInd = endInd;

		if (isMax) {
			for (int i = startInd; i <= endInd; i++) {
				if (x[i] > extrema) {
					extrema = x[i];
					extremaInd = i;
				}
			}
		} else {
			for (int i = startInd; i <= endInd; i++) {
				if (x[i] < extrema) {
					extrema = x[i];
					extremaInd = i;
				}
			}
		}

		return extremaInd;
	}

	public static int getMinIndex(float[] x) {
		return getMinIndex(x, 0);
	}

	public static int getMinIndex(float[] x, int startInd) {
		return getMinIndex(x, startInd, x.length - 1);
	}

	public static int getMinIndex(float[] x, int startInd, int endInd) {
		return getExtremaIndex(x, false, startInd, endInd);
	}

	public static int getMaxIndex(float[] x) {
		return getMaxIndex(x, 0);
	}

	public static int getMaxIndex(float[] x, int startInd) {
		return getMaxIndex(x, startInd, x.length - 1);
	}

	public static int getMaxIndex(float[] x, int startInd, int endInd) {
		return getExtremaIndex(x, true, startInd, endInd);
	}

	public static int getExtremaIndex(float[] x, boolean isMax) {
		return getExtremaIndex(x, isMax, 0);
	}

	public static int getExtremaIndex(float[] x, boolean isMax, int startInd) {
		return getExtremaIndex(x, isMax, startInd, x.length - 1);
	}

	public static int getExtremaIndex(float[] x, boolean isMax, int startInd, int endInd) {
		float extrema = x[0];
		int extremaInd = 0;
		if (startInd < 0)
			startInd = 0;
		if (endInd > x.length - 1)
			endInd = x.length - 1;
		if (startInd > endInd)
			startInd = endInd;

		if (isMax) {
			for (int i = startInd; i <= endInd; i++) {
				if (x[i] > extrema) {
					extrema = x[i];
					extremaInd = i;
				}
			}
		} else {
			for (int i = startInd; i <= endInd; i++) {
				if (x[i] < extrema) {
					extrema = x[i];
					extremaInd = i;
				}
			}
		}

		return extremaInd;
	}

	public static int getMinIndex(double[] x) {
		return getMinIndex(x, 0);
	}

	public static int getMinIndex(double[] x, int startInd) {
		return getMinIndex(x, startInd, x.length - 1);
	}

	public static int getMinIndex(double[] x, int startInd, int endInd) {
		return getExtremaIndex(x, false, startInd, endInd);
	}

	public static int getMaxIndex(double[] x) {
		return getMaxIndex(x, 0);
	}

	public static int getMaxIndex(double[] x, int[] inds) {
		double[] tmp = new double[inds.length];
		for (int i = 0; i < inds.length; i++)
			tmp[i] = x[inds[i]];

		int maxIndInd = MathUtils.getMaxIndex(tmp);

		return inds[maxIndInd];
	}

	public static int getMaxIndex(double[] x, int startInd) {
		return getMaxIndex(x, startInd, x.length - 1);
	}

	public static int getMaxIndex(double[] x, int startInd, int endInd) {
		return getExtremaIndex(x, true, startInd, endInd);
	}

	public static int getExtremaIndex(double[] x, boolean isMax) {
		return getExtremaIndex(x, isMax, 0);
	}

	public static int getExtremaIndex(double[] x, boolean isMax, int startInd) {
		return getExtremaIndex(x, isMax, startInd, x.length - 1);
	}

	public static int getExtremaIndex(double[] x, boolean isMax, int startInd, int endInd) {
		double extrema = x[0];
		int extremaInd = startInd;
		if (startInd < 0)
			startInd = 0;
		if (endInd > x.length - 1)
			endInd = x.length - 1;
		if (startInd > endInd)
			startInd = endInd;

		if (isMax) {
			for (int i = startInd; i <= endInd; i++) {
				if (x[i] > extrema) {
					extrema = x[i];
					extremaInd = i;
				}
			}
		} else {
			for (int i = startInd; i <= endInd; i++) {
				if (x[i] < extrema) {
					extrema = x[i];
					extremaInd = i;
				}
			}
		}

		return extremaInd;
	}

	public static double getMax(double[] x) {
		double maxx = x[0];
		for (int i = 1; i < x.length; i++) {
			if (x[i] > maxx)
				maxx = x[i];
		}

		return maxx;
	}

	public static float getMax(float[] x) {
		float maxx = x[0];
		for (int i = 1; i < x.length; i++) {
			if (x[i] > maxx)
				maxx = x[i];
		}

		return maxx;
	}

	public static int getMin(int[] x) {
		int minn = x[0];
		for (int i = 1; i < x.length; i++) {
			if (x[i] < minn)
				minn = x[i];
		}

		return minn;
	}

	public static double getMin(double[] x) {
		double minn = x[0];
		for (int i = 1; i < x.length; i++) {
			if (x[i] < minn)
				minn = x[i];
		}

		return minn;
	}

	public static float getMin(float[] x) {
		float maxx = x[0];
		for (int i = 1; i < x.length; i++) {
			if (x[i] < maxx)
				maxx = x[i];
		}

		return maxx;
	}

	public static double getAbsMax(double[] x) {
		return getAbsMax(x, 0, x.length - 1);
	}

	public static double getAbsMax(double[] x, int startInd, int endInd) {
		double maxx = Math.abs(x[startInd]);
		for (int i = startInd + 1; i <= endInd; i++) {
			if (Math.abs(x[i]) > maxx)
				maxx = Math.abs(x[i]);
		}

		return maxx;
	}

	// Return the local peak index for values x[startInd],x[startInd+1],...,x[endInd]
	// Note that the returned index is in the range [startInd,endInd]
	// If there is no local peak, -1 is returned. This means that the peak is either at [startInd] or [endInd].
	// However, it is the responsibility of the calling function to further check this situation as the returned index
	// will be -1 in both cases
	public static int getAbsMaxInd(double[] x, int startInd, int endInd) {
		int index = -1;
		startInd = Math.max(startInd, 0);
		endInd = Math.min(endInd, x.length - 1);
		double max = x[startInd];

		for (int i = startInd + 1; i < endInd - 1; i++) {
			if (x[i] > max && x[i] > x[i - 1] && x[i] > x[i + 1]) {
				max = x[i];
				index = i;
			}
		}

		return index;
	}

	// Return an array where each entry is set to val
	public static double[] filledArray(double val, int len) {
		double[] x = null;

		if (len > 0) {
			x = new double[len];
			for (int i = 0; i < len; i++)
				x[i] = val;
		}

		return x;
	}

	// Return an array where each entry is set to val
	public static float[] filledArray(float val, int len) {
		float[] x = null;

		if (len > 0) {
			x = new float[len];
			for (int i = 0; i < len; i++)
				x[i] = val;
		}

		return x;
	}

	// Return an array where each entry is set to val
	public static int[] filledArray(int val, int len) {
		int[] x = null;

		if (len > 0) {
			x = new int[len];
			for (int i = 0; i < len; i++)
				x[i] = val;
		}

		return x;
	}

	// Return an array filled with 0´s
	public static double[] zeros(int len) {
		return filledArray(0.0, len);
	}

	// Return an array filled with 1´s
	public static double[] ones(int len) {
		return filledArray(1.0, len);
	}

	// Return an array filled with 0´s
	public static int[] zerosInt(int len) {
		return filledArray(0, len);
	}

	// Return an array filled with 1´s
	public static int[] onesInt(int len) {
		return filledArray(1, len);
	}

	// Return an array filled with 0´s
	public static float[] zerosFloat(int len) {
		return filledArray(0.0f, len);
	}

	// Return an array filled with 1´s
	public static float[] onesFloat(int len) {
		return filledArray(1.0f, len);
	}

	public static int[] find(int[] x, int comparator, int val) {
		double[] xd = new double[x.length];
		for (int i = 0; i < x.length; i++)
			xd[i] = (double) x[i];

		return find(xd, comparator, val);
	}

	public static int[] findAnd(int[] x, int comparator1, int val1, int comparator2, int val2) {
		double[] xd = new double[x.length];
		for (int i = 0; i < x.length; i++)
			xd[i] = (double) x[i];

		return findAnd(xd, comparator1, val1, comparator2, val2);
	}

	public static int[] findOr(int[] x, int comparator1, int val1, int comparator2, int val2) {
		double[] xd = new double[x.length];
		for (int i = 0; i < x.length; i++)
			xd[i] = (double) x[i];

		return findOr(xd, comparator1, val1, comparator2, val2);
	}

	// Returns the indices that satisfy both comparator1, val1 and comparator2, val2
	public static int[] findAnd(double[] x, int comparator1, double val1, int comparator2, double val2) {
		int[] indices = null;
		int[] indices1 = find(x, comparator1, val1);
		int[] indices2 = find(x, comparator2, val2);

		if (indices1 != null && indices2 != null) {
			int total = 0;
			int i, j;
			for (i = 0; i < indices1.length; i++) {
				for (j = 0; j < indices2.length; j++) {
					if (indices1[i] == indices2[j]) {
						total++;
						break;
					}
				}
			}

			if (total > 0) {
				indices = new int[total];
				int count = 0;
				for (i = 0; i < indices1.length; i++) {
					for (j = 0; j < indices2.length; j++) {
						if (indices1[i] == indices2[j]) {
							indices[count++] = indices1[i];
							break;
						}
					}

					if (count >= total)
						break;
				}
			}
		}

		return indices;
	}

	// Returns the indices that satisfy both comparator1, val1 or comparator2, val2
	public static int[] findOr(double[] x, int comparator1, double val1, int comparator2, double val2) {
		int[] indices = null;
		int[] indices1 = find(x, comparator1, val1);
		int[] indices2 = find(x, comparator2, val2);

		if (indices1 != null || indices2 != null) {
			int total = 0;
			if (indices1 != null)
				total += indices1.length;
			if (indices2 != null)
				total += indices2.length;
			int[] tmpIndices = new int[total];
			int currentPos = 0;
			if (indices1 != null) {
				System.arraycopy(indices1, 0, tmpIndices, 0, indices1.length);
				currentPos = indices1.length;
			}
			if (indices2 != null)
				System.arraycopy(indices2, 0, tmpIndices, currentPos, indices2.length);

			indices = StringUtils.getDifferentItemsList(tmpIndices);
		}

		return indices;
	}

	public static double[] findValues(double[] x, int comparator, double val) {
		int[] inds = find(x, comparator, val);
		double[] vals = null;

		if (inds != null) {
			vals = new double[inds.length];
			for (int i = 0; i < inds.length; i++)
				vals[i] = x[inds[i]];
		}

		return vals;
	}

	// Returns the indices satisying the condition specificed by the comparator and val
	public static int[] find(double[] x, int comparator, double val) {
		int[] inds = null;
		int totalFound = 0;

		switch (comparator) {
		case EQUALS:
			for (int i = 0; i < x.length; i++) {
				if (x[i] == val)
					totalFound++;
			}
			break;
		case GREATER_THAN:
			for (int i = 0; i < x.length; i++) {
				if (x[i] > val)
					totalFound++;
			}
			break;
		case GREATER_THAN_OR_EQUALS:
			for (int i = 0; i < x.length; i++) {
				if (x[i] >= val)
					totalFound++;
			}
			break;
		case LESS_THAN:
			for (int i = 0; i < x.length; i++) {
				if (x[i] < val)
					totalFound++;
			}
			break;
		case LESS_THAN_OR_EQUALS:
			for (int i = 0; i < x.length; i++) {
				if (x[i] <= val)
					totalFound++;
			}
			break;
		case NOT_EQUALS:
			for (int i = 0; i < x.length; i++) {
				if (x[i] != val)
					totalFound++;
			}
			break;
		}

		if (totalFound > 0) {
			int currentInd = 0;
			inds = new int[totalFound];

			switch (comparator) {
			case EQUALS:
				for (int i = 0; i < x.length; i++) {
					if (x[i] == val) {
						inds[currentInd++] = i;
						totalFound++;
					}
				}
				break;
			case GREATER_THAN:
				for (int i = 0; i < x.length; i++) {
					if (x[i] > val) {
						inds[currentInd++] = i;
						totalFound++;
					}
				}
				break;
			case GREATER_THAN_OR_EQUALS:
				for (int i = 0; i < x.length; i++) {
					if (x[i] >= val) {
						inds[currentInd++] = i;
						totalFound++;
					}
				}
				break;
			case LESS_THAN:
				for (int i = 0; i < x.length; i++) {
					if (x[i] < val) {
						inds[currentInd++] = i;
						totalFound++;
					}
				}
				break;
			case LESS_THAN_OR_EQUALS:
				for (int i = 0; i < x.length; i++) {
					if (x[i] <= val) {
						inds[currentInd++] = i;
						totalFound++;
					}
				}
				break;
			case NOT_EQUALS:
				for (int i = 0; i < x.length; i++) {
					if (x[i] != val) {
						inds[currentInd++] = i;
						totalFound++;
					}
				}
				break;
			}
		}

		return inds;
	}

	public static double[] interpolate_linear(int[] x, double[] y, int[] xi) {
		assert (x.length == y.length);

		double[] yi = new double[xi.length];
		int i, j;
		boolean bFound;
		double alpha;

		for (i = 0; i < xi.length; i++) {
			bFound = false;
			for (j = 0; j < x.length - 1; j++) {
				if (xi[i] >= x[j] && xi[i] < x[j + 1]) {
					bFound = true;
					break;
				}
			}

			if (bFound) {
				alpha = (((double) xi[i]) - x[j]) / (x[j + 1] - x[j]);
				yi[i] = (1 - alpha) * y[j] + alpha * y[j + 1];
			}
		}

		if (xi[xi.length - 1] == x[x.length - 1])
			yi[xi.length - 1] = y[x.length - 1];

		return yi;
	}

	public static int CheckLimits(int val, int minVal, int maxVal) {
		int ret = val;

		if (ret < minVal)
			ret = minVal;

		if (ret > maxVal)
			ret = maxVal;

		return ret;
	}

	public static double CheckLimits(double val, double minVal, double maxVal) {
		double ret = val;

		if (ret < minVal)
			ret = minVal;

		if (ret > maxVal)
			ret = maxVal;

		return ret;
	}

	public static float CheckLimits(float val, float minVal, float maxVal) {
		float ret = val;

		if (ret < minVal)
			ret = minVal;

		if (ret > maxVal)
			ret = maxVal;

		return ret;
	}

	// Find the extremum points that are larger/smaller than numLefNs and numRightNs neighbours and larger/smaller than the given
	// th value
	public static int[] getExtrema(double[] x, int numLeftN, int numRightN, boolean isMaxima) {
		return getExtrema(x, numLeftN, numRightN, isMaxima, 0);
	}

	public static int[] getExtrema(double[] x, int numLeftN, int numRightN, boolean isMaxima, int startInd) {
		return getExtrema(x, numLeftN, numRightN, isMaxima, startInd, x.length - 1);
	}

	public static int[] getExtrema(double[] x, int numLeftN, int numRightN, boolean isMaxima, int startInd, int endInd) {
		double th;

		if (isMaxima)
			th = MathUtils.getMin(x) - 1.0;
		else
			th = MathUtils.getMax(x) + 1.0;

		return getExtrema(x, numLeftN, numRightN, isMaxima, startInd, endInd, th);
	}

	public static int[] getExtrema(double[] x, int numLeftN, int numRightN, boolean isMaxima, int startInd, int endInd, double th) {
		int[] numLeftNs = new int[x.length];
		int[] numRightNs = new int[x.length];
		Arrays.fill(numLeftNs, numLeftN);
		Arrays.fill(numRightNs, numRightN);

		return getExtrema(x, numLeftNs, numRightNs, isMaxima, startInd, endInd, th);
	}

	public static int[] getExtrema(double[] x, int[] numLeftNs, int[] numRightNs, boolean isMaxima) {
		return getExtrema(x, numLeftNs, numRightNs, isMaxima, 0);
	}

	public static int[] getExtrema(double[] x, int[] numLeftNs, int[] numRightNs, boolean isMaxima, int startInd) {
		return getExtrema(x, numLeftNs, numRightNs, isMaxima, startInd, x.length - 1);
	}

	public static int[] getExtrema(double[] x, int[] numLeftNs, int[] numRightNs, boolean isMaxima, int startInd, int endInd) {
		double th;

		if (isMaxima)
			th = MathUtils.getMin(x) - 1.0;
		else
			th = MathUtils.getMax(x) + 1.0;

		return getExtrema(x, numLeftNs, numRightNs, isMaxima, startInd, endInd, th);
	}

	public static int[] getExtrema(double[] x, int[] numLeftNs, int[] numRightNs, boolean isMaxima, int startInd, int endInd,
			double th) {
		int[] tmpInds = new int[x.length];
		int[] inds = null;
		int total = 0;

		int i, j;
		boolean bExtremum;

		if (startInd < 0)
			startInd = 0;
		if (endInd > x.length - 1)
			endInd = x.length - 1;
		if (startInd > endInd)
			startInd = endInd;

		if (isMaxima) // Search for maxima
		{
			for (i = startInd; i <= endInd; i++) {
				if (x[i] > th) {
					bExtremum = true;

					if (numLeftNs == null || i - numLeftNs[i] >= 0) {
						if (numLeftNs != null) {
							for (j = i - numLeftNs[i]; j < i; j++) {
								if (x[i] < x[j]) {
									bExtremum = false;
									break;
								}
							}
						}

						if (bExtremum) {
							if (numRightNs != null) {
								if (i + numRightNs[i] < x.length) {
									for (j = i + 1; j <= i + numRightNs[i]; j++) {
										if (x[i] < x[j]) {
											bExtremum = false;
											break;
										}
									}
								} else
									bExtremum = false;
							}
						}
					} else
						bExtremum = false;
				} else
					bExtremum = false;

				if (bExtremum)
					tmpInds[total++] = i;
			}
		} else // Search for minima
		{
			for (i = startInd; i <= endInd; i++) {
				if (x[i] < th) {
					bExtremum = true;
					if (numLeftNs == null || i - numLeftNs[i] >= 0) {
						if (numLeftNs != null) {
							for (j = i - numLeftNs[i]; j < i; j++) {
								if (x[i] > x[j]) {
									bExtremum = false;
									break;
								}
							}
						}

						if (bExtremum) {
							if (numRightNs != null) {
								if (i + numRightNs[i] < x.length) {
									for (j = i + 1; j <= i + numRightNs[i]; j++) {
										if (x[i] > x[j]) {
											bExtremum = false;
											break;
										}
									}
								} else
									bExtremum = false;
							}
						}
					} else
						bExtremum = false;
				} else
					bExtremum = false;

				if (bExtremum)
					tmpInds[total++] = i;
			}
		}

		if (total > 0) {
			inds = new int[total];
			System.arraycopy(tmpInds, 0, inds, 0, total);
		}

		return inds;
	}

	// Returns an array of values selected randomly in the interval [0.0,1.0)
	public static double[] getRandoms(int len) {
		return getRandoms(len, 0.0, 1.0);
	}

	// Returns an array of values selected randomly in the interval minVal and maxVal
	public static double[] getRandoms(int len, double minVal, double maxVal) {
		double[] x = null;

		if (len > 0) {
			x = new double[len];
			if (minVal > maxVal) {
				double tmp = minVal;
				minVal = maxVal;
				maxVal = tmp;
			}

			for (int i = 0; i < len; i++)
				x[i] = Math.random() * (maxVal - minVal) + minVal;
		}

		return x;
	}

	// Return the zero-based index of the entry of x which is closest to val
	public static int findClosest(float[] x, float val) {
		int ind = -1;
		if (x != null && x.length > 0) {
			float minDiff = Math.abs(x[0] - val);
			float tmpDiff;
			ind = 0;

			for (int i = 1; i < x.length; i++) {
				tmpDiff = Math.abs(x[i] - val);
				if (tmpDiff < minDiff) {
					minDiff = tmpDiff;
					ind = i;
				}
			}
		}

		return ind;
	}

	// Return the zero-based index of the entry of x which is closest to val
	public static int findClosest(int[] x, int val) {
		int ind = -1;
		if (x != null && x.length > 0) {
			int minDiff = Math.abs(x[0] - val);
			int tmpDiff;
			ind = 0;

			for (int i = 1; i < x.length; i++) {
				tmpDiff = Math.abs(x[i] - val);
				if (tmpDiff < minDiff) {
					minDiff = tmpDiff;
					ind = i;
				}
			}
		}

		return ind;
	}

	public static float unwrap(float phaseInRadians, float prevPhaseInRadians) {
		float unwrappedPhaseInRadians = phaseInRadians;

		while (Math.abs(unwrappedPhaseInRadians - prevPhaseInRadians) > 0.5 * TWOPI) {
			if (unwrappedPhaseInRadians > prevPhaseInRadians)
				unwrappedPhaseInRadians -= TWOPI;
			else
				unwrappedPhaseInRadians += TWOPI;
		}

		return unwrappedPhaseInRadians;
	}

	// Unwarps phaseInDegrees to range [lowestDegree, lowestDegree+360.0)
	public static float unwrapToRange(float phaseInDegrees, float lowestDegree) {
		float retVal = phaseInDegrees;
		if (retVal < lowestDegree) {
			while (retVal < lowestDegree)
				retVal += 360.0f;
		} else if (retVal >= lowestDegree + 360.0f) {
			while (retVal >= lowestDegree + 360.0f)
				retVal -= 360.0f;
		}

		return retVal;
	}

	public static double db2neper(double db) {
		return 20.0 * db / Math.log(10.0);
	}

	public static double[] db2neper(double[] dbs) {
		double[] nepers = null;

		if (dbs != null && dbs.length > 0) {
			nepers = new double[dbs.length];

			for (int i = 0; i < nepers.length; i++)
				nepers[i] = db2neper(dbs[i]);
		}

		return nepers;
	}

	public static double neper2db(double neper) {
		return (neper * Math.log(10.0)) / 20.0;
	}

	public static double[] neper2db(double[] nepers) {
		double[] dbs = null;

		if (nepers != null && nepers.length > 0) {
			dbs = new double[nepers.length];

			for (int i = 0; i < dbs.length; i++)
				dbs[i] = neper2db(nepers[i]);
		}

		return dbs;
	}

	public static double neper2linear(double neper) {
		return Math.exp(neper);
	}

	public static double[] neper2linear(double[] nepers) {
		double[] lins = null;

		if (nepers != null && nepers.length > 0) {
			lins = new double[nepers.length];

			for (int i = 0; i < lins.length; i++)
				lins[i] = neper2linear(nepers[i]);
		}

		return lins;
	}

	public static float[] sinc(float[] x, float N) {
		float[] y = null;

		if (x.length > 0) {
			y = new float[x.length];
			for (int i = 0; i < y.length; i++)
				y[i] = sinc(x[i], N);
		}

		return y;

	}

	public static float sinc(float x, float N) {
		return (float) (Math.sin(N * 0.5 * x) / (N * Math.sin(0.5 * x)));
	}

	public static double sinc(double x, double N) {
		return Math.sin(N * 0.5 * x) / (N * Math.sin(0.5 * x));
	}

	public static float[] sinc(float[] x) {
		float[] y = null;

		if (x.length > 0) {
			y = new float[x.length];
			for (int i = 0; i < y.length; i++)
				y[i] = sinc(2 * x[i], (float) (Math.PI));
		}

		return y;
	}

	public static double[] sinc(double[] x) {
		double[] y = null;

		if (x.length > 0) {
			y = new double[x.length];
			for (int i = 0; i < y.length; i++)
				y[i] = sinc(2 * x[i], Math.PI);
		}

		return y;
	}

	public static float sinc(float x) {
		return sinc(2 * x, (float) (Math.PI));
	}

	public static double sinc(double x) {
		return sinc(2 * x, Math.PI);
	}

	// Returns the index of the smallest element that is larger than %percentSmallerThan of the data in x
	// It simply sorts the data in x and then finds the smallest value that is larger than
	// the [percentSmallerThan/100.0*(x.length-1)]th entry
	public static double getSortedValue(double[] x, double percentSmallerThan) {
		int retInd = -1;

		Vector<Double> v = new Vector<Double>();
		for (int i = 0; i < x.length; i++)
			v.add(x[i]);

		Collections.sort(v);

		int index = (int) Math.floor(percentSmallerThan / 100.0 * (x.length - 1) + 0.5);
		index = Math.max(0, index);
		index = Math.min(index, x.length - 1);

		return ((Double) (v.get(index))).doubleValue();
	}

	// Factorial design of all possible paths
	// totalItemsInNodes is a vector containing the total number of element at each node,
	// The output is the zero-based indices of elements in successive nodes covering all possible paths
	// from the first node to the last
	// Note that all elements of totalItemsInNodes should be greater than 0 (otherwise it is assumed that the corresponding
	// element is 1)
	public static int[][] factorialDesign(int[] totalItemsInNodes) {
		int totalPaths = 1;

		int i, j;
		for (i = 0; i < totalItemsInNodes.length; i++) {
			if (totalItemsInNodes[i] > 0)
				totalPaths *= totalItemsInNodes[i];
		}

		int[][] pathInds = new int[totalPaths][totalItemsInNodes.length];
		int[] currentPath = new int[totalItemsInNodes.length];

		int count = 0;

		Arrays.fill(currentPath, 0);
		System.arraycopy(currentPath, 0, pathInds[count++], 0, currentPath.length);

		while (count < totalPaths) {
			for (i = currentPath.length - 1; i >= 0; i--) {
				if (currentPath[i] + 1 < Math.max(1, totalItemsInNodes[i])) {
					currentPath[i]++;
					break;
				} else
					currentPath[i] = 0;
			}

			System.arraycopy(currentPath, 0, pathInds[count], 0, currentPath.length);
			count++;
		}

		return pathInds;
	}

	// Returns the linearly mapped version of x which is in range xStart and xEnd in a new range
	// yStart and yEnd
	public static float linearMap(float x, float xStart, float xEnd, float yStart, float yEnd) {
		return (x - xStart) / (xEnd - xStart) * (yEnd - yStart) + yStart;
	}

	public static double linearMap(double x, double xStart, double xEnd, double yStart, double yEnd) {
		return (x - xStart) / (xEnd - xStart) * (yEnd - yStart) + yStart;
	}

	public static int linearMap(int x, int xStart, int xEnd, int yStart, int yEnd) {
		return (int) Math.floor(((double) x - xStart) / ((double) xEnd - xStart) * (yEnd - yStart) + yStart + 0.5);
	}

	//

	// In place sorting of array x, return value are the sorted 0-based indices
	// Sorting is from lowest to highest
	public static int[] quickSort(int[] x) {
		double[] x2 = new double[x.length];
		int i;
		for (i = 0; i < x.length; i++)
			x2[i] = x[i];

		int[] inds = quickSort(x2);

		for (i = 0; i < x.length; i++)
			x[i] = (int) x2[i];

		return inds;
	}

	public static int[] quickSort(double[] x) {
		int[] indices = new int[x.length];
		for (int i = 0; i < x.length; i++)
			indices[i] = i;

		quickSort(x, indices);

		return indices;
	}

	public static int[] quickSort(float[] x) {
		int[] indices = new int[x.length];
		for (int i = 0; i < x.length; i++)
			indices[i] = i;

		quickSort(x, indices);

		return indices;
	}

	// In place sorting of elements of array x between startIndex(included) and endIndex(included)
	// Sorting is from lowest to highest
	public static int[] quickSort(double[] x, int startIndex, int endIndex) {
		if (startIndex < 0)
			startIndex = 0;
		if (startIndex > x.length - 1)
			startIndex = x.length - 1;
		if (endIndex < startIndex)
			endIndex = startIndex;
		if (endIndex > x.length - 1)
			endIndex = x.length - 1;

		int[] indices = new int[endIndex - startIndex + 1];
		double[] x2 = new double[endIndex - startIndex + 1];
		int i;

		for (i = startIndex; i <= endIndex; i++) {
			indices[i - startIndex] = i;
			x2[i - startIndex] = x[i];
		}

		quickSort(x2, indices);

		for (i = startIndex; i <= endIndex; i++)
			x[i] = x2[i - startIndex];

		return indices;
	}

	// In place sorting of elements of array x between startIndex(included) and endIndex(included)
	// Sorting is from lowest to highest
	public static int[] quickSort(float[] x, int startIndex, int endIndex) {
		if (startIndex < 0)
			startIndex = 0;
		if (startIndex > x.length - 1)
			startIndex = x.length - 1;
		if (endIndex < startIndex)
			endIndex = startIndex;
		if (endIndex > x.length - 1)
			endIndex = x.length - 1;

		int[] indices = new int[endIndex - startIndex + 1];
		float[] x2 = new float[endIndex - startIndex + 1];
		int i;

		for (i = startIndex; i <= endIndex; i++) {
			indices[i - startIndex] = i;
			x2[i - startIndex] = x[i];
		}

		quickSort(x2, indices);

		for (i = startIndex; i <= endIndex; i++)
			x[i] = x2[i - startIndex];

		return indices;
	}

	// Sorts x, y is also sorted as x so it can be used to obtain sorted indices
	// Sorting is from lowest to highest
	public static void quickSort(double[] x, int[] y) {
		assert x.length == y.length;

		quickSort(x, y, 0, x.length - 1);
	}

	// Sorts x, y is also sorted as x so it can be used to obtain sorted indices
	// Sorting is from lowest to highest
	public static void quickSort(float[] x, int[] y) {
		assert x.length == y.length;

		quickSort(x, y, 0, x.length - 1);
	}

	// Sorting is from lowest to highest
	public static void quickSort(double[] x, int[] y, int startIndex, int endIndex) {
		if (startIndex < endIndex) {
			int j = partition(x, y, startIndex, endIndex);
			quickSort(x, y, startIndex, j - 1);
			quickSort(x, y, j + 1, endIndex);
		}
	}

	// Sorting is from lowest to highest
	public static void quickSort(float[] x, int[] y, int startIndex, int endIndex) {
		if (startIndex < endIndex) {
			int j = partition(x, y, startIndex, endIndex);
			quickSort(x, y, startIndex, j - 1);
			quickSort(x, y, j + 1, endIndex);
		}
	}

	private static int partition(double[] x, int[] y, int startIndex, int endIndex) {
		int i = startIndex;
		int j = endIndex + 1;
		double t;
		int ty;
		double pivot = x[startIndex];

		while (true) {
			do {
				++i;
			} while (i <= endIndex && x[i] <= pivot);

			do {
				--j;
			} while (x[j] > pivot);

			if (i >= j)
				break;

			t = x[i];
			ty = y[i];

			x[i] = x[j];
			y[i] = y[j];

			x[j] = t;
			y[j] = ty;
		}

		t = x[startIndex];
		ty = y[startIndex];

		x[startIndex] = x[j];
		y[startIndex] = y[j];

		x[j] = t;
		y[j] = ty;

		return j;
	}

	private static int partition(float[] x, int[] y, int startIndex, int endIndex) {
		int i = startIndex;
		int j = endIndex + 1;
		float t;
		int ty;
		float pivot = x[startIndex];

		while (true) {
			do {
				++i;
			} while (i <= endIndex && x[i] <= pivot);

			do {
				--j;
			} while (x[j] > pivot);

			if (i >= j)
				break;

			t = x[i];
			ty = y[i];

			x[i] = x[j];
			y[i] = y[j];

			x[j] = t;
			y[j] = ty;
		}

		t = x[startIndex];
		ty = y[startIndex];

		x[startIndex] = x[j];
		y[startIndex] = y[j];

		x[j] = t;
		y[j] = ty;

		return j;
	}

	// Returns a sorted version of x using sorted indices
	public static double[] sortAs(double[] x, int[] sortedIndices) {
		if (x == null)
			return null;
		else if (sortedIndices == null)
			return ArrayUtils.copy(x);
		else {
			assert x.length == sortedIndices.length;
			double[] y = new double[x.length];
			for (int i = 0; i < sortedIndices.length; i++)
				y[i] = x[sortedIndices[i]];

			return y;
		}
	}

	public static float[] sortAs(float[] x, int[] sortedIndices) {
		if (x == null)
			return null;
		else if (sortedIndices == null)
			return ArrayUtils.copy(x);
		else {
			assert x.length == sortedIndices.length;
			float[] y = new float[x.length];
			for (int i = 0; i < sortedIndices.length; i++)
				y[i] = x[sortedIndices[i]];

			return y;
		}
	}

	/***
	 * Calculates x_i = (x_i - mean(x)) / std(x) This function can deal with NaNs
	 * 
	 * @param x
	 *            x
	 * @return x
	 */
	public static double[] normalizeZscore(double[] x) {
		double mn = mean(x, 0);
		double sd = standardDeviation(x, 0);
		for (int i = 0; i < x.length; i++)
			if (!Double.isNaN(x[i]))
				x[i] = (x[i] - mn) / sd;
		return x;
	}

	public static double[] normalizeToSumUpTo(double[] x, double sumUp) {
		return normalizeToSumUpTo(x, x.length, sumUp);
	}

	public static double[] normalizeToSumUpTo(double[] x, int len, double sumUp) {
		if (len > x.length)
			len = x.length;

		double[] y = new double[len];

		double total = 0.0;
		int i;

		for (i = 0; i < len; i++)
			total += x[i];

		if (total > 0.0) {
			for (i = 0; i < len; i++)
				y[i] = sumUp * (x[i] / total);
		} else {
			for (i = 0; i < len; i++)
				y[i] = 1.0 / len;
		}

		return y;
	}

	public static double[] normalizeToRange(double[] x, int len, double minVal, double maxVal) {
		if (len > x.length)
			len = x.length;

		double[] y = new double[len];

		double xmin = MathUtils.min(x);
		double xmax = MathUtils.max(x);
		int i;

		if (xmax > xmin) {
			for (i = 0; i < len; i++)
				y[i] = (x[i] - xmin) / (xmax - xmin) * (maxVal - minVal) + minVal;
		} else {
			for (i = 0; i < len; i++)
				y[i] = (x[i] - xmin) + 0.5 * (minVal + maxVal);
		}

		return y;
	}

	public static double[] normalizeToAbsMax(double[] x, double absMax) {
		double[] y = new double[x.length];
		double currentAbsMax = getAbsMax(x);
		for (int i = 0; i < x.length; i++)
			y[i] = x[i] * absMax / currentAbsMax;

		return y;
	}

	// Shifts mean value of x
	public static void adjustMean(double[] x, double newMean) {
		double currentMean = MathUtils.mean(x);

		for (int i = 0; i < x.length; i++)
			x[i] = (x[i] - currentMean) + newMean;
	}

	//

	public static void adjustVariance(double[] x, double newVariance) {
		adjustStandardDeviation(x, Math.sqrt(newVariance));
	}

	public static void adjustMeanVariance(double[] x, double newMean, double newVariance) {
		adjustMeanStandardDeviation(x, newMean, Math.sqrt(newVariance));
	}

	public static void adjustVariance(double[] x, double newVariance, double currentMean) {
		adjustStandardDeviation(x, Math.sqrt(newVariance), currentMean);
	}

	public static void adjustMeanStandardDeviation(double[] x, double newMean, double newStandardDeviation) {
		adjustMeanStandardDeviation(x, MathUtils.mean(x), newMean, newStandardDeviation);
	}

	public static void adjustStandardDeviation(double[] x, double newStandardDeviation) {
		double currentMean = mean(x);

		adjustStandardDeviation(x, newStandardDeviation, currentMean);
	}

	// Adjusts standard deviation while keeping the mean value of x as it is
	public static void adjustStandardDeviation(double[] x, double newStandardDeviation, double currentMean) {
		double currentStdDev = standardDeviation(x, currentMean);

		for (int i = 0; i < x.length; i++)
			x[i] = ((x[i] - currentMean) * newStandardDeviation / currentStdDev) + currentMean;
	}

	//

	// Adjusts mean and standard deviation of x
	public static void adjustMeanStandardDeviation(double[] x, double currentMean, double newMean, double newStandardDeviation) {
		double currentStdDev = standardDeviation(x, currentMean);

		for (int i = 0; i < x.length; i++)
			x[i] = ((x[i] - currentMean) * newStandardDeviation / currentStdDev) + newMean;
	}

	//

	// Adjusts range so that the minimum value equals minVal and maximum equals maxVal
	public static void adjustRange(double[] x, double minVal, double maxVal) {
		double minOrig = MathUtils.min(x);
		double maxOrig = MathUtils.max(x);
		double diffOrig = maxOrig - minOrig;

		if (diffOrig > 0.0) {
			for (int i = 0; i < x.length; i++)
				x[i] = (x[i] - minOrig) / diffOrig * (maxVal - minVal) + minVal;
		} else {
			for (int i = 0; i < x.length; i++)
				x[i] = 0.5 * (maxVal + minVal);
		}
	}

	/**
	 * Adjust values in x so that all values smaller than minVal are set to minVal, and all values greater than maxVal are set to
	 * maxVal
	 * 
	 * @param x
	 *            array of doubles to adjust; if x is null, nothing happens
	 * @param minVal
	 *            minimum of all values in x after adjustment
	 * @param maxVal
	 *            maximum of all values in x after adjustment
	 * @return true if one or more values in x were modified, false if x is unchanged
	 */
	public static boolean clipRange(double[] x, double minVal, double maxVal) {
		boolean modified = false;
		if (x == null) {
			return modified;
		}
		for (int i = 0; i < x.length; i++) {
			if (x[i] < minVal) {
				x[i] = minVal;
				modified = true;
			} else if (x[i] > maxVal) {
				x[i] = maxVal;
				modified = true;
			}
		}
		return modified;
	}

	public static double median(double[] x) {
		if (x != null && x.length > 0)
			return median(x, 0, x.length - 1);
		else
			return 0.0;
	}

	public static double median(double[] xin, int firstIndex, int lastIndex) {
		if (firstIndex < 0)
			firstIndex = 0;
		if (lastIndex > xin.length - 1)
			lastIndex = xin.length - 1;
		if (lastIndex < firstIndex)
			lastIndex = firstIndex;

		double[] x = new double[lastIndex - firstIndex + 1];
		System.arraycopy(xin, firstIndex, x, 0, x.length);
		quickSort(x);

		int index = (int) Math.floor(0.5 * x.length + 0.5);
		if (index < 0)
			index = 0;
		if (index > x.length - 1)
			index = x.length - 1;

		return x[index];
	}

	// Returns 1/N sum_i=0^N-1(|x[i]|)
	public static double absMean(double[] x) {
		double m = 0.0;

		for (int i = 0; i < x.length; i++)
			m += Math.abs(x[i]);

		m /= x.length;

		return m;
	}

	// Returns variances for each row
	public static double[] getVarianceRows(double[][] x) {
		double[] variances = null;
		if (x != null) {
			variances = new double[x.length];
			for (int i = 0; i < x.length; i++)
				variances[i] = MathUtils.variance(x[i]);
		}

		return variances;
	}

	// Returns variances for each column
	public static double[] getVarianceCols(double[][] x) {
		double[] variances = null;
		if (x != null) {
			variances = new double[x[0].length];
			double[] tmp = new double[x.length];
			int i, j;
			for (j = 0; j < x[0].length; j++) {
				for (i = 0; i < x.length; i++)
					tmp[i] = x[i][j];

				variances[j] = MathUtils.variance(tmp);
			}
		}

		return variances;
	}

	public static double getGaussianPdfValueConstantTerm(int featureDimension, double detCovarianceMatrix) {
		double constantTerm = Math.pow(2 * Math.PI, 0.5 * featureDimension);
		constantTerm *= Math.sqrt(detCovarianceMatrix);
		constantTerm = 1.0 / constantTerm;

		return constantTerm;
	}

	public static double getGaussianPdfValueConstantTermLog(int featureDimension, double detCovarianceMatrix) {
		double constantTermLog = 0.5 * featureDimension * Math.log(2 * Math.PI); // double constantTerm = Math.pow(2*Math.PI,
																					// 0.5*featureDimension);
		constantTermLog += Math.log(Math.sqrt(detCovarianceMatrix));
		constantTermLog = -constantTermLog;

		return constantTermLog;
	}

	public static double getGaussianPdfValue(double[] x, double[] meanVector, double[] covarianceMatrix) {
		double constantTerm = getGaussianPdfValueConstantTerm(x.length, determinant(covarianceMatrix));

		return getGaussianPdfValue(x, meanVector, covarianceMatrix, constantTerm);
	}

	// Diagonal covariance case
	// This version enables using pre-computed constant term
	public static double getGaussianPdfValue(double[] x, double[] meanVector, double[] covarianceMatrix, double constantTerm) {
		double P = 0.0;
		int i;

		for (i = 0; i < x.length; i++)
			P += (x[i] - meanVector[i]) * (x[i] - meanVector[i]) / covarianceMatrix[i];

		P *= -0.5;

		P = constantTerm * Math.exp(P);

		return P;
	}

	// Log domain version
	public static double getGaussianPdfValueLog(double[] x, double[] meanVector, double[] covarianceMatrix, double constantTermLog) {
		double P = Double.MIN_VALUE;
		int i;
		double z;

		for (i = 0; i < x.length; i++) {
			z = (x[i] - meanVector[i]) * (x[i] - meanVector[i]);
			P = logAdd(P, Math.log(z) - Math.log(covarianceMatrix[i]));
		}

		P *= -0.5;

		P = constantTermLog + P;

		return P;
	}

	public static double getGaussianPdfValue(double[] x, double[] meanVector, double detCovarianceMatrix,
			double[][] inverseCovarianceMatrix) {
		double constantTerm = Math.pow(2 * Math.PI, 0.5 * x.length);
		constantTerm *= Math.sqrt(detCovarianceMatrix);
		constantTerm = 1.0 / constantTerm;

		return getGaussianPdfValue(x, meanVector, inverseCovarianceMatrix, constantTerm);
	}

	// Full covariance case
	// This version enables using pre-computed constant term
	public static double getGaussianPdfValue(double[] x, double[] meanVector, double[][] inverseCovarianceMatrix,
			double constantTerm) {
		double[][] z = new double[1][x.length];
		z[0] = MathUtils.subtract(x, meanVector);
		double[][] zT = MathUtils.transpoze(z);

		double[][] prod = MathUtils.matrixProduct(MathUtils.matrixProduct(z, inverseCovarianceMatrix), zT);

		double P = -0.5 * prod[0][0];

		P = constantTerm * Math.exp(P);

		return P;
	}

	// Computes the determinant of a diagonal matrix
	public static double determinant(double[] diagonal) {
		double det = 1.0;
		for (int i = 0; i < diagonal.length; i++)
			det *= diagonal[i];

		return det;
	}

	// Computes the determinant of a square matrix
	// Note that if the matrix contains large values and of a large size, one may get overflow
	public static double determinant(double[][] matrix) {
		double result = 0.0;

		if (matrix.length == 1)
			return determinant(matrix[0]);

		if (matrix.length == 1 && matrix[0].length == 1)
			return matrix[0][0];

		if (matrix.length == 2) {
			result = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
			return result;
		}

		for (int i = 0; i < matrix[0].length; i++) {
			double temp[][] = new double[matrix.length - 1][matrix[0].length - 1];
			for (int j = 1; j < matrix.length; j++) {
				for (int k = 0; k < matrix[0].length; k++) {
					if (k < i)
						temp[j - 1][k] = matrix[j][k];
					else if (k > i)
						temp[j - 1][k - 1] = matrix[j][k];
				}
			}

			result += matrix[0][i] * Math.pow(-1, (double) i) * determinant(temp);
		}

		return result;
	}

	public static double[] random(int numSamples) {
		double[] x = null;

		if (numSamples > 0) {
			x = new double[numSamples];
			for (int i = 0; i < numSamples; i++)
				x[i] = Math.random();
		}

		return x;
	}

	public static double[] random(int numSamples, double minVal, double maxVal) {
		double[] x = random(numSamples);
		MathUtils.adjustRange(x, minVal, maxVal);

		return x;
	}

	// Returns inverse of diagonal vector
	public static double[] inverse(double[] x) {
		double[] invx = new double[x.length];

		for (int i = 0; i < x.length; i++)
			invx[i] = 1.0 / (x.length * x[i]);

		return invx;
	}

	public static ComplexNumber[] inverse(ComplexNumber[] x) {
		ComplexNumber[] invx = new ComplexNumber[x.length];

		for (int i = 0; i < x.length; i++)
			invx[i] = divide(1.0, multiply(x.length, x[i]));

		return invx;
	}

	// Square matrix inversion using LU decomposition
	public static double[][] inverse(double[][] matrix) {
		double[][] invMatrix = null;
		if (matrix.length == 1) // Diagonal matrix
		{
			invMatrix = new double[1][matrix[0].length];
			invMatrix[0] = inverse(matrix[0]);
		} else // Full square matrix
		{
			invMatrix = new double[matrix.length][matrix.length];
			for (int i = 0; i < matrix.length; i++)
				System.arraycopy(matrix[i], 0, invMatrix[i], 0, matrix[i].length);

			inverseInPlace(invMatrix);
		}

		return invMatrix;
	}

	// Square complex-valued matrix inversion using LU decomposition
	public static ComplexNumber[][] inverse(ComplexNumber[][] matrix) {
		ComplexNumber[][] invMatrix = null;
		if (matrix.length == 1) // Diagonal matrix
		{
			invMatrix = new ComplexNumber[1][matrix[0].length];
			invMatrix[0] = inverse(matrix[0]);
		} else // Full square matrix
		{
			invMatrix = new ComplexNumber[matrix.length][matrix.length];
			int i, j;
			for (i = 0; i < matrix.length; i++) {
				for (j = 0; j < matrix[i].length; j++)
					invMatrix[i][j] = new ComplexNumber(matrix[i][j]);
			}

			inverseInPlace(invMatrix);
		}

		return invMatrix;
	}

	public static void inverseInPlace(double[][] matrix) {
		int dim = matrix.length;
		int i, j;

		double[][] y;
		double[] d = new double[1];
		double[] col;

		y = new double[dim][dim];

		int[] indices = new int[dim];
		col = new double[dim];

		luDecompose(matrix, dim, indices, d);
		for (j = 0; j < dim; j++) {
			for (i = 0; i < dim; i++)
				col[i] = 0.0;
			col[j] = 1.0;
			luSubstitute(matrix, indices, col);
			for (i = 0; i < dim; i++)
				y[i][j] = col[i];
		}

		for (i = 0; i < dim; i++)
			System.arraycopy(y[i], 0, matrix[i], 0, dim);
	}

	public static void inverseInPlace(ComplexNumber[][] matrix) {
		int dim = matrix.length;
		int i, j;

		ComplexNumber[][] y;
		ComplexNumber[] d = new ComplexNumber[1];
		ComplexNumber[] col;

		y = new ComplexNumber[dim][dim];

		int[] indices = new int[dim];
		col = new ComplexNumber[dim];

		luDecompose(matrix, dim, indices, d);
		for (j = 0; j < dim; j++) {
			for (i = 0; i < dim; i++)
				col[i] = new ComplexNumber(0.0, 0.0);

			col[j] = new ComplexNumber(1.0, 0.0);
			luSubstitute(matrix, indices, col);
			for (i = 0; i < dim; i++)
				y[i][j] = new ComplexNumber(col[i]);
		}

		for (i = 0; i < dim; i++) {
			for (j = 0; j < dim; j++)
				matrix[i][j] = new ComplexNumber(y[i][j]);
		}
	}

	public static void luDecompose(double[][] a, int n, int[] indx, double[] d) {
		double TINYVAL = 1e-20;
		int i, imax, j, k;
		double big, dum, sum, temp;
		double[] vv;
		imax = 0;

		vv = new double[n];
		d[0] = 1.0;

		for (i = 1; i <= n; i++) {
			big = 0.0;
			for (j = 1; j <= n; j++)
				if ((temp = Math.abs(a[i - 1][j - 1])) > big)
					big = temp;
			if (big == 0.0)
				System.out.println("Singular matrix in routine ludcmp");
			vv[i - 1] = 1.0 / big;
		}

		for (j = 1; j <= n; j++) {
			for (i = 1; i < j; i++) {
				sum = a[i - 1][j - 1];
				for (k = 1; k < i; k++)
					sum -= a[i - 1][k - 1] * a[k - 1][j - 1];
				a[i - 1][j - 1] = sum;
			}

			big = 0.0;

			for (i = j; i <= n; i++) {
				sum = a[i - 1][j - 1];
				for (k = 1; k < j; k++)
					sum -= a[i - 1][k - 1] * a[k - 1][j - 1];
				a[i - 1][j - 1] = sum;
				if ((dum = vv[i - 1] * Math.abs(sum)) >= big) {
					big = dum;
					imax = i;
				}
			}

			if (j != imax) {
				for (k = 1; k <= n; k++) {
					dum = a[imax - 1][k - 1];
					a[imax - 1][k - 1] = a[j - 1][k - 1];
					a[j - 1][k - 1] = dum;
				}
				d[0] = -d[0];
				vv[imax - 1] = vv[j - 1];
			}
			indx[j - 1] = imax;
			if (a[j - 1][j - 1] == 0.0)
				a[j - 1][j - 1] = TINYVAL;
			if (j != n) {
				dum = 1.0 / (a[j - 1][j - 1]);
				for (i = j + 1; i <= n; i++)
					a[i - 1][j - 1] *= dum;
			}
		}
	}

	public static void luDecompose(ComplexNumber[][] a, int n, int[] indx, ComplexNumber[] d) {
		double TINYVAL = 1e-20;
		int i, imax, j, k;
		ComplexNumber big = new ComplexNumber(0.0, 0.0);
		ComplexNumber dum = new ComplexNumber(0.0, 0.0);
		ComplexNumber sum = new ComplexNumber(0.0, 0.0);
		ComplexNumber temp = new ComplexNumber(0.0, 0.0);
		ComplexNumber[] vv;
		imax = 0;

		vv = new ComplexNumber[n];
		d[0] = new ComplexNumber(1.0, 0.0);

		for (i = 1; i <= n; i++) {
			big = new ComplexNumber(0.0, 0.0);
			for (j = 1; j <= n; j++) {
				if (magnitudeComplex(a[i - 1][j - 1]) > magnitudeComplex(big))
					big = new ComplexNumber(a[i - 1][j - 1]);
			}

			if (magnitudeComplex(big) == 0.0)
				System.out.println("Singular matrix in routine ludcmp");
			vv[i - 1] = divide(1.0, big);
		}

		for (j = 1; j <= n; j++) {
			for (i = 1; i < j; i++) {
				sum = new ComplexNumber(a[i - 1][j - 1]);
				for (k = 1; k < i; k++)
					sum = subtractComplex(sum, multiplyComplex(a[i - 1][k - 1], a[k - 1][j - 1]));
				a[i - 1][j - 1] = new ComplexNumber(sum);
			}

			big = new ComplexNumber(0.0, 0.0);
			for (i = j; i <= n; i++) {
				sum = new ComplexNumber(a[i - 1][j - 1]);
				for (k = 1; k < j; k++)
					sum = subtractComplex(sum, multiplyComplex(a[i - 1][k - 1], a[k - 1][j - 1]));

				a[i - 1][j - 1] = new ComplexNumber(sum);
				dum = multiply(magnitudeComplex(sum), vv[i - 1]);
				if ((magnitudeComplex(dum)) >= magnitudeComplex(big)) {
					big = new ComplexNumber(dum);
					imax = i;
				}
			}

			if (j != imax) {
				for (k = 1; k <= n; k++) {
					dum = new ComplexNumber(a[imax - 1][k - 1]);
					a[imax - 1][k - 1] = new ComplexNumber(a[j - 1][k - 1]);
					a[j - 1][k - 1] = new ComplexNumber(dum);
				}

				d[0] = multiply(-1.0, d[0]);

				vv[imax - 1] = new ComplexNumber(vv[j - 1]);
			}

			indx[j - 1] = imax;
			if (magnitudeComplex(a[j - 1][j - 1]) == 0.0)
				a[j - 1][j - 1] = new ComplexNumber(TINYVAL, TINYVAL);

			if (j != n) {
				dum = divide(1.0, a[j - 1][j - 1]);
				for (i = j + 1; i <= n; i++)
					a[i - 1][j - 1] = multiplyComplex(dum, a[i - 1][j - 1]);
			}
		}
	}

	public static void luSubstitute(double[][] a, int[] indx, double b[]) {
		int n = a.length;
		int i = 0;
		int ii = 0;
		int ip, j;
		double sum;

		for (i = 1; i <= n; i++) {
			ip = indx[i - 1];
			sum = b[ip - 1];
			b[ip - 1] = b[i - 1];
			if (ii != 0) {
				for (j = ii; j <= i - 1; j++)
					sum -= a[i - 1][j - 1] * b[j - 1];
			} else if (sum != 0.0)
				ii = i;
			b[i - 1] = sum;
		}
		for (i = n; i >= 1; i--) {
			sum = b[i - 1];
			for (j = i + 1; j <= n; j++)
				sum -= a[i - 1][j - 1] * b[j - 1];

			b[i - 1] = sum / a[i - 1][i - 1];
		}
	}

	public static void luSubstitute(ComplexNumber[][] a, int[] indx, ComplexNumber b[]) {
		int n = a.length;
		int i = 0;
		int ii = 0;
		int ip, j;
		ComplexNumber sum = new ComplexNumber(0.0, 0.0);

		for (i = 1; i <= n; i++) {
			ip = indx[i - 1];
			sum = new ComplexNumber(b[ip - 1]);
			b[ip - 1] = new ComplexNumber(b[i - 1]);
			if (ii != 0) {
				for (j = ii; j <= i - 1; j++)
					sum = subtractComplex(sum, multiplyComplex(a[i - 1][j - 1], b[j - 1]));
			} else if (magnitudeComplex(sum) != 0.0)
				ii = i;
			b[i - 1] = new ComplexNumber(sum);
		}

		for (i = n; i >= 1; i--) {
			sum = new ComplexNumber(b[i - 1]);
			for (j = i + 1; j <= n; j++)
				sum = subtractComplex(sum, multiplyComplex(a[i - 1][j - 1], b[j - 1]));

			b[i - 1] = divideComplex(sum, a[i - 1][i - 1]);
		}
	}

	//

	public static double logAdd(double x, double y) {
		if (y > x) {
			double temp = x;
			x = y;
			y = temp;
		}

		if (x == Double.NEGATIVE_INFINITY)
			return x;

		double negDiff = y - x;
		if (negDiff < -20)
			return x;

		return x + Math.log(1.0 + Math.exp(negDiff));
	}

	// Returns the index of largest element in x which is smaller than val
	// x should be sorted in increasing order to get a meaningful result
	public static int getLargestIndexSmallerThan(double[] x, double val) {
		int index = -1;

		if (x != null) {
			for (int i = 0; i < x.length; i++) {
				if (x[i] < val)
					index = i;
				else
					break;
			}
		}

		return index;
	}

	public static int[] randomSort(int[] x) {
		int i, ind;
		int[] tmpx = new int[x.length];
		int[] y = new int[x.length];
		int[] tmpx2;
		System.arraycopy(x, 0, tmpx, 0, x.length);

		for (i = 1; i < x.length; i++) {
			ind = (int) (Math.random() * tmpx.length);
			if (ind > tmpx.length - 1)
				ind = tmpx.length - 1;
			y[i - 1] = tmpx[ind];
			tmpx2 = new int[tmpx.length - 1];
			System.arraycopy(tmpx, 0, tmpx2, 0, ind);
			System.arraycopy(tmpx, ind + 1, tmpx2, ind, tmpx.length - ind - 1);

			tmpx = new int[tmpx2.length];
			System.arraycopy(tmpx2, 0, tmpx, 0, tmpx2.length);
		}
		y[x.length - 1] = tmpx[0];

		return y;
	}

	// Randomly sorts each row of x
	public static double[][] randomSort(double[][] x) {
		double[][] y = null;
		if (x != null) {
			int[] indsRnd = new int[x.length];
			int i;
			for (i = 0; i < x.length; i++)
				indsRnd[i] = i;
			indsRnd = randomSort(indsRnd);

			y = new double[x.length][];
			for (i = 0; i < x.length; i++) {
				y[i] = new double[x[indsRnd[i]].length];
				System.arraycopy(x[indsRnd[i]], 0, y[i], 0, x[indsRnd[i]].length);
			}
		}

		return y;
	}

	/**
	 * Add val x to list of int X
	 * 
	 * @param X
	 *            X
	 * @param x
	 *            x
	 * @return newX
	 */
	static public int[] addIndex(int[] X, int x) {
		int newX[] = new int[X.length + 1];
		for (int i = 0; i < X.length; i++)
			newX[i] = X[i];
		newX[X.length] = x;
		return newX;
	}

	/**
	 * Remove val x from list of int X
	 * 
	 * @param X
	 *            X
	 * @param x
	 *            x
	 * @return newX
	 */
	static public int[] removeIndex(int[] X, int x) {
		int newX[] = new int[X.length - 1];
		int j = 0;
		for (int i = 0; i < X.length; i++)
			if (X[i] != x)
				newX[j++] = X[i];
		return newX;
	}

	// This funciton is NOT an interpolation function
	// It just repeats/removes entries in x to create y that is of size newLen
	static public double[] modifySize(double[] x, int newLen) {
		double[] y = null;

		if (newLen < 1)
			return y;

		if (x.length == newLen || newLen == 1) {
			y = new double[x.length];
			System.arraycopy(x, 0, y, 0, x.length);
			return y;
		} else {
			y = new double[newLen];
			int mappedInd;
			int i;
			for (i = 1; i <= newLen; i++) {
				mappedInd = (int) (Math.floor((i - 1.0) / (newLen - 1.0) * (x.length - 1.0) + 1.5));
				if (mappedInd < 1)
					mappedInd = 1;
				else if (mappedInd > x.length)
					mappedInd = x.length;

				y[i - 1] = x[mappedInd - 1];
			}

			return y;
		}
	}

	// mean+-returnValue will be %95 confidence interval
	public static double getConfidenceInterval95(double standardDeviation) {
		return 1.96 * standardDeviation;
	}

	// mean+-returnValue will be %99 confidence interval
	public static double getConfidenceInterval99(double standardDeviation) {
		return 2.58 * standardDeviation;
	}

	public static double[] autocorr(double[] x, int P) {
		double[] R = new double[P + 1];

		int N = x.length;
		int m, n;
		for (m = 0; m <= P; m++) {
			R[m] = 0.0;
			for (n = 0; n <= N - m - 1; n++)
				R[m] += x[n] * x[n + m];
		}

		return R;
	}

	public static boolean isAnyNaN(double[] x) {
		for (int i = 0; i < x.length; i++) {
			if (Double.isNaN(x[i]))
				return true;
		}

		return false;
	}

	/**
	 * Check whether x contains Infinity
	 * 
	 * @param x
	 *            the array to check
	 * @return true if at least one value in x is Infinity, false otherwise
	 */
	public static boolean isAnyInfinity(double[] x) {
		for (double value : x) {
			if (Double.isInfinite(value)) {
				return true;
			}
		}
		return false;
	}

	public static boolean allZeros(double[] x) {
		boolean bRet = true;

		for (int i = 0; i < x.length; i++) {
			if (Math.abs(x[i]) > 1e-100) {
				bRet = false;
				break;
			}
		}

		return bRet;
	}

	public static double[] doubleRange2ByteRange(double[] x) {
		return doubleRange2ByteRange(x, -32768.0, 32767.0);
	}

	public static double[] doubleRange2ByteRange(double[] x, double doubleMin, double doubleMax) {
		return MathUtils.multiply(x, 256.0 / (doubleMax - doubleMin));
	}

	public static double[] byteRange2DoubleRange(double[] x) {
		return byteRange2DoubleRange(x, -32768.0, 32767.0);
	}

	public static double[] byteRange2DoubleRange(double[] x, double doubleMin, double doubleMax) {
		return MathUtils.multiply(x, (doubleMax - doubleMin) / 256.0);
	}

	public static float[] floatRange2ByteRange(float[] x) {
		return floatRange2ByteRange(x, -32768.0f, 32767.0f);
	}

	public static float[] floatRange2ByteRange(float[] x, float floatMin, float floatMax) {
		return MathUtils.multiply(x, 256.0f / (floatMax - floatMin));
	}

	public static float[] byteRange2FloatRange(float[] x) {
		return byteRange2FloatRange(x, -32768.0f, 32767.0f);
	}

	public static float[] byteRange2FloatRange(float[] x, float floatMin, float floatMax) {
		return MathUtils.multiply(x, (floatMax - floatMin) / 256.0f);
	}

	/**
	 * Trim the given value so that it is in the closed interval [min, max].
	 * 
	 * @param untrimmedValue
	 *            untrimmedValue
	 * @param min
	 *            min
	 * @param max
	 *            max
	 * @return min if untrimmedValue is less than min; max if untrimmedValue is more than max; untrimmedValue otherwise.
	 */
	public static double trimToRange(double untrimmedValue, double min, double max) {
		return Math.max(min, Math.min(max, untrimmedValue));
	}

	/**
	 * To interpolate Zero values with respect to NonZero values
	 * 
	 * @param contour
	 *            contour
	 * @return contour
	 */
	public static double[] interpolateNonZeroValues(double[] contour) {

		for (int i = 0; i < contour.length; i++) {
			if (contour[i] == 0) {
				int index = findNextIndexNonZero(contour, i);
				// System.out.println("i: "+i+"index: "+index);
				if (index == -1) {
					for (int j = (i == 0 ? 1 : i); j < contour.length; j++) {
						contour[j] = contour[j - 1];
					}
					break;
				} else {
					for (int j = i; j < index; j++) {
						// contour[j] = contour[i-1] * (index - j) + contour[index] * (j - (i-1)) / ( index - i );
						if (i == 0) {
							contour[j] = contour[index];
						} else {
							contour[j] = contour[j - 1] + ((contour[index] - contour[i - 1]) / (index - i + 1));
						}
					}
					i = index - 1;
				}
			}
		}

		return contour;
	}

	/**
	 * To find next NonZero index in a given array
	 * 
	 * @param contour
	 *            contour
	 * @param current
	 *            current
	 * @return -1
	 */
	public static int findNextIndexNonZero(double[] contour, int current) {
		for (int i = current + 1; i < contour.length; i++) {
			if (contour[i] != 0) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * array resize to target size using linear interpolation
	 * 
	 * @param source
	 *            source
	 * @param targetSize
	 *            targetSize
	 * @return source if source.length == targetSize, newSignal otherwise
	 */
	public static double[] arrayResize(double[] source, int targetSize) {

		if (source.length == targetSize) {
			return source;
		}

		int sourceSize = source.length;
		double fraction = (double) source.length / (double) targetSize;
		double[] newSignal = new double[targetSize];

		for (int i = 0; i < targetSize; i++) {
			double posIdx = fraction * i;
			int nVal = (int) Math.floor(posIdx);
			double diffVal = posIdx - nVal;

			if (nVal >= sourceSize - 1) {
				newSignal[i] = source[sourceSize - 1];
				continue;
			}
			// Linear Interpolation
			// newSignal[i] = (diffVal * samples[nVal+1]) + ((1 - diffVal) * samples[nVal]);
			// System.err.println("i "+i+" fraction "+fraction+" posIdx "+posIdx+" nVal "+nVal+" diffVal "+diffVal+" !!");
			double fVal = (diffVal * source[nVal + 1]) + ((1 - diffVal) * source[nVal]);
			newSignal[i] = fVal;
		}
		return newSignal;
	}

	/**
	 * Get first-order discrete difference along adjacent values in an array
	 * 
	 * @param a
	 *            a
	 * @return array of differences between adjacent values in <b>a</b>, length is <code>a.length-1</code>; otherwise return null
	 *         if <b>a</b> is null, or [] if the length of <b>a</b> is less than 2.
	 */
	public static double[] diff(double[] a) {
		if (a == null) {
			return null;
		} else if (a.length < 2) {
			return new double[0];
		}
		double[] b = new double[a.length - 1];
		for (int i = 0; i < a.length - 1; i++) {
			b[i] = a[i + 1] - a[i];
		}
		return b;
	}

	public static void main(String[] args) {
		ComplexNumber[][] x1 = new ComplexNumber[2][2];
		x1[0][0] = new ComplexNumber(1.0, 2.0);
		x1[0][1] = new ComplexNumber(2.0, 1.0);
		x1[1][0] = new ComplexNumber(1.0, 2.0);
		x1[1][1] = new ComplexNumber(3.0, 1.0);

		ComplexNumber[][] x2 = new ComplexNumber[2][2];
		x2[0][0] = new ComplexNumber(1.0, 2.0);
		x2[0][1] = new ComplexNumber(2.0, 1.0);
		x2[1][0] = new ComplexNumber(1.0, 2.0);
		x2[1][1] = new ComplexNumber(3.0, 1.0);

		ComplexNumber[][] y = matrixProduct(x1, x2);

		System.out.print(MathUtils.toString(y));

		System.out.println("Test completed...");
	}

	public static String[] toStringLines(ComplexNumber[] x) {
		String[] y = null;

		if (x != null && x.length > 0) {
			y = new String[x.length];
			for (int i = 0; i < x.length; i++) {
				if (x[i].imag >= 0)
					y[i] = String.valueOf(x[i].real) + "+i*" + String.valueOf(x[i].imag);
				else
					y[i] = String.valueOf(x[i].real) + "-i*" + String.valueOf(Math.abs(x[i].imag));
			}
		}

		return y;
	}

	public static String[] toStringLines(ComplexArray x) {
		String[] y = null;

		if (x != null && x.real.length > 0 && x.imag.length > 0) {
			assert x.real.length == x.imag.length;
			y = new String[x.real.length];
			for (int i = 0; i < x.real.length; i++) {
				if (x.imag[i] >= 0)
					y[i] = String.valueOf(x.real[i]) + "+i*" + String.valueOf(x.imag[i]);
				else
					y[i] = String.valueOf(x.real[i]) + "-i*" + String.valueOf(Math.abs(x.imag[i]));
			}
		}

		return y;
	}

	public static String toString(ComplexNumber[][] array) {
		String str = "";
		int i, j;
		for (i = 0; i < array.length; i++) {
			for (j = 0; j < array[i].length; j++) {
				str += array[i][j].toString();
				if (j < array[i].length - 1)
					str += " ";
			}
			str += System.getProperty("line.separator");
		}

		str += System.getProperty("line.separator");

		return str;
	}
}
