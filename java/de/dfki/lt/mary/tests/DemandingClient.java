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
package de.dfki.lt.mary.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.client.MaryClient;

/**
 * This class is used for testing the stability of the server under high load.
 * It continuously sends requests to the server and measures the time needed
 * for their processing.
 */
public class DemandingClient {

    private String input;
    int total;
    int failures;
    long testStartTime;
    long audioDuration;
    
    public DemandingClient(String input) {
        this.input = input;
        audioDuration = 0;
    }
    
    private void statusReport() {
        System.err.println();
        System.err.println("Sent " + total + " requests, thereof " + (total - failures) + " successful.");
        if (total == 0) return;
        long meanProcTime = (System.currentTimeMillis() - testStartTime) / total;
        System.err.print("Mean processing time per request: " + meanProcTime + " ms");
        if (audioDuration != 0) {
            System.err.print(" (" + (audioDuration / meanProcTime) + "x realtime)");
        }
        System.err.println();
    }

    public void run() throws IOException, UnknownHostException {
        // Do one first call for creating a reference file, all others will then
        // be compared to its size.
        MaryClient mc = new MaryClient();
        String inputType = System.getProperty("input.type", "TEXT_DE");
        String outputType = System.getProperty("output.type", "AUDIO");
        String audioType = System.getProperty("audio.type", "WAVE");
        if (!(audioType.equals("AIFC")
            || audioType.equals("AIFF")
            || audioType.equals("AU")
            || audioType.equals("SND")
            || audioType.equals("WAVE")
            || audioType.equals("MP3"))) {
            System.err.println("Invalid value '" + audioType + "' for property 'audio.type'");
            System.err.println();
            usage();
            System.exit(1);
        }
        String defaultVoiceName = System.getProperty("voice.default", "de7");

        File referenceFile = File.createTempFile("ref", "mary");
        long startTime = System.currentTimeMillis();
        try {
            FileOutputStream fos = new FileOutputStream(referenceFile);
            mc.process(input, inputType, outputType, audioType, defaultVoiceName, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        long referenceSize = referenceFile.length();
        if (referenceSize == 0) {
            System.err.println("First call failed: received no data. Aborting.");
            System.exit(1);
        }
        System.err.print("Reference file " + referenceFile.getCanonicalPath() + ", size: " + referenceSize);
        // Attempt to find out how long the audio file is (in seconds):
        if (outputType.equals("AUDIO")) {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(referenceFile);
                ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
                
                float samplingrate = ais.getFormat().getSampleRate();
                //System.err.println("samplingrate: " + samplingrate);
                float framelength = ais.getFrameLength();
                if (framelength < 0) { // need to count by hand
                    int n = 0;
                    while (ais.read() != -1) n++;
                    framelength = n / 2;
                }
                //System.err.println("framelength: " + framelength);
                audioDuration = (long) (1000 * framelength / samplingrate);
                System.err.print(", audio duration " + audioDuration + " ms.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.err.println();
        long endTime = System.currentTimeMillis();
        long timeout = 10 * (endTime - startTime);

        total = 0;
        failures = 0;
        testStartTime = System.currentTimeMillis();
        File testFile = File.createTempFile("test", "mary");
        while (true) {
            try {
                FileOutputStream fos = new FileOutputStream(testFile);

                mc.process(input, inputType, outputType, audioType, defaultVoiceName, fos, timeout);
                fos.close();
                long testSize = testFile.length();
                if (testSize != referenceSize) {
                    System.err.println(
                        "Received "
                            + testSize
                            + " bytes, expected "
                            + referenceSize
                            + " -- see "
                            + testFile.getCanonicalPath());
                    failures++;
                    testFile = File.createTempFile("test", "mary");
                }
                total++;
                if (total % 2 == 0) {
                    if (total % 10 == 0)
                        System.err.print("|");
                    else
                        System.err.print(".");
                }
                if (total % 100 == 0) {
                    statusReport();
                }
            } catch (Exception e) {
                e.printStackTrace();
                failures++;
            }
        }
        
    }

    public static void usage() {
        System.err.println("usage:");
        System.err.println("java [properties] " + DemandingClient.class.getName() + " [inputfile]");
        System.err.println();
        System.err.println("Properties are: -Dinput.type=INPUTTYPE");
        System.err.println("                -Doutput.type=OUTPUTTYPE");
        System.err.println("                -Daudio.type=AUDIOTYPE");
        System.err.println("                -Dvoice.default=male|female|de1|de2|de3|...");
        System.err.println("                -Dserver.host=HOSTNAME");
        System.err.println("                -Dserver.port=PORTNUMBER");
        System.err.println(
            "where INPUTTYPE is one of TEXT_DE, TEXT_EN, RAWMARYXML, TOKENISED_DE, PREPROCESSED_DE, CHUNKED_DE,");
        System.err.println(
            "                          PHONEMISED_DE, INTONISED_DE, POSTPROCESSED_DE, ACOUSTPARAMS or MBROLA,");
        System.err.println("     OUTPUTTYPE is one of TOKENISED_DE, PREPROCESSED_DE, CHUNKED_DE, PHONEMISED_DE");
        System.err.println("                          INTONISED_DE, POSTPROCESSED_DE, ACOUSTPARAMS, MBROLA, or AUDIO,");
        System.err.println("and AUDIOTYPE is one of AIFC, AIFF, AU, SND, WAVE, and MP3.");
        System.err.println("The default values for input.type and output.type are TEXT_DE and AUDIO,");
        System.err.println("respectively; the default audio.type is WAVE.");
        System.err.println();
        System.err.println("inputfile must be of type input.type.");
        System.err.println("If no inputfile is given, the program will read from standard input.");
        System.err.println();
        System.err.println("The output is written to standard output, so redirect or pipe as appropriate.");
    }

    public static void main(String[] args)
    throws IOException, UnknownHostException {
        // This code is copied from MaryClient.main(),
        // except for the loop around the processing:

        if (args.length > 0 && args[0].equals("-h")) {
            usage();
            System.exit(1);
        }

        // Respect if user wants client feedback, but default to quiet:
        if (System.getProperty("mary.client.quiet") == null)
            System.setProperty("mary.client.quiet", "true");

        BufferedReader inputReader;
        if (args.length > 0) {
            File file = new File(args[0]);
            inputReader = new BufferedReader(new FileReader(file));
        } else { // no Filename, read from stdin:
            inputReader = new BufferedReader(new InputStreamReader(System.in));
        }

        // Read input into a string:
        StringBuffer sb = new StringBuffer(1024);
        char[] buf = new char[1024];
        int nr;
        while ((nr = inputReader.read(buf)) != -1) {
            sb.append(buf, 0, nr);
        }

        String input = sb.toString();
        final DemandingClient dc = new DemandingClient(input);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                dc.statusReport();
            }
        });
        dc.run();
    }
}
