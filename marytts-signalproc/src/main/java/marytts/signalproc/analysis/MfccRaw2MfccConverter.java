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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.io.BasenameList;
import marytts.util.io.FileUtils;
import marytts.util.io.MaryRandomAccessFile;
import marytts.util.string.StringUtils;

/**
 * Converts binary MFCC files in raw SPTK format into Mary MFCC file format with header
 * 
 * @author Oytun T&uuml;rk
 */
public class MfccRaw2MfccConverter {
	public static void convertFolder(String folder, String rawMfccFileExtension, String outputMfccFileExtension, int dimension,
			int samplingRateInHz, float windowSizeInSeconds, float skipSizeInSeconds) {
		folder = StringUtils.checkLastSlash(folder);

		BasenameList b = new BasenameList(folder, rawMfccFileExtension);

		String rawMfccFile;
		String outputMfccFile;
		int numFiles = b.getListAsVector().size();
		for (int i = 0; i < numFiles; i++) {
			rawMfccFile = folder + b.getName(i) + rawMfccFileExtension;
			outputMfccFile = StringUtils.modifyExtension(rawMfccFile, outputMfccFileExtension);
			rawFile2mfccFile(rawMfccFile, outputMfccFile, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);

			System.out.println("Converted MFCC file " + String.valueOf(i + 1) + " of " + String.valueOf(numFiles));
		}
	}

	public static void rawFile2mfccFile(String rawFile, String mfccFile, int dimension, int samplingRateInHz,
			float windowSizeInSeconds, float skipSizeInSeconds) {
		Mfccs m = readRawMfccFile(rawFile, dimension);

		m.params.samplingRate = samplingRateInHz;
		m.params.skipsize = skipSizeInSeconds;
		m.params.winsize = windowSizeInSeconds;

		m.writeMfccFile(mfccFile);
	}

	// This version is for reading SPTK files that have no header
	// The header is created from user specified information
	public static Mfccs readRawMfccFile(String rawMfccFile, int dimension) {

		MfccFileHeader params = new MfccFileHeader();

		File f = new File(rawMfccFile);
		long fileSize = f.length();
		int numfrm = (int) (fileSize / (4.0 * dimension));
		Mfccs m = new Mfccs(numfrm, dimension);
		params.numfrm = numfrm;
		params.dimension = dimension;

		if (rawMfccFile != "" && FileUtils.exists(rawMfccFile)) {
			MaryRandomAccessFile stream = null;
			try {
				stream = new MaryRandomAccessFile(rawMfccFile, "rw");
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (stream != null) {
				try {
					Mfccs.readMfccsFromFloat(stream, params, m.mfccs);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return m;
	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		String folder;
		String rawMfccFileExtension = ".mgc";
		String outputMfccFileExtension = ".mfc";
		int dimension = 25;

		int samplingRateInHz = 16000;
		float windowSizeInSeconds = 0.040f; // 400 samples
		float skipSizeInSeconds = 0.005f; // 80 samples

		String baseFolder;

		/*
		 * baseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/hsmmMfccRaw_25Dimensional/"; folder = baseFolder +
		 * "hmm_gen_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension,
		 * dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "original_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "test/hmm_gen_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "test/original_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 */

		dimension = 21;
		baseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/lspRaw_21Dimensional/";
		folder = baseFolder + "hmm_gen_parameters";
		MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz,
				windowSizeInSeconds, skipSizeInSeconds);

		folder = baseFolder + "original_parameters";
		MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz,
				windowSizeInSeconds, skipSizeInSeconds);

		folder = baseFolder + "test/hmm_gen_parameters";
		MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz,
				windowSizeInSeconds, skipSizeInSeconds);

		folder = baseFolder + "test/original_parameters";
		MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz,
				windowSizeInSeconds, skipSizeInSeconds);

		/*
		 * dimension = 21; baseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mellspRaw_21Dimensional/"; folder =
		 * baseFolder + "hmm_gen_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "original_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "test/hmm_gen_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "test/original_parameters"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 */

		/*
		 * dimension = 25; baseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mfccRaw_25Dimensional/"; folder = baseFolder
		 * + "hmm_train"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension,
		 * samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "hmm_test"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "orig_train"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 * 
		 * folder = baseFolder + "orig_test"; MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension,
		 * outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
		 */
	}
}
