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
package marytts.tools.perceptiontest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import marytts.server.http.BaseHttpRequestHandler;
import marytts.server.http.FileRequestHandler;
import marytts.server.http.MaryHttpServerUtils;
import marytts.util.http.Address;
import marytts.util.io.FileUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * Processor class for file http requests to perception test server
 * 
 * @author Sathish Pammi and Oytun T&uumlrk
 */
public class FileDataRequestHandler extends BaseHttpRequestHandler
{
    private String defaultHtmlPage = "perception.html";
    
    private Set<String> validFiles = new HashSet<String>(Arrays.asList(new String[] {
        "perception.html",
        "perception.js"
        }));
    
    public FileDataRequestHandler()
    {
        super();
        
        //Add extra initialisations here
    }
    
    /*public FileDataRequestHandler(String defaultHTML) {
        super();
        defaultHtmlPage = defaultHTML;
    }
*/
    /**
     * The entry point of all HttpRequestHandlers.
     * When this method returns, the response is sent to the client.
     * We override this here to show how simple a processing we are doing for file requests.
     */
    @Override
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
    {
        String uri = request.getRequestLine().getUri();
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        //if (uri.equals("")) uri = "index.html";
        if (uri.equals("") || uri.equals("perceptionTest")) uri = defaultHtmlPage;
        logger.debug("File requested: "+uri);
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
    protected void handleClientRequest(String absPath, Map<String,String> queryItems, HttpResponse response, Address serverAddressAtClient)
    throws IOException
    {
        //System.out.println("ABS PATH: "+ absPath);
        // not used because we override handle() directly.
    }

    
    private void sendResourceAsStream(String resourceFilename, HttpResponse response) throws IOException
    {
        InputStream stream = PerceptionTestHttpServer.class.getResourceAsStream(resourceFilename);

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
        if (stream!=null) {
            MaryHttpServerUtils.toHttpResponse(stream, response, contentType);
        } else {
            MaryHttpServerUtils.errorFileNotFound(response, resourceFilename);
        }
    }
    
}

