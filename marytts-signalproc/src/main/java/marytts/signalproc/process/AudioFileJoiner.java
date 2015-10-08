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
package marytts.signalproc.process;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.audio.SilenceAudioInputStream;

public class AudioFileJoiner {

	/**
	 * Join a prefix and a suffix to each of a set of audio files, normalizing these audio files to the power of the prefix and
	 * suffix.
	 * 
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		List startAudio = new ArrayList(); // to prepend to each argument
		double[] start = null;
		List endAudio = new ArrayList(); // to append to each argument
		double[] end = null;
		List referenceAudio = new ArrayList(); // to normalise power
		int i = 0;
		String prop;
		// The audio format of the first argument is the target format!
		AudioFormat format = AudioSystem.getAudioInputStream(new File(args[0])).getFormat();
		while (!(prop = System.getProperty("audio." + (++i), "args")).equals("args")) {
			DoubleDataSource dds = null;
			if (prop.startsWith("silence:")) {
				double duration = Double.valueOf(prop.substring(prop.indexOf(':') + 1)).doubleValue();
				startAudio.add(new AudioDoubleDataSource(new SilenceAudioInputStream(duration, format)));
			} else {
				AudioInputStream ais = AudioSystem.getAudioInputStream(new File(prop));
				if (!format.equals(ais.getFormat())) // convert to target format
					ais = AudioSystem.getAudioInputStream(format, ais);
				double[] signal = new AudioDoubleDataSource(ais).getAllData();
				startAudio.add(new BufferedDoubleDataSource(signal));
				referenceAudio.add(new BufferedDoubleDataSource(signal));
			}
		}
		if (startAudio.size() > 0)
			start = new SequenceDoubleDataSource(startAudio).getAllData();

		while ((prop = System.getProperty("audio." + (++i))) != null) {
			DoubleDataSource dds = null;
			if (prop.startsWith("silence:")) {
				double duration = Double.valueOf(prop.substring(prop.indexOf(':') + 1)).doubleValue();
				endAudio.add(new AudioDoubleDataSource(new SilenceAudioInputStream(duration, format)));
			} else {
				AudioInputStream ais = AudioSystem.getAudioInputStream(new File(prop));
				if (!format.equals(ais.getFormat())) // convert to target format
					ais = AudioSystem.getAudioInputStream(format, ais);
				double[] signal = new AudioDoubleDataSource(ais).getAllData();
				endAudio.add(new BufferedDoubleDataSource(signal));
				referenceAudio.add(new BufferedDoubleDataSource(signal));
			}
		}
		if (endAudio.size() > 0)
			end = new SequenceDoubleDataSource(endAudio).getAllData();

		EnergyNormaliser powerNormaliser = null;
		if (referenceAudio.size() > 0) {
			powerNormaliser = new EnergyNormaliser(new SequenceDoubleDataSource(referenceAudio));
			System.err.println("Reference power: " + powerNormaliser.getReferencePower());
		}

		for (int k = 0; k < args.length; k++) {
			List result = new ArrayList();
			if (start != null) {
				result.add(new BufferedDoubleDataSource(start));
			}
			File inFile = new File(args[k]);
			AudioInputStream ais = AudioSystem.getAudioInputStream(inFile);
			if (!format.equals(ais.getFormat()))
				ais = AudioSystem.getAudioInputStream(format, ais);
			DoubleDataSource dds = new AudioDoubleDataSource(ais);
			if (powerNormaliser != null)
				dds = powerNormaliser.apply(dds);
			result.add(dds);
			if (end != null) {
				result.add(new BufferedDoubleDataSource(end));
			}
			DoubleDataSource resultDDS = new SequenceDoubleDataSource(result);
			AudioInputStream resultStream = new DDSAudioInputStream(resultDDS, format);
			String prefix = System.getProperty("prefix", "joined_");
			String filename = inFile.getName();
			filename = prefix + filename.substring(0, filename.lastIndexOf('.')) + ".wav";
			File outFile = new File(filename); // in the current directory
			AudioSystem.write(resultStream, AudioFileFormat.Type.WAVE, outFile);
			System.out.println("Wrote " + outFile.getPath());
		}
	}

}
