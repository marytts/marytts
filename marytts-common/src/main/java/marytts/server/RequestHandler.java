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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;

import javax.sound.sampled.AudioSystem;
import javax.xml.transform.TransformerException;

import marytts.datatypes.MaryDataType;
import marytts.server.http.MaryHttpServerUtils;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.io.LoggingReader;

import org.apache.http.HttpResponse;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.xml.sax.SAXParseException;

/**
 * A lightweight process handling one Request in a thread of its own. This is to be used when running as a socket server.
 * 
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
	 * Constructor to be used for Socket processing (running as a standalone socket server). <code>inputReader</code> is a Reader
	 * reading from from <code>dataSocket.inputStream()</code>. Passing this on is necessary because the mary server does a
	 * buffered read on that input stream, and without passing that buffered reader on, data gets lost.
	 * 
	 * @param request
	 *            request
	 * @param infoSocket
	 *            infoSocket
	 * @param dataSocket
	 *            dataSocket
	 * @param inputReader
	 *            inputReader
	 */
	public RequestHandler(Request request, Socket infoSocket, Socket dataSocket, Reader inputReader) {
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
		logger = MaryUtils.getLogger(this.getName());
		this.inputReader = new LoggingReader(inputReader, logger);
		clientLogger = MaryUtils.getLogger(this.getName() + " client");
		try {
			clientLogger.addAppender(new WriterAppender(new SimpleLayout(), new PrintWriter(infoSocket.getOutputStream(), true)));
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
		if (!(clientLogger == null || e instanceof TransformerException || e instanceof SAXParseException))
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
	 * Perform the actual processing by calling the appropriate methods of the associated <code>Request</code> object.
	 * <p>
	 * Note that while different request handlers run as different threads, they all use the same module objects. How a given
	 * module deals with several requests simultaneously is its own problem, the simplest solution being a synchronized
	 * <code>process()</code> method.
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
				if (request.getOutputType().equals(MaryDataType.get("AUDIO")) && request.getStreamAudio()) {
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
		if (MaryRuntimeUtils.lowMemoryCondition()) {
			logger.info("Low memory condition detected (only " + MaryUtils.availableMemory()
					+ " bytes left). Triggering garbage collection.");
			Runtime.getRuntime().gc();
			logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
		}

	} // run()

	public static class StreamingOutputWriter extends Thread {
		private Request request;
		private OutputStream output;
		private Logger logger;

		public StreamingOutputWriter(Request request, OutputStream output) throws Exception {
			this.request = request;
			this.output = output;
			this.setName("RW " + request.getId());
			logger = MaryUtils.getLogger(this.getName());
		}

		public void run() {
			try {
				AudioSystem.write(request.getAudio(), request.getAudioFileFormat().getType(), output);
				output.flush();
				output.close();
				logger.info("Finished writing output");
			} catch (IOException ioe) {
				logger.info("Cannot write output, client seems to have disconnected. ", ioe);
				request.abort();
			}
		}
	}

	// A reader class for debugging purposes for StreamingOutputWriter
	public static class StreamingOutputPiper extends Thread {
		private InputStream input;
		private Logger logger = null;
		private BufferedWriter textWriter = null;
		private FileOutputStream binaryWriter = null;
		private HttpResponse response = null;
		private String contentType = null;

		// Reads from input and pipes the output to logger
		public StreamingOutputPiper(InputStream input) throws Exception {
			this.input = input;

			logger = MaryUtils.getLogger(this.getName());
			textWriter = null;
			binaryWriter = null;
			response = null;
			contentType = null;
		}

		// Reads from input and pipes the output to text file
		public StreamingOutputPiper(InputStream input, String outTextFile) throws Exception {
			this.input = input;

			logger = null;
			textWriter = new BufferedWriter(new FileWriter(outTextFile));
			binaryWriter = null;
			response = null;
			contentType = null;
		}

		// Reads from input and pipes the output to binary file
		public StreamingOutputPiper(InputStream input, File outFile) throws Exception {
			this.input = input;

			logger = null;
			textWriter = null;
			binaryWriter = new FileOutputStream(outFile);
			response = null;
			contentType = null;
		}

		// Reads from input and pipes the output to Http response
		public StreamingOutputPiper(InputStream input, HttpResponse response, String contentType) throws Exception {
			this.input = input;

			logger = null;
			textWriter = null;
			binaryWriter = null;
			this.response = response;
			this.contentType = contentType;
		}

		public void run() {
			String message;

			try {
				int val;
				if (logger != null) {
					while ((val = input.read()) != -1)
						logger.debug(val + System.getProperty("line.separator"));

					input.close();
				} else if (textWriter != null) {
					while ((val = input.read()) != -1) {
						textWriter.write(val);
						textWriter.newLine();
					}

					input.close();
				} else if (binaryWriter != null) {
					while ((val = input.read()) != -1)
						binaryWriter.write((byte) val);

					input.close();
				} else if (response != null) {
					MaryHttpServerUtils.toHttpResponse(input, response, contentType);

					// Important! input.close() is not needed here since toHttpResponse already closes the input
				} else {
					System.out.println("Error: No writers initialised!");
					input.close();
					return;
				}

				message = "Finished reading output";
				if (logger != null)
					logger.info(message);
				else if (textWriter != null) {
					textWriter.write(message);
					textWriter.newLine();
					textWriter.close();
				} else if (binaryWriter != null)
					binaryWriter.close();
			} catch (IOException ioe) {
				message = "Cannot read output, client seems to have disconnected. ";
				if (logger != null)
					logger.info(message, ioe);
				else if (textWriter != null) {
					try {
						textWriter.write(message);
						textWriter.newLine();
					} catch (IOException e) {
						System.out.println("Error: Cannot write to text writer!");
					}
				}
			}
		}
	}
}
