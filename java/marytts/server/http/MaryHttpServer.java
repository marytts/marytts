/**
 * Copyright 2000-2006 DFKI GmbH.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.Version;
import marytts.client.http.MaryHtmlForm;
import marytts.client.http.MaryHttpClientUtils;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.NoSuchPropertyException;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.server.RequestHandler;
import marytts.signalproc.effects.BaseAudioEffect;
import marytts.signalproc.effects.EffectsApplier;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.ConversionUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ParseException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicLineParser;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.FileContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.entity.ContentListener;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * Listen for clients on socket port
 *          <code>MaryProperties.socketPort()</code>.
 *          For each new client, create a new RequestHandler thread.
 * <p>
 * Clients are expected to follow the following <b>protocol</b>:
 * <p>
 * A client opens two socket connections to the server. The first,
 * <code>infoSocket</code>, serves for passing meta-information,
 * such as the requested input and output types or warnings.
 * The second, <code>dataSocket</code>, serves for passing the actual
 * input and output data.
 * The server expects the communication as follows.
 * <ol>
 * <li> The client opens an <code>infoSocket</code>,
 * optionally sends one line "MARY VERSION" to obtain
 * three lines of version information, and then sends one line
 * "MARY IN=INPUTTYPE OUT=OUTPUTTYPE [AUDIO=AUDIOTYPE]",
 * where INPUTTYPE and OUTPUTTYPE can have a number of different values,
 * depending on the configuration with which the server was started.
 * For an English system, these values include:
 * <ul>
 *   <li>  TEXT_EN          plain ASCII text, English (input only) </li>
 *   <li>  SABLE         text annotated with SABLE markup (input only) </li>
 *   <li>  SSML          text annotated with SSML markup (input only) </li>
 *   <li>  APML          text annotated with APML markup (input only) </li>
 *   <li>  RAWMARYXML    untokenised MaryXML </li>
 *   <li>  TOKENS_EN     tokenized text </li>
 *   <li>  WORDS_EN      numbers and abbreviations expanded </li>
 *   <li>  POS_EN        parts of speech tags added </li>
 *   <li>  SEGMENTS_EN   phoneme symbols </li>
 *   <li>  INTONATION_EN ToBI intonation symbols </li>
 *   <li>  POSTPROCESSED_EN post-lexical phonological rules </li>
 *   <li>  ACOUSTPARAMS  acoustic parameters in MaryXML structure </li>
 *   <li>  MBROLA        phone symbols, duration and frequency values </li>
 *   <li>  AUDIO         audio data (output only) </li>
 * </ul>
 * INPUTTYPE must be earlier in this list than OUTPUTTYPE.
 * The list of input and output data types can be requested from the server by
 * sending it a line "MARY LIST DATATYPES". The server will reply with a list of lines
 * where each line represents one data type, e.g. "RAWMARYXML INPUT OUTPUT",
        "TEXT INPUT" or "AUDIO OUTPUT".
 * See the code in MaryClient.fillDataTypes().  
 * <p>
 * The optional AUDIO=AUDIOTYPE specifies the type of audio file
 * to be sent for audio output. Possible values are:
 * <ul>
 *   <li> WAVE </li>
 *   <li> AU </li>
 *   <li> SND </li>
 *   <li> AIFF </li>
 *   <li> AIFC </li>
 *   <li> MP3 </li>
 *   <li> Vorbis </li>
 *   <li> STREAMING_AU</li>
 *   <li> STREAMING_MP3</li>
 * </ul>
 * <p>
 * The optional VOICE=VOICENAME specifies the default voice with which
 * the text is to be spoken. As for the data types, possible values
 * depend on the configuration of the server. The list can be retrieved
 * by sending the server a line "MARY LIST VOICES", which will reply with
 * lines such as "de7 de female", "kevin16 en male" or "us2 en male". 
 * <p>
 * The optional EFFECTS=EFFECTSWITHPARAMETERS specifies the audio effects
 * to be applied as a post-processing step along with their parameters. 
 * EFFECTSWITHPARAMETERS is a String of the form 
 * "Effect1Name(Effect1Parameter1=Effect1Value1; Effect1Parameter2=Effect1Value2), Effect2Name(Effect2Parameter1=Effect2Value1)"
 * For example, "Robot(amount=100),Whisper(amount=50)" will convert the output into 
 * a whispered robotic voice with the specified amounts.
 * <p>
 * Example: The line
 * <pre>
 *   MARY IN=TEXT_EN OUT=AUDIO AUDIO=WAVE VOICE=kevin16 EFFECTS
 * </pre>
 * will process normal ASCII text, and send back a WAV audio file
 * synthesised with the voice "kevin16".
 * </li>
 *
 * <li> The server reads and parses this input line. If its format is correct,
 * a line containing a single integer is sent back to the client
 * on <code>infoSocket</code>. This
 * integer is a unique identification number for this request.
 * </li>
 *
 * <li> The client opens a second socket connection to the server, on the same
 * port, the <code>dataSocket</code>. As a first line on this
 * <code>dataSocket</code>,
 * it sends the single integer it had just received via the
 * <code>infoSocket</code>.
 * </li>
 *
 * <li> The server groups dataSocket and infoSocket together based on this
 * identification number, and starts reading data of the requested input
 * type from <code>dataSocket</code>.
 * </li>
 *
 * <li> If any errors or warning messages are issued during input parsing or
 * consecutive processing, these are printed to <code>infoSocket</code>.
 * </li>
 *
 * <li> The processing result is output to <code>dataSocket</code>.
 * </li>
 * </ol>
 *
 * @see RequestHttpHandler
 * @author Marc Schr&ouml;der, oytun.turk
 */

