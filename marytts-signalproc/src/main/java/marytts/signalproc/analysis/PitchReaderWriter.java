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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * File I/O for binary pitch contour files
 * 
 * @author Oytun T&uuml;rk
 */
public class PitchReaderWriter {
	public PitchFileHeader header;
	public double[] contour; // f0 values in Hz (0.0 for unvoiced)

	public PitchReaderWriter(String file) {
		contour = null;

		header = new PitchFileHeader();

		header.windowSizeInSeconds = 0.0;
		header.skipSizeInSeconds = 0.0;
		header.fs = 0;

		try {
			read_pitch_file(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public PitchReaderWriter() {
		contour = null;

		header = new PitchFileHeader();

		header.windowSizeInSeconds = 0.0;
		header.skipSizeInSeconds = 0.0;
		header.fs = 0;
	}

	// Create f0 contour from pitch marks
	// Note that, as we do not have voicing information, an all-voiced pitch contour is generated
	// using whatever pitch period is assigned to unvoiced segments in the pitch marks
	public PitchReaderWriter(int[] pitchMarks, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds) {
		contour = null;

		header = new PitchFileHeader();

		header.windowSizeInSeconds = windowSizeInSeconds;
		header.skipSizeInSeconds = skipSizeInSeconds;
		header.fs = samplingRate;
		float currentTime;
		int currentInd;

		if (pitchMarks != null && pitchMarks.length > 1) {
			int numfrm = (int) Math.floor(((float) pitchMarks[pitchMarks.length - 2]) / header.fs / header.skipSizeInSeconds
					+ 0.5);

			if (numfrm > 0) {
				float[] onsets = SignalProcUtils.samples2times(pitchMarks, header.fs);

				contour = new double[numfrm];
				for (int i = 0; i < numfrm; i++) {
					currentTime = (float) (i * header.skipSizeInSeconds + 0.5 * header.windowSizeInSeconds);
					currentInd = MathUtils.findClosest(onsets, currentTime);

					if (currentInd < onsets.length - 1)
						contour[i] = header.fs / (pitchMarks[currentInd + 1] - pitchMarks[currentInd]);
					else
						contour[i] = header.fs / (pitchMarks[currentInd] - pitchMarks[currentInd - 1]);
				}
			}
		}
	}

	public double[] getVoiceds() {
		return SignalProcUtils.getVoiceds(contour);
	}

	public void read_pitch_file(String ptcFile) throws IOException {
		if (FileUtils.exists(ptcFile)) {
			LEDataInputStream lr = new LEDataInputStream(new DataInputStream(new FileInputStream(ptcFile)));

			if (lr != null) {
				int winsize = (int) lr.readFloat();
				int skipsize = (int) lr.readFloat();
				header.fs = (int) lr.readFloat();
				header.numfrm = (int) lr.readFloat();

				header.windowSizeInSeconds = ((double) winsize) / header.fs;
				header.skipSizeInSeconds = ((double) skipsize) / header.fs;
				contour = new double[header.numfrm];

				for (int i = 0; i < header.numfrm; i++)
					contour[i] = (double) lr.readFloat();

				lr.close();
			}
		} else
			System.out.println("Pitch file not found: " + ptcFile);
	}

	public static void write_pitch_file(String ptcFile, double[] f0s, float windowSizeInSeconds, float skipSizeInSeconds,
			int samplingRate) throws IOException {
		float[] f0sFloat = new float[f0s.length];
		for (int i = 0; i < f0s.length; i++)
			f0sFloat[i] = (float) f0s[i];

		write_pitch_file(ptcFile, f0sFloat, windowSizeInSeconds, skipSizeInSeconds, samplingRate);
	}

	public static void write_pitch_file(String ptcFile, float[] f0s, float windowSizeInSeconds, float skipSizeInSeconds,
			int samplingRate) throws IOException {
		LEDataOutputStream lw = new LEDataOutputStream(new DataOutputStream(new FileOutputStream(ptcFile)));

		if (lw != null) {
			int winsize = (int) Math.floor(windowSizeInSeconds * samplingRate + 0.5);
			lw.writeFloat(winsize);

			int skipsize = (int) Math.floor(skipSizeInSeconds * samplingRate + 0.5);
			lw.writeFloat(skipsize);

			lw.writeFloat(samplingRate);

			lw.writeFloat(f0s.length);

			lw.writeFloat(f0s);

			lw.close();
		}
	}

	public void setContour(double[] newContour) {
		contour = null;
		header.numfrm = 0;
		if (newContour != null && newContour.length > 0) {
			contour = ArrayUtils.copy(newContour);
			header.numfrm = contour.length;
		}
	}
}
