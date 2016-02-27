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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;

import marytts.util.ConversionUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.string.StringUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.log4j.Logger;

/**
 * Utility functions for Mary http server
 * 
 * @author Oytun T&uuml;rk
 */
public class MaryHttpServerUtils {
	private static Logger logger = MaryUtils.getLogger("http");

	public static void toHttpResponse(double[] x, HttpResponse response, String contentType) throws IOException {
		toHttpResponse(ConversionUtils.toByteArray(x), response, contentType);
	}

	public static void toHttpResponse(int[] x, HttpResponse response, String contentType) throws IOException {
		toHttpResponse(ConversionUtils.toByteArray(x), response, contentType);
	}

	public static void toHttpResponse(String x, HttpResponse response, String contentType) throws IOException {
		toHttpResponse(ConversionUtils.toByteArray(x), response, contentType);
	}

	public static void toHttpResponse(byte[] byteArray, HttpResponse response, String contentType) throws IOException {
		NByteArrayEntity body = new NByteArrayEntity(byteArray);
		body.setContentType(contentType);
		response.setEntity(body);
		response.setStatusCode(HttpStatus.SC_OK);
	}

	public static void toHttpResponse(InputStream stream, HttpResponse response, String contentType) throws IOException {
		toHttpResponse(stream, response, contentType, -1);
	}

	public static void toHttpResponse(InputStream stream, HttpResponse response, String contentType, long streamLength)
			throws IOException {
		InputStreamEntity body = new InputStreamEntity(stream, streamLength);
		body.setContentType(contentType);
		response.setEntity(body);
		response.setStatusCode(HttpStatus.SC_OK);
	}

	public static void fileToHttpResponse(String fullPathFile, HttpResponse response, String contentType, boolean useFileChannels) {
		int status;
		final File file = new File(fullPathFile);
		if (!file.exists())
			status = HttpStatus.SC_NOT_FOUND;
		else if (!file.canRead() || file.isDirectory())
			status = HttpStatus.SC_FORBIDDEN;
		else {
			status = HttpStatus.SC_OK;
			NFileEntity entity = new NFileEntity(file, contentType, useFileChannels);
			response.setEntity(entity);
		}

		response.setStatusCode(status);
	}

	/**
	 * Convert HTTP request string into key-value pairs
	 * 
	 * @param httpString
	 *            the query part of the http request url
	 * @param performUrlDecode
	 *            whether to URL-decode the keys and values
	 * @return null if httpString == null or httpString.length == 0
	 */
	public static Map<String, String> toKeyValuePairs(String httpString, boolean performUrlDecode) {
		if (httpString == null || httpString.length() == 0) {
			return null;
		}

		Map<String, String> keyValuePairs = new HashMap<String, String>();

		StringTokenizer st = new StringTokenizer(httpString);
		String newToken = null;
		String param, val;
		int equalSignInd;
		while (st.hasMoreTokens() && (newToken = st.nextToken("&")) != null) {
			equalSignInd = newToken.indexOf("=");

			// Default values unless we have a param=value pair
			param = newToken;
			val = "";
			//

			// We have either a "param=value" pair, or "param=" only
			if (equalSignInd > -1) {
				param = newToken.substring(0, equalSignInd);
				val = newToken.substring(equalSignInd + 1);
			}

			if (performUrlDecode) {
				param = StringUtils.urlDecode(param);
				val = StringUtils.urlDecode(val);
			}
			keyValuePairs.put(param, val);
		}

		return keyValuePairs;
	}

	//

	public static String getMimeType(AudioFileFormat.Type audioType) {
		if (audioType == AudioFileFormat.Type.WAVE) {
			return "audio/x-wav";
		} else if (audioType == AudioFileFormat.Type.AU) {
			return "audio/basic";
		} else if (audioType == AudioFileFormat.Type.AIFF || audioType == AudioFileFormat.Type.AIFC) {
			return "audio/x-aiff";
		} else if (audioType.equals(MaryAudioUtils.getAudioFileFormatType("MP3"))) {
			return "audio/x-mpeg"; // "audio/x-mp3; //Does not work for Internet Explorer"
		}
		return "audio/basic"; // this is probably wrong but better than text/plain...
	}

	public static void errorFileNotFound(HttpResponse response, String uri) {
		int status = HttpStatus.SC_NOT_FOUND;
		response.setStatusCode(status);
		String message = "File " + uri + " not found";
		logger.debug("Returning HTTP status " + status + ": " + message);
		try {
			NStringEntity entity = new NStringEntity("<html><body><h1>File not found</h1><p>" + message + "</p></body></html>",
					"UTF-8");
			entity.setContentType("text/html; charset=UTF-8");
			response.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
		}
	}

	public static void errorInternalServerError(HttpResponse response, String message, Throwable exception) {
		int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
		response.setStatusCode(status);
		StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw, true));
		String logMessage = (message != null ? message + "\n" : "") + sw.toString();
		logger.debug("Returning HTTP status " + status + ": " + logMessage);
		try {
			NStringEntity entity = new NStringEntity("<html><body><h1>Internal server error</h1><p>"
					+ (message != null ? message : "") + "<pre>" + sw.toString() + "</pre></body></html>", "UTF-8");
			entity.setContentType("text/html; charset=UTF-8");
			response.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
		}
	}

	public static void errorMissingQueryParameter(HttpResponse response, String param) {
		int status = HttpStatus.SC_BAD_REQUEST;
		response.setStatusCode(status);
		String message = "Request must contain the parameter " + param;
		logger.debug("Returning HTTP status " + status + ": " + message);
		try {
			NStringEntity entity = new NStringEntity("<html><body><h1>Bad request</h1><p>" + message + ".</h1></body></html>",
					"UTF-8");
			entity.setContentType("text/html; charset=UTF-8");
			response.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
		}
	}

	public static void errorWrongQueryParameterValue(HttpResponse response, String paramName, String illegalValue,
			String explanation) {
		int status = HttpStatus.SC_BAD_REQUEST;
		response.setStatusCode(status);
		String message = "The value '" + illegalValue + "' of parameter '" + paramName + "' is not valid"
				+ (explanation != null ? ": " + explanation : "");
		logger.debug("Returning HTTP status " + status + ": " + message);
		try {
			NStringEntity entity = new NStringEntity("<html><body><h1>Bad request</h1><p>" + message + ".</h1></body></html>",
					"UTF-8");
			entity.setContentType("text/html; charset=UTF-8");
			response.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
		}
	}

}
