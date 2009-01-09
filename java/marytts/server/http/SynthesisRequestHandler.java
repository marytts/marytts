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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;

import marytts.client.http.Address;
import marytts.client.http.MaryFormData;
import marytts.client.http.MaryHttpClientUtils;
import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.server.RequestHandler.StreamingOutputPiper;
import marytts.server.RequestHandler.StreamingOutputWriter;
import marytts.util.ConversionUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.io.LoggingReader;
import marytts.util.string.StringUtils;

/**
 * Provides functionality to process synthesis http requests
 * 
 * @author Oytun T&uumlrk
 *
 */
public class SynthesisRequestHandler extends BaseHttpRequestHandler
{

    private StreamingOutputWriter outputToStream;
    private StreamingOutputPiper streamToPipe;
    private PipedOutputStream pipedOutput;
    private PipedInputStream pipedInput;
    
    public SynthesisRequestHandler()
    {
        super();

        outputToStream = null;
        streamToPipe = null;
        pipedOutput = null;
        pipedInput = null;
    }

    @Override
    protected void handleClientRequest(String absPath, Map<String,String> queryItems, HttpResponse response, Address serverAddressAtClient)
    throws IOException
    {
        logger.debug("New synthesis request: "+absPath);
        if (queryItems != null) {
            for (String key : queryItems.keySet()) {
                logger.debug("    "+key+"="+queryItems.get(key));
            }
        }
        process(serverAddressAtClient, queryItems, response);

    }

    
    
    
    
    public void process(Address serverAddressAtClient, Map<String, String> queryItems, HttpResponse response)
    {
        if (queryItems == null || !(
                queryItems.containsKey("INPUT_TYPE") 
                && queryItems.containsKey("OUTPUT_TYPE")
                && queryItems.containsKey("LOCALE")
                && queryItems.containsKey("INPUT_TEXT")
                )) {
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'INPUT_TEXT' and 'INPUT_TYPE' and 'OUTPUT_TYPE' and 'LOCALE'");
            return;
        }
        
        String inputText = queryItems.get("INPUT_TEXT");
        
        MaryDataType inputType = MaryDataType.get(queryItems.get("INPUT_TYPE"));
        if (inputType == null) {
            MaryHttpServerUtils.errorWrongQueryParameterValue(response, "INPUT_TYPE", queryItems.get("INPUT_TYPE"), null);
            return;
        }

        MaryDataType outputType = MaryDataType.get(queryItems.get("OUTPUT_TYPE"));
        if (outputType == null) {
            MaryHttpServerUtils.errorWrongQueryParameterValue(response, "OUTPUT_TYPE", queryItems.get("OUTPUT_TYPE"), null);
            return;
        }
        boolean isOutputText = true;
        boolean streamingAudio = false;
        AudioFileFormat.Type audioFileFormatType = null;
        if (outputType.name().contains("AUDIO")) {
            isOutputText = false;
            String audioTypeName = queryItems.get("AUDIO");
            if (audioTypeName == null) {
                MaryHttpServerUtils.errorMissingQueryParameter(response, "'AUDIO' when OUTPUT_TYPE=AUDIO");
                return;
            }
            if (audioTypeName.endsWith("_STREAM")) {
                streamingAudio = true;
            }
            int lastUnderscore = audioTypeName.lastIndexOf('_');
            if (lastUnderscore != -1) {
                audioTypeName = audioTypeName.substring(0, lastUnderscore);
            }
            try {
                audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(audioTypeName);
            } catch (Exception ex) {}
            if (audioFileFormatType == null) {
                MaryHttpServerUtils.errorWrongQueryParameterValue(response, "AUDIO", queryItems.get("AUDIO"), null);
                return;
            } else if (audioFileFormatType.toString().equals("MP3") && !MaryAudioUtils.canCreateMP3()) { 
                MaryHttpServerUtils.errorWrongQueryParameterValue(response, "AUDIO", queryItems.get("AUDIO"), "Conversion to MP3 not supported.");
                return;
            } 
            else if (audioFileFormatType.toString().equals("Vorbis") && !MaryAudioUtils.canCreateOgg()) {
                MaryHttpServerUtils.errorWrongQueryParameterValue(response, "AUDIO", queryItems.get("AUDIO"), "Conversion to OGG Vorbis format not supported.");
                return;
            }
        }

        Locale locale = MaryUtils.string2locale(queryItems.get("LOCALE"));
        if (locale == null) {
            MaryHttpServerUtils.errorWrongQueryParameterValue(response, "LOCALE", queryItems.get("LOCALE"), null);
            return;
        }
        
        Voice voice = null;
        String voiceName = queryItems.get("VOICE");
        if (voiceName != null) {
            if (voiceName.equals("male") || voiceName.equals("female")) {
                voice = Voice.getVoice(locale, new Voice.Gender(voiceName));
            } else {
                voice = Voice.getVoice(voiceName);
            }
            if (voice == null) {
                // a voice name was given but there is no such voice
                MaryHttpServerUtils.errorWrongQueryParameterValue(response, "VOICE", queryItems.get("VOICE"), null);
                return;
            }
        }
        if (voice == null) { // no voice tag -- use locale default if it exists.
            voice = Voice.getDefaultVoice(locale);
            logger.debug("No voice requested -- using default " + voice);
        }

        String style = queryItems.get("STYLE");
        if (style == null) style = "";
        
        String effects = toRequestedAudioEffectsString(queryItems);
        if (effects.length()>0)
            logger.debug("Audio effects requested: " + effects);
        else
            logger.debug("No audio effects requested");

        String logMsg = queryItems.get("LOG");
        if (logMsg != null) {
            logger.info("Connection info: "+logMsg);
        }

        // Now, the parse is complete.

        // Construct audio file format -- even when output is not AUDIO,
        // in case we need to pass via audio to get our output type.
        if (audioFileFormatType == null) {
            audioFileFormatType = AudioFileFormat.Type.AU;
        }
        AudioFormat audioFormat;
        if (audioFileFormatType.toString().equals("MP3")) {
            audioFormat = MaryAudioUtils.getMP3AudioFormat();
        } else if (audioFileFormatType.toString().equals("Vorbis")) {
            audioFormat = MaryAudioUtils.getOggAudioFormat();
        } else if (voice != null) {
            audioFormat = voice.dbAudioFormat();
        } else {
            audioFormat = Voice.AF16000;
        }
        AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);
        

