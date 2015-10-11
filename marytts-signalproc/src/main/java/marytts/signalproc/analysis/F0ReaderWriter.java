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
package marytts.signalproc.analysis;

import java.io.IOException;

import marytts.util.string.StringUtils;

/**
 * File I/O for binary pitch contour files
 * 
 * @author Oytun T&uuml;rk
 */
public class F0ReaderWriter extends PitchReaderWriter {

	public static final int DEFAULT_SAMPLING_RATE = 16000;
	public static final double DEFAULT_WINDOW_SIZE_IN_SECONDS = 0.0075;
	public static final double DEFAULT_SKIP_SIZE_IN_SECONDS = 0.01;

	public F0ReaderWriter(String f0File) {
		this(f0File, DEFAULT_SAMPLING_RATE, DEFAULT_WINDOW_SIZE_IN_SECONDS, DEFAULT_SKIP_SIZE_IN_SECONDS);
	}

	public F0ReaderWriter(String f0File, int samplingRate, double windowSizeInSeconds, double skipSizeInSeconds) {
		contour = null;

		header = new PitchFileHeader();

		header.windowSizeInSeconds = 0.0;
		header.skipSizeInSeconds = 0.0;
		header.fs = 0;

		try {
			read_f0_file(f0File);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public F0ReaderWriter() {
		contour = null;

		header = new PitchFileHeader();

		header.windowSizeInSeconds = 0.0;
		header.skipSizeInSeconds = 0.0;
		header.fs = 0;
	}

	public void read_f0_file(String f0File) throws IOException {
		read_f0_file(f0File, DEFAULT_SAMPLING_RATE, DEFAULT_WINDOW_SIZE_IN_SECONDS, DEFAULT_SKIP_SIZE_IN_SECONDS);
	}

	// Reads from Snack/Wavesurfer generated .f0 files
	public void read_f0_file(String f0File, int samplingRate, double windowSizeInSeconds, double skipSizeInSeconds)
			throws IOException {
		String[] lines = StringUtils.readTextFile(f0File, "ASCII");
		if (lines != null && lines[0] != null) {
			contour = new double[lines.length];

			int endIndex;
			for (int i = 0; i < lines.length; i++) {
				endIndex = lines[i].indexOf(" ");
				contour[i] = Double.valueOf(lines[i].substring(0, endIndex));
			}

			header.fs = samplingRate;
			header.numfrm = contour.length;

			header.windowSizeInSeconds = windowSizeInSeconds;
			header.skipSizeInSeconds = skipSizeInSeconds;
		}
	}
}
