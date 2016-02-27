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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.AudioProcessor;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class EnergyNormaliser implements AudioProcessor {
	protected double amplitudeFactor;
	protected double referencePower;

	/**
	 * Adapt the amplitudes of a signal such that the energy changes by the given factor.
	 * 
	 * @param energyFactor
	 *            energy factor
	 */
	public EnergyNormaliser(double energyFactor) {
		this.amplitudeFactor = Math.sqrt(energyFactor);
		this.referencePower = -1;
	}

	/**
	 * Adapt the amplitudes of a signal such that the average power is the same as the one in the reference.
	 * 
	 * @param reference
	 *            an audio input stream with the reference power.
	 */
	public EnergyNormaliser(AudioInputStream reference) {
		this.referencePower = determineAveragePower(reference);
		this.amplitudeFactor = -1;
	}

	/**
	 * Adapt the amplitudes of a signal such that the average power is the same as the one in the reference.
	 * 
	 * @param reference
	 *            an audio signal with the reference power.
	 */
	public EnergyNormaliser(DoubleDataSource reference) {
		this.referencePower = determineAveragePower(reference);
		this.amplitudeFactor = -1;
	}

	public double getAmplitudeFactor() {
		return amplitudeFactor;
	}

	public double getReferencePower() {
		return referencePower;
	}

	/**
	 * For a given audio input stream, determine the average power.
	 * 
	 * @param ais
	 *            audio input stream for which to determine the average power
	 * @return a non-negative double representing the average power as energy per sample, i.e. the total energy divided by the
	 *         total duration.
	 */
	public static double determineAveragePower(AudioInputStream ais) {
		if (ais == null)
			throw new NullPointerException("Received null argument");
		DoubleDataSource signal = new AudioDoubleDataSource(ais);
		return determineAveragePower(signal);
	}

	/**
	 * For a given audio signal, determine the average power.
	 * 
	 * @param signal
	 *            a double data source for which to determine the average power
	 * @return a non-negative double representing the average power as energy per sample, i.e. the total energy divided by the
	 *         total duration.
	 */
	public static double determineAveragePower(DoubleDataSource signal) {
		if (signal == null)
			throw new NullPointerException("Received null argument");
		double[] signalData = signal.getAllData();
		return determineAveragePower(signalData);
	}

	/**
	 * For a given audio signal and sampling rate, determine the average power.
	 * 
	 * @param signal
	 *            audio signal for which to determine the average power
	 * @return a non-negative double representing the average power as energy per sample, i.e. the total energy divided by the
	 *         total duration.
	 */
	public static double determineAveragePower(double[] signal) {
		if (signal == null)
			throw new NullPointerException("Received null argument");
		if (signal.length == 0)
			return 0;
		double energy = MathUtils.sum(MathUtils.multiply(signal, signal));
		if (energy == 0 || signal.length == 0)
			return 0;
		else
			return energy / signal.length;
	}

	public AudioInputStream apply(AudioInputStream ais) {
		AudioDoubleDataSource adds = new AudioDoubleDataSource(ais);
		return new DDSAudioInputStream(this.apply(adds), adds.getAudioFormat());
	}

	public DoubleDataSource apply(DoubleDataSource signal) {
		final double factor;
		DoubleDataSource source;
		if (amplitudeFactor >= 0) { // Simple multiplication with amplitude factor
			factor = amplitudeFactor;
			source = signal;
		} else { // reference power -- need to compute average power first.
			assert referencePower >= 0;
			double[] signalData = signal.getAllData();
			double power = determineAveragePower(signalData);
			if (power == 0)
				factor = 0;
			else
				factor = Math.sqrt(referencePower / power);
			source = new BufferedDoubleDataSource(signalData);
		}
		System.err.println("Applying amplitude factor: " + factor);
		return new BufferedDoubleDataSource(source, new InlineDataProcessor() {
			public void applyInline(double[] buf, int off, int len) {
				for (int i = off; i < off + len; i++) {
					buf[i] *= factor;
				}
			}
		});
	}

	public static void main(String[] args) throws Exception {
		/*
		 * AudioInputStream dummyAIS = AudioSystem.getAudioInputStream(new File(args[0])); double[] testSignal = new
		 * double[16000]; for (int i=0; i<testSignal.length; i++) { testSignal[i] =
		 * 10000*Math.sin(2*Math.PI*i*1000/testSignal.length); } //FunctionGraph graph = new FunctionGraph(0, 1, testSignal);
		 * //graph.showInJFrame("testSignal", true, false); DDSAudioInputStream testAIS = new DDSAudioInputStream(new
		 * BufferedDoubleDataSource(testSignal), dummyAIS.getFormat()); //AudioSystem.write(testAIS, AudioFileFormat.Type.WAVE,
		 * new File("testSignal.wav")); //testAIS = new DDSAudioInputStream(new BufferedDoubleDataSource(testSignal),
		 * dummyAIS.getFormat()); EnergyNormaliser normaliser = new EnergyNormaliser(2); AudioInputStream resultAIS =
		 * normaliser.apply(testAIS); System.err.println("Constructed resultAIS"); double[] testResult = new
		 * AudioDoubleDataSource(resultAIS).getAllData(); System.err.println("Got all data from resultAIS"); FunctionGraph
		 * resultGraph = new FunctionGraph(0, 1, testResult); resultGraph.showInJFrame("testResult", true, false);
		 */
		double[] totalEnergies = new double[args.length];
		double[] maxAmplitude = new double[args.length];
		for (int i = 0; i < args.length; i++) {
			double[] signal = new AudioDoubleDataSource(AudioSystem.getAudioInputStream(new File(args[i]))).getAllData();
			totalEnergies[i] = 0;
			for (int j = 0; j < signal.length; j++) {
				totalEnergies[i] += signal[j] * signal[j];
			}
			maxAmplitude[i] = MathUtils.max(signal);
			System.err.println(args[i] + ": total energy = " + totalEnergies[i] + ", max amplitude = " + maxAmplitude[i]);
		}
		double meanEnergy = MathUtils.mean(totalEnergies);
		int indexMaxAmplitude = MathUtils.findGlobalPeakLocation(maxAmplitude);
		System.err.println("Mean energy: " + meanEnergy);
		System.err.println("Highest amplitude found in " + args[indexMaxAmplitude]);
		int MAX_AMPLITUDE = 32767;
		for (int i = 0; i < args.length; i++) {
			double energyFactor = meanEnergy / totalEnergies[i];
			System.err.println(args[i] + ": applying factor " + energyFactor + " = " + meanEnergy + " / " + totalEnergies[i]);
			if (maxAmplitude[i] * Math.sqrt(energyFactor) > MAX_AMPLITUDE) {
				System.err.println("Warning: signal clipping in file " + args[i] + "_norm.wav");
			}
			EnergyNormaliser norm = new EnergyNormaliser(energyFactor);
			File f = new File(args[i]);
			AudioInputStream ais = AudioSystem.getAudioInputStream(f);
			AudioInputStream result = norm.apply(ais);
			String outFileName = args[i].substring(0, args[i].lastIndexOf('.')) + "_norm.wav";
			File outFile = new File(outFileName);
			AudioSystem.write(result, AudioFileFormat.Type.WAVE, outFile);
		}
	}
}
