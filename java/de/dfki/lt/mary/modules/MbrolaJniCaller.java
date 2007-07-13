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
package de.dfki.lt.mary.modules;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.synthesis.MbrolaVoice;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.AudioDestination;

/**
 * The MBROLA-via-JNI caller. This can work as a normal MARY module, converting
 * MBROLA markup data into audio, or it can be indirectly called from the
 * MbrolaSynthesizer.
 *
 * @author Marc Schr&ouml;der
 */

public class MbrolaJniCaller extends MbrolaCaller {
    /* Return true on success, false on failure: */
    private native boolean mbrolaStartup();
    private native void mbrolaShutdown();
    private native void mbrolaReset();
    private native void mbrolaClose();
    private native String mbrolaLastError();
    /* Return true on success, false on failure: */
    private native boolean mbrolaInitVoice(String voicePath, boolean tolerant);
    private native boolean mbrolaSynthesise(String text);

    static {
        System.loadLibrary("MbrolaJNI");
    }

    private Voice currentlyLoadedVoice = null;

    /**
     * This AudioDestination will be filled with the results of the
     * synthesis process. Because of the JNI callback, this must be
     * kept at class level, which means that synthesiseOneSection
     * must be synchronized.
     */
    private AudioDestination audioDestination = null;



    public MbrolaJniCaller() {
        super("MbrolaJniCaller", MaryDataType.get("MBROLA"), MaryDataType.get("AUDIO"));
    }

    public synchronized void startup() throws Exception {
        boolean ok = mbrolaStartup();
        if (!ok) {
            handleNativeError();
        }
        super.startup();
    }

    public synchronized void shutdown()
    {
        mbrolaShutdown();
        super.shutdown();
    }

    private void setVoice(Voice voice) {
        // This is what we would like to have done in order to have fast synthesis;
        // unfortunately, it leads to small deviations in the length of the audio data
        // from one call to another.
        //if (currentlyLoadedVoice != null && currentlyLoadedVoice.equals(voice)) {
        //	return;
        //}
        // So instead we use the slow, but reliable variant, namely to reload
        // the voice every time.
        if (currentlyLoadedVoice != null)
            mbrolaClose();
        assert voice instanceof MbrolaVoice : "Not an MBROLA voice: "+voice.getName();
        mbrolaInitVoice(((MbrolaVoice)voice).path(), true); // true = tolerant, don't abort at missing diphones
        currentlyLoadedVoice = voice;
    }

    private void handleNativeError() throws IOException {
        currentlyLoadedVoice = null;
        String errMsg = "Mbrola error " + mbrolaLastError();
        mbrolaReset();
        throw new IOException(errMsg);

    }

    /**
     * Synthesise one chunk of MBROLA markup with a given voice.
     * @param mbrolaMarkup the input data in the native format expected by
     * the synthesis engine
     * @param voice the voice with which to synthesise the data
     * @return an AudioInputStream in the native audio format of the voice
     */
    public synchronized AudioInputStream synthesiseOneSection(String mbrolaMarkup, Voice voice) throws IOException {
        if (mbrolaMarkup == null || voice == null) {
            throw new IllegalArgumentException("Received null argument.");
        }
        audioDestination = new AudioDestination();
        logger.debug("Keeping audio data in " + (audioDestination.isInRam() ? "RAM" : " a temp file"));
		// Workaround: Mbrola.dll code seems to have difficulties with mbrola data
		// longer than about 8300 bytes.
		int index = 0;
		boolean ok = true;
		while (ok && index < mbrolaMarkup.length()) {
			int endIndex = index + 8000;
			if (endIndex > mbrolaMarkup.length())
				endIndex = mbrolaMarkup.length();
			else
				endIndex = mbrolaMarkup.lastIndexOf("#\n", endIndex) + 2;
			String toSynthesise = mbrolaMarkup.substring(index, endIndex);
            assert voice instanceof MbrolaVoice : "Not an MBROLA voice: "+voice.getName();
			logger.info("Setting Mbrola voice `" + voice.getName() + "' (" + ((MbrolaVoice)voice).path() + ")");
			setVoice(voice);
			logger.info("Synthesising audio data.");
			logger.debug("Writing MbrolaMarkup input:\n" + toSynthesise + "\n");
			ok = mbrolaSynthesise(toSynthesise);
			index = endIndex;
		}
        if (!ok)
            handleNativeError();
        logger.info("Finished synthesising.");
        return audioDestination.convertToAudioInputStream(voice.dbAudioFormat());
    }
    

    /* To be called from native code. */
    private void callbackSaveAudioData(byte[] audioData) {
        logger.debug("Read " + audioData.length + " bytes from MBROLA synth.");
        try {
            audioDestination.write(audioData);
        } catch (IOException e) {
            logger.error(e);
        }

    }
}
