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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;

import marytts.Version;
import marytts.client.http.MaryHtmlForm;
import marytts.datatypes.MaryDataType;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.signalproc.effects.BaseAudioEffect;
import marytts.signalproc.effects.EffectsApplier;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;

/**
 * @author oytun.turk
 *
 */
public class InfoRequestProcessor extends BaselineRequestProcessor {

    public InfoRequestProcessor()
    {
        super();
    }
    
    public void sendDefaultHtmlPage(Address serverAddressAtClient, HttpResponse response) throws IOException, InterruptedException
    {
        MaryWebHttpClientHandler webHttpClient = new MaryWebHttpClientHandler();

        MaryHtmlForm htmlForm = new MaryHtmlForm(serverAddressAtClient,
                                                 getMaryVersion(),
                                                 getVoices(),
                                                 getDataTypes(),
                                                 getAudioFileFormatTypes(),
                                                 getAudioEffectHelpTextLineBreak(),
                                                 getDefaultAudioEffects(),
                                                 getDefaultVoiceExampleTexts());

        webHttpClient.toHttpResponse(htmlForm, response);
    }
    
    public boolean process(Address serverAddressAtClient, Map<String, String> keyValuePairs, HttpResponse response) throws IOException, InterruptedException
    {
        boolean bProcessed = handleInfoRequests(keyValuePairs);

        if (keyValuePairs!=null)
        {
            boolean isWebBrowserClient = false;
            String tmpVal = keyValuePairs.get("WEB_BROWSER_CLIENT");
            if (tmpVal!=null && tmpVal.compareTo("true")==0)
                isWebBrowserClient = true;

            if (isWebBrowserClient) //Generate info response for web browser client
            {
                MaryHtmlForm htmlForm = new MaryHtmlForm(serverAddressAtClient,
                                                         keyValuePairs,
                                                         getMaryVersion(),
                                                         getVoices(),
                                                         getDataTypes(),
                                                         getAudioFileFormatTypes(),
                                                         getAudioEffectHelpTextLineBreak(),
                                                         getDefaultAudioEffects(),
                                                         getDefaultVoiceExampleTexts());

                MaryWebHttpClientHandler webHttpClient = new MaryWebHttpClientHandler();
                webHttpClient.toHttpResponse(htmlForm, response);
            }
            else //Generate info response for GUI client
                MaryHttpServerUtils.toHttpResponse(keyValuePairs, response, "text/html; charset=UTF-8"); 
        }
        
        return bProcessed;
    }
    
  //Tries to fill in requested information from the client
    //A major difference from the non-HTTP server is that multiple requests can be handled within single client request
    private boolean handleInfoRequests(Map<String, String> keyValuePairs) throws IOException 
    {
        boolean bRet = false;
        boolean bAtLeastOneRequestSucceeded = false;
        Set<String> keys = keyValuePairs.keySet();
        String currentKey, currentValue, params;
        int paramInd;
        String queryString = "?";
        for (Iterator<String> it = keys.iterator(); it.hasNext();)
        {
            currentKey = it.next();
            currentValue = keyValuePairs.get(currentKey);
            
            if (currentValue.startsWith(queryString))
            {
                paramInd = currentValue.indexOf(queryString)+queryString.length()+1;
                if (paramInd>=0 && paramInd<currentValue.length())
                    params = currentValue.substring(paramInd);
                else
                    params = "";
                bRet = handleInfoRequest(currentKey, params, keyValuePairs);
                
                if (bRet==false)
                    logger.debug("Request failed: " + currentKey + ( ( (it.hasNext()) ? (" ...proceeding with next one") : ("") )));
                else
                    bAtLeastOneRequestSucceeded = true;
            }   
        }
        
        return bAtLeastOneRequestSucceeded;
    }
    
    private boolean handleInfoRequest(String request, String param, Map<String, String> keyValuePairs) throws IOException 
    {
        logger.debug("InfoRequest: " + request);
        
        if (request.compareTo("VERSION")==0) 
        {
            //Send version information to client.
            keyValuePairs.put(request, getMaryVersion());
            
            return true;
        } 
        else if (request.compareTo("DATA_TYPES")==0) 
        {
            //List all known datatypes
            keyValuePairs.put(request, getDataTypes());
            
            return true;
        } 
        else if (request.compareTo("VOICES")==0) 
        {
            //List all known voices
            keyValuePairs.put(request, getVoices());
            
            return true;
        } 
        else if (request.compareTo("AUDIO_FILE_FORMAT_TYPES")==0) 
        {
            //Send audio file format types to client
            keyValuePairs.put(request, getAudioFileFormatTypes());
            
            return true;
        } 
        else if (request.compareTo("EXAMPLE_TEXT")==0)
        {
            //Send an example text for a given data type
            keyValuePairs.put(request, getExampleText(param, keyValuePairs));
            
            return true;
        }
        else if (request.compareTo("VOICE_EXAMPLE_TEXT")==0) 
        { 
            //The request is about the example text of a limited domain unit selection voice
            keyValuePairs.put(request, getVoiceExampleText(param, keyValuePairs));
            
            return true; 
        }
        else if (request.compareTo("DEFAULT_AUDIO_EFFECTS")==0)
        { 
            //The request is about the available audio effects
            keyValuePairs.put(request, getDefaultAudioEffects());
            
            return true;
        }
        else if (request.compareTo("AUDIO_EFFECT_HELP_TEXT_LINE_BREAK")==0)
        {
            keyValuePairs.put(request, getAudioEffectHelpTextLineBreak());
            
            return true;
        }
        else if (request.compareTo("AUDIO_EFFECT_DEFAULT_PARAM")==0)
        {
            String output = getAudioEffectDefaultParam(param);
            
            if (output!="")
            {
                keyValuePairs.put(request, output);
            
                return true;
            }
            else
                return false;
        }
        else if (request.compareTo("FULL_AUDIO_EFFECT")==0)
        {    
            StringTokenizer tt = new StringTokenizer(param);
            String effectName, currentEffectParams;
            if (tt.hasMoreTokens())
                effectName = tt.nextToken();
            else
            {
                logger.error("Effect name needed!");
                return false;
            }
            
            if (tt.hasMoreTokens())
                currentEffectParams = tt.nextToken();
            else
            {
                logger.error("Current effect parameters needed!");
                return false;
            }
            
            String output = getFullAudioEffect(effectName, currentEffectParams);
            
            if (output!="")
            {
                keyValuePairs.put(request, output);
                return true;
            }
            else
            {
                logger.error("Unable to get full effect string!");
                return false;
            }
        }
        else if (request.compareTo("AUDIO_EFFECT_HELP_TEXT")==0)
        {
            String output = getAudioEffectHelpText(param);

            if (output!="")
            {
                keyValuePairs.put(request, output);

                return true;
            }
            else
                return false;
        }
        else if (request.compareTo("IS_HMM_AUDIO_EFFECT")==0)
        {
            String output = isHmmAudioEffect(param);

            if (output!="")
            {
                keyValuePairs.put(request, output);

                return true;
            }
            else
                return false;
        }
        else
            return false;
    }
}
