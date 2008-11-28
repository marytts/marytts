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
 *   <li> WAVE_FILE </li>
 *   <li> AU_FILE </li>
 *   <li> AIFF </li>
 *   <li> AIFC_FILE </li>
 *   <li> MP3_FILE </li>
 *   <li> Vorbis_FILE </li>
 *   <li> AU_STREAM</li>
 *   <li> MP3_STREAM</li>
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
