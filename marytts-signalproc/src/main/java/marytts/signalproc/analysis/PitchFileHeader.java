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

import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;
import marytts.util.io.MaryRandomAccessFile;

/**
 * Implements a structured header with file I/O functionality for binary files that store frame based f0 values
 * 
 * @author Oytun T&uuml;rk
 */
public class PitchFileHeader {
	public double windowSizeInSeconds; // Window size in seconds
	public double skipSizeInSeconds; // Skip size in seconds
	public int fs; // Rate in Hz
	public int numfrm; // Number of frames
	public double voicingThreshold; // Voicing threshold
	public static double DEFAULT_VOICING_THRESHOLD = 0.35; // Default voicing threshold

	public double minimumF0; // Min f0 in Hz
	public double maximumF0; // Max f0 in Hz
	public static double DEFAULT_MINIMUM_F0 = 50.0;
	public static double DEFAULT_MAXIMUM_F0 = 500.0;

	public boolean isDoublingCheck;
	public boolean isHalvingCheck;
	public static boolean DEFAULT_DOUBLING_CHECK = true;
	public static boolean DEFAULT_HALVING_CHECK = true;

	public double centerClippingRatio;
	public static double DEFAULT_CENTER_CLIPPING_RATIO = 0.5;

	public double cutOff1; // Lower cut-off freq for bandpass filter in Hz (or cut-off freq for lowpass filter if cutOff2<=0.0)
	public double cutOff2; // Upper cut-off freq for bandpass filter in Hz (Set to <=0.0 if you want a lowpass filter with cut-off
							// at cutOff1 Hz.)
	public static double DEFAULT_CUTOFF1 = DEFAULT_MINIMUM_F0 - 20.0;
	public static double DEFAULT_CUTOFF2 = DEFAULT_MAXIMUM_F0 + 200.0;

	public PitchFileHeader() {
		windowSizeInSeconds = 0.040;
		skipSizeInSeconds = 0.005;
		fs = 0;
		numfrm = 0;
		voicingThreshold = DEFAULT_VOICING_THRESHOLD;
		minimumF0 = DEFAULT_MINIMUM_F0;
		maximumF0 = DEFAULT_MAXIMUM_F0;
		isDoublingCheck = DEFAULT_DOUBLING_CHECK;
		isHalvingCheck = DEFAULT_HALVING_CHECK;
		centerClippingRatio = DEFAULT_CENTER_CLIPPING_RATIO;
		cutOff1 = DEFAULT_CUTOFF1;
		cutOff2 = DEFAULT_CUTOFF2;
	}

	public PitchFileHeader(PitchFileHeader existingHeader) {
		windowSizeInSeconds = existingHeader.windowSizeInSeconds;
		skipSizeInSeconds = existingHeader.skipSizeInSeconds;
		fs = existingHeader.fs;
		numfrm = existingHeader.numfrm;
		voicingThreshold = existingHeader.voicingThreshold;
		minimumF0 = existingHeader.minimumF0;
		maximumF0 = existingHeader.maximumF0;
		isDoublingCheck = existingHeader.isDoublingCheck;
		isHalvingCheck = existingHeader.isHalvingCheck;
		centerClippingRatio = existingHeader.centerClippingRatio;
		cutOff1 = existingHeader.cutOff1;
		cutOff2 = existingHeader.cutOff2;
	}

	public void readPitchHeader(MaryRandomAccessFile stream) throws IOException {
		readPitchHeader(stream, true);
	}

	public void readPitchHeader(MaryRandomAccessFile stream, boolean bLeaveStreamOpen) throws IOException {
		if (stream != null) {
			windowSizeInSeconds = stream.readDouble();
			skipSizeInSeconds = stream.readDouble();
			fs = stream.readInt();
			numfrm = stream.readInt();
			voicingThreshold = stream.readDouble();
			minimumF0 = stream.readDouble();
			maximumF0 = stream.readDouble();
			isDoublingCheck = stream.readBoolean();
			isHalvingCheck = stream.readBoolean();
			centerClippingRatio = stream.readDouble();
			cutOff1 = stream.readDouble();
			cutOff2 = stream.readDouble();

			if (!bLeaveStreamOpen) {
				stream.close();
				stream = null;
			}
		}
	}

	public void readPitchHeaderOld(LEDataInputStream stream) throws IOException {
		readPitchHeaderOld(stream, true);
	}

	// The old version kept window size and skip size in samples so perform conversion to seconds after reading them from file
	public void readPitchHeaderOld(LEDataInputStream stream, boolean bLeaveStreamOpen) throws IOException {
		if (stream != null) {
			windowSizeInSeconds = stream.readFloat();
			skipSizeInSeconds = stream.readFloat();
			fs = stream.readInt();
			numfrm = stream.readInt();
			voicingThreshold = DEFAULT_VOICING_THRESHOLD;
			minimumF0 = DEFAULT_MINIMUM_F0;
			maximumF0 = DEFAULT_MAXIMUM_F0;
			isDoublingCheck = DEFAULT_DOUBLING_CHECK;
			isHalvingCheck = DEFAULT_HALVING_CHECK;
			centerClippingRatio = DEFAULT_CENTER_CLIPPING_RATIO;
			cutOff1 = DEFAULT_CUTOFF1;
			cutOff2 = DEFAULT_CUTOFF2;

			windowSizeInSeconds = windowSizeInSeconds / fs;
			skipSizeInSeconds = skipSizeInSeconds / fs;

			if (!bLeaveStreamOpen) {
				stream.close();
				stream = null;
			}
		}
	}

	public void writePitchHeader(String pitchFile) throws IOException {
		writePitchHeader(pitchFile, false);
	}

	// This version returns the file output stream for further use, i.e. if you want to write additional information
	// in the file use this version
	public MaryRandomAccessFile writePitchHeader(String pitchFile, boolean bLeaveStreamOpen) throws IOException {
		MaryRandomAccessFile stream = new MaryRandomAccessFile(pitchFile, "rw");

		if (stream != null) {
			writePitchHeader(stream);

			if (!bLeaveStreamOpen) {
				stream.close();
				stream = null;
			}
		}

		return stream;
	}

	public void writePitchHeader(MaryRandomAccessFile ler) throws IOException {
		ler.writeDouble(windowSizeInSeconds);
		ler.writeDouble(skipSizeInSeconds);
		ler.writeInt(fs);
		ler.writeInt(numfrm);
		ler.writeDouble(voicingThreshold);
		ler.writeDouble(minimumF0);
		ler.writeDouble(maximumF0);
		ler.writeBoolean(isDoublingCheck);
		ler.writeBoolean(isHalvingCheck);
		ler.writeDouble(centerClippingRatio);
		ler.writeDouble(cutOff1);
		ler.writeDouble(cutOff2);
	}

	public void writePitchHeaderOld(String pitchFile) throws IOException {
		writePitchHeaderOld(pitchFile, false);
	}

	// This version returns the file output stream for further use, i.e. if you want to write additional information
	// in the file use this version
	public LEDataOutputStream writePitchHeaderOld(String pitchFile, boolean bLeaveStreamOpen) throws IOException {
		LEDataOutputStream stream = new LEDataOutputStream(pitchFile);

		if (stream != null) {
			writePitchHeaderOld(stream);

			if (!bLeaveStreamOpen) {
				stream.close();
				stream = null;
			}
		}

		return stream;
	}

	public void writePitchHeaderOld(LEDataOutputStream ler) throws IOException {
		ler.writeDouble(windowSizeInSeconds);
		ler.writeDouble(skipSizeInSeconds);
		ler.writeInt(fs);
		ler.writeInt(numfrm);
	}

}
