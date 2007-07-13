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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.NoSuchPropertyException;
import de.dfki.lt.mary.modules.synthesis.MbrolaVoice;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.AudioDestination;
import de.dfki.lt.mary.util.AudioReader;
import de.dfki.lt.mary.util.StreamLogger;

/**
 * The mbrola caller. This can work as a normal MARY module, converting MBROLA
 * data into audio, or it can be indirectly called from the MbrolaSynthesizer.
 *
 * @author Marc Schr&ouml;der
 */

public class MbrolaCaller extends SynthesisCallerBase {
    private String baseCmd;
    private int timeout;

    /**
     * This is so a subclass of MbrolaCaller can also use the superclass constructor.
     * @param name
     * @param inputType
     * @param outputType
     */
    protected MbrolaCaller(String name, MaryDataType inputType, MaryDataType outputType) {
        super(name, inputType, outputType);
    }

    public MbrolaCaller() throws NoSuchPropertyException {
        super("MbrolaCaller", MaryDataType.get("MBROLA"), MaryDataType.get("AUDIO"));
        String basePath = System.getProperty("mary.base") + File.separator + "bin" + File.separator;
        baseCmd = basePath + "mbrola";
        timeout = MaryProperties.needInteger("modules.timeout");
    }

    /**
     * Synthesise one chunk of MBROLA data with a given voice.
     * @param mbrolaData the MBROLA data in the usual MBROLA pho format
     * @param voice the Voice with which to synthesise the data
     * @return an AudioInputStream in the native audio format of the voice
     * @throws IOException if communication with external module fails
     */
    public AudioInputStream synthesiseOneSection(String mbrolaData, Voice voice) throws IOException {
        assert getState() == MODULE_RUNNING;
        if (mbrolaData == null || voice == null) {
            throw new IllegalArgumentException("Received null argument.");
        }
        assert voice instanceof MbrolaVoice : "Not an MBROLA voice: "+voice.getName();
        // Construct command line for external program call
        // mbrola reads from its stdin, writes headerless data to its stdout
        String[] cmd = new String[] {baseCmd, "-e", ((MbrolaVoice)voice).path(), "-", "-.raw"};

        // Timeout facility:
        int MAX_NR_ATTEMPTS = 2;
        int nrAttempts = 0;
        do {
            nrAttempts++;
            AudioDestination audioDestination = new AudioDestination();
            logger.debug("Keeping audio data in " + (audioDestination.isInRam() ? "RAM" : " a temp file"));
            StringBuffer cmdString = new StringBuffer();
            for (int i=0; i<cmd.length; i++) {
                cmdString.append(cmd[i]); cmdString.append(" ");
            }
            logger.info("Starting Synthesis with command: " + cmdString.toString().trim());
            Process process = Runtime.getRuntime().exec(cmd);
            // Workaround for Java 1.4.1 bug:
            if (System.getProperty("java.vendor").startsWith("Sun")
                && System.getProperty("java.version").startsWith("1.4.1")) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
            // Now attempt the external processing
            PrintWriter toWriter = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
            final InputStream from = new BufferedInputStream(process.getInputStream());
            StreamLogger errorLogger = new StreamLogger(process.getErrorStream(), name() + " err", null);
            errorLogger.start();
            AudioReader readingThread = new AudioReader(from, audioDestination);
            readingThread.start();
            // ErrorLogger will die when it reads end-of-file.
            logger.info("Writing to module.");
            logger.debug("Writing MBROLA input:\n" + mbrolaData + "\n");
            toWriter.print(mbrolaData);
            toWriter.flush();
            toWriter.close();
            boolean timeoutOccurred = false;
            do {
                try {
                    readingThread.join(timeout);
                } catch (InterruptedException e) {
                    logger.warn("Unexpected interruption while waiting for reader thread.");
                }
                timeoutOccurred = System.currentTimeMillis() - readingThread.latestSeenTime() >= timeout;
            } while (readingThread.isAlive() && !timeoutOccurred);
            // Now, in any case (timeour or not), get rid of Process:
            if (process != null) process.destroy();
            if (!timeoutOccurred) {
                return audioDestination.convertToAudioInputStream(voice.dbAudioFormat());
            }
            logger.warn("Timeout occurred in attempt " + nrAttempts + " out of " + MAX_NR_ATTEMPTS);
        } while (nrAttempts < MAX_NR_ATTEMPTS);
        throw new IOException("Repeated timeouts -- cannot synthesise.");
    }

}
