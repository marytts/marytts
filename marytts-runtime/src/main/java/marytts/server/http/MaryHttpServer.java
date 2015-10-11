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
package marytts.server.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;

import marytts.server.MaryProperties;
import marytts.util.MaryUtils;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.log4j.Logger;

/**
 * Listen for clients as an Http server at port <code>MaryProperties.socketPort()</code>.
 * <p>
 * There are two types of clients that can be handled:
 * <p>
 * (1) Non-web browser clients (2) Web browser clients
 * <p>
 * Note that non-web browser clients can mimic web browser clients by setting WEB_BROWSER_CLIENT parameter to "true" in the Http
 * request string
 * <p>
 * Clients can request the following (See below for more details):
 * <p>
 * (1) A file such as Mary icon or an audio file (2) Information like available voices, example texts, available audio formats,
 * etc (3) Synthesis of an appropriate input with appropriate additional parameters
 * <p>
 * For all clients, the responses are always sent in an HttpResponse. The entity in the response body can represent:
 * <p>
 * (1) An html page (applies only to web browser clients) (2) Some binary data (such as bytes of Mary icon for file requests, or
 * bytes of audio data for synthesis requests) (3) Some piece of text
 * <p>
 * A valid Mary Http request string is a collection of individual key-value pairs combined in Http request style:
 * <code>address?pair1&amp;pair2&amp;pair3...</code> etc.
 * <p>
 * The <code>address</code> identifies the kind of thing that the client is asking for:
 * <ul>
 * <li><code>version</code> requests the version of the MARY server;</li>
 * <li><code>datatypes</code> requests the list of available data types;</li>
 * <li><code>locales</code> requests the list of available locales / language components;</li>
 * <li><code>voices</code> requests the list of available voices;</li>
 * <li><code>audioformats</code> requests the list of supported audio file format types;</li>
 * <li><code>exampletext?voice=hmm-slt</code> requests the example text for the given voice;</li>
 * <li><code>exampletext?datatype=RAWMARYXML&amp;locale=de</code> requests an example text for data of the given type and locale;</li>
 * <li><code>audioeffects</code> requests the list of default audio effects;</li>
 * <li><code>audioeffect-default-param?effect=Robot</code> requests the default parameters of the given audio effect;</li>
 * <li><code>audioeffect-full?effect=Robot&amp;params=amount:100.0</code> requests a full description of the given audio effect,
 * including effect name, parameters and help text;</li>
 * <li><code>audioeffect-help?effect=Robot</code> requests a help text describing the given audio effect;</li>
 * <li><code>audioeffect-is-hmm-effect?effect=Robot</code> requests a boolean value (plain text "yes" or "no") indicating whether
 * or not the given effect is an effect that operates on HMM-based voices only;</li>
 * <li><code>features?locale=de</code> requests the list of available features that can be computed for the given locale;</li>
 * <li><code>features?voice=hmm-slt</code> requests the list of available features that can be computed for the given voice;</li>
 * <li><code>vocalizations?voice=dfki-poppy</code> requests the list of vocalization names that are available with the given
 * voice;
 * <li><code>styles?voice=dfki-pavoque-styles</code> requests the list of style names that are available with the given voice;
 * <li><code>process</code> requests the synthesis of some text (see below).</li>
 * </ul>
 * <p>
 * In Each pair has the following structure:
 * <p>
 * <code>KEY=VALUE</code>
 * <p>
 * where the following keys are used for passing additional information from server to client and/or vice versa:
 * <p>
 * INPUT_TYPE (input data type)
 * <p>
 * OUTPUT_TYPE (output data type)
 * <p>
 * AUDIO (audio format. It may include streaming/non-streaming information as well. Example values for non-streaming formats:
 * AU_FILE, MP3_FILE, WAVE_FILE Example values for streaming formats: AU_STREAM, MP3_STREAM)
 * <p>
 * STYLE (Style descriptor)
 * <p>
 * INPUT_TEXT (Input text to be synthesised)
 * <p>
 * OUTPUT_TEXT (Output text - if the output type is not audio)
 * <p>
 * SYNTHESIS_OUTPUT (A key to ask for synthesis, or to represent synthesis result. Example values: SYNTHESIS_OUTPUT=? instantiates
 * a synthesis request In response, the server can set SYNTHESIS_OUTPUT to DONE, PENDING, or FAILED depending on the validity and
 * type of te request PENDING is a special case used for handling double requests due to EMBED or OBJECT tags in web browser
 * client html pages
 * 
 * <p>
 * Additionally, web browser clients should use the following key-value pair to tell the server about their type:
 * <p>
 * WEB_BROWSER_CLIENT=true (All other values will be interpreted as non-web browser client)
 * <p>
 * An easy way to test the http server is as follows:
 * <p>
 * (1) Run mary server in "http" mode by setting server=http in marybase.config
 * <p>
 * (2) Copy and paste the following to a web browser´s address bar:
 * <p>
 * <a href=
 * "http://localhost:59125/process?INPUT_TYPE=TEXT&OUTPUT_TYPE=AUDIO&INPUT_TEXT=Welcome+to+the+world+of+speech+synthesis!&AUDIO=AU&LOCALE=en_US&VOICE=hsmm-slt"
 * >marytts server intro </a>
 * <p>
 * Provided that the server runs at localhost:59125 (or change "http://localhost:59125/" part as required), the web browser
 * supports AUDIO type (if not try other formats such as WAVE, MP3, OGG or install a plug-in to play the target format), and the
 * VOICE is installed (hmm-slt), the synthesis result should be sent to the web browser for playback or saving (depending on web
 * browser settings).
 * <p>
 * 
 * check {@link InfoRequestHandler}, {@link FileRequestHandler}, {@link SynthesisRequestHandler} .
 * 
 * @author Oytun T&uuml;rk, Marc Schr&ouml;der
 */

