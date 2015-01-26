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
package marytts.signalproc.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JPanel;

import marytts.signalproc.analysis.FrameBasedAnalyser;
import marytts.signalproc.analysis.ShortTermPhaseSpectrumAnalyser;
import marytts.signalproc.window.GaussWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         Shows a phasogram of input signal
 * 
 */
public class Phasogram extends Spectrogram {
	public static final int DEFAULT_WINDOWSIZE = 2047;
	public static final Window DEFAULT_WINDOW = new GaussWindow(DEFAULT_WINDOWSIZE);
	public static final int DEFAULT_WINDOWSHIFT = 1;
	public static final int DEFAULT_FFTSIZE = 2048;
	protected static final double FREQ_MAX = 4000; // Hz of upper limit frequency to show

	public Phasogram(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Phasogram(AudioInputStream ais, int width, int height) {
		this(ais, DEFAULT_WINDOW, DEFAULT_WINDOWSHIFT, DEFAULT_FFTSIZE, width, height);
	}

	public Phasogram(AudioInputStream ais, Window window, int windowShift, int fftSize) {
		this(ais, window, windowShift, fftSize, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Phasogram(AudioInputStream ais, Window window, int windowShift, int fftSize, int width, int height) {
		super(ais, window, windowShift, fftSize, width, height);
	}

	public Phasogram(double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Phasogram(double[] signal, int samplingRate, int width, int height) {
		this(signal, samplingRate, DEFAULT_WINDOW, DEFAULT_WINDOWSHIFT, DEFAULT_FFTSIZE, width, height);
	}

	public Phasogram(double[] signal, int samplingRate, Window window, int windowShift, int fftSize, int width, int height) {
		super(signal, samplingRate, window, windowShift, fftSize, width, height);
	}

	protected void update() {
		ShortTermPhaseSpectrumAnalyser spectrumAnalyser = new ShortTermPhaseSpectrumAnalyser(
				new BufferedDoubleDataSource(signal), fftSize, window, windowShift, samplingRate);
		spectra = new ArrayList();
		// Frequency resolution of the FFT:
		deltaF = spectrumAnalyser.getFrequencyResolution();
		long startTime = System.currentTimeMillis();
		FrameBasedAnalyser.FrameAnalysisResult[] results = spectrumAnalyser.analyseAllFrames();
		for (int i = 0; i < results.length; i++) {
			double[] phaseSpectrum = (double[]) results[i].get();
			spectra.add(phaseSpectrum);
		}
		long endTime = System.currentTimeMillis();
		System.err.println("Computed " + spectra.size() + " phase spectra in " + (endTime - startTime) + " ms.");

		spectra_indexmax = (int) (FREQ_MAX / deltaF);
		if (spectra_indexmax > fftSize / 2)
			spectra_indexmax = fftSize / 2; // == spectra[i].length
		super.updateData(0, (double) windowShift / samplingRate, new double[spectra.size()]);
		// correct y axis boundaries, for graph:
		ymin = 0.;
		ymax = spectra_indexmax * deltaF;

		repaint();
	}

	protected void initialiseDependentWindows() {
		// do nothing, no dependent windows here.
	}

	protected JPanel getControls() {
		return null; // no controls
	}

	protected void drawSpectrum(Graphics2D g, double[] spectrum, int image_X, int image_width, int image_refY, int image_height) {
		double yScaleFactor = (double) image_height / spectra_indexmax;
		if (image_width < 2)
			image_width = 2;
		int rect_height = (int) Math.ceil(yScaleFactor);
		if (rect_height < 2)
			rect_height = 2;
		for (int i = 0; i < spectra_indexmax; i++) {
			int color;
			// 0 phase is white, +-Pi is black.
			assert spectrum[i] >= -Math.PI && spectrum[i] <= Math.PI;
			if (Double.isNaN(spectrum[i])) {
				color = 255; // white
			} else {
				color = (int) (255 * (Math.PI + spectrum[i]) / (2 * Math.PI));
			}
			g.setColor(new Color(color, color, color));
			g.fillRect(image_X, image_refY - (int) (i * yScaleFactor), image_width, rect_height);
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			double[] signal = new AudioDoubleDataSource(ais).getAllData();
			int nFrames = 500;
			int startSample = 5000;
			int fftSize = DEFAULT_FFTSIZE;
			double[] excerpt = new double[nFrames + fftSize];
			System.arraycopy(signal, startSample, excerpt, 0, excerpt.length);
			Phasogram signalSpectrum = new Phasogram(excerpt, (int) ais.getFormat().getSampleRate());
			signalSpectrum.showInJFrame(args[i], false, true);
		}
	}

}
