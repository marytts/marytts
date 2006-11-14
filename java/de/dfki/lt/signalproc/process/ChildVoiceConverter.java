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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class ChildVoiceConverter
{

    public static void main(String[] args) throws Exception
    {
        double samplingRateFactor = Double.valueOf(args[0]).doubleValue();
        for (int i=1; i<args.length; i++) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
            AudioFormat af = new AudioFormat((int)(ais.getFormat().getSampleRate()*samplingRateFactor), ais.getFormat().getSampleSizeInBits(), ais.getFormat().getChannels(),
                    true, ais.getFormat().isBigEndian());
            DDSAudioInputStream ais2 = new DDSAudioInputStream(new AudioDoubleDataSource(ais), af);
            String outFileName = args[i].substring(0, args[i].length()-4) + "_child.wav";
            AudioSystem.write(ais2, AudioFileFormat.Type.WAVE, new File(outFileName));
        }
    }
}
