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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

import marytts.server.Request;
import marytts.util.string.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.protocol.BufferingHttpClientHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

/**
 * @author oytun.turk
 *
 */
public class MaryHttpServerUtils {

    public static EntityTemplate string2responseBody(String responseLine)
    {
        EntityTemplate body = new EntityTemplate(new MaryContentProducer(responseLine));
        body.setContentType("text/html; charset=UTF-8");
        
        return body;
    }
    
    public static EntityTemplate stringArray2responseBody(String[] responseLines)
    {
        EntityTemplate body = new EntityTemplate(new MaryContentProducer(responseLines));
        body.setContentType("text/html; charset=UTF-8");
        
        return body;
    }
    
    public static EntityTemplate vector2responseBody(Vector<String> responseLines)
    {
        EntityTemplate body = new EntityTemplate(new MaryContentProducer(responseLines));
        body.setContentType("text/html; charset=UTF-8");
        
        return body;
    }
    
    public static EntityTemplate textFile2responseBody(String filename)
    {
        String responsePage = StringUtils.readTextFileIntoString(filename);
        
        EntityTemplate body = new EntityTemplate(new MaryContentProducer(responsePage));
        body.setContentType("text/html; charset=UTF-8");
        
        return body;
    }
    
    //TO DO: How to send audio data to client and how to process it in the client?
    public static void respondWithAudio(AudioInputStream audio, Type type, HttpResponse response) throws IOException
    {
        //OutputStream output = null;
        //AudioSystem.write(audio, type, output);
        //output.flush();
    }
    
    //TO DO: How to send error message to client and how to process it in the client?
    public static void respondWithErrorMessage(String host, int port, String errorMessage)
    {
        
    }
    
    //TO DO: How to send text data to client and how to process it in the client?
    public static void respondWithText(String text, HttpResponse response)
    {
        
    }
    
    //TO DO: How to get text data from client
    public static void getTextFromClient(String clientAddress, String endMarker)
    {
        
    }
    
    //TO DO: How to send error message to client
    public static void respondWithErrorMessage(String clientAddress, String errorMessage)
    {
        
    }
    
    //TO DO: How to send error message to client
    public static void respondWithWarningMessage(String clientAddress, String warningMessage)
    {
        
    }
    
    public static void string2response(String output, HttpResponse response)
    {
        NByteArrayEntity body = new NByteArrayEntity(output.getBytes());

        body.setContentType("text/html; charset=UTF-8");
        response.setEntity(body);
    }
}
