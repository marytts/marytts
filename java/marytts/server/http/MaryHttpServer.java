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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
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
import marytts.client.http.Address;
import marytts.client.http.MaryBaseClient;
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
import org.apache.http.message.BasicHttpResponse;
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
 * Listen for clients as an Http server at port
 *          <code>MaryProperties.socketPort()</code>.
 * <p>        
 * There are two types of clients that can be handled:
 * <p>
 * (1) Non-web browser clients
 * (2) Web browser clients
 * <p>
 * Note that non-web browser clients can mimic web browser clients by setting WEB_BROWSER_CLIENT parameter to "true"
 * in the Http request string
 * <p>
 * Clients can request the following (See below for more details):
 * <p>
 * (1) A file such as Mary icon or an audio file
 * (2) Information like available voices, example texts, available audio formats, etc
 * (3) Synthesis of an appropriate input with appropriate additional parameters
 * <p>
 * For all clients, the responses are always sent in an HttpResponses.
 * The entity in the response body can represent:
 * <p>
 * (1) An html page (applies only to web browser clients)
 * (2) Some binary data (such as bytes of Mary icon for file requests, or bytes of audio data for synthesis requests)
 * (3) Some piece of text
 * <p>
 * A valid Mary Http request string is a collection of individual key-value pairs combined in Http request sytle:
 * pair1&pair2&pair3... etc
 * <p>
 * Each pair has the following structure:
 * <p>
 * KEY=VALUE where
 * <p>
 * VALUE should start with a question mark (?) for querying the actual value corresponding to the KEY.
 * <p>
 * In addition, for some KEYs, VALUE may contain parameters separated by spaces after the question mark.
 * <p>
 * For example:
 * <p>
 * VOICE_EXAMPLE_TEXT=? hmm-slt 
 * <p>
 * returns the example text for voice "hmm-slt" from the server.
 * <p>
 * A list of KEYs for requesting information from the server are as follows:
 * <p>
 * VERSION (asks server version) 
 * <p>
 * DATA_TYPES (asks available data types - both input and output) 
 * <p>
 * VOICES (asks all available voices) 
 * <p>
 * AUDIO_FILE_FORMAT_TYPES (asks all supported audio formats) 
 * <p>
 * EXAMPLE_TEXT (asks example texts given a data type and locale
 * <p>
 * VOICE_EXAMPLE_TEXT (asks example texts given a voice) 
 * <p>
 * DEFAULT_AUDIO_EFFECTS (asks for the default audio effects set)
 * <p>
 * AUDIO_EFFECT_HELP_TEXT_LINE_BREAK (asks for the line break symbol used in audio effect help texts)
 * <p>
 * AUDIO_EFFECT_DEFAULT_PARAM (asks for the default parameters of an audio effect. Its parameter should be the effect name)
 * <p>
 * FULL_AUDIO_EFFECT (asks for a full audio effect - effect name + parameters and help texts)
 * <p>
 * AUDIO_EFFECT_HELP_TEXT (asks for the help text of an audio effect)
 * <p>
 * IS_HMM_AUDIO_EFFECT (asks if a given effect is only available for hmm voices
 *         
 * <p>
 * The following keys are used for passing additional information from server to client and/or vice versa:
 * <p>
 * INPUT_TYPE (input data type)
 * <p>
 * OUTPUT_TYPE (output data type)
 * <p>
 * AUDIO (audio format. It may include streaming/non-streaming information as well.
 *        Example values for non-streaming formats: AU_FILE, MP3_FILE, WAVE_FILE
 *        Example values for streaming formats: AU_STREAM, MP3_STREAM)
 * <p>
 * STYLE (Style descriptor)
 * <p>
 * INPUT_TEXT (Input text to be synthesised)
 * <p>
 * OUTPUT_TEXT (Output text - if the output type is not audio)
 * <p>
 * SYNTHESIS_OUTPUT (A key to ask for synthesis, or to represent synthesis result.
 *                   Example values: SYNTHESIS_OUTPUT=? instantiates a synthesis request
 *                                   In response, the server can set SYNTHESIS_OUTPUT to DONE, PENDING, or FAILED depending on the validity and type of te request
 *                                   PENDING is a special case used for handling double requests due to <EMBED< or <OBJECT> tags in web browser client html pages                           
 *     
 * <p>
 * Additionally, web browser clients should use the following key-value pair to tell the server about their type:
 * <p>
 * WEB_BROWSER_CLIENT=true (All other values will be interpreted as non-web browser client)
 * <p>
 * An easy way to test the http server is as follows:
 * <p>
 * (1) Run mary server in "http" mode by setting server=http in marybase.config
 * <p>
 * (2) Copy and paste the following to a web browser´s address bar:
 * <p>
 * http://localhost:59125/?INPUT_TYPE=TEXT&OUTPUT_TYPE=AUDIO&INPUT_TEXT=Welcome+to+the+world+of+speech+synthesis!&AUDIO=AU&SYNTHESIS_OUTPUT=%3F&LOCALE=en_US&VOICE=hsmm-slt
 * <p>
 * Provided that the server runs at localhost:59125 (or change "http://localhost:59125/" part as required), 
 * the web browser supports AUDIO type (if not try other formats such as WAVE, MP3, OGG or install a plug-in to play the target format),
 * and the VOICE is installed (hmm-slt), the synthesis result should be sent to the web browser for playback or saving (depending on web browser settings).
 * <p>
 * @see InfoRequestProcessor, FileRequestProcessor, SynthesisRequestProcessor, BaselineRequestProcessor, RequestHttp, MaryWebHttpClientHandler
 * @author Oytun T&uumlrk, Marc Schr&ouml;der 
 */

public class MaryHttpServer {
    private static Logger logger;
    private int runningNumber = 1;
    private Map<String,Object[]> requestMap;

    public MaryHttpServer() {
        logger = Logger.getLogger("server");
    }
    
    private synchronized String getResponseID() 
    {
        String id = "OUTPUT_AUDIO_RESPONSE_ID_" + String.valueOf(runningNumber);
        
        if (runningNumber<Integer.MAX_VALUE)
            runningNumber++;
        else
        {
            logger.debug("Resetting runningNumber in order not to exceed integer limits...");
            runningNumber = 1;
        }
        
        return id;
    }

    public void run() throws IOException, NoSuchPropertyException 
    {
        logger.info("Starting server.");
        requestMap = Collections.synchronizedMap(new HashMap<String, Object[]>());
        
        int localPort = MaryProperties.needInteger("socket.port");
        
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0) // 0 means no timeout, any positive value means time out in miliseconds (i.e. 50000 for 50 seconds)
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
        
        int numParallelThreads = MaryProperties.getInteger("server.http.parallelthreads");
        ListeningIOReactor ioReactor = new DefaultListeningIOReactor(numParallelThreads, params);
        
        logger.info("Waiting for client to connect on port " + localPort);
        
        try {
            ioReactor.listen(new InetSocketAddress(localPort));
            ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
            logger.info("Interrupted");
        } catch (IOException e) {
            logger.info("Cannot write to client.");
        }
        logger.debug("Shutdown");
    }

    public class HttpClientHandler extends SimpleNHttpRequestHandler implements HttpRequestHandler  
    {    
        private final boolean useFileChannels = true;
        
        private FileRequestProcessor fileRequestProcessor;
        private InfoRequestProcessor infoRequestProcessor;
        private SynthesisRequestProcessor synthesisRequestProcessor;
        
        public HttpClientHandler() 
        {
            super();
            
            logger = Logger.getLogger("server");
            
            fileRequestProcessor = new FileRequestProcessor();
            infoRequestProcessor = new InfoRequestProcessor();
            synthesisRequestProcessor = new SynthesisRequestProcessor();
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
            
            logger.debug("Preprocessed request: " + preprocessedParameters);
         
            return preprocessedParameters;
        }

        /**
         * Implement the protocol for communicating with an HTTP client.
         * The Http server receives parameters from a Http request (fullParameters)
         *                     and has to write the Http response inside (response)
         * In addition, the server address at client has to be specified
         * 
         * An HTTP request can be received from two basic types of clients:
         *   (1) A client that has its own code (i.e. a GUI client)
         *   (2) A client that requires its code to be generated at run-time by the server (i.e. a web browser client)
         * For case (2), more complicated processing is required since all client code has to be generated 
         * properly at run-time based on the request in addition to the appropriate "synthesis" response 
         * 
         * When the Http server receives a request from the client, the following cases are considered:
         *   CASE1: Check if a web browser client is asking for Mary icon (favicon.ico) and send it as a resource stream using FileRequestProcessor
         *  If CASE1 does not hold:
         *   CASE2: Check if a second-time connection is received from a web browser client about a previous request.
         *          This is required to handle <EMBED> and <OBJECT> tags that could be put in html pages.
         *          Such tags result in two requests being sent to the server:
         *          Request1: The server has to take parameters(i.e. request ID), and keep it until second request with this ID
         *                    !!!It´s important *not* to generate any response to this first request!!!
         *          Request2: The server produces the corresponding response corresponding to Request1         
         *  If CASE2 does not hold:  
         *    CASE3: Web browser client is asking for the default html page, i.e. when fullParameters is null or empty("") or it contains the key-value pair "DEFAULT_PAGE=?".   
         *           The server replies with an html page filled in with default client parameters using InforRequestProcessor.
         *  If CASE3 does not hold:
         *    CASE4: Either a synthesis or an information request is received.
         *      CASE4a: A synthesis request is received.
         *              The server calls SynthesisRequestProcessor to handle the request.
         *      If CASE4a does not hold
         *      CASE4b: An information request is received.
         *              The server calls InformationRequestProcessor to handle the request.
         *  If CASE4 does not old:
         *    CASE5: Invalid request. An error message is displayed in logger.
         *                
         * @throws Exception 
         */
        private void handleClientRequest(String fullParameters, HttpResponse response, Address serverAddressAtClient) throws Exception 
        {   
            if (fullParameters!=null && fullParameters.compareToIgnoreCase("favicon.ico")==0) //CASE1: Web browser client asking for Mary icon
            {
                fileRequestProcessor.sendResourceAsStream(fullParameters, response); //Put icon as a resource stream into HttpResponse

                return;
            }
            else if (fullParameters!=null && fullParameters.startsWith("OUTPUT_AUDIO_RESPONSE_ID_")) //CASE2: Check if second time connection received for a prvious request
            {
                String currentKey = StringUtils.getFileName(fullParameters);
                Object[] objects = new Object[3];
                objects = requestMap.get(currentKey);
                if (objects!=null)
                {
                    int numPrevCalls = (Integer)objects[0]; //Check how many times this request has been made

                    if (numPrevCalls==0) //This is the first call, increase the total number of calls to this request. Do *not* generate any response yet!
                    {
                        numPrevCalls++;
                        objects[0] = numPrevCalls;
                        requestMap.put(currentKey, objects);

                        return;
                    }
                    else if (numPrevCalls==1) //This is the second call, now it is time to generate the response
                    {
                        //Second synthesis request call for non-web browser clients
                        objects[0] = 0;
                        Address savedServerAddressAtClient = (Address)objects[1];
                        Map<String, String> savedKeyValuePairs = (Map<String, String>)objects[2];
                        synthesisRequestProcessor.process(savedServerAddressAtClient, savedKeyValuePairs, currentKey, response);
                        
                        requestMap.remove(currentKey);

                        return;
                    }
                    else //This should never be the case but remove the response as a precaution
                        requestMap.remove(currentKey);
                }

                return;
            }
            
            logger.debug("Read HTML form request: '" + fullParameters + "'");
            
            Map<String, String> keyValuePairs = MaryHttpClientUtils.toKeyValuePairs(fullParameters, false);
            
            boolean isDefaultPageRequested = false;
            if (keyValuePairs==null || (keyValuePairs.get("DEFAULT_PAGE")!=null && keyValuePairs.get("DEFAULT_PAGE").compareTo("?")==0))
                isDefaultPageRequested = true;
            
            if (isDefaultPageRequested) //CASE3: Web browser client is asking for the default html page
            {
                boolean isWebBrowserClient = false;
                if (keyValuePairs==null || (keyValuePairs.get("WEB_BROWSER_CLIENT")!=null && keyValuePairs.get("WEB_BROWSER_CLIENT").compareTo("true")==0))
                    isWebBrowserClient = true;
                
                if (isWebBrowserClient)
                    infoRequestProcessor.sendDefaultHtmlPage(serverAddressAtClient, response); //Respond with default html page
                
                return;
            }
            else //CASE4: Either a synthesis or an information request is received
            {
                String tmp = keyValuePairs.get("SYNTHESIS_OUTPUT");
                if (tmp!=null && tmp.compareTo("?")==0) //CASE4a: Synthesis request received
                {
                    //Audio streaming for web browser clients require special processing: 
                    // - First a response page should be created
                    // - That page should ask for embedded audio
                    // - Upon this second request, the request should be processed and the output should be streamed
                    // Here is the first call of the synthesis request which is common with non-web browser clients
                    // The second call is up above 
                    
                    String currentID = getResponseID();
                    synthesisRequestProcessor.process(serverAddressAtClient, keyValuePairs, currentID, response);

                    Object[] objects = new Object[3];
                    objects[0] = 0;
                    objects[1] = serverAddressAtClient;
                    objects[2] = keyValuePairs;

                    requestMap.put(currentID, objects);
                    
                    return;
                } 
                else //CASE4b: Information request is received
                {
                    boolean ok = infoRequestProcessor.process(serverAddressAtClient, keyValuePairs, response);
                    
                    if (!ok) //CASE5: Invalid request
                        logger.error("Error: Cannot process this request!");
                    
                    return;
                }
            }
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
