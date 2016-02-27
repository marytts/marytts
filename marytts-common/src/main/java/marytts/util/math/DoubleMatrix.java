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
package marytts.util.math;

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;

/**
 * @author Oytun T&uuml;rk
 */
public class DoubleMatrix {
	public double[][] vectors;
	public int numVectors;
	public int dimension;

	public DoubleMatrix() {
		vectors = null;
		allocate(0, 0);
	}

	public DoubleMatrix(int numVectorsIn, int dimensionIn) {
		vectors = null;
		allocate(numVectorsIn, dimensionIn);
	}

	public DoubleMatrix(String dataFile) {
		vectors = null;
		read(dataFile);
	}

	public DoubleMatrix(double[][] x) {
		setVectors(x);
	}

	public void setVectors(double[][] x) {
		if (x != null) {
			int i;
			int dimensionIn = x[0].length;
			for (i = 1; i < x.length; i++)
				assert x[i].length == dimensionIn;

			allocate(x.length, dimensionIn);

			for (i = 0; i < numVectors; i++)
				System.arraycopy(x[i], 0, vectors[i], 0, dimension);
		} else
			allocate(0, 0);
	}

	public void allocate(int numVectorsIn, int dimensionIn) {
		if (numVectorsIn > 0) {
			if (numVectors != numVectorsIn) {
				numVectors = numVectorsIn;
				vectors = new double[numVectors][];
			}

			if (dimensionIn > 0) {
				if (dimension != dimensionIn) {
					dimension = dimensionIn;
					for (int i = 0; i < numVectors; i++)
						vectors[i] = new double[dimension];
				}

				dimension = dimensionIn;
			} else
				dimension = 0;
		} else {
			vectors = null;
			numVectors = 0;
			dimension = 0;
		}
	}

	public void write(String dataFile) {
		MaryRandomAccessFile fp = null;
		try {
			fp = new MaryRandomAccessFile(dataFile, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (fp != null) {
			try {
				fp.writeIntEndian(numVectors);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				fp.writeIntEndian(dimension);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (numVectors > 0 && dimension > 0) {
				for (int i = 0; i < numVectors; i++) {
					try {
						fp.writeDoubleEndian(vectors[i]);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			try {
				fp.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void read(String dataFile) {
		MaryRandomAccessFile fp = null;
		try {
			fp = new MaryRandomAccessFile(dataFile, "r");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (fp != null) {
			int numVectorsIn = 0;
			int dimensionIn = 0;

			try {
				numVectorsIn = fp.readIntEndian();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				dimensionIn = fp.readIntEndian();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (numVectorsIn > 0) {
				if (numVectors != numVectorsIn)
					vectors = new double[numVectorsIn][];

				numVectors = numVectorsIn;
				dimension = dimensionIn;

				if (dimension > 0) {
					for (int i = 0; i < numVectors; i++) {
						try {
							vectors[i] = fp.readDoubleEndian(dimension);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					for (int i = 0; i < numVectors; i++)
						vectors[i] = null;

					dimension = 0;
				}
			} else {
				vectors = null;
				numVectors = 0;
				dimension = 0;
			}

			try {
				fp.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			allocate(0, 0);
	}
}