public class MaryHttpServer {
    //private ServerSocket server;
    private static Logger logger;
    private int runningNumber = 1;
    //private Map<Integer,Object[]> clientMap;
    private Map<String, Integer> audioOutputMap;

    public MaryHttpServer() {
        logger = Logger.getLogger("server");
    }

    public void run() throws IOException, NoSuchPropertyException 
    {
        audioOutputMap = new HashMap<String, Integer>();
        logger.info("Starting server.");
        //clientMap = Collections.synchronizedMap(new HashMap<Integer,Object[]>());
        
        int localPort = MaryProperties.needInteger("socket.port");
        
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 50000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        BufferingHttpServiceHandler handler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);

        // Set up request handlers
        HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
        reqistry.register("*", new HttpClientHandler());

        handler.setHandlerResolver(reqistry);

        // Provide an event logger
        handler.setEventListener(new EventLogger());

        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(handler, params);
        ListeningIOReactor ioReactor = new DefaultListeningIOReactor(2, params);
        
        logger.info("Waiting for client to connect on port " + localPort);
        
        try {
            ioReactor.listen(new InetSocketAddress(localPort));
            ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
            logger.info("Interrupted");
        } catch (IOException e) {
            logger.info("Cannot write to client.");
        }
        System.out.println("Shutdown");
    }

    private synchronized int getID() {
        return runningNumber++;
    }

    public class HttpClientHandler extends SimpleNHttpRequestHandler implements HttpRequestHandler  
    {    
        private final boolean useFileChannels = true;
        
        public HttpClientHandler() 
        {
            super();
            
            logger = Logger.getLogger("server");
        }

        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
        {            
            Header[] tmp = request.getHeaders("Host");
            
            Address serverAddressAtClient = getServerAddressAtClient(tmp[0].getValue().toString());
            logger.info("New connection from client");
            
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            String fullParameters = null;
            
            if (method.equals("GET") || method.equals("POST"))
            {   
                fullParameters = request.getRequestLine().getUri().toString();
                fullParameters = preprocess(fullParameters);
                
                //Try and get parameters from different HTTP POST requests if you have not been able to do this above
                if (method.equals("POST") && fullParameters.length()<1) 
                {
                    String fullParameters2 = "";
                    
                    if (request instanceof HttpEntityEnclosingRequest)
                    {
                        try {
                            fullParameters2 = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
                        } catch (ParseException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                    else if (request instanceof BasicHttpEntityEnclosingRequest)
                    {
                        try {
                            fullParameters2 = EntityUtils.toString(((BasicHttpEntityEnclosingRequest) request).getEntity());
                        } catch (ParseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    
                    if (fullParameters2.length()>0)
                        fullParameters = preprocess(fullParameters2);
                }
            }
            else
            {
                try {
                    throw new MethodNotSupportedException(method + " method not supported");
                } catch (MethodNotSupportedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } 
            }

            //Parse request and create appropriate response
            try {
                handleClientRequest(fullParameters, response, serverAddressAtClient);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        private Address getServerAddressAtClient(String fullHeader)
        {
            String fullAddress = fullHeader.trim();
            int index = fullAddress.indexOf('?');
            
            if (index>0)
                fullAddress = fullAddress.substring(0, index);
            
            return new Address(fullAddress);
        }
        
        private String preprocess(String fullParameters)
        {
            String preprocessedParameters = fullParameters.trim();
            int index1 = preprocessedParameters.indexOf('/');
            int index2 = preprocessedParameters.indexOf('?');
            int index = 0;
            if (index1>-1)
                index=index1+1;
            if (index2>-1 && index2>=index)
                index=index2+1;
            
            preprocessedParameters = preprocessedParameters.substring(index);
            
            preprocessedParameters = StringUtils.urlDecode(preprocessedParameters);
            
            System.out.println("Preprocessed request: " + preprocessedParameters);
         
            return preprocessedParameters;
        }

        /**
         * Implement the protocol for communicating with an HTTP client.
         * @throws Exception 
         */
        private void handleClientRequest(String fullParameters, HttpResponse response, Address serverAddressAtClient) throws Exception 
        {   
            String tempOutputAudioFilePrefix = "mary_audio_out_temp_";
            if (fullParameters!=null && (fullParameters.compareToIgnoreCase("favicon.ico")==0 || fullParameters.startsWith(tempOutputAudioFilePrefix))) //Check first whether a file is being requested
            {
                String fullPathFile = "";
                boolean isDeleteFiles = false;
                if (fullParameters.compareToIgnoreCase("favicon.ico")==0)
                {
                    URL resUrl = MaryHttpServer.class.getResource(fullParameters);
                    if (resUrl!=null)
                    {
                        fullPathFile = resUrl.getPath();
                        /*
                        while (fullPathFile.startsWith("/"))
                            fullPathFile = fullPathFile.substring(1, fullPathFile.length());
                        while (fullPathFile.startsWith("\\"))
                            fullPathFile = fullPathFile.substring(1, fullPathFile.length());
                            */
                    }
                }
                else if (fullParameters.startsWith(tempOutputAudioFilePrefix))
                {
                    fullPathFile = fullParameters;
                    isDeleteFiles = true;
                }
                
                if (fullPathFile!="")
                {
                    logger.debug("Audio output file requested by client:" + fullPathFile);
                    
                    int status = MaryHttpServerUtils.fileToHttpResponse(fullPathFile, response, useFileChannels);
                    
                    if (isDeleteFiles)
                    {
                        //Check the map and delete files that have already been sent
                        //Note that this always checks previous files, so there is no way to delete the last file synthesized
                        //That file remains under the working folder
                        Set<String> prevFiles = audioOutputMap.keySet();
                        String strFile;
                        for (Iterator<String> it = prevFiles.iterator(); it.hasNext();)
                        {
                            strFile = it.next();
                            if (audioOutputMap.get(strFile)==2)
                            {
                                FileUtils.delete(strFile);
                                audioOutputMap.remove(strFile);
                            }
                        }
                        //
                        
                        //Put the new file
                        Integer numRequested = audioOutputMap.get(fullPathFile);
                        if (numRequested==null)
                            audioOutputMap.put(fullPathFile, new Integer(1));
                        else
                            audioOutputMap.put(fullPathFile, ++numRequested);
                        //
                    }
                    
                    response.setStatusCode(status);
                    return;
                }
                
                return;
            }
            
            logger.debug("Read HTML form request: '" + fullParameters + "'");
            
            Map<String, String> keyValuePairs = MaryHttpClientUtils.toKeyValuePairs(fullParameters, false);
            
            boolean isWebBrowserClient = false;
            boolean bProcessed = false;
            boolean isDefaultPageRequested = false;
            if (keyValuePairs==null)
            {
                isDefaultPageRequested = true;
                isWebBrowserClient = true;
            }
            else
            {
                String tmpVal = keyValuePairs.get("DEFAULT_PAGE");
                if (tmpVal!=null && tmpVal.compareTo("?")==0)
                    isDefaultPageRequested = true;
                
                tmpVal = keyValuePairs.get("WEB_BROWSER_CLIENT");
                if (tmpVal!=null && tmpVal.compareTo("true")==0)
                    isWebBrowserClient = true;
                else
                    isWebBrowserClient = false; 
            }
            
            if (isDefaultPageRequested) //A web browser client is asking for the default html page
            {
                if (isWebBrowserClient)
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
                else
                    throw new Exception("Invalid request to Mary server!");
            }
            else
            {
                String tmp = keyValuePairs.get("SYNTHESIS_OUTPUT");
                if (tmp!=null && tmp.compareTo("?")==0)
                {
                    handleSynthesisRequest(keyValuePairs, response);
                    
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

                    bProcessed = true;
                }

                if (!bProcessed)
                {
                    bProcessed = handleRequests(keyValuePairs);

                    if (keyValuePairs!=null)
                    {
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
                            MaryHttpServerUtils.toHttpResponse(keyValuePairs, response); 
                    }
                }
            }
            
            if (bProcessed)
                response.setStatusCode(HttpStatus.SC_OK);
            else
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        }

        //Tries to fill in requested information from the client
        //A major difference from the non-HTTP server is that multiple requests can be handled within single client request
        private boolean handleRequests(Map<String, String> keyValuePairs) throws IOException 
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
                    bRet = handleRequest(currentKey, params, keyValuePairs);
                    
                    if (bRet==false)
                        logger.debug("Request failed: " + currentKey + ( ( (it.hasNext()) ? (" ...proceeding with next one") : ("") )));
                    else
                        bAtLeastOneRequestSucceeded = true;
                }   
            }
            
            return bAtLeastOneRequestSucceeded;
        }
        
        private boolean handleRequest(String request, String param, Map<String, String> keyValuePairs) throws IOException 
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
        
        private boolean handleSynthesisRequest(Map<String, String> keyValuePairs, HttpResponse response) throws Exception
        {
            //String outputLine = null;
            int id = 0;

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
                // this request's id:
                id = getID();

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

                RequestHttp request = new RequestHttp(inputType, outputType, locale, voice, effects, style, id, audioFileFormat, streamingAudio);

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
        
        private String toRequestedAudioEffectsString(Map<String, String> keyValuePairs)
        {
            String effects = "";
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
                                effects += "+";
                            
                            tt = new StringTokenizer(currentKey, "_");
                            if (tt.hasMoreTokens()) tt.nextToken(); //Skip "effects_"
                            if (tt.hasMoreTokens()) //The next token is the effect name
                            {
                                currentEffectName = tt.nextToken();

                                currentEffectParams = keyValuePairs.get("effect_" + currentEffectName + "_parameters");
                                if (currentEffectParams!=null && currentEffectParams.length()>0)
                                    effects += currentEffectName + "(" + currentEffectParams + ")";
                                else
                                    effects += currentEffectName;
                            }
                        }
                    }
                }
            }
            
            return effects;
        }

        private String getMaryVersion()
        {
            String output = "Mary TTS server " + Version.specificationVersion() + " (impl. " + Version.implementationVersion() + ")";

            return output;
        }

        private String getDataTypes()
        {
            String output = "";
            
            List<MaryDataType> allTypes = MaryDataType.getDataTypes();
            
            for (MaryDataType t : allTypes) 
            {
                output += t.name();
                if (t.isInputType())
                    output += " INPUT";
                if (t.isOutputType())
                    output += " OUTPUT";

                output += System.getProperty("line.separator");
            }

            return output;
        }

        private String getVoices()
        {
            String output = "";
            Collection<Voice> voices = Voice.getAvailableVoices();
            for (Iterator<Voice> it = voices.iterator(); it.hasNext();) 
            {
                Voice v = (Voice) it.next();
                if (v instanceof InterpolatingVoice) {
                    // do not list interpolating voice
                } else if (v instanceof UnitSelectionVoice)
                {
                    output += v.getName() + " " 
                    + v.getLocale() + " " 
                    + v.gender().toString() + " " 
                    + "unitselection" + " "
                    +((UnitSelectionVoice)v).getDomain()
                    + System.getProperty("line.separator");
                }
                else if (v instanceof HMMVoice)
                {
                    output += v.getName() + " " 
                    + v.getLocale()+ " " 
                    + v.gender().toString()+ " "
                    + "hmm"
                    + System.getProperty("line.separator");
                }
                else
                {
                    output += v.getName() + " " 
                    + v.getLocale()+ " " 
                    + v.gender().toString() + " "
                    + "other"
                    + System.getProperty("line.separator");
                }
            }
            
            return output;
        }
        
        private String getDefaultVoiceName()
        {
            String defaultVoiceName = "";
            String allVoices = getVoices();
            if (allVoices!=null && allVoices.length()>0)
            {
                StringTokenizer tt = new StringTokenizer(allVoices, System.getProperty("line.separator"));
                if (tt.hasMoreTokens())
                {
                    defaultVoiceName = tt.nextToken();
                    StringTokenizer tt2 = new StringTokenizer(defaultVoiceName, " ");
                    if (tt2.hasMoreTokens())
                        defaultVoiceName = tt2.nextToken();
                }
            }
            
            return defaultVoiceName;
        }

        private String getAudioFileFormatTypes()
        {
            String output = "";
            AudioFileFormat.Type[] audioTypes = AudioSystem.getAudioFileTypes();
            for (int t=0; t<audioTypes.length; t++)
                output += audioTypes[t].getExtension() + " " + audioTypes[t].toString() + System.getProperty("line.separator");

            return output;
        }

        private String getExampleText(String parameters)
        {
            return getExampleText(parameters, null);
        }
        
        private String getExampleText(String parameters, Map<String, String> pairs)
        {
            String output = "";
            StringTokenizer st = new StringTokenizer(parameters);
            if (st.hasMoreTokens()) 
            {
                String typeName = st.nextToken();
                try {
                    //Next should be locale:
                    Locale locale = MaryUtils.string2locale(st.nextToken());
                    MaryDataType type = MaryDataType.get(typeName);
                    if (type != null) {
                        String exampleText = type.exampleText(locale);
                        if (exampleText != null)
                            output += exampleText.trim() + System.getProperty("line.separator");

                        if (pairs!=null)
                        {
                            if (type.isInputType())
                                pairs.put("INPUT_TEXT", output);
                            else
                                pairs.put("OUTPUT_TEXT", output);
                        }
                    }
                } catch (Error err) {} // type doesn't exist
            }

            return output;
        }

        private Vector<String> getDefaultVoiceExampleTexts()
        {
            String defaultVoiceName = getDefaultVoiceName();
            Vector<String> defaultVoiceExampleTexts = null;
            defaultVoiceExampleTexts = StringUtils.processVoiceExampleText(getVoiceExampleText(defaultVoiceName, null));
            if (defaultVoiceExampleTexts==null) //Try for general domain
            {
                String str = getExampleText("TEXT" + " " + Voice.getVoice(defaultVoiceName).getLocale());
                if (str!=null && str.length()>0)
                {
                    defaultVoiceExampleTexts = new Vector<String>();
                    defaultVoiceExampleTexts.add(str);
                }
            }
            
            return defaultVoiceExampleTexts;
        }
        
        private String getVoiceExampleText(String parameters, Map<String, String> pairs)
        {
            String output = "";
            StringTokenizer st = new StringTokenizer(parameters);

            if (st.hasMoreTokens()) 
            {
                String voiceName = st.nextToken();
                Voice v = Voice.getVoice(voiceName);
                
                if (v != null) 
                {
                    String text = "";
                    if (v instanceof marytts.unitselection.UnitSelectionVoice)
                        output += ((marytts.unitselection.UnitSelectionVoice)v).getExampleText();
                    
                    if (pairs!=null)
                        pairs.put("INPUT_TEXT", output);
                }
            }

            return output;
        }

        private String getDefaultAudioEffects()
        {
            // <EffectSeparator>charEffectSeparator</EffectSeparator>
            // <Effect>
            //   <Name>effect´s name</Name> 
            //   <SampleParam>example parameters string</SampleParam>
            //   <HelpText>help text string</HelpText>
            // </Effect>
            // <Effect>
            //   <Name>effect´s name</effectName> 
            //   <SampleParam>example parameters string</SampleParam>
            //   <HelpText>help text string</HelpText>
            // </Effect>
            // ...
            // <Effect>
            //   <Name>effect´s name</effectName> 
            //   <SampleParam>example parameters string</SampleParam>
            //   <HelpText>help text string</HelpText>
            // </Effect>
            String audioEffectClass = "<EffectSeparator>" + EffectsApplier.chEffectSeparator + "</EffectSeparator>";

            for (int i=0; i<MaryProperties.effectClasses().size(); i++)
            {
                audioEffectClass += "<Effect>";
                audioEffectClass += "<Name>" + MaryProperties.effectNames().elementAt(i) + "</Name>";
                audioEffectClass += "<Param>" + MaryProperties.effectSampleParams().elementAt(i) + "</Param>";
                audioEffectClass += "<SampleParam>" + MaryProperties.effectSampleParams().elementAt(i) + "</SampleParam>";
                audioEffectClass += "<HelpText>" + MaryProperties.effectHelpTexts().elementAt(i) + "</HelpText>";
                audioEffectClass += "</Effect>";
            }

            return audioEffectClass;
        }

        private String getAudioEffectHelpTextLineBreak()
        {
            return BaseAudioEffect.strLineBreak;
        }

        private String getAudioEffectDefaultParam(String effectName)
        {
            String output = "";
            boolean bFound = false;
            for (int i=0; i<MaryProperties.effectNames().size(); i++)
            {
                //int tmpInd = inputLine.indexOf(MaryProperties.effectNames().elementAt(i));
                //if (tmpInd>-1)
                if (effectName.compareTo(MaryProperties.effectNames().elementAt(i))==0)
                {   
                    //the request is about the parameters of a specific audio effect
                    BaseAudioEffect ae = null;
                    try {
                        ae = (BaseAudioEffect)Class.forName(MaryProperties.effectClasses().elementAt(i)).newInstance();
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (ae!=null)
                    {
                        String audioEffectParams = ae.getExampleParameters();
                        output = audioEffectParams.trim();
                    }

                    break;
                }
            }

            return output;
        }
        
        private String getFullAudioEffect(String effectName, String currentEffectParams)
        {
            String output = "";

            for (int i=0; i<MaryProperties.effectNames().size(); i++)
            {
                if (effectName.compareTo(MaryProperties.effectNames().elementAt(i))==0)
                {   
                    //the request is about the parameters of a specific audio effect
                    BaseAudioEffect ae = null;
                    try {
                        ae = (BaseAudioEffect)Class.forName(MaryProperties.effectClasses().elementAt(i)).newInstance();
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (ae!=null)
                    {
                        ae.setParams(currentEffectParams);
                        output = ae.getFullEffectAsString();
                    }

                    break;
                }
            }
            return output;
        }

        private String getAudioEffectHelpText(String effectName)
        {
            String output = "";

            for (int i=0; i<MaryProperties.effectNames().size(); i++)
            {
                if (effectName.compareTo(MaryProperties.effectNames().elementAt(i))==0)
                {   
                    //the request is about the parameters of a specific audio effect

                    BaseAudioEffect ae = null;
                    try {
                        ae = (BaseAudioEffect)Class.forName(MaryProperties.effectClasses().elementAt(i)).newInstance();
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (ae!=null)
                    {
                        String helpText = ae.getHelpText();
                        output = helpText.trim();
                    }

                    break;
                }
            }
            
            return output;
        }

        private String isHmmAudioEffect(String effectName)
        {
            String output = "";

            for (int i=0; i<MaryProperties.effectNames().size(); i++)
            {
                if (effectName.compareTo(MaryProperties.effectNames().elementAt(i))==0)
                {   
                    BaseAudioEffect ae = null;
                    try {
                        ae = (BaseAudioEffect)Class.forName(MaryProperties.effectClasses().elementAt(i)).newInstance();
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (ae!=null)
                    {
                        output = "no";

                        if (ae.isHMMEffect())
                            output = "yes";
                    }

                    break;
                }
            }
            
            return output;
        }

        public ConsumingNHttpEntity entityRequest(
                final HttpEntityEnclosingRequest request,
                final HttpContext context) throws HttpException, IOException {
            return new ConsumingNHttpEntityTemplate(
                    request.getEntity(),
                    new FileWriteListener(useFileChannels));
        }
    }
    
    static class FileWriteListener implements ContentListener {
        private final File file;
        private final FileInputStream inputFile;
        private final FileChannel fileChannel;
        private final boolean useFileChannels;
        private long idx = 0;

        public FileWriteListener(boolean useFileChannels) throws IOException {
            this.file = File.createTempFile("tmp", ".tmp", null);
            this.inputFile = new FileInputStream(file);
            this.fileChannel = inputFile.getChannel();
            this.useFileChannels = useFileChannels;
        }

        public void contentAvailable(ContentDecoder decoder, IOControl ioctrl)
                throws IOException {
            long transferred;
            if(useFileChannels && decoder instanceof FileContentDecoder) {
                transferred = ((FileContentDecoder) decoder).transfer(
                        fileChannel, idx, Long.MAX_VALUE);
            } else {
                transferred = fileChannel.transferFrom(
                        new ContentDecoderChannel(decoder), idx, Long.MAX_VALUE);
            }

            if(transferred > 0)
                idx += transferred;
        }

        public void finished() {
            try {
                inputFile.close();
            } catch(IOException ignored) {}
            try {
                fileChannel.close();
            } catch(IOException ignored) {}
        }
    }

    static class EventLogger implements EventListener
    {
        public void connectionOpen(final NHttpConnection conn) 
        {
            logger.info(
                    "Connection from "
                    + conn.getContext().getAttribute(ExecutionContext.HTTP_TARGET_HOST) //conn.getInetAddress().getHostName()
                    );
        }

        public void connectionTimeout(final NHttpConnection conn) 
        {
            logger.info("Connection timed out: " + conn);
        }

        public void connectionClosed(final NHttpConnection conn) 
        {
            logger.info("Connection closed: " + conn);
        }

        public void fatalIOException(final IOException ex, final NHttpConnection conn) 
        {
            logger.info("I/O error: " + ex.getMessage());
        }

        public void fatalProtocolException(final HttpException ex, final NHttpConnection conn) 
        {
            logger.info("HTTP error: " + ex.getMessage());
        }
    }
}
