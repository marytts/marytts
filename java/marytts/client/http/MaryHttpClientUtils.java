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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import marytts.util.string.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author oytun.turk
 *
 */
public class MaryHttpClientUtils {
    //Check if these are still required
    public static HttpResponse sendHttpPostRequest(DefaultHttpClient client, String host, int port, String query) throws HttpException
    {
        return sendHttpRequest(client, host, port, query, "POST");
    }
    
    public static String httpPostRequestStringResponse(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response =  sendHttpRequest(client, host, port, query, "POST");
        
        return response2String(response);
    }
    
    public static String[] httpPostRequestStringArrayResponse(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response =  sendHttpRequest(client, host, port, query, "POST");
        
        return response2StringArray(response);
    }
    
    public static InputStream httpPostRequestInputStreamResponse(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response =  sendHttpRequest(client, host, port, query, "POST");
        
        return response2InputStream(response);
    }
    
    public static HttpResponse sendHttpGetRequest(DefaultHttpClient client, String host, int port, String query) throws HttpException
    {
        return sendHttpRequest(client, host, port, query, "GET");
    }
    
    public static String httpGetRequestStringResponse(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response =  sendHttpRequest(client, host, port, query, "GET");
        
        return response2String(response);
    }
    
    public static String[] httpGetRequestStringArrayResponse(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response =  sendHttpRequest(client, host, port, query, "GET");
        
        return response2StringArray(response);
    }
    
    public static InputStream httpGetRequestInputStreamResponse(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response =  sendHttpRequest(client, host, port, query, "GET");
        
        return response2InputStream(response);
    }
    
    public static HttpResponse sendHttpRequest(DefaultHttpClient client, String host, int port, String query, String requestMethod) throws HttpException
    {
        HttpResponse response = null;
        
        String completeQuery = host + ":" + String.valueOf(port) + "?" + query;
        requestMethod = requestMethod.toUpperCase();
        
        HttpUriRequest sender = null;
        
        if (requestMethod.equals("GET"))
        {
            try {
                sender = new HttpGet(completeQuery);
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (requestMethod.equals("POST"))
        {
            try {
                sender = new HttpPost(completeQuery);
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
            throw new MethodNotSupportedException(requestMethod + " method not supported");
        
        if (sender!=null)
        {
            try {
                response = client.execute(sender);
            } catch (HttpException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
          
        return response;
    }
    
    public static String response2String(HttpResponse response) throws IOException
    {
        String responseString = "";
        
        if (response!=null)
        {
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent(); //This is how to get a direct input stream from server. Mary client can use this as audio data source from server etc

            if (is!=null)
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = "";
                System.out.println("--- Incoming bytes from server to client ---");
                while((line = br.readLine()) != null)
                {
                    responseString += line + System.getProperty("line.separator");
                    System.out.println(line);
                }

                System.out.println("--- That was all from server ---");
            }
            
            try {
                entity.consumeContent();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return responseString;
    }
    
    public static String[] response2StringArray(HttpResponse response) throws IOException
    {
        String allInOneLine = response2String(response);
        
        return StringUtils.string2StringArray(allInOneLine);
    }
    
    public static InputStream response2InputStream(HttpResponse response) throws IOException
    {
        InputStream is = null;
        
        if (response!=null)
        {

            HttpEntity entity = response.getEntity();
            is = entity.getContent(); //This is how to get a direct input stream from server. Mary client can use this as audio data source from server etc
        }
        
        return is;
    }
    
    public static String getHttpServerInfoLine(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response = sendHttpPostRequest(client, host, port, query);
        
        return response2String(response);
    }
    
    public static String[] getHttpServerInfoLines(DefaultHttpClient client, String host, int port, String query) throws HttpException, IOException
    {
        HttpResponse response = sendHttpPostRequest(client, host, port, query);
        
        return response2StringArray(response);
    }
    //
}
