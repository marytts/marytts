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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.server.RequestHandler.StreamingOutputPiper;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.http.Address;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

/**
 * Provides functionality to process synthesis http requests
 *
 * @author Oytun T&uuml;rk
 *
 */
public class SynthesisRequestHandler extends BaseHttpRequestHandler {
	private static int id = 0;

	private static synchronized int getId() {
		return id++;
	}

	private StreamingOutputPiper streamToPipe;
	private PipedOutputStream pipedOutput;
	private PipedInputStream pipedInput;

	public SynthesisRequestHandler() {
		super();

		streamToPipe = null;
		pipedOutput = null;
		pipedInput = null;
	}

	@Override
	protected void handleClientRequest(String absPath, Map<String, String> queryItems,
                                       HttpResponse response, Address serverAddressAtClient)
        throws IOException
    {
		logger.debug("New synthesis request: " + absPath);
		if (queryItems != null) {
			for (String key : queryItems.keySet()) {
				logger.debug("    " + key + "=" + queryItems.get(key));
			}
		}
		process(serverAddressAtClient, queryItems, response);

	}

	public void process(Address serverAddressAtClient,
                        Map<String, String> queryItems,
                        HttpResponse response)
    {
		if (queryItems == null ||
            !(queryItems.containsKey("CONFIGURATION") && queryItems.containsKey("LOCALE") && queryItems.containsKey("INPUT_TEXT")))
        {
			MaryHttpServerUtils.errorMissingQueryParameter(response,
                                                           "'INPUT_TEXT' and 'INPUT_TYPE' and 'OUTPUT_TYPE' and 'LOCALE'");
			return;
		}

		String inputText = queryItems.get("INPUT_TEXT");
		boolean isOutputText = true;
        String configuration = queryItems.get("CONFIGURATION");
        String input_data = queryItems.get("INPUT_TEXT");

        boolean ok = true;
		final Request maryRequest = new Request(configuration, input_data);

        try {
            maryRequest.process();
        } catch (Exception e) {
            String message = "error in process";
            logger.warn(message, e);
            MaryHttpServerUtils.errorInternalServerError(response, message, e);
            ok = false;
        }

        if (ok)
        {
            // Write output data to client
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                maryRequest.writeOutputData(outputStream);
                String contentType = "text/plain; charset=UTF-8"; // FIXME: need to be think for the audio
                MaryHttpServerUtils.toHttpResponse(outputStream.toByteArray(), response, contentType);
            } catch (Exception e) {
                String message = "Cannot write output";
                logger.warn(message, e);
                MaryHttpServerUtils.errorInternalServerError(response, message, e);
                ok = false;
            }
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
	}
}
