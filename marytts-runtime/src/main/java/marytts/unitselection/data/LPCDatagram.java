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
import marytts.util.io.General;

public class LPCDatagram extends Datagram {
	protected short[] quantizedCoeffs;
	protected byte[] quantizedResidual;

	/**
	 * Construct an LPC datagram from quantized data.
	 * 
	 * @param setDuration
	 *            the duration, in samples, of the data represented by this datagram
	 * @param quantizedCoeffs
	 *            the quantized LPC coefficients
	 * @param quantizedResidual
	 *            the quantized residual
	 */
	public LPCDatagram(long setDuration, short[] quantizedCoeffs, byte[] quantizedResidual) {
		super(setDuration);
		this.quantizedCoeffs = quantizedCoeffs;
		this.quantizedResidual = quantizedResidual;
	}

	/**
	 * Construct an LPC datagram from unquantized data.
	 * 
	 * @param setDuration
	 *            the duration, in samples, of the data represented by this datagram
	 * @param coeffs
	 *            the (unquantized) LPC coefficients
	 * @param residual
	 *            the (unquantized) residual
	 * @param lpcMin
	 *            lpcMin
	 * @param lpcRange
	 *            lpcRange
	 */
	public LPCDatagram(long setDuration, float[] coeffs, short[] residual, float lpcMin, float lpcRange) {
		super(setDuration);
		this.quantizedCoeffs = General.quantize(coeffs, lpcMin, lpcRange);
		this.quantizedResidual = General.shortToUlaw(residual);
	}

	/**
	 * Constructor which pops a datagram from a random access file.
	 * 
	 * @param raf
	 *            the random access file to pop the datagram from.
	 * @param lpcOrder
	 *            lpcOrder
	 * @throws IOException
	 *             IOException
	 * @throws EOFException
	 *             EOFException
	 */
	public LPCDatagram(RandomAccessFile raf, int lpcOrder) throws IOException, EOFException {
		super(raf.readLong()); // duration
		int len = raf.readInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		if (len < 2 * lpcOrder) {
			throw new IOException("LPC datagram too short (len=" + len
					+ "): cannot be shorter than the space needed for lpc coefficients (2*" + lpcOrder + ")");
		}
		// For speed concerns, read into a byte[] first:
		byte[] buf = new byte[len];
		raf.readFully(buf);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));

		int residualLength = len - 2 * lpcOrder;
		quantizedCoeffs = new short[lpcOrder];
		quantizedResidual = new byte[residualLength];
		for (int i = 0; i < lpcOrder; i++) {
			quantizedCoeffs[i] = dis.readShort();
		}
		System.arraycopy(buf, 2 * lpcOrder, quantizedResidual, 0, residualLength);
	}

	/**
	 * Constructor which pops a datagram from a byte buffer.
	 * 
	 * @param bb
	 *            the byte buffer to pop the datagram from.
	 * @param lpcOrder
	 *            lpcOrder
	 * @throws IOException
	 *             IOException
	 * @throws EOFException
	 *             EOFException
	 */
	public LPCDatagram(ByteBuffer bb, int lpcOrder) throws IOException, EOFException {
		super(bb.getLong()); // duration
		int len = bb.getInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		if (len < 2 * lpcOrder) {
			throw new IOException("LPC datagram too short (len=" + len
					+ "): cannot be shorter than the space needed for lpc coefficients (2*" + lpcOrder + ")");
		}

		int residualLength = len - 2 * lpcOrder;
		quantizedCoeffs = new short[lpcOrder];
		quantizedResidual = new byte[residualLength];
		for (int i = 0; i < lpcOrder; i++) {
			quantizedCoeffs[i] = bb.getShort();
		}
		bb.get(quantizedResidual);
	}

	/**
	 * Get the length, in bytes, of the datagram's data field.
	 */
	public int getLength() {
		return 2 * quantizedCoeffs.length + quantizedResidual.length;
	}

	/**
	 * Get the LPC order, i.e. the number of LPC coefficients.
	 * 
	 * @return the lpc order
	 * @see #getQuantizedCoeffs()
	 * @see #getCoeffs(float lpcMin, float lpcRange)
	 */
	public int lpcOrder() {
		return quantizedCoeffs.length;
	}

	/**
	 * Get the quantized lpc coefficients
	 * 
	 * @return an array of shorts, length lpcOrder()
	 * @see #lpcOrder()
	 * @see #getCoeffs(float lpcMin, float lpcRange)
	 */
	public short[] getQuantizedCoeffs() {
		return quantizedCoeffs;
	}

	/**
	 * Get the quantized residual.
	 * 
	 * @return an array of bytes
	 */
	public byte[] getQuantizedResidual() {
		return quantizedResidual;
	}

	/**
	 * Get the LPC coefficients, unquantized using the given lpc min and range values.
	 * 
	 * @param lpcMin
	 *            the lpc minimum
	 * @param lpcRange
	 *            the lpc range
	 * @return an array of floats, length lpcOrder()
	 * @see #lpcOrder()
	 * @see #getQuantizedCoeffs()
	 */
	public float[] getCoeffs(float lpcMin, float lpcRange) {
		return General.unQuantize(quantizedCoeffs, lpcMin, lpcRange);
	}

	/**
	 * Get the unquantized residual
	 * 
	 * @return an array of shorts
	 */
	public short[] getResidual() {
		return General.ulawToShort(quantizedResidual);
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
		for (int i = 0; i < quantizedCoeffs.length; i++) {
			out.writeShort(quantizedCoeffs[i]);
		}
		out.write(quantizedResidual);
	}

	/**
	 * Tests if this datagram is equal to another datagram.
	 * 
	 * @param other
	 *            other
	 * @return true if everything passes
	 */
	public boolean equals(Datagram other) {
		if (!(other instanceof LPCDatagram))
			return false;
		LPCDatagram otherLPC = (LPCDatagram) other;
		if (this.duration != otherLPC.duration)
			return false;
		if (this.quantizedCoeffs.length != otherLPC.quantizedCoeffs.length)
			return false;
		if (this.quantizedResidual.length != otherLPC.quantizedResidual.length)
			return false;
		for (int i = 0; i < this.quantizedCoeffs.length; i++) {
			if (this.quantizedCoeffs[i] != otherLPC.quantizedCoeffs[i])
				return false;
		}
		for (int i = 0; i < this.quantizedResidual.length; i++) {
			if (this.quantizedResidual[i] != otherLPC.quantizedResidual[i])
				return false;
		}
		return true;
	}

}
