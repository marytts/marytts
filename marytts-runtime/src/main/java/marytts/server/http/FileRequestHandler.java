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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import marytts.util.http.Address;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 * Processor class for file http requests to Mary server
 * 
 * @author Oytun T&uuml;rk
 */
public class FileRequestHandler extends BaseHttpRequestHandler {

	private Set<String> validFiles = new HashSet<String>(Arrays.asList(new String[] { "favicon.ico", "index.html",
			"documentation.html", "mary.js" }));

	public FileRequestHandler() {
		super();

		// Add extra initialisations here
	}

	/**
	 * The entry point of all HttpRequestHandlers. When this method returns, the response is sent to the client. We override this
	 * here to show how simple a processing we are doing for file requests.
	 */
	@Override
	public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) {
		String uri = request.getRequestLine().getUri();
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		if (uri.equals(""))
			uri = "index.html";
		logger.debug("File requested: " + uri);
		if (validFiles.contains(uri)) {
			try {
				sendResourceAsStream(uri, response);
			} catch (IOException ioe) {
				logger.debug("Cannot send file", ioe);
				MaryHttpServerUtils.errorInternalServerError(response, "Cannot send file", ioe);
			}
		} else {
			MaryHttpServerUtils.errorFileNotFound(response, uri);
		}
	}

	@Override
	protected void handleClientRequest(String absPath, Map<String, String> queryItems, HttpResponse response,
			Address serverAddressAtClient) throws IOException {
		// not used because we override handle() directly.
	}

	private void sendResourceAsStream(String resourceFilename, HttpResponse response) throws IOException {
		InputStream stream = MaryHttpServer.class.getResourceAsStream(resourceFilename);

		String contentType;
		if (resourceFilename.endsWith(".html"))
			contentType = "text/html; charset=UTF-8";
		else if (resourceFilename.endsWith(".wav"))
			contentType = "audio/wav";
		else if (resourceFilename.endsWith(".m3u"))
			contentType = "audio/x-mpegurl";
		else if (resourceFilename.endsWith(".swf"))
			contentType = "application/x-shockwave-flash";
		else
			contentType = "text/plain";
		if (stream != null) {
			MaryHttpServerUtils.toHttpResponse(stream, response, contentType);
		} else {
			MaryHttpServerUtils.errorFileNotFound(response, resourceFilename);
		}
	}

}
