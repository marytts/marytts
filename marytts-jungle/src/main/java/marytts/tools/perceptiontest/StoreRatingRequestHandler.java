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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

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
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.xml.sax.SAXException;

/**
 * A handler class to store user ratings
 * @author Sathish Pammi
 */
public class StoreRatingRequestHandler extends BaseHttpRequestHandler
{
    private DataRequestHandler infoRH;
    private UserRatingStorer userRatingRH;
    public StoreRatingRequestHandler()
    {
        super();
        
        //Add extra initialisations here
    }
    
    public StoreRatingRequestHandler(DataRequestHandler infoRH, UserRatingStorer userRatingRH) {
        super();
        this.infoRH = infoRH;
        this.userRatingRH = userRatingRH;
    }

    @Override
    protected void handleClientRequest(String absPath, Map<String,String> queryItems, HttpResponse response, Address serverAddressAtClient)
    throws IOException
    {
        
        Set<String> keySet = queryItems.keySet();
        
        if(absPath.equals("/userRating") && keySet.size() > 0){
            
            String infoResponse = null;
            
            if ( keySet.size() > 1 
                    && keySet.contains("EMAIL")
                    && keySet.contains("RESULTS")
                    && keySet.contains("PRESENT_SAMPLE_BASENAME")
                    && keySet.contains("PRESENT_SAMPLE_NUMBER") )
            {
                //infoResponse = storeUserRatings(queryItems);
                storeUserRatings(queryItems);
            }
        }
        
    }
    
    private void storeUserRatings(Map<String, String> queryItems) {
        //String infoResponce = getCaseTwoInfoResponse(queryItems);
        //System.out.println(queryItems.get("RESULTS"));
        String eMailID = queryItems.get("EMAIL");
        int presentSampleNumber = (new Integer(queryItems.get("PRESENT_SAMPLE_NUMBER"))).intValue();
        String baseName = queryItems.get("PRESENT_SAMPLE_BASENAME");
        String result = queryItems.get("RESULTS");
        
        try {
            userRatingRH.writeSampleResult(eMailID, presentSampleNumber, baseName, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   
}

