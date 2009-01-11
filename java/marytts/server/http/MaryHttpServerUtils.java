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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFileFormat;

import marytts.client.http.MaryFormData;
import marytts.util.ConversionUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.string.StringUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.nio.entity.BufferingNHttpEntity;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.jsresources.AppendableSequenceAudioInputStream;

/**
 * Utility functions for Mary http server
 * 
 * @author Oytun T&uumlrk
 */
public class MaryHttpServerUtils 
{
    
    public static void toHttpResponse(double[] x, HttpResponse response, String contentType) throws IOException
    {   
        toHttpResponse(ConversionUtils.toByteArray(x), response, contentType);
    }
    
    public static void toHttpResponse(int[] x, HttpResponse response, String contentType) throws IOException
    {   
        toHttpResponse(ConversionUtils.toByteArray(x), response, contentType);
    }

    public static void toHttpResponse(String x, HttpResponse response, String contentType) throws IOException
    {   
        toHttpResponse(ConversionUtils.toByteArray(x), response, contentType);
    }
    
    public static void toHttpResponse(ByteArrayOutputStream baos, HttpResponse response, String contentType) throws IOException
    {
        toHttpResponse(baos.toByteArray(), response, contentType);
    }
    
    public static void toHttpResponse(byte[] byteArray, HttpResponse response, String contentType) throws IOException
    {
        NByteArrayEntity body = new NByteArrayEntity(byteArray);
        body.setContentType(contentType);
        response.setEntity(body);
        response.setStatusCode(HttpStatus.SC_OK);
    }
    
    public static void toHttpResponse(InputStream stream, HttpResponse response, String contentType) throws IOException
    {
        toHttpResponse(stream, response, contentType, -1);
    }
    
    public static void toHttpResponse(InputStream stream, HttpResponse response, String contentType, long streamLength) throws IOException
    {
        InputStreamEntity body = new InputStreamEntity(stream, streamLength); 
        body.setContentType(contentType);
        response.setEntity(body);
        response.setStatusCode(HttpStatus.SC_OK);
    }
    
    public static void fileToHttpResponse(String fullPathFile, HttpResponse response, String contentType, boolean useFileChannels)
    {
        int status;
        final File file = new File(fullPathFile);
        if (!file.exists())
            status = HttpStatus.SC_NOT_FOUND;
        else if (!file.canRead() || file.isDirectory())
            status = HttpStatus.SC_FORBIDDEN;
        else 
        {
            status = HttpStatus.SC_OK;
            NFileEntity entity = new NFileEntity(file, contentType, useFileChannels);
            response.setEntity(entity);
        }

        response.setStatusCode(status);
    }

    
    
    /** 
     * Convert HTTP request string into key-value pairs
     * @param httpString the query part of the http request url
     * @param performUrlDecode whether to URL-decode the keys and values
     * @return
     */
    public static Map<String, String> toKeyValuePairs(String httpString, boolean performUrlDecode)
    {
        if (httpString==null || httpString.length()==0) {
            return null;
        }

        Map<String, String> keyValuePairs = new HashMap<String, String>();

        StringTokenizer st = new StringTokenizer(httpString);
        String newToken = null;
        String param, val;
        int equalSignInd;
        while (st.hasMoreTokens() && (newToken = st.nextToken("&"))!=null) {
            equalSignInd = newToken.indexOf("=");

            //Default values unless we have a param=value pair
            param = newToken;
            val = "";
            //

            //We have either a "param=value" pair, or "param=" only
            if (equalSignInd>-1) {
                param = newToken.substring(0, equalSignInd);
                val = newToken.substring(equalSignInd+1);
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
    
    
    
    
    
    public static String getMimeType(AudioFileFormat.Type audioType) throws Exception
    {
        if (audioType == AudioFileFormat.Type.WAVE) {
            return "audio/x-wav";
        } else if (audioType == AudioFileFormat.Type.AU) {
            return "audio/basic";
        } else if (audioType == AudioFileFormat.Type.AIFF
                || audioType == AudioFileFormat.Type.AIFC) {
            return "audio/x-aiff";
        } else if (audioType.equals(MaryAudioUtils.getAudioFileFormatType("MP3"))) {
            return "audio/x-mpeg";  //"audio/x-mp3; //Does not work for Internet Explorer"
        }
        return "audio/basic"; // this is probably wrong but better than text/plain...
    }

    public static void errorFileNotFound(HttpResponse response, String uri)
    {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        try {
            NStringEntity entity = new NStringEntity(
                    "<html><body><h1>File " + uri +
                    " not found</h1></body></html>", "UTF-8");
            entity.setContentType("text/html; charset=UTF-8");
            response.setEntity(entity);
        } catch (UnsupportedEncodingException e){}
    }
    
    public static void errorInternalServerError(HttpResponse response, String message, Throwable exception)
    {
        response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        try {
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw, true));
            NStringEntity entity = new NStringEntity(
                    "<html><body><h1>Internal server error</h1><p>"
                    +(message != null?message:"")
                    +"<pre>"
                    + sw.toString()
                    + "</pre></body></html>", "UTF-8");
            entity.setContentType("text/html; charset=UTF-8");
            response.setEntity(entity);
        } catch (UnsupportedEncodingException e){}
    }
    
    public static void errorMissingQueryParameter(HttpResponse response, String param)
    {
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        try {
            NStringEntity entity = new NStringEntity(
                    "<html><body><h1>Bad request</h1><p>Request must contain the parameter " + param +
                    ".</h1></body></html>", "UTF-8");
            entity.setContentType("text/html; charset=UTF-8");
            response.setEntity(entity);
        } catch (UnsupportedEncodingException e){}
    }

    public static void errorWrongQueryParameterValue(HttpResponse response, String paramName, String illegalValue, String explanation)
    {
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        try {
            NStringEntity entity = new NStringEntity(
                    "<html><body><h1>Bad request</h1><p>The value '" + illegalValue + "' of parameter '" + paramName +
                    "' is not valid"
                    + (explanation != null ? ": "+explanation : "")
                    +".</h1></body></html>", "UTF-8");
            entity.setContentType("text/html; charset=UTF-8");
            response.setEntity(entity);
        } catch (UnsupportedEncodingException e){}
    }

}
