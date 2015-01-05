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
package marytts.util.data.audio;

import javax.sound.sampled.AudioFormat;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.math.FFTTest;
import marytts.util.math.MathUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class AudioDoubleDataSourceTest {
	@Test
	public void testGetAllData1() {
		int samplingRate = 16000;
		AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplingRate, 16, 1, 2, samplingRate, false);
		double[] testSignal = FFTTest.getSampleSignal(16000);
		DDSAudioInputStream ais = new DDSAudioInputStream(new BufferedDoubleDataSource(testSignal), af);
		double[] result = new AudioDoubleDataSource(ais).getAllData();
		Assert.assertTrue(result.length == testSignal.length);
	}

	@Test
	public void testGetAllData2() {
		int samplingRate = 16000;
		AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplingRate, 16, 1, 2, samplingRate, false);
		double[] signal = FFTTest.getSampleSignal(16000);
		DDSAudioInputStream ais = new DDSAudioInputStream(new BufferedDoubleDataSource(signal), af);
		double[] result = new AudioDoubleDataSource(ais).getAllData();
		double err = MathUtils.sumSquaredError(signal, result);
		Assert.assertTrue("Error: " + err, err < 1.E-20);
	}

}
