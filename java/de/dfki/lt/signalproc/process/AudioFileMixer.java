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

package de.dfki.lt.signalproc.process;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.jsresources.SequenceAudioInputStream;

import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MixerDoubleDataSource;
import de.dfki.lt.signalproc.util.NoiseDoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.util.SilenceAudioInputStream;
import de.dfki.lt.signalproc.util.SilenceDoubleDataSource;

public class AudioFileMixer
{

    /**
     * Mix a number of audio files to each of a set of audio files,
     * normalising these audio files to the average power of the reference audio files.
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        List audio = new ArrayList(); // to play in parallel to each argument
        double[] audioData = null;
        List referenceAudio = new ArrayList(); // to normalise power
        List noiseSpecs = new ArrayList();
        double maxDuration = 0;
        int i=0;
        String prop;
        // The audio format of the first argument is the target format!
        AudioFormat format;
        if (args.length > 0) format = AudioSystem.getAudioInputStream(new File(args[0])).getFormat();
        else format = new AudioFormat
            (AudioFormat.Encoding.PCM_SIGNED,
                16000, // samples per second
                16, // bits per sample
                1, // mono
                2, // nr. of bytes per frame
                16000, // nr. of frames per second
                false // little-endian
                );
        while (!(prop = System.getProperty("audio."+(++i), "")).equals("")) {
            DoubleDataSource dds = null;
            if (prop.startsWith("noise:")) {
                noiseSpecs.add(prop);
            } else {
                String[] info = prop.split(":");
                String filename = info[info.length-1];
                double start = 0;
                if (info.length > 1)
                    start = Double.valueOf(info[0]).doubleValue();
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filename));
                if (!format.equals(ais.getFormat())) // convert to target format
                    ais = AudioSystem.getAudioInputStream(format, ais);
                double[] signal = new AudioDoubleDataSource(ais).getAllData();
                double duration = signal.length / format.getSampleRate();
                if (duration > maxDuration)
                    maxDuration = duration;
                referenceAudio.add(new BufferedDoubleDataSource(signal));
                dds = new BufferedDoubleDataSource(signal);
                if (start > 0) 
                    dds = new SequenceDoubleDataSource(new DoubleDataSource[] {
                        new SilenceDoubleDataSource((long)(start*format.getSampleRate())),
                        dds
                    });
                audio.add(dds);
            }
        }

        EnergyNormaliser powerNormaliser = null;
        if (referenceAudio.size() > 0) {
            powerNormaliser = new EnergyNormaliser(new SequenceDoubleDataSource(referenceAudio));
            System.err.println("Reference power: "+powerNormaliser.getReferencePower());
        }

        for (Iterator it = noiseSpecs.iterator(); it.hasNext(); ) {
            String spec = (String) it.next();
            String[] info = spec.split(":");
            double start = 0;
            if (info.length > 2)
                start = Double.valueOf(info[1]).doubleValue();
            double duration = maxDuration - start;
            if (info.length > 3)
                duration = Double.valueOf(info[2]).doubleValue();
            double db = Double.valueOf(info[info.length-1]).doubleValue();
            DoubleDataSource noise = new NoiseDoubleDataSource((long) (duration*format.getSampleRate()), db);
            if (start > 0) 
                noise = new SequenceDoubleDataSource(new DoubleDataSource[] {
                    new SilenceDoubleDataSource((long)(start*format.getSampleRate())),
                    noise
                });
            audio.add(noise);
        }

        if (audio.size() > 0)
            audioData = new MixerDoubleDataSource(audio).getAllData();

        // If no arguments are present:
        if (args.length == 0) {
            AudioInputStream audioStream = new DDSAudioInputStream(new BufferedDoubleDataSource(audioData), format);
            String prefix = System.getProperty("prefix", "mixed_");
            File outFile = new File(prefix+".wav"); // in the current directory
            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outFile);
            System.out.println("Wrote "+outFile.getPath());
            System.exit(0);
        }

        // How to time the audio given as the arguments:
        double argsStart = Double.valueOf(System.getProperty("audio.args", "0")).doubleValue();
        for (int k=0; k<args.length; k++) {
            List result = new ArrayList();
            if (audioData != null) {
                result.add(new BufferedDoubleDataSource(audioData));
            }
            File inFile = new File(args[k]);
            AudioInputStream ais = AudioSystem.getAudioInputStream(inFile);
            if (!format.equals(ais.getFormat()))
                ais = AudioSystem.getAudioInputStream(format, ais);
            DoubleDataSource dds = new AudioDoubleDataSource(ais);
            if (powerNormaliser != null)
                dds = powerNormaliser.apply(dds);
            if (argsStart > 0) {
                dds = new SequenceDoubleDataSource(new DoubleDataSource[] {
                        new SilenceDoubleDataSource((long)(argsStart*format.getSampleRate())),
                        dds
                    });
            }
            result.add(dds);
            DoubleDataSource resultDDS = new MixerDoubleDataSource(result);
            AudioInputStream resultStream = new DDSAudioInputStream(resultDDS, format);
            String prefix = System.getProperty("prefix", "mixed_");
            String filename = inFile.getName();
            filename = prefix + filename.substring(0, filename.lastIndexOf('.')) + ".wav";
            File outFile = new File(filename); // in the current directory
            AudioSystem.write(resultStream, AudioFileFormat.Type.WAVE, outFile);
            System.out.println("Wrote "+outFile.getPath());
        }
    }

}
