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
package marytts.signalproc.analysis.distance;

import java.io.IOException;

/**
 * Implements root-mean-square LP spectral envelope distance between two speech frames
 * 
 * @author Oytun T&uuml;rk
 */
public class RmsLogLPSpectralEnvelopeDistortionComputer extends BaselineLPSpectralEnvelopeDistortionComputer {

	public RmsLogLPSpectralEnvelopeDistortionComputer() {
		super();
	}

	public double frameDistance(double[] frm1, double[] frm2, int fftSize, int lpOrder) {
		super.frameDistance(frm1, frm2, fftSize, lpOrder);

		return SpectralDistanceMeasures.rmsLogSpectralDist(frm1, frm2, fftSize, lpOrder);
	}

	public void mainParametricInterspeech2008(String outputFolder, String method, String emotion, String outputFilePostExtension)
			throws IOException {
		String tgtFolder = outputFolder + "target/" + emotion;
		String srcFolder = outputFolder + "source/" + emotion;
		String tfmFolder = outputFolder + method + "/" + emotion;
		String outputFile = outputFolder + method + "_" + emotion + "_" + outputFilePostExtension;
		String infoString = method + " " + emotion;

		mainParametric(srcFolder, tgtFolder, tfmFolder, outputFile, infoString);
	}

	// Put source and target wav and lab files into two folders and call this function
	public void mainInterspeech2008() throws IOException {
		String method; // "1_codebook"; "2_frame"; "3_gmm";
		String emotion; // "angry"; "happy"; "sad"; "all";
		String outputFolder = "D:/Oytun/DFKI/voices/Interspeech08_out/objective_test/";
		String tgtFolder, srcFolder, tfmFolder, outputFile, infoString;
		String outputFilePostExtension = "itakuraSaitoLPSpectralEnvelope.txt";

		// Method 1: Weighted codebook
		method = "1_codebook";
		emotion = "angry";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "happy";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "sad";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "all";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		//

		// Method 2: Frame weighting
		method = "2_frame";
		emotion = "angry";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "happy";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "sad";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "all";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		//

		// Method 3: GMM
		method = "3_gmm";
		emotion = "angry";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "happy";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "sad";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		emotion = "all";
		mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
		//

		System.out.println("Objective test completed...");
	}

	// Put source and target wav and lab files into two folders and call this function
	public void mainHmmVoiceConversion() throws IOException {
		RmsLogLPSpectralEnvelopeDistortionComputer sdc = new RmsLogLPSpectralEnvelopeDistortionComputer();

		String baseInputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/";
		String baseOutputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/objective_test/";
		boolean isBark = true;
		String method1, method2, folder1, folder2, referenceFolder, outputFile, infoString;

		referenceFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/origTarget";

		// No-GV vs GV
		method1 = "NOGV";
		method2 = "GV";
		folder1 = baseInputFolder + "hmmSource_nogv";
		folder2 = baseInputFolder + "hmmSource_gv";
		outputFile = baseOutputFolder + "IS_" + method1 + "_" + method2 + ".txt";
		infoString = method1 + " " + method2;
		mainParametric(folder1, folder2, referenceFolder, outputFile, infoString);

		// No-GV vs SC
		method1 = "NOGV";
		method2 = "NOGV+SC";
		folder1 = baseInputFolder + "hmmSource_nogv";
		folder2 = baseInputFolder + "tfm_nogv_1092files_128mixes";
		outputFile = baseOutputFolder + "IS_" + method1 + "_" + method2 + ".txt";
		infoString = method1 + " " + method2;
		mainParametric(folder1, folder2, referenceFolder, outputFile, infoString);

		// GV vs SC
		method1 = "GV";
		method2 = "GV+SC";
		folder1 = baseInputFolder + "hmmSource_gv";
		folder2 = baseInputFolder + "tfm_gv_1092files_128mixes";
		outputFile = baseOutputFolder + "IS_" + method1 + "_" + method2 + ".txt";
		infoString = method1 + " " + method2;
		mainParametric(folder1, folder2, referenceFolder, outputFile, infoString);

		System.out.println("Objective test completed...");
	}

	public static void main(String[] args) throws IOException {
		RmsLogLPSpectralEnvelopeDistortionComputer d = new RmsLogLPSpectralEnvelopeDistortionComputer();

		// d.mainInterspeech2008();

		d.mainHmmVoiceConversion();
	}
}
