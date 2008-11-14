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
import java.io.IOException;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;

import marytts.client.http.MaryHtmlForm;
import marytts.client.http.MaryHttpClientUtils;
import marytts.util.ConversionUtils;
import marytts.util.data.audio.AudioDoubleDataSource;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.entity.NFileEntity;

/**
 * @author oytun.turk
 *
 */
public class MaryHttpServerUtils 
{
    public static void toHttpResponse(Map<String, String> keyValuePairs, HttpResponse response) throws IOException
    {
        toHttpResponse(MaryHttpClientUtils.toHttpString(keyValuePairs), response);
    }
    
    public static void toHttpResponse(AudioInputStream audio, HttpResponse response) throws IOException
    {
        AudioDoubleDataSource signal = new AudioDoubleDataSource(audio);

        toHttpResponse(signal.getAllData(), response);
    }
    
    public static void toHttpResponse(double[] x, HttpResponse response) throws IOException
    {   
        toHttpResponse(ConversionUtils.toByteArray(x), response);
    }
    
    public static void toHttpResponse(int[] x, HttpResponse response) throws IOException
    {   
        toHttpResponse(ConversionUtils.toByteArray(x), response);
    }
    
    public static void toHttpResponse(String x, HttpResponse response) throws IOException
    {   
        toHttpResponse(ConversionUtils.toByteArray(x), response);
    }
    
    public static void toHttpResponse(byte[] byteArray, HttpResponse response)
    {
        NByteArrayEntity body = new NByteArrayEntity(byteArray);
        body.setContentType("text/html; charset=UTF-8");
        response.setEntity(body);
    }
    
    public static int fileToHttpResponse(String fullPathFile, HttpResponse response, boolean useFileChannels)
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
            NFileEntity entity = new NFileEntity(file, "text/html", useFileChannels);
            response.setEntity(entity);
        }

        return status;
    }
}
