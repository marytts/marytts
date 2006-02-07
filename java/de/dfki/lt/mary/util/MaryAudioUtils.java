/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.util;

import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.jsresources.SequenceAudioInputStream;
import org.tritonus.share.sampled.AudioFileTypes;
import org.tritonus.share.sampled.Encodings;

import de.dfki.lt.mary.modules.synthesis.Voice;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class MaryAudioUtils {

    /**
      * Create a single AudioInputStream from a vector of AudioInputStreams.
      * The AudioInputStreams are expected to have the same AudioFormat.
      * @param audioInputStreams a vector containing one or more AudioInputStreams.
      * @return a single AudioInputStream
      * @throws NullPointerException if any of the arguments are null
      * @throws IllegalArgumentException if the vector contains no elements,
      * any element is not an AudioInputStream.
      */
     public static AudioInputStream createSingleAudioInputStream(Vector audioInputStreams) {
         if (audioInputStreams == null)
             throw new NullPointerException("Received null vector of AudioInputStreams");
         if (audioInputStreams.isEmpty())
             throw new IllegalArgumentException("Received empty vector of AudioInputStreams");
         AudioInputStream singleStream;
         if (audioInputStreams.size() == 1)
             singleStream = (AudioInputStream) audioInputStreams.get(0);
         else {
             AudioFormat audioFormat =
                 ((AudioInputStream) audioInputStreams.get(0)).getFormat();
             singleStream =
                 new SequenceAudioInputStream(audioFormat, audioInputStreams);
         }
         return singleStream;
     }

    /**
     * Return an audio file format type for the given string.
     * In addition to the built-in types, this can deal with MP3
     * supported by tritonus.
     */
    public static AudioFileFormat.Type getAudioFileFormatType(String name)
    {
        AudioFileFormat.Type at;
        if (name.equals("MP3")) {
            // Supported by tritonus plugin
            at = AudioFileTypes.getType("MP3", "mp3");
        } else {
            try {
                at = (AudioFileFormat.Type) AudioFileFormat.Type.class.getField(name).get(null);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid audio type: " + name);
            }
        }
        return at;
    }

    /**
     * Determine whether conversion to mp3 is possible.
     *
     */
    public static boolean canCreateMP3()
    {
        return AudioSystem.isConversionSupported(getMP3AudioFormat(), Voice.AF22050);
    }
    
    public static AudioFormat getMP3AudioFormat()
    {
        return new AudioFormat(
            Encodings.getEncoding("MPEG1L3"),
            AudioSystem.NOT_SPECIFIED,
            AudioSystem.NOT_SPECIFIED,
            1,
            AudioSystem.NOT_SPECIFIED,
            AudioSystem.NOT_SPECIFIED,
            false);
            //endianness doesn't matter
    }

}
