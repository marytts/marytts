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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import marytts.util.ConversionUtils;
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
public class MaryHttpClientUtils 
{    
    public static InputStream toInputStream(HttpResponse response) throws IOException
    {
        InputStream is = null;
        
        if (response!=null)
        {
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        }
        
        return is;
    }
    
    public static byte[] toByteArray(HttpResponse response) throws IOException
    {
        InputStream is = toInputStream(response);
        
        if (is!=null)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int byteVal;
            while ((byteVal=is.read())!=-1)
                baos.write(byteVal);
            
            return baos.toByteArray();
        }
        else
            return null;    
    }
    
    public static String toString(HttpResponse response) throws IOException
    {
        return ConversionUtils.toString(toByteArray(response));
    }
    
    public static String[] toStringArray(HttpResponse response) throws IOException
    {
        return StringUtils.toStringArray(toString(response));
    }

    public static double[] toDoubleArray(HttpResponse response) throws IOException
    {
        return ConversionUtils.toDoubleArray(toByteArray(response));
    }
    
    public static int[] toIntArray(HttpResponse response) throws IOException
    {
        return ConversionUtils.toIntArray(toByteArray(response));
    }
}
