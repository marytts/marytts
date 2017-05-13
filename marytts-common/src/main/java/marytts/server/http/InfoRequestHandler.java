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
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.http.Address;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NStringEntity;

/**
 * Processor class for information http requests to Mary server
 *
 * @author Oytun T&uuml;rk, Marc Schr&ouml;der
 */
public class InfoRequestHandler extends BaseHttpRequestHandler {

	public InfoRequestHandler() {
		super();
	}

	@Override
	protected void handleClientRequest(String absPath, Map<String, String> queryItems, HttpResponse response,
			Address serverAddressAtClient) throws IOException {
		// Individual info request
		String infoResponse = handleInfoRequest(absPath, queryItems, response);
		if (infoResponse == null) { // error condition, handleInfoRequest has
									// set an error message
			return;
		}

		response.setStatusCode(HttpStatus.SC_OK);
		try {
			NStringEntity entity = new NStringEntity(infoResponse, "UTF-8");
			entity.setContentType("text/plain; charset=UTF-8");
			response.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
		}
	}

	private String handleInfoRequest(String absPath, Map<String, String> queryItems, HttpResponse response) {
		logger.debug("New info request: " + absPath);
		if (queryItems != null) {
			for (String key : queryItems.keySet()) {
				logger.debug("    " + key + "=" + queryItems.get(key));
			}
		}

		assert absPath.startsWith("/") : "Absolute path '" + absPath + "' does not start with a slash!";
		String request = absPath.substring(1); // without the initial slash

		if (request.equals("version"))
			return MaryRuntimeUtils.getMaryVersion();
		else if (request.equals("locales"))
			return MaryRuntimeUtils.getLocales();
		else if (request.equals("features") || request.equals("features-discrete")) {
			return null;
		}
		MaryHttpServerUtils.errorFileNotFound(response, request);
		return null;
	}

}
