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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class ChildVoiceConverter {

	public static void main(String[] args) throws Exception {
		double samplingRateFactor = Double.valueOf(args[0]).doubleValue();
		for (int i = 1; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			AudioFormat af = new AudioFormat((int) (ais.getFormat().getSampleRate() * samplingRateFactor), ais.getFormat()
					.getSampleSizeInBits(), ais.getFormat().getChannels(), true, ais.getFormat().isBigEndian());
			DDSAudioInputStream ais2 = new DDSAudioInputStream(new AudioDoubleDataSource(ais), af);
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_child.wav";
			AudioSystem.write(ais2, AudioFileFormat.Type.WAVE, new File(outFileName));
		}
	}
}
