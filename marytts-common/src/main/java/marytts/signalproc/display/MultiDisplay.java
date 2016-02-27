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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import marytts.util.data.audio.MaryAudioUtils;

/**
 * @author marc
 * 
 *         To change the template for this generated type comment go to Code and Comments under Window, Preferences, Java, Code
 *         Generation
 * 
 */
public class MultiDisplay extends JFrame {
	public static final int DEFAULT_WIDTH = 800;

	public static final int DEFAULT_HEIGHT = 600;

	protected SignalGraph signalGraph;

	protected Spectrogram spectrogram;

	protected F0Graph f0Graph;

	protected EnergyGraph energyGraph;

	protected SilenceMarker silenceMarker;

	protected List allGraphs = new ArrayList();

	public MultiDisplay(AudioInputStream ais, String title) {
		this(ais, title, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public MultiDisplay(AudioInputStream ais, String title, boolean exitOnClose) {
		this(ais, title, DEFAULT_WIDTH, DEFAULT_HEIGHT, exitOnClose);
	}

	public MultiDisplay(AudioInputStream ais, String title, int width, int height, boolean exitOnClose) {
		super(title);
		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono audio signals");
		}
		int samplingRate = (int) ais.getFormat().getSampleRate();
		double[] audioData = MaryAudioUtils.getSamplesAsDoubleArray(ais);
		initialise(audioData, samplingRate, width, height, exitOnClose);
	}

	public MultiDisplay(AudioInputStream ais, String title, int width, int height) {
		this(ais, title, width, height, true);
	}

	public MultiDisplay(double[] signal, int samplingRate, String title, int width, int height) {
		super(title);
		initialise(signal, samplingRate, width, height, true);
	}

	public MultiDisplay(double[] signal, int samplingRate, String title, int width, int height, boolean exitOnClose) {
		super(title);
		initialise(signal, samplingRate, width, height, exitOnClose);
	}

	protected void initialise(double[] signal, int samplingRate, int width, int height, boolean exitOnClose) {
		setSize(width, height);
		JPanel zoomPanel = new JPanel();
		zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.Y_AXIS));
		getContentPane().add(zoomPanel, BorderLayout.WEST);
		JButton zoomIn = new JButton("Zoom In");
		zoomIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				changeZoomX(2);
				signalGraph.requestFocus();
			}
		});
		zoomPanel.add(zoomIn);
		JButton zoomOut = new JButton("Zoom Out");
		zoomOut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				changeZoomX(0.5);
				signalGraph.requestFocus();
			}
		});
		zoomPanel.add(zoomOut);

		int graphWidth = width - zoomPanel.getPreferredSize().width - 30;
		JPanel graphPanel = new JPanel();
		graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.Y_AXIS));

		JScrollPane scroll = new JScrollPane(graphPanel);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		getContentPane().add(scroll, BorderLayout.CENTER);
		signalGraph = new SignalGraph(signal, samplingRate, graphWidth, height * 15 / 100);
		allGraphs.add(signalGraph);
		spectrogram = new Spectrogram(signal, samplingRate, graphWidth, height * 40 / 100);
		allGraphs.add(spectrogram);
		f0Graph = new F0Graph(signal, samplingRate, graphWidth, height * 20 / 100);
		allGraphs.add(f0Graph);
		energyGraph = new EnergyGraph(signal, samplingRate, graphWidth, height * 15 / 100);
		allGraphs.add(energyGraph);
		silenceMarker = new SilenceMarker(signal, samplingRate, graphWidth, height * 5 / 100);
		allGraphs.add(silenceMarker);

		final CursorDisplayer glass = new CursorDisplayer();
		setGlassPane(glass);
		glass.setVisible(true);

		for (Iterator it = allGraphs.iterator(); it.hasNext();) {
			FunctionGraph g = (FunctionGraph) it.next();
			graphPanel.add(g);
			// Now register every graph with every other graph as a
			// listener/source pair:
			for (Iterator it2 = allGraphs.iterator(); it2.hasNext();) {
				FunctionGraph g2 = (FunctionGraph) it2.next();
				if (g2 != g) {
					g.addCursorListener(g2);
				}
			}
			glass.addCursorSource(g);
			g.addCursorListener(glass);
		}

		if (exitOnClose) {
			addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent evt) {
					System.exit(0);
				}
			});
		}

		setVisible(true);
		signalGraph.requestFocus();
	}

	protected void changeZoomX(double factor) {
		for (Iterator it = allGraphs.iterator(); it.hasNext();) {
			FunctionGraph g = (FunctionGraph) it.next();
			g.setZoomX(g.getZoomX() * factor);
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			MultiDisplay multiDisplay = new MultiDisplay(ais, args[i]);
		}

	}
}
