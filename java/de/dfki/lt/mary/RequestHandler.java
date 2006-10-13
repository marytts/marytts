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
package de.dfki.lt.mary;

// General Java Classes
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;

import javax.sound.sampled.AudioSystem;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.xml.sax.SAXParseException;

import de.dfki.lt.mary.util.LoggingReader;
import de.dfki.lt.mary.util.MaryUtils;

/**
 * A lightweight process handling one Request in a thread of its own.
 * This is to be used when running as a socket server.
 * @author Marc Schr&ouml;der
 */

public class RequestHandler extends Thread {
    private Request request;
    private Socket infoSocket;
    private Socket dataSocket;
    private LoggingReader inputReader;
    private Logger logger;
    private Logger clientLogger;

    /**
     * Constructor to be used for Socket processing (running as a standalone
     * socket server).  <code>inputReader</code> is a Reader reading from from
     * <code>dataSocket.inputStream()</code>. Passing this on is necessary
     * because the mary server does a buffered read on that input stream, and
     * without passing that buffered reader on, data gets lost.
     */
    public RequestHandler(
        Request request,
        Socket infoSocket,
        Socket dataSocket,
        Reader inputReader) {
        if (request == null)
            throw new NullPointerException("Cannot handle null request");
        this.request = request;
        if (infoSocket == null)
            throw new NullPointerException("Received null infoSocket");
        this.infoSocket = infoSocket;
        if (dataSocket == null)
            throw new NullPointerException("Received null dataSocket");
        this.dataSocket = dataSocket;
        this.setName("RH " + request.getId());
        logger = Logger.getLogger(this.getName());
        this.inputReader = new LoggingReader(inputReader, logger);
        clientLogger = Logger.getLogger(this.getName() + " client");
        try {
            clientLogger.addAppender(
                new WriterAppender(
                    new SimpleLayout(),
                    new PrintWriter(infoSocket.getOutputStream(), true)));
            clientLogger.setLevel(Level.WARN);
            // What goes to clientLogger does not go to the normal logfile:
            clientLogger.setAdditivity(false);
        } catch (IOException e) {
            logger.warn("Cannot write warnings to client", e);
        }
    }

    private void clientLogWarning(String message, Exception e) {
        // Only print to client if there is one;
        // do not print TransformerException and SAXParseException
        // (that has been done by the LoggingErrorHander already).
        if (!(clientLogger == null
            || e instanceof TransformerException
            || e instanceof SAXParseException))
            clientLogger.warn(message + "\n" + e.toString());
        // No stack trace on clientLogger
    }

    private void clientLogError(String message, Throwable e) {
        // Only print to client if there is one;
        // do not print TransformerException and SAXParseException
        // (that has been done by the LoggingErrorHander already).
        if (!(clientLogger == null || e instanceof TransformerException || e instanceof SAXParseException))
            clientLogger.error(message + "\n" + MaryUtils.getThrowableAndCausesAsString(e));
        // No stack trace on clientLogger
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
     * @see de.dfki.lt.mary.modules.MaryModule
     * @see de.dfki.lt.mary.modules.ExternalModule
     * @see de.dfki.lt.mary.modules.InternalModule
     */
    public void run() {
        boolean ok = true;
        // tasks:
        // * read the input according to its type
        // * determine which modules are needed
        // * in turn, write to and read from each module according to data type
        // * write output according to its type

        try {
            request.readInputData(inputReader);
        } catch (Exception e) {
            String message = "Problem reading input";
            logger.warn(message, e);
            clientLogWarning(message, e);
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
                    rw = new StreamingOutputWriter(request, dataSocket.getOutputStream());
                    rw.start();
                }
                
                request.process();
            } catch (Throwable e) {
                String message = "Processing failed.";
                logger.error(message, e);
                clientLogError(message, e);
                ok = false;
            }

        // For simple clients, we need to close the infoSocket before sending
        // the data on dataSocket. Otherwise there may be deadlock.
        try {
            if (clientLogger != null) {
                clientLogger.removeAllAppenders();
                clientLogger = null;
            }
            infoSocket.close();
        } catch (IOException e) {
            logger.warn("Couldn't close info socket properly.", e);
            ok = false;
        }

        // Write output:
        if (ok) {
            if (!streamingOutput) {
                try {
                    request.writeOutputData(dataSocket.getOutputStream());
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
        try {
            dataSocket.close();
        } catch (IOException e) {
            logger.warn("Couldn't close data socket properly.", e);
            ok = false;
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

    public static class StreamingOutputWriter extends Thread
    {
        private Request request;
        private OutputStream output;
        private Logger logger;
        
        public StreamingOutputWriter(Request request, OutputStream output) throws Exception
        {
            this.request = request;
            this.output = output;
            this.setName("RW " + request.getId());
            logger = Logger.getLogger(this.getName());
        }
        
        public void run()
        {
            try {
                AudioSystem.write(request.getAudio(), request.getAudioFileFormat().getType(), output);
                output.flush();
                logger.info("Finished writing output");
            } catch (IOException ioe) {
                logger.info("Cannot write output, client seems to have disconnected. ", ioe);
                request.abort();
            }
        }
    }
}
