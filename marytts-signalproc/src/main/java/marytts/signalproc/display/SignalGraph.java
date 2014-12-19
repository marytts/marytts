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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.audio.MaryAudioUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         Displays a zoomable and playable signal graph
 * 
 */
public class SignalGraph extends FunctionGraph {
	public SignalGraph(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public SignalGraph(AudioInputStream ais, int width, int height) {
		super();
		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono audio signals");
		}
		int samplingRate = (int) ais.getFormat().getSampleRate();
		double[] audioData = MaryAudioUtils.getSamplesAsDoubleArray(ais);
		initialise(audioData, samplingRate, width, height);
	}

	public SignalGraph(final double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public SignalGraph(final double[] signal, int samplingRate, int width, int height) {
		initialise(signal, samplingRate, width, height);
	}

	protected void initialise(final double[] signal, int samplingRate, int width, int height) {
		super.initialise(width, height, 0, 1. / samplingRate, signal);
		updateSound(signal, samplingRate);
	}

	protected void update(double[] signal, int samplingRate) {
		super.updateData(0, 1. / samplingRate, signal);
		updateSound(signal, samplingRate);
	}

	protected void updateSound(double[] signal, int samplingRate) {
		AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplingRate, 16, 1, 2, samplingRate, false);
		DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
		final Clip clip;
		final Timer timer = new Timer(true);
		try {
			clip = (Clip) AudioSystem.getLine(info);
			clip.open(new DDSAudioInputStream(new BufferedDoubleDataSource(signal), audioFormat));
			System.err.println("Created clip");
			// Set it up so that pressing the space bar will play the audio
			getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "startOrStopAudio");
			getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "startOrStopAudio");
			getActionMap().put("startOrStopAudio", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					synchronized (clip) {
						if (clip.isActive()) {
							System.err.println("Stopping clip.");
							clip.stop();
						} else {
							System.err.println("Rewinding clip.");
							if (Double.isNaN(positionCursor.x)) { // no cursor, play from start
								clip.setFramePosition(0);
							} else { // play from cursor position
								clip.setFramePosition(X2indexX(positionCursor.x));
							}
							if (!Double.isNaN(rangeCursor.x)) { // range set?
								System.err.println("Setting timer task");
								int endFrame = X2indexX(rangeCursor.x);
								timer.schedule(new ClipObserver(clip, endFrame), 50, 50);
							}
							System.err.println("Starting clip.");
							clip.start();
						}
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class ClipObserver extends TimerTask {
		protected Clip clip;
		protected int endFrame;

		public ClipObserver(Clip clip, int endFrame) {
			this.clip = clip;
			this.endFrame = endFrame;
		}

		public void run() {
			System.err.println("Timer task running");
			if (!clip.isActive() // already stopped?
					|| clip.getFramePosition() >= endFrame) {
				System.err.println("Timer task stopping clip.");
				clip.stop();
				this.cancel();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			SignalGraph signalGraph = new SignalGraph(ais);
			signalGraph.showInJFrame(args[i], true, false);
		}
	}
}
