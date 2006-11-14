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
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.util.SilenceAudioInputStream;

public class AudioFileJoiner
{

    /**
     * Join a prefix and a suffix to each of a set of audio files,
     * normalising these audio files to the power of the prefix and suffix.
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        List startAudio = new ArrayList(); // to prepend to each argument
        double[] start = null;
        List endAudio = new ArrayList(); // to append to each argument
        double[] end = null;
        List referenceAudio = new ArrayList(); // to normalise power
        int i=0;
        String prop;
        // The audio format of the first argument is the target format!
        AudioFormat format = AudioSystem.getAudioInputStream(new File(args[0])).getFormat();
        while (!(prop = System.getProperty("audio."+(++i), "args")).equals("args")) {
            DoubleDataSource dds = null;
            if (prop.startsWith("silence:")) {
                double duration = Double.valueOf(prop.substring(prop.indexOf(':')+1)).doubleValue();
                startAudio.add(new AudioDoubleDataSource(new SilenceAudioInputStream(duration, format)));
            } else {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(prop));
                if (!format.equals(ais.getFormat())) // convert to target format
                    ais = AudioSystem.getAudioInputStream(format, ais);
                double[] signal = new AudioDoubleDataSource(ais).getAllData();
                startAudio.add(new BufferedDoubleDataSource(signal));
                referenceAudio.add(new BufferedDoubleDataSource(signal));
            }
        }
        if (startAudio.size() > 0)
            start = new SequenceDoubleDataSource(startAudio).getAllData();
        
        while ((prop = System.getProperty("audio."+(++i))) != null) {
            DoubleDataSource dds = null;
            if (prop.startsWith("silence:")) {
                double duration = Double.valueOf(prop.substring(prop.indexOf(':')+1)).doubleValue();
                endAudio.add(new AudioDoubleDataSource(new SilenceAudioInputStream(duration, format)));
            } else {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(prop));
                if (!format.equals(ais.getFormat())) // convert to target format
                    ais = AudioSystem.getAudioInputStream(format, ais);
                double[] signal = new AudioDoubleDataSource(ais).getAllData();
                endAudio.add(new BufferedDoubleDataSource(signal));
                referenceAudio.add(new BufferedDoubleDataSource(signal));
            }
        }
        if (endAudio.size() > 0)
            end = new SequenceDoubleDataSource(endAudio).getAllData();

        EnergyNormaliser powerNormaliser = null;
        if (referenceAudio.size() > 0) {
            powerNormaliser = new EnergyNormaliser(new SequenceDoubleDataSource(referenceAudio));
            System.err.println("Reference power: "+powerNormaliser.getReferencePower());
        }
        
        for (int k=0; k<args.length; k++) {
            List result = new ArrayList();
            if (start != null) {
                result.add(new BufferedDoubleDataSource(start));
            }
            File inFile = new File(args[k]);
            AudioInputStream ais = AudioSystem.getAudioInputStream(inFile);
            if (!format.equals(ais.getFormat()))
                ais = AudioSystem.getAudioInputStream(format, ais);
            DoubleDataSource dds = new AudioDoubleDataSource(ais);
            if (powerNormaliser != null)
                dds = powerNormaliser.apply(dds);
            result.add(dds);
            if (end != null) {
                result.add(new BufferedDoubleDataSource(end));
            }
            DoubleDataSource resultDDS = new SequenceDoubleDataSource(result);
            AudioInputStream resultStream = new DDSAudioInputStream(resultDDS, format);
            String prefix = System.getProperty("prefix", "joined_");
            String filename = inFile.getName();
            filename = prefix + filename.substring(0, filename.lastIndexOf('.')) + ".wav";
            File outFile = new File(filename); // in the current directory
            AudioSystem.write(resultStream, AudioFileFormat.Type.WAVE, outFile);
            System.out.println("Wrote "+outFile.getPath());
        }
    }

}
