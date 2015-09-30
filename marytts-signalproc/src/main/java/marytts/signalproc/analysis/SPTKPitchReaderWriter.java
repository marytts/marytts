/**
 * Copyright 2010 DFKI GmbH.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;

/**
 * File I/O for binary SPTK pitch contour files
 * 
 * @author Sathish Pammi
 * 
 */
public class SPTKPitchReaderWriter {

	private PitchFileHeader header;
	private double[] contour; // f0 values in Hz (0.0 for unvoiced)
	private final float NEGATIVE_MAXIMUM = (float) -1.00E10;

	/**
	 * read a SPTK lf0 file with following default values windowSizeInSeconds = 0.005f; skipSizeInSeconds = 0.005f; fs = 16000; //
	 * in Hz
	 * 
	 * @param lf0SPTKFile
	 *            lf0SPTKFile
	 */
	public SPTKPitchReaderWriter(String lf0SPTKFile) {

		this(lf0SPTKFile, 0.005f, 0.005f, 16000);

	}

	/**
	 * read a SPTK lf0 file with external settings
	 * 
	 * @param lf0SPTKFile
	 *            lf0SPTKFile
	 * @param windowSizeInSeconds
	 *            windowSizeInSeconds
	 * @param skipSizeInSeconds
	 *            skipSizeInSeconds
	 * @param samplingRate
	 *            samplingRate
	 */
	public SPTKPitchReaderWriter(String lf0SPTKFile, float windowSizeInSeconds, float skipSizeInSeconds, int samplingRate) {
		contour = null;

		header = new PitchFileHeader();
		header.windowSizeInSeconds = windowSizeInSeconds;
		header.skipSizeInSeconds = skipSizeInSeconds;
		header.fs = samplingRate;

		try {
			contour = readSPTKF0Data(lf0SPTKFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * create a SPTK Pitch reader writer with external contour
	 * 
	 * @param contour
	 *            contour
	 * @param header
	 *            header
	 */
	public SPTKPitchReaderWriter(double[] contour, PitchFileHeader header) {
		this.contour = contour;
		this.header = header;
	}

	/**
	 * get f0 values in Hz (0.0 for unvoiced)
	 * 
	 * @return self.contour
	 */
	public double[] getF0Contour() {
		return this.contour;
	}

	/**
	 * get pitch file header
	 * 
	 * @return self.header
	 */
	public PitchFileHeader getPitchFileHeader() {
		return this.header;
	}

	/**
	 * 
	 * @param lf0SPTKFile
	 *            lf0SPTKFile
	 * @return totalFrame
	 * @throws IOException
	 *             IO Exception
	 */
	private int getNumberOfFrames(String lf0SPTKFile) throws IOException {

		LEDataInputStream lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0SPTKFile)));
		float fval;
		int totalFrame = 0;

		/* First i need to know the size of the vectors */
		try {
			while (true) {
				fval = lf0Data.readFloat();
				totalFrame++;
			}
		} catch (EOFException e) {
			lf0Data.close();
		}

		return totalFrame;
	}

	/**
	 * convert a SPTK file into contour[]
	 * 
	 * @param lf0SPTKFile
	 *            lf0SPTKFile
	 * @return null if !FileUtils.exists(lf0SPTKFile), f0Data otherwise
	 * @throws IOException
	 *             IO Exception
	 */
	private double[] readSPTKF0Data(String lf0SPTKFile) throws IOException {

		if (!FileUtils.exists(lf0SPTKFile)) {
			System.out.println("SPTK Pitch file not found: " + lf0SPTKFile);
			return null;
		}

		int numberOfFrames = getNumberOfFrames(lf0SPTKFile);
		LEDataInputStream lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0SPTKFile)));

		double[] f0Data = new double[numberOfFrames];
		for (int i = 0; i < numberOfFrames; i++) {
			float f0Value = lf0Data.readFloat();
			if (f0Value < 0) {
				f0Data[i] = 0.0f;
			} else {
				f0Data[i] = new Double(Math.exp(f0Value));
			}
		}
		return f0Data;
	}

	/**
	 * write contour into a lf0 file in SPTK format
	 * 
	 * @param sptkFileName
	 *            sptk File Name
	 * @throws IOException
	 *             IO Exception
	 */
	public void writeIntoSPTKLF0File(String sptkFileName) throws IOException {
		LEDataOutputStream lf0Data = new LEDataOutputStream(new BufferedOutputStream(new FileOutputStream(sptkFileName)));
		for (int i = 0; i < this.contour.length; i++) {
			double f0Val = contour[i];
			if (contour[i] == 0.0f) {
				lf0Data.writeFloat(NEGATIVE_MAXIMUM);
			} else {
				lf0Data.writeFloat((float) Math.log(contour[i]));
			}
		}
		lf0Data.flush();
		lf0Data.close();
	}

	/**
	 * write contour into a lf0 file in MARY PTC format
	 * 
	 * @param ptcFileName
	 *            ptc file name
	 * @throws IOException
	 *             IO Exception
	 */
	public void writeIntoMARYPTCfile(String ptcFileName) throws IOException {
		PitchReaderWriter.write_pitch_file(ptcFileName, this.contour, (float) this.header.windowSizeInSeconds,
				(float) this.header.skipSizeInSeconds, this.header.fs);
	}

	/**
	 * @param args
	 *            args
	 * @throws IOException
	 *             IO Exception
	 */
	public static void main(String[] args) throws IOException {

		String lf0File = "/home/sathish/Desktop/test/Poppy2_091.lf0";
		SPTKPitchReaderWriter sprw = new SPTKPitchReaderWriter(lf0File);
		double[] f0contour = sprw.getF0Contour();
		PitchFileHeader hdr = sprw.getPitchFileHeader();
		SPTKPitchReaderWriter sprw1 = new SPTKPitchReaderWriter(f0contour, hdr);
		sprw1.writeIntoSPTKLF0File("/home/sathish/Desktop/test/Dummy.lf0");

		SPTKPitchReaderWriter sprw2 = new SPTKPitchReaderWriter("/home/sathish/Desktop/test/Dummy.lf0");
		f0contour = sprw2.getF0Contour();
		hdr = sprw2.getPitchFileHeader();

		for (int i = 0; i < f0contour.length; i++) {
			System.out.println((float) Math.log(f0contour[i]));
		}

		System.out.println("No. of frames: " + f0contour.length);

	}

}
