/**
 * Copyright 2004-2010 DFKI GmbH.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import marytts.signalproc.analysis.FrameBasedAnalyser;
import marytts.signalproc.analysis.ShortTermCepstrumAnalyser;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.string.PrintfFormat;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class Cepstrogram extends FunctionGraph {
	public static final int DEFAULT_WINDOW = Window.HAMMING;
	public static final int DEFAULT_FFTSIZE = 1024;
	public static final int DEFAULT_WINDOWSHIFT = 32;
	protected double dynamicRange; // dB below global maximum to show
	protected static final double QUEF_MAX = 0.016; // 16 ms = upper limit quefrency to show

	protected double[] signal;
	protected int samplingRate;
	protected Window window;
	protected int windowShift;
	protected int fftSize;

	protected List<double[]> cepstra;
	protected double cepstra_max = 0.;
	protected double cepstra_min = 0.;
	protected double deltaQ = 0.; // distance in quefrency ms between two cepstrum samples
	protected int cepstra_indexmax = 0; // index in each spectrum corresponding to QUEF_MAX

	public Cepstrogram(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Cepstrogram(AudioInputStream ais, int width, int height) {
		this(ais, Window.get(DEFAULT_WINDOW, DEFAULT_FFTSIZE / 4 + 1), DEFAULT_WINDOWSHIFT, DEFAULT_FFTSIZE, width, height);
	}

	public Cepstrogram(AudioInputStream ais, Window window, int windowShift, int fftSize) {
		this(ais, window, windowShift, fftSize, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Cepstrogram(AudioInputStream ais, Window window, int windowShift, int fftSize, int width, int height) {
		super();
		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono audio signals");
		}
		if (!MathUtils.isPowerOfTwo(fftSize))
			throw new IllegalArgumentException("fftSize must be a power of two");
		AudioDoubleDataSource signalSource = new AudioDoubleDataSource(ais);
		initialise(signalSource.getAllData(), signalSource.getSamplingRate(), window, windowShift, fftSize, width, height);
	}

	public Cepstrogram(double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Cepstrogram(double[] signal, int samplingRate, int width, int height) {
		this(signal, samplingRate, Window.get(DEFAULT_WINDOW, DEFAULT_FFTSIZE / 4 + 1), DEFAULT_WINDOWSHIFT, DEFAULT_FFTSIZE,
				width, height);
	}

	public Cepstrogram(double[] signal, int samplingRate, Window window, int windowShift, int fftSize, int width, int height) {
		initialise(signal, samplingRate, window, windowShift, fftSize, width, height);
	}

	protected void initialise(double[] aSignal, int aSamplingRate, Window aWindow, int aWindowShift, int aFftSize, int width,
			int height) {
		this.signal = aSignal;
		this.samplingRate = aSamplingRate;
		this.window = aWindow;
		this.windowShift = aWindowShift;
		this.fftSize = aFftSize;
		super.initialise(width, height, 0, (double) aWindowShift / aSamplingRate, new double[10]);
		update();
	}

	protected void update() {
		ShortTermCepstrumAnalyser cepstrumAnalyser = new ShortTermCepstrumAnalyser(new BufferedDoubleDataSource(signal), fftSize,
				8192, window, windowShift, samplingRate);
		cepstra = new ArrayList<double[]>();
		// Frequency resolution of the FFT:
		deltaQ = cepstrumAnalyser.getQuefrencyResolution();
		long startTime = System.currentTimeMillis();
		cepstra_max = Double.NaN;
		cepstra_min = Double.NaN;
		FrameBasedAnalyser.FrameAnalysisResult<double[]>[] results = cepstrumAnalyser.analyseAllFrames();
		for (int i = 0; i < results.length; i++) {
			double[] cepstrum = results[i].get();
			cepstra.add(cepstrum);
			// Still do the preemphasis inline:
			for (int j = 0; j < cepstrum.length; j++) {
				// double freqPreemphasis = PREEMPHASIS / Math.log(2) * Math.log((j+1)*deltaF/1000.);
				// spectrum[j] += freqPreemphasis;
				if (Double.isNaN(cepstra_min) || cepstrum[j] < cepstra_min) {
					cepstra_min = cepstrum[j];
				}
				if (Double.isNaN(cepstra_max) || cepstrum[j] > cepstra_max) {
					cepstra_max = cepstrum[j];
				}
			}
		}
		// dynamicRange = (cepstra_max - cepstra_min);
		dynamicRange = cepstra_max - 0;
		long endTime = System.currentTimeMillis();
		System.err.println("Computed " + cepstra.size() + " cepstra in " + (endTime - startTime) + " ms.");

		cepstra_indexmax = (int) (QUEF_MAX / deltaQ);
		if (cepstra_indexmax > cepstrumAnalyser.getInverseFFTWindowLength() / 2)
			cepstra_indexmax = cepstrumAnalyser.getInverseFFTWindowLength() / 2; // == cepstra[i].length
		super.updateData(0, (double) windowShift / samplingRate, new double[cepstra.size()]);
		// correct y axis boundaries, for graph:
		ymax = 0.;
		ymin = -cepstra_indexmax * deltaQ;
		repaint();
	}

	/**
	 * While painting the graph, draw the actual function data.
	 * 
	 * @param g
	 *            the graphics2d object to paint in
	 * @param image_fromX
	 *            first visible X coordinate of the Graph display area (= after subtracting space reserved for Y axis)
	 * @param image_toX
	 *            last visible X coordinate of the Graph display area (= after subtracting space reserved for Y axis)
	 * @param image_refX
	 *            X coordinate of the origin, in the display area
	 * @param image_refY
	 *            Y coordinate of the origin, in the display area
	 * @param startY
	 *            the start position on the Y axis (= the lower bound of the drawing area)
	 * @param image_height
	 *            the height of the drawable region for the y values
	 * @param data
	 *            data
	 * @param currentGraphColor
	 *            current graph color
	 * @param currentGraphStyle
	 *            current graph style
	 * @param currentDotStyle
	 *            current dot style
	 */
	@Override
	protected void drawData(Graphics2D g, int image_fromX, int image_toX, int image_refX, int image_refY, int startY,
			int image_height, double[] data, Color currentGraphColor, int currentGraphStyle, int currentDotStyle) {
		int index_fromX = imageX2indexX(image_fromX);
		int index_toX = imageX2indexX(image_toX);
		// System.err.println("Drawing cepstra from image " + image_fromX + " to " + image_toX);
		for (int i = index_fromX; i < index_toX; i++) {
			// System.err.println("Drawing spectrum " + i);
			int spectrumWidth = indexX2imageX(1);
			if (spectrumWidth == 0)
				spectrumWidth = 1;
			drawCepstrum(g, cepstra.get(i), image_refX + indexX2imageX(i), spectrumWidth, image_refY, image_height);
		}
	}

	protected void drawCepstrum(Graphics2D g, double[] cepstrum, int image_X, int image_width, int image_refY, int image_height) {
		double yScaleFactor = (double) image_height / cepstra_indexmax;
		if (image_width < 2)
			image_width = 2;
		int rect_height = (int) Math.ceil(yScaleFactor);
		if (rect_height < 2)
			rect_height = 2;
		for (int i = 0; i < cepstra_indexmax; i++) {
			int color;
			if (Double.isNaN(cepstrum[i]) || cepstrum[i] < cepstra_max - dynamicRange) {
				color = 255; // white
			} else {
				color = (int) (255 * (cepstra_max - cepstrum[i]) / dynamicRange);

			}
			g.setColor(new Color(color, color, color));
			g.fillRect(image_X, image_refY + (int) (i * yScaleFactor), image_width, rect_height);
		}
	}

	protected String getLabel(double x, double y) {
		int precisionX = -(int) (Math.log(getXRange()) / Math.log(10)) + 2;
		if (precisionX < 0)
			precisionX = 0;
		int indexX = X2indexX(x);
		double[] spectrum = (double[]) cepstra.get(indexX);
		int precisionY = -(int) (Math.log(getYRange()) / Math.log(10)) + 2;
		if (precisionY < 0)
			precisionY = 0;
		double E = spectrum[Y2indexY(y)];
		int precisionE = 1;
		return "E(" + new PrintfFormat("%." + precisionX + "f").sprintf(x) + ","
				+ new PrintfFormat("%." + precisionY + "f").sprintf(y) + ")="
				+ new PrintfFormat("%." + precisionE + "f").sprintf(E);

	}

	protected int imageY2indexY(int imageY) {
		double y = imageY2Y(imageY);
		return Y2indexY(y);
	}

	protected int Y2indexY(double y) {
		assert ymin == 0; // or we would have to write (ymax-ymin) or so below
		return (int) (cepstra_indexmax * y / ymax);
	}

	protected JPanel getControls() {
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		// FFT size slider:
		JLabel fftLabel = new JLabel("FFT size:");
		fftLabel.setAlignmentX(CENTER_ALIGNMENT);
		controls.add(fftLabel);
		int min = 5;
		int max = 13;
		int deflt = (int) (Math.log(this.fftSize) / Math.log(2));
		JSlider fftSizeSlider = new JSlider(JSlider.VERTICAL, min, max, deflt);
		fftSizeSlider.setAlignmentX(CENTER_ALIGNMENT);
		fftSizeSlider.setMajorTickSpacing(1);
		fftSizeSlider.setPaintTicks(true);
		fftSizeSlider.setSnapToTicks(true);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		for (int i = min; i <= max; i++) {
			int twoPowI = 1 << i; // 2^i, e.g. i==8 => twoPowI==256
			labelTable.put(new Integer(i), new JLabel(String.valueOf(twoPowI)));
		}
		fftSizeSlider.setLabelTable(labelTable);
		fftSizeSlider.setPaintLabels(true);
		fftSizeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				JSlider source = (JSlider) ce.getSource();
				if (!source.getValueIsAdjusting()) {
					int logfftSize = (int) source.getValue();
					int newFftSize = 1 << logfftSize;
					if (newFftSize != Cepstrogram.this.fftSize) {
						Cepstrogram.this.fftSize = newFftSize;
						Cepstrogram.this.window = Window.get(Cepstrogram.this.window.type(), newFftSize / 4 + 1);
						Cepstrogram.this.update();
					}
				}
			}
		});
		controls.add(fftSizeSlider);

		// Window type:
		JLabel windowTypeLabel = new JLabel("Window type:");
		windowTypeLabel.setAlignmentX(CENTER_ALIGNMENT);
		controls.add(windowTypeLabel);
		int[] windowTypes = Window.getAvailableTypes();
		Window[] windows = new Window[windowTypes.length];
		int selected = 0;
		for (int i = 0; i < windowTypes.length; i++) {
			windows[i] = Window.get(windowTypes[i], 1);
			if (windowTypes[i] == this.window.type())
				selected = i;
		}
		JComboBox windowList = new JComboBox(windows);
		windowList.setAlignmentX(CENTER_ALIGNMENT);
		windowList.setSelectedIndex(selected);
		windowList.setMaximumSize(windowList.getPreferredSize());
		windowList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				int newWindowType = ((Window) cb.getSelectedItem()).type();
				if (newWindowType != Cepstrogram.this.window.type()) {
					Cepstrogram.this.window = Window.get(newWindowType, Cepstrogram.this.window.getLength());
					Cepstrogram.this.update();
				}
			}
		});
		controls.add(windowList);
		return controls;
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			Cepstrogram signalSpectrum = new Cepstrogram(ais);
			signalSpectrum.showInJFrame(args[i], true, true);
		}
	}

}