        RequestHttp request = new RequestHttp(inputType, outputType, locale, voice, effects, style, audioFileFormat, streamingAudio);
        
        boolean ok = handleClientRequest(inputText, request, response);
        
       
        if (ok)
            logger.info("Request handled successfully.");
        else
            logger.info("Request couldn't be handled successfully.");
        if (MaryUtils.lowMemoryCondition()) {
            logger.info("Low memory condition detected (only " + MaryUtils.availableMemory() + " bytes left). Triggering garbage collection.");
            Runtime.getRuntime().gc();
            logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
        }
    }



    private boolean handleClientRequest(String inputText, RequestHttp request, HttpResponse response)
    {
        Reader reader = new LoggingReader(new BufferedReader(new StringReader(inputText)), logger);

        outputToStream = null;
        streamToPipe = null;
        pipedOutput = null;
        pipedInput = null;
        
        // tasks:
        // * read the input according to its type
        // * determine which modules are needed
        // * in turn, write to and read from each module according to data type
        // * write output according to its type

        try {
            request.readInputData(reader);
        } catch (Exception e) {
            String message = "Problem reading input";
            logger.warn(message, e);
            MaryHttpServerUtils.errorInternalServerError(response, message, e);
            return false;
        }

        boolean streamingOutput = false;

        // Process input data to output data
        try {
            if (request.getOutputType().equals(MaryDataType.get("AUDIO")) && request.getStreamAudio()) {
                streamingOutput = true;

                pipedOutput = new PipedOutputStream();
                pipedInput = new PipedInputStream(pipedOutput);
                outputToStream = new StreamingOutputWriter(request, pipedOutput);

                //Pipe to an input stream
                String mimeType = MaryHttpServerUtils.getMimeType(request.getAudioFileFormat().getType());
                streamToPipe = new StreamingOutputPiper(pipedInput, response, mimeType); 
                outputToStream.start();
                streamToPipe.start();
            }
            request.process();
        } catch (Throwable e) {
            String message = "Processing failed.";
            logger.error(message, e);
            MaryHttpServerUtils.errorInternalServerError(response, message, e);
            return false;
        }

        // Write output
        if (!streamingOutput) { //Non-streaming output
            try {
                request.writeNonstreamingOutputData(response);
            } catch (Exception e) {
                String message = "Cannot write output";
                logger.warn(message, e);
                MaryHttpServerUtils.errorInternalServerError(response, message, e);
                return false;
            } 
        } else { //Streaming output
            try {
                streamToPipe.join();
            } catch (InterruptedException e) {
            }
        }
        return true;
    }

    
    protected String toRequestedAudioEffectsString(Map<String, String> keyValuePairs)
    {
        StringBuilder effects = new StringBuilder();
        StringTokenizer tt;
        Set<String> keys = keyValuePairs.keySet();
        String currentKey;
        String currentEffectName, currentEffectParams;
        for (Iterator<String> it = keys.iterator(); it.hasNext();)
        {
            currentKey = it.next();
            if (currentKey.startsWith("effect_"))
            {
                if (currentKey.endsWith("_selected"))
                {
                    if (keyValuePairs.get(currentKey).compareTo("on")==0)
                    {
                        if (effects.length()>0)
                            effects.append("+");
                        
                        tt = new StringTokenizer(currentKey, "_");
                        if (tt.hasMoreTokens()) tt.nextToken(); //Skip "effects_"
                        if (tt.hasMoreTokens()) //The next token is the effect name
                        {
                            currentEffectName = tt.nextToken();

                            currentEffectParams = keyValuePairs.get("effect_" + currentEffectName + "_parameters");
                            if (currentEffectParams!=null && currentEffectParams.length()>0)
                                effects.append(currentEffectName).append("(").append(currentEffectParams).append(")");
                            else
                                effects.append(currentEffectName);
                        }
                    }
                }
            }
        }
        
        return effects.toString();
    }

}
