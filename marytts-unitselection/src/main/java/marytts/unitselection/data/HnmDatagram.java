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

import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.util.data.Datagram;

/**
 * A datagram that encapsulates a harmonics plus noise modelled speech frame
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class HnmDatagram extends Datagram {

	public HntmSpeechFrame frame; // Hnm parameters for a speech frame

	/**
	 * Construct a HNM datagram.
	 * 
	 * @param setDuration
	 *            the duration, in samples, of the data represented by this datagram
	 * @param frame
	 *            the parameters of HNM for a speech frame.
	 */
	public HnmDatagram(long setDuration, HntmSpeechFrame frame) {
		super(setDuration);

		this.frame = new HntmSpeechFrame(frame);
	}

	/**
	 * Constructor which pops a datagram from a random access file.
	 * 
	 * @param raf
	 *            the random access file to pop the datagram from.
	 * @param noiseModel
	 *            the noise model
	 * @throws IOException
	 *             IOException
	 * @throws EOFException
	 *             EOFException
	 */
	public HnmDatagram(RandomAccessFile raf, int noiseModel) throws IOException, EOFException {
		super(raf.readLong()); // duration
		int len = raf.readInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		if (len < 4 * 3) {
			throw new IOException("Hnm with waveform noise datagram too short (len=" + len
					+ "): cannot be shorter than the space needed for first three Hnm parameters (4*3)");
		}

		// For speed concerns, read into a byte[] first:
		byte[] buf = new byte[len];
		raf.readFully(buf);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));

		frame = new HntmSpeechFrame(dis, noiseModel);
	}

	/**
	 * Constructor which pops a datagram from a byte buffer.
	 * 
	 * @param bb
	 *            the byte buffer to pop the datagram from.
	 * @param noiseModel
	 *            noiseModel
	 * @throws IOException
	 *             IOException
	 * @throws EOFException
	 *             EOFException
	 */
	public HnmDatagram(ByteBuffer bb, int noiseModel) throws IOException, EOFException {
		super(bb.getLong()); // duration
		int len = bb.getInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		if (len < 4 * 3) {
			throw new IOException("Hnm with waveform noise datagram too short (len=" + len
					+ "): cannot be shorter than the space needed for first three Hnm parameters (4*3)");
		}

		frame = new HntmSpeechFrame(bb, noiseModel);
	}

	/**
	 * Get the length, in bytes, of the datagram's data field.
	 */
	public int getLength() {
		return frame.getLength();
	}

	/**
	 * Get the sinusoidal speech frame
	 * 
	 * @return frame
	 */
	public HntmSpeechFrame getFrame() {
		return frame;
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

		frame.write(out);
	}

	/**
	 * Tests if this datagram is equal to another datagram.
	 * 
	 * @param other
	 *            other
	 * @return true if pass everything
	 */
	public boolean equals(Datagram other) {
		if (!(other instanceof HnmDatagram))
			return false;
		HnmDatagram otherHnm = (HnmDatagram) other;
		if (this.duration != otherHnm.duration)
			return false;
		if (!this.frame.equals(otherHnm.frame))
			return false;

		return true;
	}

}
