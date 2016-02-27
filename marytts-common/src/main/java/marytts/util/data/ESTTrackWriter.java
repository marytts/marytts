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
package marytts.util.data;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import marytts.util.io.General;

public class ESTTrackWriter {

	private float[] times = null;
	private float[][] frames = null;
	private String feaType = "";

	/**
	 * Plain constructor.
	 * 
	 * @param setTimes
	 *            The vector of frame locations
	 * @param setFrames
	 *            The frames -- can be null if only times are to be written
	 * @param setFeaType
	 *            A string indicating the feature type, for header info (e.g., "LPCC")
	 */
	public ESTTrackWriter(float[] setTimes, float[][] setFrames, String setFeaType) {
		this.times = setTimes;
		this.frames = setFrames;
		this.feaType = setFeaType;
	}

	/**
	 * Triggers the writing of the file to the disk.
	 * 
	 * @param fName
	 *            The name of the file to write to.
	 * @param isBinary
	 *            true for a binary write, or false for an ascii text write.
	 * @param isBigEndian
	 *            true for a big endian write (PowerPC or SPARC), or false for a little endian write (Intel).
	 * 
	 * @throws IOException
	 *             IO Exception
	 */
	public void doWriteAndClose(String fName, boolean isBinary, boolean isBigEndian) throws IOException {

		// Open the file for writing
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fName)));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Can't open file [" + fName + "] for writing.", e);
		}
		// Output the header
		dos.writeBytes("EST_File Track\n");
		if (isBinary) {
			dos.writeBytes("DataType binary\n");
			if (isBigEndian)
				dos.writeBytes("ByteOrder 10\n");
			else
				dos.writeBytes("ByteOrder 01\n");
		} else {
			dos.writeBytes("DataType ascii\n");
		}

		int numChannels;
		if (frames != null && frames.length > 0 && frames[0] != null)
			numChannels = frames[0].length;
		else
			numChannels = 0; // e.g., for pitchmarks
		dos.writeBytes("NumFrames " + times.length + "\n" + "NumChannels " + numChannels + "\n" + "NumAuxChannels 0\n"
				+ "EqualSpace 0\n" + "BreaksPresent true\n" + "CommentChar ;\n");
		String K;
		for (int k = 0; k < numChannels; k++) {
			K = Integer.toString(k);
			dos.writeBytes("Channel_" + K + " " + feaType + "_" + K + "\n");
		}
		dos.writeBytes("EST_Header_End\n");
		// Output the data:
		// - in binary mode
		if (isBinary) {
			for (int i = 0; i < times.length; i++) {
				General.writeFloat(dos, isBigEndian, times[i]);
				General.writeFloat(dos, isBigEndian, 1.0f);
				for (int k = 0; k < numChannels; k++) {
					General.writeFloat(dos, isBigEndian, frames[i][k]);
				}
			}
		}
		// - in ASCII mode
		else {
			for (int i = 0; i < times.length; i++) {
				dos.writeBytes(Float.toString(times[i]));
				dos.writeBytes("\t1\t");
				if (numChannels > 0) {
					dos.writeBytes(Float.toString(frames[i][0]));
					for (int k = 1; k < frames[0].length; k++) {
						dos.writeBytes(" " + Float.toString(frames[i][k]));
					}
				}
				dos.writeBytes("\n");
			}
		}
		// Flush and close
		dos.flush();
		dos.close();

	}

}
