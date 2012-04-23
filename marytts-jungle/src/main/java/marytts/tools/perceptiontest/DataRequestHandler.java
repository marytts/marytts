/**
 * Copyright 2009 DFKI GmbH.
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import marytts.server.http.BaseHttpRequestHandler;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.http.Address;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NStringEntity;
import org.xml.sax.SAXException;

/**
 * 
 * The data request handler for perception test
 * @author Sathish Pammi
 */
public class DataRequestHandler extends BaseHttpRequestHandler
{
 
    private TestDataLoader testData; 
    
    public DataRequestHandler()
    {
        super();
    }
    
    public DataRequestHandler(String testXmlName) {
        super();
        try {
            System.out.println("Loading file : "+testXmlName);
            testData = new TestDataLoader(testXmlName);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleClientRequest(String absPath, Map<String,String> queryItems, HttpResponse response, Address serverAddressAtClient)
    throws IOException 
    {
        // Individual info request
        String infoResponse = handleInfoRequest(absPath, queryItems, response);
        if (infoResponse == null) { // error condition, handleInfoRequest has set an error message
            return;
        }

        response.setStatusCode(HttpStatus.SC_OK);
        try {
            NStringEntity entity = new NStringEntity(infoResponse, "UTF-8");
            entity.setContentType("text/plain; charset=UTF-8");
            response.setEntity(entity);
        } catch (UnsupportedEncodingException e){}
    }


    private String handleInfoRequest(String absPath, Map<String, String> queryItems, HttpResponse response)
    {
        logger.debug("New info request: "+absPath);
        if (queryItems != null) {
            for (String key : queryItems.keySet()) {
                logger.debug("    "+key+"="+queryItems.get(key));
            }
        }

        assert absPath.startsWith("/") : "Absolute path '"+absPath+"' does not start with a slash!";
        String request = absPath.substring(1); // without the initial slash
        
        if (request.equals("queryStatement")) return getQueryStatement();
        else if (request.equals("options")) return getOptions();
        else if (request.equals("sample")) return getSampleData();
        else if (request.equals("title")) return getTitle();
        else if (request.equals("audioformats")) return MaryRuntimeUtils.getAudioFileFormatTypes();
        else if (request.equals("exampletext")) {
            if (queryItems != null) {
                // Voice example text
                String voice = queryItems.get("voice");
                if (voice != null) {
                    return MaryRuntimeUtils.getVoiceExampleText(voice);
                }
                String datatype = queryItems.get("datatype");
                String locale = queryItems.get("locale");
                if (datatype != null && locale != null) {
                    Locale loc = MaryUtils.string2locale(locale);
                    return MaryRuntimeUtils.getExampleText(datatype, loc);
                }
            }
            //MaryHttpServerUtils.errorMissingQueryParameter(response, "'datatype' and 'locale' or 'voice'");
            return null;
        }
      
        //MaryHttpServerUtils.errorFileNotFound(response, request);
        return null;
    }

    public String getTitle() {
        return testData.getTestTile();
    }

    public String getSampleData() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getOptions() {
        
        String optionString = "";
        String questionType = testData.getTestQuestionType();
        String[] options = testData.getAnswerOptions();
        optionString += questionType+" ";
        for(String str : options){
            optionString += str+"|";
        }
        return optionString.substring(0, optionString.length()-1);
    }

    public String getQueryStatement() {
        return testData.getTestQuestion();
    }
    
    public Map<String, String> getTestSamples(){
        return this.testData.getTestSamples();
    }
    
    public int getNumberOfSamples(){
        return this.testData.getTestSamples().size();
    }
    
    public String getSampleBaseName(int num){
        String[] keySet = testData.getTestSamples().keySet().toArray(new String[0]);
        return keySet[num];
    }
    
    public String getSampleWaveFile(int num){
        Map<String, String> testSamples = testData.getTestSamples();
        String[] keySet = testSamples.keySet().toArray(new String[0]);
        return testSamples.get(keySet[num]);
    }
   
}

