/**
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.signalproc.display;

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

import de.dfki.lt.signalproc.AudioUtils;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class SignalGraph extends FunctionGraph
{
    public SignalGraph(AudioInputStream ais)
    {
        this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
            
    public SignalGraph(AudioInputStream ais, int width, int height)
    {
        super();
        if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
        }
        if (ais.getFormat().getChannels() > 1) {
            throw new IllegalArgumentException("Can only deal with mono audio signals");
        }
        int samplingRate = (int) ais.getFormat().getSampleRate();
        double[] audioData = AudioUtils.getSamplesAsDoubleArray(ais);
        initialise(audioData, samplingRate, width, height);
    }

    public SignalGraph(final double[] signal, int samplingRate)
    {
        this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public SignalGraph(final double[] signal, int samplingRate, int width, int height)
    {
        initialise(signal, samplingRate, width, height);
    }
    
    protected void initialise(final double[] signal, int samplingRate, int width, int height)
    {
        super.initialise(width, height, 0, 1./samplingRate, signal);
        updateSound(signal, samplingRate);
    }
    
    protected void update(double[] signal, int samplingRate)
    {
        super.updateData(0, 1./samplingRate, signal);
        updateSound(signal, samplingRate);
    }
    
    protected void updateSound(double[] signal, int samplingRate)
    {
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
                    synchronized(clip) {
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
    
    public class ClipObserver extends TimerTask
    {
        protected Clip clip;
        protected int endFrame;
        
        public ClipObserver(Clip clip, int endFrame)
        {
            this.clip = clip;
            this.endFrame = endFrame;
        }
        
        public void run()
        {
            System.err.println("Timer task running");
            if (!clip.isActive() // already stopped?
                || clip.getFramePosition() >= endFrame) {
                System.err.println("Timer task stopping clip.");
                clip.stop();
                this.cancel();
            }
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        for (int i=0; i<args.length; i++) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
            SignalGraph signalGraph = new SignalGraph(ais);
            signalGraph.showInJFrame(args[i], true, false);
        }
    }
}
