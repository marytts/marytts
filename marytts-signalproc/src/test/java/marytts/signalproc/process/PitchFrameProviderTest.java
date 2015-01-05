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

import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.text.ESTTextfileDoubleDataSource;
import marytts.util.math.MathUtils;

import org.junit.Test;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class PitchFrameProviderTest {
	@Test
	public void testIdentity1() throws Exception {
		AudioInputStream ais = AudioSystem.getAudioInputStream(PitchFrameProviderTest.class
				.getResourceAsStream("arctic_a0123.wav"));
		int samplingRate = (int) ais.getFormat().getSampleRate();
		DoubleDataSource signal = new AudioDoubleDataSource(ais);
		double[] origSignal = signal.getAllData();
		signal = new BufferedDoubleDataSource(origSignal);
		DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new InputStreamReader(
				PitchFrameProviderTest.class.getResourceAsStream("arctic_a0123.pm")));
		double[] origPitchmarks = pitchmarks.getAllData();
		double audioDuration = origSignal.length / (double) samplingRate;
		if (origPitchmarks[origPitchmarks.length - 1] < audioDuration) {
			System.out.println("correcting last pitchmark to total audio duration: " + audioDuration);
			origPitchmarks[origPitchmarks.length - 1] = audioDuration;
		}
		pitchmarks = new BufferedDoubleDataSource(origPitchmarks);
		PitchFrameProvider pfp = new PitchFrameProvider(signal, pitchmarks, null, samplingRate);
		double[] result = new double[origSignal.length];
		double[] frame = null;
		int resultPos = 0;
		while ((frame = pfp.getNextFrame()) != null) {
			int periodLength = pfp.validSamplesInFrame();
			System.arraycopy(frame, 0, result, resultPos, periodLength);
			resultPos += periodLength;
		}
		assertTrue("Got back " + resultPos + ", expected " + origSignal.length, resultPos == origSignal.length);
		double err = MathUtils.sumSquaredError(origSignal, result);
		assertTrue("Error: " + err, err < 1.E-20);
	}

	@Test
	public void testIdentity2() throws Exception {
		AudioInputStream ais = AudioSystem.getAudioInputStream(PitchFrameProviderTest.class
				.getResourceAsStream("arctic_a0123.wav"));
		int samplingRate = (int) ais.getFormat().getSampleRate();
		DoubleDataSource signal = new AudioDoubleDataSource(ais);
		double[] origSignal = signal.getAllData();
		signal = new BufferedDoubleDataSource(origSignal);
		DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new InputStreamReader(
				PitchFrameProviderTest.class.getResourceAsStream("arctic_a0123.pm")));
		double[] origPitchmarks = pitchmarks.getAllData();
		double audioDuration = origSignal.length / (double) samplingRate;
		if (origPitchmarks[origPitchmarks.length - 1] < audioDuration) {
			System.out.println("correcting last pitchmark to total audio duration: " + audioDuration);
			origPitchmarks[origPitchmarks.length - 1] = audioDuration;
		}
		pitchmarks = new BufferedDoubleDataSource(origPitchmarks);
		PitchFrameProvider pfp = new PitchFrameProvider(new SequenceDoubleDataSource(new DoubleDataSource[] { signal,
				new BufferedDoubleDataSource(new double[1000]) }), pitchmarks, null, samplingRate, 2, 1);
		double[] result = new double[origSignal.length];
		double[] frame = null;
		int resultPos = 0;
		while ((frame = pfp.getNextFrame()) != null) {
			int toCopy = Math.min(pfp.getFrameShiftSamples(), pfp.validSamplesInFrame());
			System.arraycopy(frame, 0, result, resultPos, toCopy);
			resultPos += toCopy;
		}
		assertTrue("Got back " + resultPos + ", expected " + origSignal.length, resultPos == origSignal.length);
		double err = MathUtils.sumSquaredError(origSignal, result);
		assertTrue("Error: " + err, err < 1.E-20);
	}

}
