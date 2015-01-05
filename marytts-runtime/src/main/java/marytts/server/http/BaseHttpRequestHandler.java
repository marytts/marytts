/**
 * Copyright 2007 DFKI GmbH.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.Version;
import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.datatypes.MaryDataType;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.signalproc.effects.AudioEffect;
import marytts.signalproc.effects.AudioEffects;
import marytts.signalproc.effects.BaseAudioEffect;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.http.Address;
import marytts.util.string.StringUtils;
import marytts.vocalizations.VocalizationSynthesizer;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.FileContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.entity.ContentListener;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * Provides baseline functionality to process http requests to the Mary server.
 * 
 * @author Oytun T&uuml;rk, Marc Schröder
 */
public abstract class BaseHttpRequestHandler extends SimpleNHttpRequestHandler implements HttpRequestHandler {
	private final boolean useFileChannels = true;

	protected static Logger logger;
	private int runningNumber = 1;
	private Map<String, Object[]> requestMap;

	public BaseHttpRequestHandler() {
		super();
		logger = MaryUtils.getLogger("server");
		requestMap = Collections.synchronizedMap(new HashMap<String, Object[]>());

	}

	/**
	 * The entry point of all HttpRequestHandlers. When this method returns, the response is sent to the client.
	 */
	public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException,
			IOException {
		try {
			Header[] tmp = request.getHeaders("Host");
			Address serverAddressAtClient = getServerAddressAtClient(tmp[0].getValue());
			String uri = request.getRequestLine().getUri();

			String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
			if (!(method.equals("GET") || method.equals("POST"))) {
				throw new MethodNotSupportedException(method + " method not supported");
			}

			String absPath = null;
			String query = null;
			if (uri != null && uri.length() > 0) {
				if (!uri.startsWith("/")) {
					throw new HttpException("Unexpected uri: '" + uri + "' does not start with a slash");
				}
				int q = uri.indexOf('?');
				if (q == -1) {
					absPath = uri;
				} else {
					absPath = uri.substring(0, q);
					query = uri.substring(q + 1);
				}
			}
			Map<String, String> queryItems = null;
			if (query != null && query.length() > 0) {
				queryItems = MaryHttpServerUtils.toKeyValuePairs(query, true);
			}

			// Try and get parameters from different HTTP POST requests if you have not been able to do this above
			if (method.equals("POST") && queryItems == null && request instanceof HttpEntityEnclosingRequest) {
				try {
					String postQuery = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
					queryItems = MaryHttpServerUtils.toKeyValuePairs(postQuery, true);
				} catch (Exception e) {
					logger.debug("Cannot read post query", e);
					MaryHttpServerUtils.errorInternalServerError(response, "Cannot read post query", e);
				}
			}

			// Parse request and create appropriate response
			handleClientRequest(absPath, queryItems, response, serverAddressAtClient);

		} catch (RuntimeException re) {
			logger.warn("runtime exception in handle():", re);
		}
	}

	protected abstract void handleClientRequest(String absPath, Map<String, String> queryItems, HttpResponse response,
			Address serverAddressAtClient) throws IOException;

	protected Address getServerAddressAtClient(String fullHeader) {
		String fullAddress = fullHeader.trim();
		int index = fullAddress.indexOf('?');

		if (index > 0)
			fullAddress = fullAddress.substring(0, index);

		return new Address(fullAddress);
	}

	public ConsumingNHttpEntity entityRequest(final HttpEntityEnclosingRequest request, final HttpContext context)
			throws HttpException, IOException {
		return new ConsumingNHttpEntityTemplate(request.getEntity(), new FileWriteListener(useFileChannels));
	}

	static class FileWriteListener implements ContentListener {
		private final File file;
		private final FileInputStream inputFile;
		private final FileChannel fileChannel;
		private final boolean useFileChannels;
		private long idx = 0;

		public FileWriteListener(boolean useFileChannels) throws IOException {
			this.file = File.createTempFile("tmp", ".tmp", null);
			this.inputFile = new FileInputStream(file);
			this.fileChannel = inputFile.getChannel();
			this.useFileChannels = useFileChannels;
		}

		public void contentAvailable(ContentDecoder decoder, IOControl ioctrl) throws IOException {
			long transferred;
			if (useFileChannels && decoder instanceof FileContentDecoder) {
				transferred = ((FileContentDecoder) decoder).transfer(fileChannel, idx, Long.MAX_VALUE);
			} else {
				transferred = fileChannel.transferFrom(new ContentDecoderChannel(decoder), idx, Long.MAX_VALUE);
			}

			if (transferred > 0)
				idx += transferred;
		}

		public void finished() {
			try {
				inputFile.close();
			} catch (IOException ignored) {
			}
			try {
				fileChannel.close();
			} catch (IOException ignored) {
			}
		}
	}

}
