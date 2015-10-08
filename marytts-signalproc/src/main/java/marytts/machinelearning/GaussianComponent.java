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
import marytts.util.math.MathUtils;

/**
 * 
 * Implements a single Gaussian component with a mean vector and a covariance matrix It also computes terms for pdf computation
 * out of this Gaussian component once the mean and covariance is specified
 * 
 * @author Oytun T&uuml;rk
 */
public class GaussianComponent {
	public double[] meanVector;
	public double[][] covMatrix;

	// These are used in pdf computation
	private double[][] invCovMatrix;
	private double detCovMatrix;
	private double constantTerm;
	private double constantTermLog;

	//

	public GaussianComponent() {
		this(0, true);
	}

	public GaussianComponent(int featureDimensionIn, boolean isDiagonal) {
		init(featureDimensionIn, isDiagonal);
	}

	public GaussianComponent(GaussianComponent existing) {
		init(existing.meanVector, existing.covMatrix);
	}

	public GaussianComponent(Cluster c) {
		init(c.meanVector, c.covMatrix);
	}

	public void init(int featureDimensionIn, boolean isDiagonal) {
		if (featureDimensionIn > 0) {
			meanVector = new double[featureDimensionIn];
			if (isDiagonal)
				covMatrix = new double[1][featureDimensionIn];
			else
				covMatrix = new double[featureDimensionIn][featureDimensionIn];
		} else {
			meanVector = null;
			covMatrix = null;
		}
	}

	public void init(double[] meanVectorIn, double[][] covMatrixIn) {
		setMeanVector(meanVectorIn);
		setCovMatrix(covMatrixIn);

		for (int i = 0; i < covMatrix.length; i++)
			assert meanVector.length == covMatrix[i].length;
	}

	public void setMeanVector(double[] meanVectorIn) {
		setMeanVector(meanVectorIn, 0, meanVectorIn.length);
	}

	public void setMeanVector(double[] bigVector, int startIndex, int meanLength) {
		if (bigVector != null && meanLength > 0) {
			if (startIndex + meanLength > bigVector.length)
				meanLength = bigVector.length - startIndex;

			if (meanVector == null || meanLength != meanVector.length)
				meanVector = new double[meanLength];

			for (int i = 0; i < meanLength; i++)
				meanVector[i] = bigVector[startIndex + i];
		} else
			meanVector = null;
	}

	public void setCovMatrix(double[][] covMatrixIn) {
		if (covMatrixIn.length == 1)
			setCovMatrix(covMatrixIn, 0, 0, covMatrixIn[0].length);
		else
			setCovMatrix(covMatrixIn, 0, 0, covMatrixIn.length);
	}

	public void setCovMatrix(double[][] bigCovMatrix, int rowStartIndex, int colStartIndex, int covLength) {
		if (bigCovMatrix != null && covLength > 0) {
			if (bigCovMatrix.length == 1) // Diagonal
			{
				int startIndex = Math.max(rowStartIndex, colStartIndex);
				if (startIndex + covLength > bigCovMatrix[0].length)
					covLength = bigCovMatrix[0].length - startIndex;

				if (covMatrix == null || covMatrix.length > 1 || covMatrix[0].length != covLength)
					covMatrix = new double[1][covLength];

				System.arraycopy(bigCovMatrix[0], startIndex, covMatrix[0], 0, covLength);
			} else // Full
			{
				int i, j;
				for (i = 0; i < bigCovMatrix.length; i++) {
					if (colStartIndex + covLength > bigCovMatrix[i].length)
						covLength = bigCovMatrix[i].length - colStartIndex;
				}

				if (rowStartIndex + covLength > bigCovMatrix.length)
					covLength = bigCovMatrix.length - rowStartIndex;

				if (covMatrix == null)
					covMatrix = new double[covLength][];

				for (i = rowStartIndex; i < rowStartIndex + covLength; i++) {
					if (covMatrix[i - rowStartIndex] == null || covMatrix[i - rowStartIndex].length != covLength)
						covMatrix[i - rowStartIndex] = new double[covLength];

					for (j = colStartIndex; j < colStartIndex + covLength; j++)
						covMatrix[i - rowStartIndex][j - colStartIndex] = bigCovMatrix[i][j];
				}
			}
		} else
			covMatrix = null;

		setDerivedValues();
	}

