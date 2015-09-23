/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.unitselection.data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import marytts.util.data.Datagram;

public class MCepDatagram extends Datagram {

	protected float[] coeffs;

	/**
	 * Construct a MCep datagram from a float vector.
	 * 
	 * @param setDuration
	 *            the duration, in samples, of the data represented by this datagram
	 * @param coeffs
	 *            the array of Mel-Cepstrum coefficients.
	 */
	public MCepDatagram(long setDuration, float[] coeffs) {
		super(setDuration);
		this.coeffs = coeffs;
	}

	/**
	 * Constructor which pops a datagram from a random access file.
	 * 
	 * @param raf
	 *            the random access file to pop the datagram from.
	 * @param order
	 *            order
	 * @throws IOException
	 *             IOException
	 * @throws EOFException
	 *             EOFException
	 */
	public MCepDatagram(RandomAccessFile raf, int order) throws IOException, EOFException {
		super(raf.readLong()); // duration
		int len = raf.readInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		if (len < 4 * order) {
			throw new IOException("Mel-Cepstrum datagram too short (len=" + len
					+ "): cannot be shorter than the space needed for Mel-Cepstrum coefficients (4*" + order + ")");
		}
		// For speed concerns, read into a byte[] first:
		byte[] buf = new byte[len];
		raf.readFully(buf);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));

		coeffs = new float[order];
		for (int i = 0; i < order; i++) {
			coeffs[i] = dis.readFloat();
		}
	}

	/**
	 * Constructor which pops a datagram from a byte buffer.
	 * 
	 * @param bb
	 *            the byte buffer to pop the datagram from.
	 * @param order
	 *            order
	 * @throws IOException
	 *             IOException
	 * @throws EOFException
	 *             EOFException
	 */
	public MCepDatagram(ByteBuffer bb, int order) throws IOException, EOFException {
		super(bb.getLong()); // duration
		int len = bb.getInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		if (len < 4 * order) {
			throw new IOException("Mel-Cepstrum datagram too short (len=" + len
					+ "): cannot be shorter than the space needed for Mel-Cepstrum coefficients (4*" + order + ")");
		}
		coeffs = new float[order];
		for (int i = 0; i < order; i++) {
			coeffs[i] = bb.getFloat();
		}
	}

	/**
	 * Get the length, in bytes, of the datagram's data field.
	 */
	public int getLength() {
		return 4 * coeffs.length;
	}

	/**
	 * Get the order, i.e. the number of MEl-Cepstrum coefficients.
	 * 
	 * @return the order
	 * @see #getCoeffs()
	 */
	public int order() {
		return coeffs.length;
	}

	/**
	 * Get the array of Mel-Cepstrum coefficients.
	 * 
	 * @return coeffs
	 */
	public float[] getCoeffs() {
		return coeffs;
	}

	/**
	 * Get the array of Mel-Cepstrum coefficients.
	 * 
	 * @return ret
	 */
	public double[] getCoeffsAsDouble() {
		double[] ret = new double[coeffs.length];
		for (int i = 0; i < coeffs.length; i++) {
			ret[i] = (double) (coeffs[i]);
		}
		return (ret);
	}

	/**
	 * Get a particular Mel-Cepstrum coefficient.
	 * 
	 * @param i
	 *            i
	 * @return coeffs[i]
	 */
	public float getCoeff(int i) {
		return coeffs[i];
	}

	/**
	 * Write this datagram to a random access file or data output stream.
	 * 
	 * @param out
	 *            out
	 * @throws IOException
	 *             IOException
	 */
	public void write(DataOutput out) throws IOException {
		out.writeLong(duration);
		out.writeInt(getLength());
		for (int i = 0; i < coeffs.length; i++) {
			out.writeFloat(coeffs[i]);
		}
	}

	/**
	 * Tests if this datagram is equal to another datagram.
	 * 
	 * @param other
	 *            other
	 * @return true if everything passes
	 */
	public boolean equals(Datagram other) {
		if (!(other instanceof MCepDatagram))
			return false;
		MCepDatagram otherMCep = (MCepDatagram) other;
		if (this.duration != otherMCep.duration)
			return false;
		if (this.coeffs.length != otherMCep.coeffs.length)
			return false;
		for (int i = 0; i < this.coeffs.length; i++) {
			if (this.coeffs[i] != otherMCep.coeffs[i])
				return false;
		}
		return true;
	}

}