public class MaryHttpServer extends Thread {
	private static Logger logger;

	private boolean isReady = false;

	public MaryHttpServer() {
		logger = MaryUtils.getLogger("server");
	}

	public boolean isReady() {
		return isReady;
	}

	public void run() {
		logger.info("Starting server.");

		int localPort = MaryProperties.needInteger("socket.port");

		HttpParams params = new BasicHttpParams();
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0)
				// 0 means no timeout, any positive value means time out in miliseconds (i.e. 50000 for 50 seconds)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
				.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
				.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

		BasicHttpProcessor httpproc = new BasicHttpProcessor();
		httpproc.addInterceptor(new ResponseDate());
		httpproc.addInterceptor(new ResponseServer());
		httpproc.addInterceptor(new ResponseContent());
		httpproc.addInterceptor(new ResponseConnControl());

		BufferingHttpServiceHandler handler = new BufferingHttpServiceHandler(httpproc, new DefaultHttpResponseFactory(),
				new DefaultConnectionReuseStrategy(), params);

		// Set up request handlers
		HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
		registry.register("/process", new SynthesisRequestHandler());
		InfoRequestHandler infoRH = new InfoRequestHandler();
		registry.register("/version", infoRH);
		registry.register("/datatypes", infoRH);
		registry.register("/locales", infoRH);
		registry.register("/voices", infoRH);
		registry.register("/audioformats", infoRH);
		registry.register("/exampletext", infoRH);
		registry.register("/audioeffects", infoRH);
		registry.register("/audioeffect-default-param", infoRH);
		registry.register("/audioeffect-full", infoRH);
		registry.register("/audioeffect-help", infoRH);
		registry.register("/audioeffect-is-hmm-effect", infoRH);
		registry.register("/features", infoRH);
		registry.register("/features-discrete", infoRH);
		registry.register("/vocalizations", infoRH);
		registry.register("/styles", infoRH);
		registry.register("*", new FileRequestHandler());

		handler.setHandlerResolver(registry);

		// Provide an event logger
		handler.setEventListener(new EventLogger());

		IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(handler, params);

		int numParallelThreads = MaryProperties.getInteger("server.http.parallelthreads", 5);

		logger.info("Waiting for client to connect on port " + localPort);

		try {
			ListeningIOReactor ioReactor = new DefaultListeningIOReactor(numParallelThreads, params);
			ioReactor.listen(new InetSocketAddress(localPort));
			isReady = true;
			ioReactor.execute(ioEventDispatch);
		} catch (InterruptedIOException ex) {
			logger.info("Interrupted", ex);
		} catch (IOException e) {
			logger.info("Problem with HTTP connection", e);
		}
		logger.debug("Shutdown");
	}

	static class EventLogger implements EventListener {
		public void connectionOpen(final NHttpConnection conn) {
			logger.info("Connection from " + conn.getContext().getAttribute(ExecutionContext.HTTP_TARGET_HOST) // conn.getInetAddress().getHostName()
			);
		}

		public void connectionTimeout(final NHttpConnection conn) {
			logger.info("Connection timed out: " + conn);
		}

		public void connectionClosed(final NHttpConnection conn) {
			logger.info("Connection closed: " + conn);
		}

		public void fatalIOException(final IOException ex, final NHttpConnection conn) {
			logger.info("I/O error: " + ex.getMessage());
		}

		public void fatalProtocolException(final HttpException ex, final NHttpConnection conn) {
			logger.info("HTTP error: " + ex.getMessage());
		}
	}
}
