/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.modules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;

import marytts.datatypes.MaryDataType;
import marytts.exceptions.NoSuchPropertyException;
import marytts.modules.synthesis.MbrolaVoice;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.util.MaryRuntimeUtils;
import marytts.util.data.audio.AudioDestination;
import marytts.util.data.audio.AudioReader;
import marytts.util.io.StreamLogger;


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
        super("MbrolaCaller", MaryDataType.MBROLA, MaryDataType.AUDIO);
        String basePath = System.getProperty("mary.base") + File.separator + "bin" + File.separator;
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            baseCmd = basePath + "mbrola_cygwin.exe";
        } else if (osName.equals("Linux")) {
            baseCmd = basePath + "mbrola-linux-i386";
        } else if (osName.startsWith("Mac OS")) {
            baseCmd = basePath + "mbrola-darwin-ppc";
        } else if (osName.equals("Solaris") || osName.equals("SunOS")) {
            baseCmd = basePath + "mbrola-solaris";
        } else {
            // fallback to fragile agnostic brute-force search:
            baseCmd = findMbrolaBinary(basePath);
        }
        if (baseCmd == null) {
            throw new NullPointerException("No mbrola binary found in "+basePath+" that can be run on this machine.");
        }
        logger.debug("Found mbrola binary in "+baseCmd);
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
            AudioDestination audioDestination = MaryRuntimeUtils.createAudioDestination();
            logger.debug("Keeping audio data in " + (audioDestination.isInRam() ? "RAM" : " a temp file"));
            StringBuilder cmdString = new StringBuilder();
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

    /**
     * Try to find an mbrola binary that can be run on the present platform. Do this in the brute-force way:
     * try out all things in our bin/ directory that have mbrola in their name. If we can run them, we are done.
     * @param binPath
     * @return the full path of a runnable mbrola binary, or null if none could be found. 
     */
    private String findMbrolaBinary(String binPath) {
        String fileSeparator = System.getProperty("file.separator");
        if (!binPath.endsWith(fileSeparator)) {
            binPath += fileSeparator;
        }
        String[] mbrolas = new File(binPath).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().contains("mbrola");
            }
        });
        String mbrola = null;
        // Now go through all the files in bin/ called "*mbrola*" and try to run them:
        for (String exe : mbrolas) {
            try {
                Process p = Runtime.getRuntime().exec(new String[] {binPath+exe, "-h"});
                // The following line causes exitValue() to alternate randomly between 0 and 141 (SIGPIPE);
                // the latter case leaves mbrola == null and crashes the MARY server at startup:
                //p.getInputStream().close();
                p.waitFor();
                // I don't see how this was intended to work; were we just assuming that the first runnable exe is the right one for Windows?
                if (p.exitValue() == 0  || System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                    mbrola = exe;
                    break;
                }
            } catch (Exception e) {
                // no, this is not the right one
            }
        }
        if (mbrola != null) {
            return binPath + mbrola;
        }
        return null;
    }
}

