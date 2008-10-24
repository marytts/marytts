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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.Version;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.NoSuchPropertyException;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.signalproc.effects.BaseAudioEffect;
import marytts.signalproc.effects.EffectsApplier;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.string.StringUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
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
    public static final String WEB_BROWSER_CLIENT_REQUEST_HEADER = "WEB_BROWSER_CLIENT";

    public MaryHttpServer() {
        logger = Logger.getLogger("server");
    }

    public void run() throws IOException, NoSuchPropertyException 
    {
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

    public class HttpClientHandler implements HttpRequestHandler  
    {           
        public HttpClientHandler() 
        {
            super();
            
            logger = Logger.getLogger("server");
        }

        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException 
        {
            Header[] tmp = request.getHeaders("Host");
            
            String clientAddress = getClientAddress(tmp[0].getValue().toString());
            
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            
            BufferedReader buffReader = null;
            String fullParameters = null;
            
            if (method.equals("GET") || method.equals("POST"))
            {   
                    fullParameters = request.getRequestLine().getUri().toString();
                    fullParameters = preprocess(fullParameters);
                    buffReader = new BufferedReader(new StringReader(fullParameters));
            }
            else
            {
                throw new MethodNotSupportedException(method + " method not supported"); 
            }

            //Parse request and create appropriate response
            try {
                handleClientRequest(buffReader, response, clientAddress);
            }
            catch (Exception e)
            {
                logger.info("Error parsing request:", e);

                String errorMessage = "";
                errorMessage += "Error parsing request:" + System.getProperty("line.separator");
                errorMessage += e.getMessage() + System.getProperty("line.separator");
                MaryHttpServerUtils.toResponse(errorMessage, response);
            }
        }
        
        private String getClientAddress(String fullHeader)
        {
            String clientAddress = fullHeader.trim();
            int index = clientAddress.indexOf('?');
            
            if (index>0)
                clientAddress = clientAddress.substring(0, index);
            
            return clientAddress;
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
            preprocessedParameters = StringUtils.replace(preprocessedParameters, "%20", " ");
            preprocessedParameters = StringUtils.replace(preprocessedParameters, "_HTTPREQUESTLINEBREAK_", System.getProperty("line.separator"));
            
            System.out.println("Preprocessed request: " + preprocessedParameters);
         
            return preprocessedParameters;
        }

        /**
         * Implement the protocol for communicating with an HTTP client.
         * This function is the modified version of MaryServer.ClientHandler.handle() for HTTP version
         *  
         */
        private void handleClientRequest(BufferedReader buffReader, HttpResponse response, String clientAddress) throws Exception 
        {                 
            // !!!! reject all clients that are not from authorized domains?

            // Read one line from client
            //BufferedReader buffReader = null;
            //PrintWriter outputWriter = null;
            String line = null;
            //buffReader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            //outputWriter = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true);
            line = buffReader.readLine();
            
            logger.debug("read request: `"+line+"'");
            boolean isWebFormClient = false;
            if (line == null) //This can be a web client asking for the default Mary Web client page
            {
                isWebFormClient = true;
                response.setStatusCode(HttpStatus.SC_OK);
                
                //Send the MARY Web Client page by first filling in fields appropriately 
                // using information from Mary server
                //String webClientHtmlFile = "D:/Oytun/DFKI/java/workspace/OpenMary/trunk/html/MaryWebClient.html";
                final String responsePage = "<html><body><h1>123456</h1></body></html>";

                MaryHttpServerUtils.toResponse(responsePage, response);

                return;
            }
            
            if (line.startsWith(WEB_BROWSER_CLIENT_REQUEST_HEADER))
            {
                isWebFormClient = true;
                line = line.substring(WEB_BROWSER_CLIENT_REQUEST_HEADER.length());
            }
            
            String outputLine = "";
            if (handleInfoRequest(line, response, isWebFormClient)) 
            {
                return;
            }
            else if (handleSynthesisRequest(line, buffReader, response, clientAddress)) //Synthesis request.
            {
                return;
            }  
            else 
            {
                // complain
                String nl = System.getProperty("line.separator");
                throw new Exception(
                    "Expected either a line"
                        + nl
                        + "MARY IN=<INPUTTYPE> OUT=<OUTPUTTYPE> [AUDIO=<AUDIOTYPE>]"
                        + nl
                        + "or a line containing only a number identifying a request.");
            }
        }

        private boolean handleInfoRequest(String inputLine, HttpResponse response, boolean isWebFormClient) throws IOException 
        {
            String output = "";
            // Optional version information:
            if (inputLine.startsWith("MARY VERSION")) 
            {
                logger.debug("InfoRequest " + inputLine);
                // Write version information to client.
                output += "Mary TTS server " + Version.specificationVersion() + " (impl. " + Version.implementationVersion() + ")";
                output += System.getProperty("line.separator");
                
                // Empty line marks end of info:
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true;
            } 
            else if (inputLine.startsWith("MARY LIST DATATYPES")) 
            {
                logger.debug("InfoRequest " + inputLine);
                // List all known datatypes
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
                // Empty line marks end of info:
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true;
            } 
            else if (inputLine.startsWith("MARY LIST VOICES")) 
            {
                logger.debug("InfoRequest " + inputLine);
                // list all known voices
                Collection voices = Voice.getAvailableVoices();
                for (Iterator it = voices.iterator(); it.hasNext();) 
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
                // Empty line marks end of info:
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true;
            } 
            else if (inputLine.startsWith("MARY LIST AUDIOFILEFORMATTYPES")) 
            {
                logger.debug("InfoRequest " + inputLine);
                AudioFileFormat.Type[] audioTypes = AudioSystem.getAudioFileTypes();
                for (int t=0; t<audioTypes.length; t++)
                    output += audioTypes[t].getExtension()+" "+audioTypes[t].toString() + System.getProperty("line.separator");
               
                // Empty line marks end of info:
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true;
            } 
            else if (inputLine.startsWith("MARY EXAMPLETEXT")) 
            {
                logger.debug("InfoRequest " + inputLine);
                // send an example text for a given data type
                StringTokenizer st = new StringTokenizer(inputLine);
                // discard two tokens (MARY and EXAMPLETEXT)
                st.nextToken();
                st.nextToken();
                if (st.hasMoreTokens()) 
                {
                    String typeName = st.nextToken();
                    try {
                        // next should be locale:
                        Locale locale = MaryUtils.string2locale(st.nextToken());
                        MaryDataType type = MaryDataType.get(typeName);
                        if (type != null) {
                            String exampleText = type.exampleText(locale);
                            if (exampleText != null)
                                output += exampleText.trim() + System.getProperty("line.separator");
                        }
                    } catch (Error err) {} // type doesn't exist
                }
                // upon failure, simply return nothing
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true;
            } 
            else if (inputLine.startsWith("MARY VOICE EXAMPLETEXT")) 
            { 
                //the request is about the example text of 
                //a limited domain unit selection voice

                logger.debug("InfoRequest " + inputLine);
                // send an example text for a given data type
                StringTokenizer st = new StringTokenizer(inputLine);
                // discard three tokens (MARY, VOICE, and EXAMPLETEXT)
                st.nextToken();
                st.nextToken();
                st.nextToken();
                if (st.hasMoreTokens()) {
                    String voiceName = st.nextToken();
                    Voice v = Voice.getVoice(voiceName);
                    if (v != null) {
                        String text = ((marytts.unitselection.UnitSelectionVoice) v).getExampleText();
                        output += text + System.getProperty("line.separator");
                    }
                }
                // upon failure, simply return nothing
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true; 
            }
            else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTS"))
            { 
                //the request is about the available audio effects
                logger.debug("InfoRequest " + inputLine);

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
                    audioEffectClass += "<Param>" + MaryProperties.effectParams().elementAt(i) + "</Param>";
                    audioEffectClass += "<SampleParam>" + MaryProperties.effectSampleParams().elementAt(i) + "</SampleParam>";
                    audioEffectClass += "<HelpText>" + MaryProperties.effectHelpTexts().elementAt(i) + "</HelpText>";
                    audioEffectClass += "</Effect>";
                }
                
                output += audioEffectClass + System.getProperty("line.separator");
                    
                // upon failure, simply return nothing
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true;
            }
            else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTHELPTEXTLINEBREAK"))
            {
                logger.debug("InfoRequest " + inputLine);
                
                output += BaseAudioEffect.strLineBreak + System.getProperty("line.separator");
                
                // upon failure, simply return nothing
                output += System.getProperty("line.separator");
                
                MaryHttpServerUtils.toResponse(output, response);
                
                return true;
            }
            else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTPARAM "))
            {
                for (int i=0; i<MaryProperties.effectNames().size(); i++)
                {
                    int tmpInd = inputLine.indexOf("MARY VOICE GETAUDIOEFFECTPARAM "+MaryProperties.effectNames().elementAt(i));
                    if (tmpInd>-1)
                    {   
                        //the request is about the parameters of a specific audio effect
                        logger.debug("InfoRequest " + inputLine);

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
                            String audioEffectParams = MaryProperties.effectParams().elementAt(i);
                            output += audioEffectParams.trim() + System.getProperty("line.separator");
                        }
                     
                        // upon failure, simply return nothing
                        output += System.getProperty("line.separator");
                        
                        MaryHttpServerUtils.toResponse(output, response);
                        
                        return true;
                    }
                }
                
                return false;
            }
            else if (inputLine.startsWith("MARY VOICE GETFULLAUDIOEFFECT "))
            {
                for (int i=0; i<MaryProperties.effectNames().size(); i++)
                {
                    int tmpInd = inputLine.indexOf("MARY VOICE GETFULLAUDIOEFFECT "+MaryProperties.effectNames().elementAt(i));
                    if (tmpInd>-1)
                    {   
                        //the request is about the parameters of a specific audio effect
                        logger.debug("InfoRequest " + inputLine);

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
                            ae.setParams(MaryProperties.effectParams().elementAt(i));
                            String audioEffectFull = ae.getFullEffectAsString();
                            output += audioEffectFull.trim() + System.getProperty("line.separator");
                        }
                     
                        // upon failure, simply return nothing
                        output += System.getProperty("line.separator");
                        
                        MaryHttpServerUtils.toResponse(output, response);
                        
                        return true;
                    }
                }
                
                return false;
            }
            else if (inputLine.startsWith("MARY VOICE SETAUDIOEFFECTPARAM "))
            {
                String effectName;
                for (int i=0; i<MaryProperties.effectNames().size(); i++)
                {
                    effectName = MaryProperties.effectNames().elementAt(i);
                    int tmpInd = inputLine.indexOf("MARY VOICE SETAUDIOEFFECTPARAM " + effectName);
                    if (tmpInd>-1)
                    {   
                        //the request is about changing the parameters of a specific audio effect
                        logger.debug("InfoRequest " + inputLine);
                        
                        int ind = inputLine.indexOf(effectName);
                        String strTmp = inputLine.substring(ind, inputLine.length());
                        int ind2 = strTmp.indexOf('_');
                        String strParamNew = strTmp.substring(ind2+1, strTmp.length());

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
                            ae.setParams(strParamNew);
                            String audioEffectParams = ae.getParamsAsString(false);
                            MaryProperties.effectParams().set(i, audioEffectParams);
                            output += audioEffectParams + System.getProperty("line.separator");
                        }
                     
                        // upon failure, simply return nothing
                        output += System.getProperty("line.separator");
                        
                        MaryHttpServerUtils.toResponse(output, response);
                        
                        return true;
                    }
                }
                
                return false;
            }
            else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTHELPTEXT "))
            {
                for (int i=0; i<MaryProperties.effectNames().size(); i++)
                {
                    int tmpInd = inputLine.indexOf("MARY VOICE GETAUDIOEFFECTHELPTEXT " + MaryProperties.effectNames().elementAt(i));
                    if (tmpInd>-1)
                    {   
                        //the request is about the parameters of a specific audio effect
                        logger.debug("InfoRequest " + inputLine);

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
                            output += helpText.trim() + System.getProperty("line.separator");
                        }
                     
                        // upon failure, simply return nothing
                        output += System.getProperty("line.separator");
                        
                        MaryHttpServerUtils.toResponse(output, response);
                        
                        return true;
                    }
                }
                
                return false;
            }
            else if (inputLine.startsWith("MARY VOICE ISHMMAUDIOEFFECT "))
            {
                for (int i=0; i<MaryProperties.effectNames().size(); i++)
                {
                    int tmpInd = inputLine.indexOf("MARY VOICE ISHMMAUDIOEFFECT " + MaryProperties.effectNames().elementAt(i));
                    if (tmpInd>-1)
                    {   
                        //the request is about the parameters of a specific audio effect
                        logger.debug("InfoRequest " + inputLine);

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
                            String strRet = "no";
                            
                            if (ae.isHMMEffect())
                                strRet = "yes";
                            
                            output += strRet.trim() + System.getProperty("line.separator");
                        }
                     
                        // upon failure, simply return nothing
                        output += System.getProperty("line.separator");
                        
                        MaryHttpServerUtils.toResponse(output, response);
                        
                        return true;
                    }
                }
                
                return false;
            }
            else
                return false;
        }

        private boolean handleSynthesisRequest(String inputLine, BufferedReader reader, HttpResponse response, String clientAddress) throws Exception 
        {
            String outputLine = null;
            int id = 0;
            // * if MARY ..., then
            if (inputLine.startsWith("MARY")) 
            {
                StringTokenizer t = new StringTokenizer(inputLine);
                String helper = null;
                MaryDataType inputType = null;
                MaryDataType outputType = null;
                Locale locale = null;
                Voice voice = null;
                String style = "";
                String effects = "";
               
                AudioFileFormat.Type audioFileFormatType = null;
                boolean streamingAudio = false;

                if (t.hasMoreTokens())
                    t.nextToken(); // discard MARY head
                // IN=
                if (t.hasMoreTokens()) {
                    String token = t.nextToken();
                    StringTokenizer tt = new StringTokenizer(token, "=");
                    if (tt.countTokens() == 2 && tt.nextToken().equals("IN")) {
                        // The value of IN=
                        helper = tt.nextToken(); // the input type
                        inputType = MaryDataType.get(helper);
                        if (inputType == null) {
                            throw new Exception("Invalid input type: " + helper);
                        }
                    } else {
                        throw new Exception("Expected IN=<INPUTTYPE>");
                    }
                } else { // IN is required
                    throw new Exception("Expected IN=<INPUTTYPE>");
                }
                // OUT=
                if (t.hasMoreTokens()) {
                    String token = t.nextToken();
                    StringTokenizer tt = new StringTokenizer(token, "=");
                    if (tt.countTokens() == 2 && tt.nextToken().equals("OUT")) {
                        // The value of OUT=
                        helper = tt.nextToken(); // the output type
                        outputType = MaryDataType.get(helper);
                        if (outputType == null) {
                            throw new Exception("Invalid output type: " + helper);
                        }
                    } else {
                        throw new Exception("Expected OUT=<OUTPUTTYPE>");
                    }
                } else { // OUT is required
                    throw new Exception("Expected OUT=<OUTPUTTYPE>");
                }
                // LOCALE=
                if (t.hasMoreTokens()) {
                    String token = t.nextToken();
                    StringTokenizer tt = new StringTokenizer(token, "=");
                    if (tt.countTokens() == 2 && tt.nextToken().equals("LOCALE")) {
                        // The value of LOCALE=
                        helper = tt.nextToken(); // the output type
                        locale = MaryUtils.string2locale(helper);
                    } else {
                        throw new Exception("Expected LOCALE=<locale>");
                    }
                } else { // LOCALE is required
                    throw new Exception("Expected LOCALE=<locale>");
                }
                if (t.hasMoreTokens()) {
                    String token = t.nextToken();
                    boolean tokenConsumed = false;
                    StringTokenizer tt = new StringTokenizer(token, "=");
                    // AUDIO (optional and ignored if output type != AUDIO)
                    if (tt.countTokens() == 2 && tt.nextToken().equals("AUDIO")) {
                        tokenConsumed = true;
                        if (outputType == MaryDataType.get("AUDIO")) {
                            // The value of AUDIO=
                            String typeString = tt.nextToken();
                            if (typeString.startsWith("STREAMING_")) {
                                streamingAudio = true;
                                audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(typeString.substring(10));
                            } else {
                                audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(typeString);
                            }
                        }
                    } else { // no AUDIO field
                        if (outputType == MaryDataType.get("AUDIO")) {
                            throw new Exception("Expected AUDIO=<AUDIOTYPE>");
                        }
                    }

                    if (tokenConsumed && t.hasMoreTokens()) {
                        token = t.nextToken();
                        tokenConsumed = false;
                    }
                    
                    // Optional VOICE field
                    if (!tokenConsumed) {
                        tt = new StringTokenizer(token, "=");
                        if (tt.countTokens() == 2 && tt.nextToken().equals("VOICE")) {
                            tokenConsumed = true;
                            // the values of VOICE=
                            String voiceName = tt.nextToken();
                            if ((voiceName.equals("male") || voiceName.equals("female"))
                                && locale != null) {
                                // Locale-specific interpretation of gender
                                voice = Voice.getVoice(locale, new Voice.Gender(voiceName));
                            } else {
                                // Plain old voice name
                                voice = Voice.getVoice(voiceName);
                            }
                            if (voice == null) {
                                throw new Exception(
                                    "No voice matches `"
                                        + voiceName
                                        + "'. Use a different voice name or remove VOICE= tag from request.");
                            }
                        }
                    }
                    if (voice == null) {
                        // no voice tag -- use locale default
                        voice = Voice.getDefaultVoice(locale);
                        logger.debug("No voice requested -- using default " + voice);
                    }
                    if (tokenConsumed && t.hasMoreTokens()) {
                        token = t.nextToken();
                        tokenConsumed = false;
                    }
                    
                    //Optional STYLE field
                    style = "";
                    if (!tokenConsumed) {
                        tt = new StringTokenizer(token, "=");
                        if (tt.countTokens()==2 && tt.nextToken().equals("STYLE")) {
                            tokenConsumed = true;
                            // the values of STYLE=
                            style = tt.nextToken();
                        }
                    }
                    if (style == "")
                        logger.debug("No style requested");
                    else
                        logger.debug("Style requested: " + style);
                    
                    if (tokenConsumed && t.hasMoreTokens()) {
                        token = t.nextToken();
                        tokenConsumed = false;
                    }
                    //
                    
                    //Optional EFFECTS field
                    effects = "";
                    if (!tokenConsumed) {
                        tt = new StringTokenizer(token, "=");
                        if (tt.countTokens()==2 && tt.nextToken().equals("EFFECTS")) {
                            tokenConsumed = true;
                            // the values of EFFECTS=
                            effects = tt.nextToken();
                        }
                    }
                    if (effects == "")
                        logger.debug("No audio effects requested");
                    else
                        logger.debug("Audio effects requested: " + effects);
                    
                    if (tokenConsumed && t.hasMoreTokens()) {
                        token = t.nextToken();
                        tokenConsumed = false;
                    }
                    //
                    
                    // Optional LOG field
                    // If present, the rest of the line counts as the value of LOG=
                    if (!tokenConsumed) {
                        tt = new StringTokenizer(token, "=");
                        if (tt.countTokens() >= 2 && tt.nextToken().equals("LOG")) {
                            tokenConsumed = true;
                            // the values of LOG=
                            helper = tt.nextToken();
                            // Rest of line:
                            while (t.hasMoreTokens())
                                helper = helper + " " + t.nextToken();
                            logger.info("Connection info: " + helper);
                        }
                    }
                }

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
                
                //   -- create new clientMap entry
                //Object[] value = new Object[2];
                //value[0] = clientAddress;
                //value[1] = request;
                //clientMap.put(id, value);
                
                //String output = String.valueOf(id) + System.getProperty("line.separator");
                //MaryHttpServerUtils.string2response(output, response);
                //outputLine = output;
                
                //inputLine = outputLine.trim();
                
                // * if number
                //int id = 0;
                //try {
                //    id = Integer.parseInt(inputLine);
                //} catch (NumberFormatException e) {
                //    return false;
                //}
                
                //   -- find corresponding infoSocket and request in clientMap
                //Socket infoSocket = null;
                //String infoSocketAddress = null;
                String infoSocketAddress = clientAddress;
                //RequestHttp request = null;
                
                /*
                // Wait up to TIMEOUT milliseconds for the first ClientHandler
                // to write its clientMap entry:
                long TIMEOUT = 1000;
                long startTime = System.currentTimeMillis();
                Object[] value = null;
                do {
                    Thread.yield();
                    value = (Object[]) clientMap.get(id);
                } while (value == null && System.currentTimeMillis() - startTime < TIMEOUT);
                if (value != null) {
                    infoSocketAddress = (String) value[0];
                    request = (RequestHttp) value[1];
                }
                // Verify that the request is non-null and that the
                // corresponding socket comes from the same IP address:
                if (request == null
                    || infoSocketAddress == null
                    || !infoSocketAddress.equals(clientAddress)) {
                    throw new Exception("Invalid identification number.");
                    // Don't be more specific, because in general it is none of
                    // their business whether in principle someone else has
                    // this id.
                }

                //   -- delete clientMap entry
                try {
                    clientMap.remove(id);
                } catch (UnsupportedOperationException e) {
                    logger.info("Cannot remove clientMap entry", e);
                }
                */
                
                Thread.yield();
                
                //   -- send off to new request
                RequestHttpHandler rh = new RequestHttpHandler(request, response, infoSocketAddress, reader);
                
                rh.start();
                
                rh.join();
                
                response = rh.response;
                
                return true;
            }
            
            return false;
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
