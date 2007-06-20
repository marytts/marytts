/**
 * Copyright 2000-2007 DFKI GmbH.
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
package de.dfki.lt.mary.modules.en;

import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.jsresources.AppendableSequenceAudioInputStream;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.tests.AllTests;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.NoiseDoubleDataSource;


/**
 * Use an individual FreeTTS module for English synthesis.
 *
 * @author Marc Schr&ouml;der
 */

public class HTSEngine extends InternalModule
{

    public HTSEngine()
    {
        super("HTSEngine",
              MaryDataType.get("HTSCONTEXT_EN"),
              MaryDataType.get("AUDIO")
              );
    }


    public MaryData process(MaryData d)
    throws Exception
    {
        String context = d.getPlainText();
        
        // TODO: Marcela to add Java port of the HTS Engine.
        
        MaryData output = new MaryData(outputType());
        if (d.getAudioFileFormat() != null) {
            output.setAudioFileFormat(d.getAudioFileFormat());
            if (d.getAudio() != null) {
                // This (empty) AppendableSequenceAudioInputStream object allows a 
                // thread reading the audio data on the other "end" to get to our data as we are producing it.
                assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
                output.setAudio(d.getAudio());
            }

        }
        AudioInputStream dummyAudio = AudioSystem.getAudioInputStream(AllTests.class.getResourceAsStream("test.wav"));
        //AudioInputStream dummyAudio = new DDSAudioInputStream(new NoiseDoubleDataSource(32000, -6), d.getAudioFileFormat().getFormat());
        output.appendAudio(dummyAudio);
        return output;
    }




}
