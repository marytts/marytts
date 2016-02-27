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
package marytts.signalproc.sinusoidal;

import marytts.util.math.ComplexArray;

/**
 * Single speech frame sinusoids with spectrum
 * 
 * @author Oytun T&uuml;rk
 */
public class NonharmonicSinusoidalSpeechFrame extends BaseSinusoidalSpeechFrame {
	public Sinusoid[] sinusoids;
	public double[] systemAmps;
	public double[] systemPhases;
	public float[] systemCeps;
	public ComplexArray frameDfts;
	public float time;
	public float voicing;
	public float maxFreqOfVoicing;

	public NonharmonicSinusoidalSpeechFrame(int numSins) {
		if (numSins > 0)
			sinusoids = new Sinusoid[numSins];
		else
			sinusoids = null;

		systemAmps = null;
		systemPhases = null;
		systemCeps = null;
		frameDfts = null;
		time = -1.0f;
		voicing = -1.0f;
		maxFreqOfVoicing = -1.0f;
	}

	public NonharmonicSinusoidalSpeechFrame(NonharmonicSinusoidalSpeechFrame existing) {
		this(existing.sinusoids.length);

		for (int i = 0; i < existing.sinusoids.length; i++)
			sinusoids[i] = new Sinusoid(existing.sinusoids[i]);

		setSystemAmps(existing.systemAmps);
		setSystemPhases(existing.systemPhases);
		setSystemCeps(existing.systemCeps);
		setFrameDfts(existing.frameDfts);
		time = existing.time;
		voicing = existing.voicing;
		maxFreqOfVoicing = existing.maxFreqOfVoicing;
	}

	public void setSystemAmps(double[] newAmps) {
		if (newAmps != null && newAmps.length > 0) {
			systemAmps = new double[newAmps.length];
			System.arraycopy(newAmps, 0, systemAmps, 0, newAmps.length);
		} else
			systemAmps = null;
	}

	public void setSystemPhases(double[] newPhases) {
		if (newPhases != null && newPhases.length > 0) {
			systemPhases = new double[newPhases.length];
			System.arraycopy(newPhases, 0, systemPhases, 0, newPhases.length);
		} else
			systemPhases = null;
	}

	public void setSystemCeps(float[] newCeps) {
		if (newCeps != null && newCeps.length > 0) {
			systemCeps = new float[newCeps.length];
			System.arraycopy(newCeps, 0, systemCeps, 0, newCeps.length);
		} else
			systemCeps = null;
	}

	public void setFrameDfts(ComplexArray newDfts) {
		if (newDfts != null)
			frameDfts = new ComplexArray(newDfts);
		else
			frameDfts = null;
	}
}
