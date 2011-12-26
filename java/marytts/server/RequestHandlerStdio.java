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
package marytts.server;

// General Java Classes
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;

import javax.sound.sampled.AudioSystem;
import javax.xml.transform.TransformerException;

import marytts.datatypes.MaryDataType;
import marytts.server.http.MaryHttpServerUtils;
import marytts.util.MaryUtils;
import marytts.util.io.LoggingReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.xml.sax.SAXParseException;


/**
 * A lightweight process handling one Request in a thread of its own.
 * This is to be used when running as a stdio server.
 * @author Marc Schr&ouml;der
 */

public class RequestHandlerStdio {
    private Request request;
    private BufferedReader inputReader;
    private Logger logger;

    /**
     * Constructor to be used for Socket processing (running as a standalone
     * socket server).  <code>inputReader</code> is a Reader reading from from
     * <code>dataSocket.inputStream()</code>. Passing this on is necessary
     * because the mary server does a buffered read on that input stream, and
     * without passing that buffered reader on, data gets lost.
     */
    public RequestHandlerStdio(
        Request request,
        BufferedReader inputReader)
    {
        if (request == null)
            throw new NullPointerException("Cannot handle null request");
        this.request = request;
        logger = MaryUtils.getLogger("server");
        this.inputReader = inputReader;
    }

    /**
     * Perform the actual processing by calling the appropriate methods
     * of the associated <code>Request</code> object.
     * <p>
     * Note that while different request handlers run as different threads,
     * they all use the same module objects. How a given module deals with
     * several requests simultaneously is its own problem, the simplest
     * solution being a synchronized <code>process()</code> method.
     *
     * @see Request
     * @see marytts.modules.MaryModule
     * @see marytts.modules.ExternalModule
     * @see marytts.modules.InternalModule
     */
    public void run() {
        boolean ok = true;
        // tasks:
        // * read the input according to its type
        // * determine which modules are needed
        // * in turn, write to and read from each module according to data type
        // * write output according to its type

        try {
            String inputText = readInputData();
            request.setInputData(inputText);
        } catch (Exception e) {
            String message = "Problem reading input";
            logger.warn(message, e);
            ok = false;
        }

        boolean streamingOutput = false;
        StreamingOutputWriter rw = null;
        // Process input data to output data
        if (ok)
            try {
                if (request.getOutputType().equals(MaryDataType.get("AUDIO"))
                        && request.getStreamAudio()) {
                    streamingOutput = true;
                    rw = new StreamingOutputWriter(request, inputReader, System.out);
                    rw.start();
                }
                
                request.process();
            } catch (Throwable e) {
                String message = "Processing failed.";
                logger.error(message, e);
                ok = false;
            }

        // Write output:
        if (ok) {
            if (!streamingOutput) {
                try {
                    request.writeOutputData(System.out);
                } catch (Exception e) {
                    String message = "Cannot write output, client seems to have disconnected.";
                    logger.warn(message, e);
                    ok = false;
                }
            } else { // streaming output
                try {
                    rw.join();
                } catch (InterruptedException ie) {
                    logger.warn(ie);
                }
            }
        }            
        if (ok)
            logger.info("Request handled successfully.");
        else
            logger.info("Request couldn't be handled successfully.");
        if (MaryUtils.lowMemoryCondition()) {
            logger.info("Low memory condition detected (only " + MaryUtils.availableMemory() + " bytes left). Triggering garbage collection.");
            Runtime.getRuntime().gc();
            logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
        }

    } // run()

    // Read input text from the client.  A line by itself with just a '.' indicates the end.
    // If the line starts with a dot, subtract one, as the client needs to escape
    // this line by prepending a dot.
    private String readInputData()
    {
        String line;
        try {
            line = inputReader.readLine();
        } catch (IOException e) {
            // Client must have gone away
            return "";
        }
        String inputText = "";
        while(!line.equals(".")) {
            if(line.startsWith(".")) {
                line = line.substring(1);
            }
            inputText += line;
            try {
                line = inputReader.readLine();
            } catch (IOException e) {
                return inputText;
            }
        }
        return inputText;
    }
            
    public static class StreamingOutputWriter extends Thread
    {
        private Request request;
        private BufferedReader reader;
        private PrintStream output;
        private Logger logger;
        
        public StreamingOutputWriter(Request request, BufferedReader reader, PrintStream output) throws Exception
        {
            this.request = request;
            this.reader = reader;
            this.output = output;
            this.setName("RW " + request.getId());
            logger = MaryUtils.getLogger(this.getName());
        }
        
        public void run()
        {
            try {
                StdioOutputStream stdioOut = new StdioOutputStream(reader, output);
                AudioSystem.write(request.getAudio(), request.getAudioFileFormat().getType(), stdioOut);
                stdioOut.finish();
                logger.info("Finished writing output");
            } catch (IOException ioe) {
                logger.info("Cannot write output, client seems to have disconnected. ", ioe);
                request.abort();
            }
        }
        
        private class StdioOutputStream extends OutputStream {
            
            BufferedReader reader;
            PrintStream output;
            int numWritten = 0;
            
            StdioOutputStream(BufferedReader reader, PrintStream output)
            {
                this.reader = reader;
                this.output = output;
            }
            
            private boolean userCanceled()
            {
                output.write(0x80);
                output.write(0x00);
                output.flush();
                String line;
                try {
                    line = reader.readLine().trim();
                } catch (IOException e) {
                    // Same as cancel
                    return true;
                }
                return !line.equals("true");
            }

            @Override
            public void write(int value) throws IOException {
                output.write(value);
                numWritten++;
                if(numWritten == 512) {
                    numWritten = 0;
                    if(userCanceled()) {
                        throw new IOException("speech canceled by client");
                    }
                }
            }
            
            // Indicate the end of audio data with empty block of data.
            public void finish()
            {
                if(userCanceled()) {
                    return;
                }
                if(numWritten != 0) {
                    // Have to check that we didn't just send an empty block
                    userCanceled();
                }
            }
        }
    }
    
}

