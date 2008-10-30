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

package marytts.client.http;

import java.io.IOException;

/**
 * @author oytun.turk
 *
 * This class implements a web browser client
 * It is different as compared to other Mary clients in the sense that 
 * it is dynamically created by MaryHttpServer to send an appropriate html form 
 * to a web browser that connect to a Mary server.
 * This form is updated dynamically by user requests and server responses.
 * 
 */
public class MaryWebHttpClient extends MaryHttpForm {
    public MaryWebHttpClient(String serverHost, int serverPort) throws IOException, InterruptedException
    {
        super(serverHost, serverPort, "");
        
        httpRequester.fillWebBrowserHttpRequestHeader(serverHost, serverPort);
        
        initialise();
    }
    
    public void initialise() throws IOException, InterruptedException
    {
        fillServerVersion();
        fillDataTypes();
        fillVoices();
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        String serverHost = "localhost";
        int serverPort = 59125;
        
        MaryWebHttpClient mw = new MaryWebHttpClient(serverHost, serverPort);
        
        
    }
}
