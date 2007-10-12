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
package de.dfki.lt.mary.modules.synthesis;

import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.w3c.dom.Element;

/**
 * Provide a common interface for all waveform synthesizers, to be called from
 * within the "wrapping" Synthesis module.
 */
public interface WaveformSynthesizer
{
    /** Start up the waveform synthesizer. This must be called once before
     * calling synthesize(). */
    public void startup() throws Exception;

    /**
      * Perform a power-on self test by processing some example input data.
      * @throws Error if the module does not work properly.
      */
     public void powerOnSelfTest() throws Error;

    /**
     * Synthesize a given part of a MaryXML document. This method is expected
     * to be thread-safe.
     * @param tokensAndBoundaries the part of the MaryXML document to
     * synthesize; a list containing a number of adjacent <t> and <boundary>
     * elements.
     * @return an AudioInputStream in synthesizer-native audio format.
     * @throws IllegalArgumentException if the voice requested for this section
     * is incompatible with this WaveformSynthesizer.
     */
    public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice)
        throws SynthesisException;
}
