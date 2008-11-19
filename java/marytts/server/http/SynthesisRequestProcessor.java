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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.http.HttpResponse;

import marytts.client.http.MaryHtmlForm;
import marytts.client.http.MaryHttpClientUtils;
import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.util.ConversionUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.string.StringUtils;

/**
 * @author oytun.turk
 *
 */
public class SynthesisRequestProcessor extends BaselineRequestProcessor {

    public SynthesisRequestProcessor()
    {
        super();
        
        //Add extra initialisations here
    }
    
    public void process(Address serverAddressAtClient, 
                        Map<String, String> keyValuePairs, 
                        String tempOutputAudioFilePrefix,
                        HttpResponse response) throws Exception
    {
        handleSynthesisRequest(keyValuePairs, response);

        if (keyValuePairs!=null)
        {
            boolean isWebBrowserClient = false;
            String tmpVal = keyValuePairs.get("WEB_BROWSER_CLIENT");
            if (tmpVal!=null && tmpVal.compareTo("true")==0)
                isWebBrowserClient = true;

            if (isWebBrowserClient)
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

                if (htmlForm.isOutputText)
                {
                    htmlForm.outputText = ConversionUtils.toString(MaryHttpClientUtils.toByteArray(response));
                    MaryWebHttpClientHandler webHttpClient = new MaryWebHttpClientHandler();
                    webHttpClient.toHttpResponse(htmlForm, response);
                }
                else
                {
                    byte[] outputBytes = MaryHttpClientUtils.toByteArray(response); 
                    String fileExt = htmlForm.audioFileFormatTypes[htmlForm.audioFormatSelected];
                    int spaceInd = fileExt.indexOf(' ');
                    fileExt = fileExt.substring(0, spaceInd);
                    String randomFile = StringUtils.getRandomFileName(tempOutputAudioFilePrefix, 10, fileExt);
                    File file = new File(randomFile);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(outputBytes);
                    fos.close();
                    logger.debug("Output written to file:" + randomFile);

                    MaryWebHttpClientHandler webHttpClient = new MaryWebHttpClientHandler();
                    webHttpClient.outputAudioFile = randomFile;
                    webHttpClient.toHttpResponse(htmlForm, response);
                }
            }
        }
    }
    
    private boolean handleSynthesisRequest(Map<String, String> keyValuePairs, HttpResponse response) throws Exception
    {
        if (keyValuePairs.get("SYNTHESIS_OUTPUT").compareTo("?")==0) 
        {
            String helper;
            StringTokenizer tt;
            String style = "";
            String effects = "";

            AudioFileFormat.Type audioFileFormatType = null;
            boolean streamingAudio = false;
            
            //INPUT
            helper = keyValuePairs.get("INPUT_TYPE");
            if (helper==null)
                throw new Exception("Expected INPUT_TYPE=<input>");
            
            MaryDataType inputType = MaryDataType.get(helper);
            if (inputType == null) {
                throw new Exception("Invalid input type: " + helper);
            }
            //
            
            //OUTPUT
            helper = keyValuePairs.get("OUTPUT_TYPE");
            if (helper==null)
                throw new Exception("Expected OUTPUT_TYPE=<output>");
            
            MaryDataType outputType = MaryDataType.get(helper);
            if (outputType == null) {
                throw new Exception("Invalid output type: " + keyValuePairs.get("OUTPUT_TYPE"));
            }
            //
            
            //LOCALE
            Locale locale = MaryUtils.string2locale(keyValuePairs.get("LOCALE"));
            if (locale==null)
                throw new Exception("Expected LOCALE=<locale>");
            //
            
            //AUDIO
            helper = keyValuePairs.get("AUDIO");
            if (helper==null) //no AUDIO field
            {
                if (outputType == MaryDataType.get("AUDIO"))
                    throw new Exception("Expected AUDIO=<AUDIOTYPE>");
            }
            else
            {
                // The value of AUDIO=
                String streaming = "STREAMING_";
                if (helper.startsWith(streaming)) 
                {
                    streamingAudio = true;
                    audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(helper.substring(streaming.length()));
                } 
                else
                    audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(helper);    
            }
            //

            //VOICE
            Voice voice = null;
            helper = keyValuePairs.get("VOICE");
            if (helper!=null)
            {
                tt = new StringTokenizer(helper, "=");
                String voiceName = tt.nextToken();
                if ((voiceName.equals("male") || voiceName.equals("female")) && locale != null) 
                {
                    // Locale-specific interpretation of gender
                    voice = Voice.getVoice(locale, new Voice.Gender(voiceName));
                } 
                else 
                {
                    // Plain old voice name
                    voice = Voice.getVoice(voiceName);
                }
                
                if (voice == null) 
                    throw new Exception("No voice matches `" + voiceName + "'. Use a different voice name or remove VOICE= tag from request.");
            }

            if (voice == null) // no voice tag -- use locale default
            {
                voice = Voice.getDefaultVoice(locale);
                logger.debug("No voice requested -- using default " + voice);
            }
            //

            //Optional STYLE field
            helper = keyValuePairs.get("STYLE");
            if (helper!=null && helper.length()>0)
            {
                style = helper;
                logger.debug("Style requested: " + style);
            }
            else
            {
                style = "";
                logger.debug("No style requested");
            }
            //
                
            //Optional: Audio effects
            effects = toRequestedAudioEffectsString(keyValuePairs);

            if (effects.length()>0)
                logger.debug("Audio effects requested: " + effects);
            else
                logger.debug("No audio effects requested");
            //

            //Optional LOG field
            helper = keyValuePairs.get("LOG");
            if (helper!=null)
                logger.info("Connection info: " + helper);

            // Now, the parse is complete.

            // Construct audio file format -- even when output is not AUDIO,
            // in case we need to pass via audio to get our output type.
            AudioFileFormat audioFileFormat = null;
            if (audioFileFormatType == null) {
                audioFileFormatType = AudioFileFormat.Type.WAVE;
            }
            AudioFormat audioFormat = voice.dbAudioFormat();
            if (audioFileFormatType.toString().equals("MP3")) {
                if (!MaryAudioUtils.canCreateMP3())
                    throw new UnsupportedAudioFileException("Conversion to MP3 not supported.");
                audioFormat = MaryAudioUtils.getMP3AudioFormat();
            } else if (audioFileFormatType.toString().equals("Vorbis")) {
                if (!MaryAudioUtils.canCreateOgg())
                    throw new UnsupportedAudioFileException("Conversion to OGG Vorbis format not supported.");
                audioFormat = MaryAudioUtils.getOggAudioFormat();
            }
            audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);

            RequestHttp request = new RequestHttp(inputType, outputType, locale, voice, effects, style, audioFileFormat, streamingAudio);

            //Thread.yield();

            //Send off to new request
            String inputText = keyValuePairs.get("INPUT_TEXT");
            BufferedReader reader = new BufferedReader(new StringReader(inputText));
            RequestHttpHandler rh = new RequestHttpHandler(request, response, reader);

            rh.start();

            rh.join();

            response = rh.response;
            
            keyValuePairs.put("SYNTHESIS_OUTPUT", "DONE");

            return true;
        }
        
        keyValuePairs.put("SYNTHESIS_OUTPUT", "FAILED");

        return false;
    }
}
