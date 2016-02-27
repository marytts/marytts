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
package marytts.signalproc.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.MaryRandomAccessFile;
import marytts.util.signal.SignalProcUtils;

/**
 * A class that extracts frame based root-mean-square (RMS) energy contour
 * 
 * @author Oytun T&uuml;rk
 */
public class EnergyContourRms {
	public EnergyFileHeader header;
	public double[] contour;

	public EnergyContourRms() throws IOException {
		this(null);
	}

	public EnergyContourRms(String wavFile) throws IOException {
		this(wavFile, null);
	}

	public EnergyContourRms(String wavFile, String energyFile) throws IOException {
		this(wavFile, energyFile, EnergyFileHeader.DEFAULT_WINDOW_SIZE, EnergyFileHeader.DEFAULT_SKIP_SIZE);
	}

	public EnergyContourRms(String wavFileIn, double windowSizeInSecondsIn, double skipSizeInSecondsIn) throws IOException {
		this(wavFileIn, null, windowSizeInSecondsIn, skipSizeInSecondsIn);
	}

	public EnergyContourRms(String wavFileIn, String energyFileOut, double windowSizeInSecondsIn, double skipSizeInSecondsIn)
			throws IOException {
		header = new EnergyFileHeader();

		header.windowSizeInSeconds = windowSizeInSecondsIn;
		header.skipSizeInSeconds = skipSizeInSecondsIn;

		if (wavFileIn != null) {
			AudioInputStream inputAudio = null;
			try {
				inputAudio = AudioSystem.getAudioInputStream(new File(wavFileIn));
			} catch (UnsupportedAudioFileException e) {
				throw new IOException("Unsupported audio file: " + wavFileIn);
			}

			if (inputAudio != null) {
				header.samplingRate = (int) inputAudio.getFormat().getSampleRate();

				AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);

				contour = SignalProcUtils.getEnergyContourRms(signal.getAllData(), header.windowSizeInSeconds,
						header.skipSizeInSeconds, header.samplingRate);

				header.totalFrames = contour.length;

				if (energyFileOut != null && energyFileOut != null) {
					WriteEnergyFile(this, energyFileOut);
				}
			}
		}
	}

	public static void WriteEnergyFile(EnergyContourRms en, String energyFile) throws IOException {
		if (en.contour != null) {
			en.header.totalFrames = en.contour.length;

			MaryRandomAccessFile ler = null;
			try {
				ler = new MaryRandomAccessFile(energyFile, "rw");
			} catch (FileNotFoundException e) {
				throw new IOException("File not found: " + energyFile);
			}

			en.header.write(ler);

			ler.writeDouble(en.contour);

			ler.close();
		}
	}

	public static EnergyContourRms ReadEnergyFile(String energyFile) throws IOException {
		EnergyContourRms en = null;
		MaryRandomAccessFile ler = null;
		try {
			ler = new MaryRandomAccessFile(energyFile, "r");
		} catch (FileNotFoundException e) {
			throw new IOException("File not found: " + energyFile);
		}

		en = new EnergyContourRms();
		en.header.read(ler, true);

		en.contour = ler.readDouble(en.header.totalFrames);

		ler.close();

		return en;
	}
}