	// Computes the inverse covariance, determinant, constant term to be used in pdf evalutaion
	public void setDerivedValues() {
		if (covMatrix != null) {
			invCovMatrix = MathUtils.inverse(covMatrix);
			detCovMatrix = MathUtils.determinant(covMatrix);
			constantTerm = MathUtils.getGaussianPdfValueConstantTerm(covMatrix[0].length, detCovMatrix);
			constantTermLog = MathUtils.getGaussianPdfValueConstantTermLog(covMatrix[0].length, detCovMatrix);
		} else {
			invCovMatrix = null;
			detCovMatrix = 0.0;
			constantTerm = 0.0;
			constantTermLog = 0.0;
		}
	}

	public boolean isDiagonalCovariance() {
		if (meanVector != null && covMatrix != null) {
			if (covMatrix.length == 1 && meanVector.length > 1 && covMatrix[0].length == meanVector.length)
				return true;
		}

		return false;
	}

	public double[] getCovMatrixDiagonal() {
		if (covMatrix != null)
			return covMatrix[0];
		else
			return null;
	}

	public double[][] getInvCovMatrix() {
		return invCovMatrix;
	}

	public double getDetCovMatrix() {
		return detCovMatrix;
	}

	public double getConstantTerm() {
		return constantTerm;
	}

	public double getConstantTermLog() {
		return constantTermLog;
	}

	public void write(MaryRandomAccessFile stream) throws IOException {
		boolean isDiagonal = isDiagonalCovariance();
		stream.writeBooleanEndian(isDiagonal);

		if (meanVector != null) {
			stream.writeIntEndian(meanVector.length);
			stream.writeDoubleEndian(meanVector);
		} else
			stream.writeIntEndian(0);

		int i;

		if (covMatrix != null)
			stream.writeIntEndian(covMatrix.length);
		else
			stream.writeIntEndian(0);

		if (covMatrix != null) {
			for (i = 0; i < covMatrix.length; i++) {
				if (covMatrix[i] != null) {
					stream.writeIntEndian(covMatrix[i].length);
					stream.writeDoubleEndian(covMatrix[i]);
				} else
					stream.writeIntEndian(0);
			}
		}

		if (invCovMatrix != null)
			stream.writeIntEndian(invCovMatrix.length);
		else
			stream.writeIntEndian(0);

		if (invCovMatrix != null) {
			for (i = 0; i < invCovMatrix.length; i++) {
				if (invCovMatrix[i] != null) {
					stream.writeIntEndian(invCovMatrix[i].length);
					stream.writeDoubleEndian(invCovMatrix[i]);
				} else
					stream.writeIntEndian(0);
			}
		}

		stream.writeDoubleEndian(detCovMatrix);
		stream.writeDoubleEndian(constantTerm);
		stream.writeDoubleEndian(constantTermLog);
	}

	public void read(MaryRandomAccessFile stream) throws IOException {
		boolean isDiagonal = stream.readBooleanEndian(); // This is for compatibility with C version

		int tmpLen, tmpLen2;
		tmpLen = stream.readIntEndian();

		if (tmpLen > 0)
			meanVector = stream.readDoubleEndian(tmpLen);
		else
			meanVector = null;

		int i;

		tmpLen = stream.readIntEndian();

		if (tmpLen > 0) {
			covMatrix = new double[tmpLen][];

			for (i = 0; i < tmpLen; i++) {
				tmpLen2 = stream.readIntEndian();
				if (tmpLen2 > 0)
					covMatrix[i] = stream.readDoubleEndian(tmpLen2);
				else
					covMatrix[i] = null;
			}
		} else
			covMatrix = null;

		tmpLen = stream.readIntEndian();

		if (tmpLen > 0) {
			invCovMatrix = new double[tmpLen][];

			for (i = 0; i < tmpLen; i++) {
				tmpLen2 = stream.readIntEndian();
				if (tmpLen2 > 0)
					invCovMatrix[i] = stream.readDoubleEndian(tmpLen2);
				else
					invCovMatrix[i] = null;
			}
		} else
			invCovMatrix = null;

		detCovMatrix = stream.readDoubleEndian();
		constantTerm = stream.readDoubleEndian();
		constantTermLog = stream.readDoubleEndian();
	}

	public double probability(double[] x) {
		double P;
		if (covMatrix.length == 1) // Diagonal
			P = MathUtils.getGaussianPdfValue(x, meanVector, covMatrix[0], getConstantTerm());
		else
			// Full-covariance
			P = MathUtils.getGaussianPdfValue(x, meanVector, getDetCovMatrix(), getInvCovMatrix());

		return P;
	}
}
