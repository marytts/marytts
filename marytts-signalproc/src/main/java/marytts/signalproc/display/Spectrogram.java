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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import marytts.signalproc.analysis.CepstrumSpeechAnalyser;
import marytts.signalproc.analysis.FrameBasedAnalyser;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.ShortTermLogSpectrumAnalyser;
import marytts.signalproc.filter.FIRFilter;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.RectWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.ArrayUtils;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;
import marytts.util.string.PrintfFormat;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class Spectrogram extends FunctionGraph {
	public static final int DEFAULT_WINDOWSIZE = 65;
	public static final int DEFAULT_WINDOW = Window.HAMMING;
	public static final int DEFAULT_WINDOWSHIFT = 32;
	public static final int DEFAULT_FFTSIZE = 256;
	protected static final double PREEMPHASIS = 6.0; // dB per Octave
	protected static final double DYNAMIC_RANGE = 40.0; // dB below global maximum to show
	protected static final double FREQ_MAX = 8000.0; // Hz of upper limit frequency to show

	protected double[] signal;
	protected int samplingRate;
	protected Window window;
	protected int windowShift;
	protected int fftSize;
	protected GraphAtCursor[] graphsAtCursor = new GraphAtCursor[] { new SpectrumAtCursor(), new PhasogramAtCursor(),
			new LPCAtCursor(), new CepstrumAtCursor(), };

	protected List<double[]> spectra;
	protected double spectra_max = 0.;
	protected double spectra_min = 0.;
	protected double deltaF = 0.; // distance in Hz between two spectrum samples
	protected int spectra_indexmax = 0; // index in each spectrum corresponding to FREQ_MAX

	public Spectrogram(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Spectrogram(AudioInputStream ais, int width, int height) {
		this(ais, Window.get(DEFAULT_WINDOW, DEFAULT_WINDOWSIZE), DEFAULT_WINDOWSHIFT, DEFAULT_FFTSIZE, width, height);
	}

	public Spectrogram(AudioInputStream ais, Window window, int windowShift, int fftSize) {
		this(ais, window, windowShift, fftSize, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Spectrogram(AudioInputStream ais, Window window, int windowShift, int fftSize, int width, int height) {
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

	public Spectrogram(double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public Spectrogram(double[] signal, int samplingRate, int width, int height) {
		this(signal, samplingRate, Window.get(DEFAULT_WINDOW, DEFAULT_WINDOWSIZE), DEFAULT_WINDOWSHIFT, DEFAULT_FFTSIZE, width,
				height);
	}

	public Spectrogram(double[] signal, int samplingRate, Window window, int windowShift, int fftSize, int width, int height) {
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
		initialiseDependentWindows();
	}

	protected void update() {
		ShortTermLogSpectrumAnalyser spectrumAnalyser = new ShortTermLogSpectrumAnalyser(new BufferedDoubleDataSource(signal),
				fftSize, window, windowShift, samplingRate);
		spectra = new ArrayList<double[]>();
		// Frequency resolution of the FFT:
		deltaF = spectrumAnalyser.getFrequencyResolution();
		long startTime = System.currentTimeMillis();
		spectra_max = Double.NaN;
		spectra_min = Double.NaN;
		FrameBasedAnalyser.FrameAnalysisResult<double[]>[] results = spectrumAnalyser.analyseAllFrames();
		for (int i = 0; i < results.length; i++) {
			double[] spectrum = (double[]) results[i].get();
			spectra.add(spectrum);
			// Still do the preemphasis inline:
			for (int j = 0; j < spectrum.length; j++) {
				double freqPreemphasis = PREEMPHASIS / Math.log(2) * Math.log((j + 1) * deltaF / 1000.);
				spectrum[j] += freqPreemphasis;
				if (Double.isNaN(spectra_min) || spectrum[j] < spectra_min) {
					spectra_min = spectrum[j];
				}
				if (Double.isNaN(spectra_max) || spectrum[j] > spectra_max) {
					spectra_max = spectrum[j];
				}
			}
		}
		long endTime = System.currentTimeMillis();
		System.err.println("Computed " + spectra.size() + " spectra in " + (endTime - startTime) + " ms.");

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
		addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				int imageX = e.getX() - paddingLeft;
				double x = imageX2X(imageX);
				for (int i = 0; i < graphsAtCursor.length; i++) {
					if (graphsAtCursor[i].show) {
						graphsAtCursor[i].update(x);
					}
				}
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});
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
	 *            currentGraphColor
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
		// System.err.println("Drawing spectra from image " + image_fromX + " to " + image_toX);
		for (int i = index_fromX; i < index_toX; i++) {
			// System.err.println("Drawing spectrum " + i);
			int spectrumWidth = indexX2imageX(1);
			if (spectrumWidth == 0)
				spectrumWidth = 1;
			drawSpectrum(g, (double[]) spectra.get(i), image_refX + indexX2imageX(i), spectrumWidth, image_refY, image_height);
		}
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
			if (Double.isNaN(spectrum[i]) || spectrum[i] < spectra_max - DYNAMIC_RANGE) {
				color = 255; // white
			} else {
				color = (int) (255 * (spectra_max - spectrum[i]) / DYNAMIC_RANGE);

			}
			g.setColor(new Color(color, color, color));
			g.fillRect(image_X, image_refY - (int) (i * yScaleFactor), image_width, rect_height);
		}
	}

	public double[] getSpectrumAtTime(double t) {
		int index = (int) ((t - x0) / xStep);
		if (index < 0 || index >= spectra.size()) {
			return null;
		}
		return (double[]) spectra.get(index);
	}

	protected String getLabel(double x, double y) {
		int precisionX = -(int) (Math.log(getXRange()) / Math.log(10)) + 2;
		if (precisionX < 0)
			precisionX = 0;
		int indexX = X2indexX(x);
		double[] spectrum = (double[]) spectra.get(indexX);
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
		return (int) (spectra_indexmax * y / ymax);
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
					if (newFftSize != Spectrogram.this.fftSize) {
						Spectrogram.this.fftSize = newFftSize;
						Spectrogram.this.window = Window.get(Spectrogram.this.window.type(), newFftSize / 4 + 1);
						Spectrogram.this.update();
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
				if (newWindowType != Spectrogram.this.window.type()) {
					Spectrogram.this.window = Window.get(newWindowType, Spectrogram.this.window.getLength());
					Spectrogram.this.update();
				}
			}
		});
		controls.add(windowList);

		// Controls for graphs at cursor:
		for (int i = 0; i < graphsAtCursor.length; i++) {
			controls.add(graphsAtCursor[i].getControls());
		}

		return controls;
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			Spectrogram signalSpectrum = new Spectrogram(ais);
			signalSpectrum.showInJFrame(args[i], true, true);
		}
	}

	/**
	 * Determine the next free location for a dependent and put the window there.
	 * 
	 * @param jf
	 *            jf
	 */
	protected void setDependentWindowLocation(JFrame jf) {
		if (nextDependentWindowX == 0 && nextDependentWindowY == 0) {
			// first dependent window:
			nextDependentWindowX = getTopLevelAncestor().getWidth();
		}
		jf.setLocationRelativeTo(this);
		jf.setLocation(nextDependentWindowX, nextDependentWindowY);
		nextDependentWindowY += jf.getHeight();
	}

	private static int nextDependentWindowX;
	private static int nextDependentWindowY;

	public abstract class GraphAtCursor {
		private JPanel controls;
		protected FunctionGraph graph;

		protected boolean show = false;

		public abstract void update(double x);

		public JPanel getControls() {
			if (controls == null) {
				controls = createControls();
			}
			return controls;
		}

		protected abstract JPanel createControls();

		protected void updateGraph(FunctionGraph someGraph, String title) {
			if (someGraph.getParent() == null) {
				JFrame jf = someGraph.showInJFrame(title, 400, 250, false, false);
				setDependentWindowLocation(jf);
			} else {
				JFrame jf = (JFrame) SwingUtilities.getWindowAncestor(someGraph);
				jf.setTitle(title);
				jf.setVisible(true); // just to be sure
				someGraph.repaint();
			}
		}
	}

	public class SpectrumAtCursor extends GraphAtCursor {
		public void update(double x) {
			if (Double.isNaN(x))
				return;
			int centerIndex = (int) (x * samplingRate);
			assert centerIndex >= 0 && centerIndex < signal.length;
			int windowLength = 1024;
			int leftIndex = centerIndex - windowLength / 2;
			if (leftIndex < 0)
				leftIndex = 0;
			double[] signalExcerpt = new HammingWindow(windowLength).apply(signal, leftIndex);
			double[] spectrum = FFT.computeLogPowerSpectrum(signalExcerpt);
			if (graph == null) {
				graph = new FunctionGraph(300, 200, 0, samplingRate / windowLength, spectrum);
			} else {
				graph.updateData(0, samplingRate / windowLength, spectrum);
			}
			super.updateGraph(graph, "Spectrum at " + new PrintfFormat("%.3f").sprintf(x) + " s");
		}

		protected JPanel createControls() {
			JPanel controls = new JPanel();
			JCheckBox checkSpectrum = new JCheckBox("Show spectrum");
			checkSpectrum.setAlignmentX(CENTER_ALIGNMENT);
			checkSpectrum.setSelected(show);
			checkSpectrum.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.DESELECTED) {
						show = false;
						if (graph != null)
							graph.getTopLevelAncestor().setVisible(false);
					} else if (e.getStateChange() == ItemEvent.SELECTED) {
						show = true;
						update(positionCursor.x);
						if (graph != null) {
							graph.getTopLevelAncestor().setVisible(true);
						}
					}
				}
			});
			controls.add(checkSpectrum);
			return controls;
		}
	}

	public class PhasogramAtCursor extends GraphAtCursor {
		public void update(double x) {
			if (Double.isNaN(x))
				return;
			int centerIndex = (int) (x * samplingRate);
			assert centerIndex >= 0 && centerIndex < signal.length;
			// Want to show a phasogram of 10 ms centered around cursor position:
			int halfWindowLength = samplingRate / 200;
			double[] signalExcerpt;
			if (graph == null) {
				signalExcerpt = new double[2 * halfWindowLength + Phasogram.DEFAULT_FFTSIZE];
			} else {
				assert graph instanceof Phasogram;
				signalExcerpt = ((Phasogram) graph).signal;
			}
			int leftIndex = centerIndex - halfWindowLength;
			if (leftIndex < 0)
				leftIndex = 0;
			int len = signalExcerpt.length;
			if (leftIndex + len >= signal.length)
				len = signal.length - leftIndex;
			System.arraycopy(signal, leftIndex, signalExcerpt, 0, len);
			// System.err.println("Copied excerpt from signal pos " + leftIndex + ", len " + len);
			if (len < signalExcerpt.length) {
				Arrays.fill(signalExcerpt, len, signalExcerpt.length, 0);
			}

			if (graph == null) {
				graph = new Phasogram(signalExcerpt, samplingRate, 300, 200);
			} else {
				((Phasogram) graph).update();
			}
			super.updateGraph(graph, "Phasogram at " + new PrintfFormat("%.3f").sprintf(x) + " s");
		}

		protected JPanel createControls() {
			JPanel controls = new JPanel();
			JCheckBox checkPhasogram = new JCheckBox("Show phasogram");
			checkPhasogram.setAlignmentX(CENTER_ALIGNMENT);
			checkPhasogram.setSelected(show);
			checkPhasogram.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.DESELECTED) {
						show = false;
						if (graph != null)
							graph.getTopLevelAncestor().setVisible(false);
					} else if (e.getStateChange() == ItemEvent.SELECTED) {
						show = true;
						update(positionCursor.x);
						if (graph != null)
							graph.getTopLevelAncestor().setVisible(true);
					}
				}
			});
			controls.add(checkPhasogram);
			return controls;
		}
	}

	public class LPCAtCursor extends GraphAtCursor {
		protected int lpcOrder = 50;
		protected SignalGraph lpcResidueAtCursor = null;

		public void update(double x) {
			if (Double.isNaN(x))
				return;
			int centerIndex = (int) (x * samplingRate);
			assert centerIndex >= 0 && centerIndex < signal.length;
			int windowLength = 1024;
			int leftIndex = centerIndex - windowLength / 2;
			if (leftIndex < 0)
				leftIndex = 0;
			double[] signalExcerpt = new HammingWindow(windowLength).apply(signal, leftIndex);
			LpcAnalyser.LpCoeffs lpc = LpcAnalyser.calcLPC(signalExcerpt, lpcOrder);
			double[] coeffs = lpc.getOneMinusA();
			double g_db = 2 * MathUtils.db(lpc.getGain()); // *2 because g is signal, not energy
			double[] fftCoeffs = new double[windowLength];
			System.arraycopy(coeffs, 0, fftCoeffs, 0, coeffs.length);
			double[] lpcSpectrum = FFT.computeLogPowerSpectrum(fftCoeffs);
			for (int i = 0; i < lpcSpectrum.length; i++) {
				lpcSpectrum[i] = -lpcSpectrum[i] + g_db;
			}

			if (graph == null) {
				graph = new FunctionGraph(300, 200, 0, samplingRate / windowLength, lpcSpectrum);
			} else {
				graph.updateData(0, samplingRate / windowLength, lpcSpectrum);
			}
			updateGraph(graph, "LPC spectrum (order " + lpcOrder + ") at " + new PrintfFormat("%.3f").sprintf(x) + " s");

			// And the residue:
			FIRFilter whiteningFilter = new FIRFilter(coeffs);
			double[] signalExcerpt2 = new RectWindow(lpcOrder + windowLength).apply(signal, leftIndex - lpcOrder);
			double[] residue = whiteningFilter.apply(signalExcerpt2);
			double[] usableSignal = ArrayUtils.subarray(signalExcerpt2, lpcOrder, windowLength);
			double[] usableResidue = ArrayUtils.subarray(residue, lpcOrder, windowLength);
			double predictionGain = MathUtils.db(MathUtils.sum(MathUtils.multiply(usableSignal, usableSignal))
					/ MathUtils.sum(MathUtils.multiply(usableResidue, usableResidue)));
			System.err.println("LPC prediction gain: " + predictionGain + " dB");
			if (lpcResidueAtCursor == null) {
				lpcResidueAtCursor = new SignalGraph(usableResidue, samplingRate, 300, 200);
			} else {
				lpcResidueAtCursor.update(usableResidue, samplingRate);
			}
			super.updateGraph(lpcResidueAtCursor, "LPC residue at " + new PrintfFormat("%.3f").sprintf(x) + " s");
		}

		protected JPanel createControls() {
			JPanel controls = new JPanel();
			controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
			JCheckBox checkLPC = new JCheckBox("Show LPC");
			checkLPC.setAlignmentX(CENTER_ALIGNMENT);
			checkLPC.setSelected(show);
			checkLPC.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.DESELECTED) {
						show = false;
						if (graph != null)
							graph.getTopLevelAncestor().setVisible(false);
						if (lpcResidueAtCursor != null)
							lpcResidueAtCursor.getTopLevelAncestor().setVisible(false);
					} else if (e.getStateChange() == ItemEvent.SELECTED) {
						show = true;
						update(positionCursor.x);
						if (graph != null)
							graph.getTopLevelAncestor().setVisible(true);
						if (lpcResidueAtCursor != null)
							lpcResidueAtCursor.getTopLevelAncestor().setVisible(true);
					}
				}
			});
			controls.add(checkLPC);
			// LPC order slider:
			JLabel lpcLabel = new JLabel("LPC order:");
			lpcLabel.setAlignmentX(CENTER_ALIGNMENT);
			controls.add(lpcLabel);
			int min = 1;
			int max = 100;
			JSlider lpcSlider = new JSlider(JSlider.HORIZONTAL, min, max, lpcOrder);
			lpcSlider.setAlignmentX(CENTER_ALIGNMENT);
			lpcSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ce) {
					JSlider source = (JSlider) ce.getSource();
					if (!source.getValueIsAdjusting()) {
						lpcOrder = (int) source.getValue();
						System.err.println("Adjusted lpc order to " + lpcOrder);
						if (show)
							update(positionCursor.x);
					}
				}
			});
			controls.add(lpcSlider);
			return controls;
		}
	}

	public class CepstrumAtCursor extends GraphAtCursor {
		protected int cepstrumCutoff = 50;
		protected FunctionGraph cepstrumSpectrumAtCursor = null;

		public void update(double x) {
			if (Double.isNaN(x))
				return;
			int centerIndex = (int) (x * samplingRate);
			assert centerIndex >= 0 && centerIndex < signal.length;
			int windowLength = 1024;
			int leftIndex = centerIndex - windowLength / 2;
			if (leftIndex < 0)
				leftIndex = 0;
			// Create a zero-padded version of the signal excerpt:
			double[] signalExcerpt = new double[2 * windowLength];
			new HammingWindow(windowLength).apply(signal, leftIndex, signalExcerpt, 0);
			double[] realCepstrum = CepstrumSpeechAnalyser.realCepstrum(signalExcerpt);
			if (graph == null) {
				graph = new FunctionGraph(300, 200, 0, samplingRate, realCepstrum);
			} else {
				graph.updateData(0, samplingRate, realCepstrum);
			}
			super.updateGraph(graph, "Cepstrum at " + new PrintfFormat("%.3f").sprintf(x) + " s");

			// And the spectral envelope computed from a low-pass cut-off version of the cepstrum:
			double[] lowCepstrum = CepstrumSpeechAnalyser.filterLowPass(realCepstrum, cepstrumCutoff);
			double[] real = lowCepstrum;
			double[] imag = new double[real.length];
			FFT.transform(real, imag, false);
			double[] cepstrumSpectrum = ArrayUtils.subarray(real, 0, real.length / 2);
			if (cepstrumSpectrumAtCursor == null) {
				cepstrumSpectrumAtCursor = new FunctionGraph(300, 200, 0, samplingRate / real.length, cepstrumSpectrum);
			} else {
				cepstrumSpectrumAtCursor.updateData(0, samplingRate / real.length, cepstrumSpectrum);
			}
			super.updateGraph(cepstrumSpectrumAtCursor, "Cepstrum spectrum (cutoff " + cepstrumCutoff + ") at "
					+ new PrintfFormat("%.3f").sprintf(x) + " s");
		}

		protected JPanel createControls() {
			JPanel controls = new JPanel();
			controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
			JCheckBox checkCepstrum = new JCheckBox("Show Cepstrum");
			checkCepstrum.setAlignmentX(CENTER_ALIGNMENT);
			checkCepstrum.setSelected(show);
			checkCepstrum.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.DESELECTED) {
						show = false;
						if (graph != null)
							graph.getTopLevelAncestor().setVisible(false);
						if (cepstrumSpectrumAtCursor != null)
							cepstrumSpectrumAtCursor.getTopLevelAncestor().setVisible(false);
					} else if (e.getStateChange() == ItemEvent.SELECTED) {
						show = true;
						update(positionCursor.x);
						if (graph != null)
							graph.getTopLevelAncestor().setVisible(true);
						if (cepstrumSpectrumAtCursor != null)
							cepstrumSpectrumAtCursor.getTopLevelAncestor().setVisible(true);
					}
				}
			});
			controls.add(checkCepstrum);
			// Cepstrum cutoff slider:
			JLabel cepstrumLabel = new JLabel("Cepstrum cutoff:");
			cepstrumLabel.setAlignmentX(CENTER_ALIGNMENT);
			controls.add(cepstrumLabel);
			int min = 1;
			int max = 256;
			JSlider cepstrumSlider = new JSlider(JSlider.HORIZONTAL, min, max, cepstrumCutoff);
			cepstrumSlider.setAlignmentX(CENTER_ALIGNMENT);
			cepstrumSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ce) {
					JSlider source = (JSlider) ce.getSource();
					if (!source.getValueIsAdjusting()) {
						cepstrumCutoff = (int) source.getValue();
						System.err.println("Adjusted cepstrum cutoff to " + cepstrumCutoff);
						if (show)
							update(positionCursor.x);
					}
				}
			});
			controls.add(cepstrumSlider);
			return controls;
		}
	}

}
