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
package marytts.signalproc.process;

import java.io.File;
import java.io.FileReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.text.ESTTextfileDoubleDataSource;
import marytts.util.data.text.LabelfileDoubleDataSource;
import marytts.util.io.FileUtils;

public class PitchLsfInterpolator {

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		double r = Double.parseDouble(System.getProperty("r", "0.5"));
		String file1 = null;
		String pm1 = null;
		String file2 = null;
		String pm2 = null;
		DoubleDataSource label1 = null;
		DoubleDataSource label2 = null;
		if (args.length == 4) {
			file1 = args[0];
			pm1 = args[1];
			file2 = args[2];
			pm2 = args[3];
		} else if (args.length == 6) {
			file1 = args[0];
			pm1 = args[1];
			label1 = new LabelfileDoubleDataSource(new FileReader(args[2]));
			file2 = args[3];
			pm2 = args[4];
			label2 = new LabelfileDoubleDataSource(new FileReader(args[5]));
			// Safety check: verify that we have the same number of labels in both files
			double[] labelData1 = label1.getAllData();
			double[] labelData2 = label2.getAllData();
			if (labelData1.length != labelData2.length) {
				System.err.println("Warning: Number of labels is different!");
				System.err.println(args[2] + ":");
				System.err.println(FileUtils.getFileAsString(new File(args[2]), "ASCII"));
				System.err.println(args[5] + ":");
				System.err.println(FileUtils.getFileAsString(new File(args[5]), "ASCII"));
			} // but continue
			label1 = new BufferedDoubleDataSource(labelData1);
			label2 = new BufferedDoubleDataSource(labelData2);
		} else {
			System.out
					.println("Usage: java [-Dr=<mixing ratio> marytts.signalproc.process.PitchLSFInterpolator signal.wav signal.pm [signal.lab] other.wav other.pm [other.lab]");
			System.out.println("where");
			System.out
					.println("    <mixing ratio> is a value between 0.0 and 1.0 indicating how much of \"other\" is supposed to be mixed into \"signal\"");
			System.exit(1);
		}

		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(file1));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pm1));
		AudioInputStream otherAudio = AudioSystem.getAudioInputStream(new File(file2));
		DoubleDataSource otherSource = new AudioDoubleDataSource(otherAudio);
		DoubleDataSource otherPitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pm2));
		int predictionOrder = Integer.getInteger("signalproc.lpcanalysisresynthesis.predictionorder", 20).intValue();
		FramewiseMerger foas = new FramewiseMerger(signal, pitchmarks, samplingRate, label1, otherSource, otherPitchmarks,
				samplingRate, label2, new LSFInterpolator(predictionOrder, r));
		DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
		String outFileName = file1.substring(0, file1.length() - 4) + "_"
				+ file2.substring(file2.lastIndexOf("\\") + 1, file2.length() - 4) + "_" + r + "_ps.wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		long endTime = System.currentTimeMillis();
		int audioDuration = (int) (AudioSystem.getAudioFileFormat(new File(file1)).getFrameLength() / (double) samplingRate * 1000);
		System.out.println("Pitch-synchronous LSF-based interpolatin took " + (endTime - startTime) + " ms for " + audioDuration
				+ " ms of audio");

	}

}
