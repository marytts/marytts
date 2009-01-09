/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.server.http;

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

import marytts.client.http.Address;
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
 * Processor class for file http requests to Mary server
 * 
 * @author Oytun T&uumlrk
 */
public class FileRequestHandler extends BaseHttpRequestHandler
{
    
    private Set<String> validFiles = new HashSet<String>(Arrays.asList(new String[] {
        "favicon.ico",
        "index.html",
        "mary.js",
        "sparcle.wav"
        }));
    
    public FileRequestHandler()
    {
        super();
        
        //Add extra initialisations here
    }

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
        if (uri.equals("")) uri = "index.html";
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
        // not used because we override handle() directly.
    }

    
    private void sendResourceAsStream(String resourceFilename, HttpResponse response) throws IOException
    {
        InputStream stream = MaryHttpServer.class.getResourceAsStream(resourceFilename);

        String contentType;
        if (resourceFilename.endsWith(".html"))
            contentType = "text/html; charset=UTF-8";
        else if (resourceFilename.endsWith(".wav"))
            contentType = "audio/wav";
        else
            contentType = "text/plain";
        if (stream!=null) {
            MaryHttpServerUtils.toHttpResponse(stream, response, contentType);
        } else {
            MaryHttpServerUtils.errorFileNotFound(response, resourceFilename);
        }
    }
    
    
}
