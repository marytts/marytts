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
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;

import marytts.client.http.Address;
import marytts.client.http.MaryBaseClient;
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
public class SynthesisRequestProcessor extends BaselineRequestProcessor {

    private StreamingOutputWriter outputToStream;
    private StreamingOutputPiper streamToPipe;
    private PipedOutputStream pipedOutput;
    private PipedInputStream pipedInput;
    
    public SynthesisRequestProcessor()
    {
        super();

        outputToStream = null;
        streamToPipe = null;
        pipedOutput = null;
        pipedInput = null;
    }

    public boolean process(Address serverAddressAtClient, Map<String, String> keyValuePairs, String responseID, HttpResponse response) throws Exception
    {
        RequestHttp request = null;
        boolean ok = false;
        if (keyValuePairs.get("SYNTHESIS_OUTPUT").compareTo("?")==0 || keyValuePairs.get("SYNTHESIS_OUTPUT").compareTo("PENDING")==0) 
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

            boolean isOutputText = true;
            if (outputType.name().contains("AUDIO"))
                isOutputText = false;
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
                int scoreInd = helper.indexOf('_');
                if (helper.endsWith("_STREAM")) 
                    streamingAudio = true;
                else if (helper.endsWith("_FILE"))
                    streamingAudio = false; 
                else
                {
                    scoreInd = helper.length();
                    streamingAudio = false; 
                }
                
                audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(helper.substring(0, scoreInd));
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
                audioFileFormatType = AudioFileFormat.Type.AU;
            }
            AudioFormat audioFormat = voice.dbAudioFormat();
            if (audioFileFormatType.toString().equals("MP3")) 
            {
                if (!MaryAudioUtils.canCreateMP3())
                    throw new UnsupportedAudioFileException("Conversion to MP3 not supported.");
                
                audioFormat = MaryAudioUtils.getMP3AudioFormat();
            } 
            else if (audioFileFormatType.toString().equals("Vorbis")) 
            {
                if (!MaryAudioUtils.canCreateOgg())
                    throw new UnsupportedAudioFileException("Conversion to OGG Vorbis format not supported.");
                
                audioFormat = MaryAudioUtils.getOggAudioFormat();
            }
            audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);

            request = new RequestHttp(inputType, outputType, locale, voice, effects, style, audioFileFormat, streamingAudio);

            //Send off to new request
            boolean isWebBrowserClient = false;
            if (keyValuePairs!=null)
            {
                String tmpVal = keyValuePairs.get("WEB_BROWSER_CLIENT");
                if (tmpVal!=null && tmpVal.compareTo("true")==0)
                    isWebBrowserClient = true;
            }
            
            boolean isSecondCall = false;
            String tmpVal = keyValuePairs.get("SYNTHESIS_OUTPUT");
            if (tmpVal!=null && tmpVal.compareTo("PENDING")==0)
                isSecondCall = true;

            if (isWebBrowserClient && !isSecondCall)
                ok = handleWebBrowserClientFirstRequest(request, response, serverAddressAtClient, keyValuePairs, responseID);
            else
                ok = handleClientRequest(request, response, keyValuePairs);
            
            if (ok)
                logger.info("Request handled successfully.");
            else
                logger.info("Request couldn't be handled successfully.");
            if (MaryUtils.lowMemoryCondition()) 
            {
                logger.info("Low memory condition detected (only " + MaryUtils.availableMemory() + " bytes left). Triggering garbage collection.");
                Runtime.getRuntime().gc();
                logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
            }
        }  
        
        return ok;
    }

    public boolean handleWebBrowserClientFirstRequest(RequestHttp request, 
                                                      HttpResponse response, 
                                                      Address serverAddressAtClient, 
                                                      Map<String, String> keyValuePairs, 
                                                      String responseID) throws Exception 
    {
        if (request == null)
            throw new NullPointerException("Cannot handle null request");

        String inputText = keyValuePairs.get("INPUT_TEXT");
        Reader reader = new LoggingReader(new BufferedReader(new StringReader(inputText)), logger);

        outputToStream = null;
        streamToPipe = null;
        pipedOutput = null;
        pipedInput = null;
        
        boolean ok = true;
        // tasks:
        // * read the input according to its type
        // * determine which modules are needed
        // * in turn, write to and read from each module according to data type
        // * for non-streaming output: write output according to its type
        // * for streaming output: 
        //       write the output to a pipe and put the pipe input into an http response entity
        //       for direct access by the http client
        try {
            request.readInputData(reader);
        } catch (Exception e) {
            String message = "Problem reading input";
            logger.warn(message, e);
            ok = false;
        }

        boolean streamingOutput = false;
        boolean isSecondCallRequired = false;
        if (request.getOutputType().equals(MaryDataType.get("AUDIO")))
            isSecondCallRequired = true;

        // Process input data to output data
        if (ok)
        {        
            try 
            {
                if (!isSecondCallRequired) 
                    request.process();
            } 
            catch (Throwable e) 
            {
                String message = "Processing failed.";
                logger.error(message, e);
                ok = false;
            }
        }

        // Write output
        String synthesisStatus = "FAILED";
        ByteArrayOutputStream outputStream = null;

        if (isSecondCallRequired) //Synthesis result will be kept for the next call
            synthesisStatus = "PENDING";
        else //Synthesis result will be sent by the end of this call
        {
            outputStream = new ByteArrayOutputStream();
            try 
            {
                request.writeOutputData(outputStream);
                synthesisStatus = "DONE";
            } 
            catch (Exception e) 
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                synthesisStatus = "FAILED";
            }
        }
        
        keyValuePairs.put("SYNTHESIS_OUTPUT", synthesisStatus);

        MaryBaseClient htmlForm = new MaryBaseClient(serverAddressAtClient,
                keyValuePairs,
                getMaryVersion(),
                getVoices(),
                getDataTypes(),
                getAudioFileFormatTypes(),
                getAudioEffectHelpTextLineBreak(),
                getDefaultAudioEffects(),
                getDefaultVoiceExampleTexts());

        String contentType = "text/html; charset=UTF-8";
        if (htmlForm!=null)
        {
            if (!isSecondCallRequired) //Non-audio output to web browser client
            {
                if (outputStream!=null)
                    htmlForm.outputText = ConversionUtils.toString(outputStream.toByteArray());
                else
                    htmlForm.outputText = "Cannot convert this type of input to selected output...";
            }
            else //Initiate audio output request in web browser client for second request
                htmlForm.outputAudioResponseID = String.valueOf(responseID) + "." + request.getAudioFileFormat().getType().getExtension();  
        }
        else  //There was something wrong, call the default page
        {
            htmlForm = new MaryBaseClient(serverAddressAtClient,
                    keyValuePairs,
                    getMaryVersion(),
                    getVoices(),
                    getDataTypes(),
                    getAudioFileFormatTypes(),
                    getAudioEffectHelpTextLineBreak(),
                    getDefaultAudioEffects(),
                    getDefaultVoiceExampleTexts());
        }

        MaryWebHttpClientHandler webHttpClient = new MaryWebHttpClientHandler();
        webHttpClient.toHttpResponse(htmlForm, response, contentType); //Send the html page

        return ok;
    }
    
    public boolean handleClientRequest(RequestHttp request, 
                                    HttpResponse response, 
                                    Map<String, String> keyValuePairs) throws Exception 
    {
        if (request == null)
            throw new NullPointerException("Cannot handle null request");

        String inputText = keyValuePairs.get("INPUT_TEXT");
        Reader reader = new LoggingReader(new BufferedReader(new StringReader(inputText)), logger);

        outputToStream = null;
        streamToPipe = null;
        pipedOutput = null;
        pipedInput = null;
        
        boolean ok = true;
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
            ok = false;
        }

        boolean streamingOutput = false;

        // Process input data to output data
        if (ok)
        {
            try 
            {
                if (request.getOutputType().equals(MaryDataType.get("AUDIO")) && request.getStreamAudio()) 
                {
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
            } 
            catch (Throwable e) 
            {
                String message = "Processing failed.";
                logger.error(message, e);
                ok = false;
            }
        }

        // Write output
        String synthesisStatus = "FAILED";
        ByteArrayOutputStream outputStream = null;

        if (ok) 
        {  
            if (!streamingOutput) //Non-streaming output
                request.writeNonstreamingOutputData(response); 
            else //Streaming output
                streamToPipe.join();
            
            synthesisStatus = "DONE";
        }   

        keyValuePairs.put("SYNTHESIS_OUTPUT", synthesisStatus);

        return ok;
    }
}
