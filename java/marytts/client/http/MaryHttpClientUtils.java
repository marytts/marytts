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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import marytts.util.ConversionUtils;
import marytts.util.string.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

/**
 * Utilits functions for Http clients
 * 
 * @author Oytun T&uumlrk
 */
public class MaryHttpClientUtils 
{    
    public static final String EQUAL_SIGN = "EQUAL_SIGN_IN_HTTP";
    public static final String AND_SIGN = "AND_SIGN_IN_HTTP";
    
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
    
    /*
    public static String toString(HttpResponse response) throws IOException
    {
        return singleFormElementStringToString(ConversionUtils.toString(toByteArray(response)));
    }
    
    public static String[] toStringArray(HttpResponse response) throws IOException
    {
        return StringUtils.toStringArray(toString(response));
    }

    //Converts key=value to value
    public static String singleFormElementStringToString(String singleFormElementString)
    {
        int equalIndex = singleFormElementString.indexOf("=");
        if (equalIndex>=0)
        {
            if (equalIndex+1<singleFormElementString.length())
                return singleFormElementString.substring(equalIndex+1);
            else
                return "";
        }
        else
            return singleFormElementString;
    }
    */

    //TO DO: These two functions may need updating since the server returns key=value pairs now
    public static double[] toDoubleArray(HttpResponse response) throws IOException
    {
        return ConversionUtils.toDoubleArray(toByteArray(response));
    }
    
    public static int[] toIntArray(HttpResponse response) throws IOException
    {
        return ConversionUtils.toIntArray(toByteArray(response));
    }
    //
    
    //Convert keyValuePairs to HTTP request string
    public static String toHttpString(Map<String, String> keyValuePairs)
    {
        Set<String> keys = keyValuePairs.keySet();
        String currentKey, currentValue;
        String httpString = "";
        for (Iterator<String> it = keys.iterator(); it.hasNext();)
        {
            currentKey = it.next();
            currentValue = keyValuePairs.get(currentKey);
            httpString += currentKey + "=" + currentValue;
            if (it.hasNext())
                httpString += "&";
        }
        
        return StringUtils.urlEncode(httpString);
    }
    //
    
    public static Map<String, String> toKeyValuePairs(HttpResponse response, boolean performUrlDecode) throws IOException
    {
        return toKeyValuePairs(ConversionUtils.toString(toByteArray(response)), performUrlDecode);
    }
    
    //Convert HTTP request string into key-value pairs
    public static Map<String, String> toKeyValuePairs(String httpString, boolean performUrlDecode)
    {
        Map<String, String> keyValuePairs = null;
        if (httpString!=null)
        {
            if (httpString.length()>0)
            {
                if (performUrlDecode)
                    httpString = StringUtils.urlDecode(httpString);
                
                StringTokenizer st = new StringTokenizer(httpString);
                String newToken = null;
                String param, val;
                int equalSignInd;
                while (st.hasMoreTokens() && (newToken = st.nextToken("&"))!=null)
                {   
                    equalSignInd = newToken.indexOf("=");

                    //Default values unless we have a param=value pair
                    param = newToken;
                    val = "";
                    //

                    //We have either a "param=value" pair, or "param=" only
                    if (equalSignInd>-1)
                    {
                        param = newToken.substring(0, equalSignInd);

                        if (equalSignInd+1<newToken.length()) //"param=val" pair
                            val = newToken.substring(equalSignInd+1);
                        else //"param=" only
                            val = "";
                    }

                    if (keyValuePairs==null)
                        keyValuePairs = new HashMap<String, String>();
                    
                    keyValuePairs.put(param, val);
                }
            }
        }
        
        return keyValuePairs;
    }
    //
}