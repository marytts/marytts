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
import java.io.UnsupportedEncodingException;
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

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import marytts.Version;
import marytts.client.http.Address;
import marytts.client.http.MaryFormData;
import marytts.datatypes.MaryDataType;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
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
 * Processor class for information http requests to Mary server
 * 
 * @author Oytun T&uuml;rk, Marc Schr&ouml;der
 */
public class InfoRequestHandler extends BaseHttpRequestHandler
{

    public InfoRequestHandler()
    {
        super();
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
        
        if (request.equals("version")) return getMaryVersion();
        else if (request.equals("datatypes")) return getDataTypes();
        else if (request.equals("voices")) return getVoices();
        else if (request.equals("audioformats")) return getAudioFileFormatTypes();
        else if (request.equals("exampletext")) {
            if (queryItems != null) {
                // Voice example text
                String voice = queryItems.get("voice");
                if (voice != null) {
                    return getVoiceExampleText(voice);
                }
                String datatype = queryItems.get("datatype");
                String locale = queryItems.get("locale");
                if (datatype != null && locale != null) {
                    Locale loc = MaryUtils.string2locale(locale);
                    return getExampleText(datatype, loc);
                }
            }
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'datatype' and 'locale' or 'voice'");
            return null;
        }
        else if (request.equals("audioeffects")) return getDefaultAudioEffects();
        else if (request.equals("audioeffect-default-param")) {
            if (queryItems != null) {
                String effect = queryItems.get("effect");
                if (effect != null)
                    return getAudioEffectDefaultParam(effect);
            }
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'effect'");
            return null;
        }
        else if (request.equals("audioeffect-full")) {
            if (queryItems != null) {
                String effect = queryItems.get("effect");
                String params = queryItems.get("params");
                if (effect != null && params != null) {
                    return getFullAudioEffect(effect, params);
                }
            }
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'effect' and 'params'");
            return null;
        }
        else if (request.equals("audioeffect-help")) {
            if (queryItems != null) {
                String effect = queryItems.get("effect");
                if (effect != null) {
                    return getAudioEffectHelpText(effect);
                }
            }
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'effect'");
            return null;
        }
        else if (request.equals("audioeffect-is-hmm-effect")) {
            if (queryItems != null) {
                String effect = queryItems.get("effect");
                if (effect != null) {
                    return isHmmAudioEffect(effect);
                }
            }
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'effect'");
            return null;
        }
        else if (request.equals("features")) {
            if (queryItems != null) {
                // List of features that can be computed for the voice
                FeatureProcessorManager mgr = null;
                String voiceName = queryItems.get("voice");
                String localeName = queryItems.get("locale");
                if (voiceName != null) {
                    Voice voice = Voice.getVoice(voiceName);
                    if (voice == null) {
                        MaryHttpServerUtils.errorWrongQueryParameterValue(response, "voice", voiceName, "No voice with that name");
                        return null;
                    }
                    mgr = FeatureRegistry.getFeatureProcessorManager(voice);
                    if (mgr == null) {
                        mgr = FeatureRegistry.getFeatureProcessorManager(voice.getLocale());
                    }
                    if (mgr == null) {
                        mgr = FeatureRegistry.getFallbackFeatureProcessorManager();
                    }
                } else if (localeName != null) {
                    Locale locale = MaryUtils.string2locale(localeName);
                    mgr = FeatureRegistry.getFeatureProcessorManager(locale);
                    if (mgr == null) {
                        mgr = FeatureRegistry.getFallbackFeatureProcessorManager();
                    }
                }
                if (mgr != null)
                    return mgr.listFeatureProcessorNames();
            }
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'voice' or 'locale'");
            return null;
        }
        
        MaryHttpServerUtils.errorFileNotFound(response, request);
        return null;
    }
    
    
    
    
    
}
