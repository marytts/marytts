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
package de.dfki.lt.mary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.htsengine.HMMVoice;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.UnitSelectionVoice;
import de.dfki.lt.mary.unitselection.interpolation.InterpolatingVoice;
import de.dfki.lt.mary.util.MaryAudioUtils;
import de.dfki.lt.signalproc.effects.BaseAudioEffect;
import de.dfki.lt.signalproc.effects.EffectsApplier;

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
        "TEXT_DE LOCALE=de INPUT" or "AUDIO OUTPUT".
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
 * @see RequestHandler
 * @author Marc Schr&ouml;der
 */

public class MaryServer {
    private ServerSocket server;
    private Logger logger;
    private int runningNumber = 1;
    private Map<Integer,Object[]> clientMap;

    public MaryServer() {
        logger = Logger.getLogger("server");
    }

    public void run() throws IOException, NoSuchPropertyException {
        logger.info("Starting server.");
        clientMap = Collections.synchronizedMap(new HashMap<Integer,Object[]>());
        server = new ServerSocket(MaryProperties.needInteger("socket.port"));

        while (true) {
            logger.info("Waiting for client to connect on port " + server.getLocalPort());
            Socket client = server.accept();
            logger.info(
                "Connection from "
                    + client.getInetAddress().getHostName()
                    + " ("
                    + client.getInetAddress().getHostAddress()
                    + ").");
            new ClientHandler(client).start();
        }
    }

    private synchronized int getID() {
        return runningNumber++;
    }

    public class ClientHandler extends Thread {
        Socket client;

        public ClientHandler(Socket client) {
            this.client = client;
        }

        public void run() {
            logger = Logger.getLogger("server");
            try {
                handle();
            } catch (Exception e) {
                logger.info("Error parsing request:", e);
                try {
                    PrintWriter outputWriter = new PrintWriter(client.getOutputStream(), true);
                    outputWriter.println("Error parsing request:");
                    outputWriter.println(e.getMessage());
                    outputWriter.close();
                    client.close();
                } catch (IOException ioe) {
                    logger.info("Cannot write to client.");
                }
            }
        }
        /**
         * Implement the protocol for communicating with a socket client.
         */
        private void handle() throws Exception {
            // !!!! reject all clients that are not from authorized domains?

            // Read one line from client
            BufferedReader buffReader = null;
            PrintWriter outputWriter = null;
            String line = null;
            buffReader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            outputWriter = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true);
            line = buffReader.readLine();
            logger.debug("read request: `"+line+"'");
            if (line == null) {
                logger.info("Client seems to have disconnected - cannot read.");
                return;
            }
            
            // A: General information request, no synthesis.
            // This may consist of one or several lines of info requests and
            // may either stand alone or precede another request.
            while (handleInfoRequest(line, outputWriter)) {
                // In case this precedes another request, try to read another line:
                line = buffReader.readLine();
                if (line == null)
                    return;
            }
           
            // VARIANT B1: Synthesis request.
            if (handleSynthesisRequest(line, outputWriter)) {
                return;
                // VARIANT B2: Second connection of synthesis request.
            } else if (handleNumberRequest(line, buffReader)) {
                return;
            } else {
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

        private boolean handleInfoRequest(String inputLine, PrintWriter outputWriter) {
            // Optional version information:
            if (inputLine.startsWith("MARY VERSION")) {
                logger.debug("InfoRequest " + inputLine);
                // Write version information to client.
                outputWriter.println("Mary TTS server " + Version.specificationVersion() + " (impl. " + Version.implementationVersion() + ")");
                // Empty line marks end of info:
                outputWriter.println();
                return true;
            } else if (inputLine.startsWith("MARY LIST DATATYPES")) {
                logger.debug("InfoRequest " + inputLine);
                // List all known datatypes
                Vector allTypes = MaryDataType.getDataTypes();
                for (Iterator it = allTypes.iterator(); it.hasNext();) {
                    MaryDataType t = (MaryDataType) it.next();
                    outputWriter.print(t.name());
                    if (t.getLocale() != null)
                        outputWriter.print(" LOCALE=" + t.getLocale());
                    if (t.isInputType())
                        outputWriter.print(" INPUT");
                    if (t.isOutputType())
                        outputWriter.print(" OUTPUT");
                    outputWriter.println();
                }
                // Empty line marks end of info:
                outputWriter.println();
                return true;
            } else if (inputLine.startsWith("MARY LIST VOICES")) {
                logger.debug("InfoRequest " + inputLine);
                // list all known voices
                Collection voices = Voice.getAvailableVoices();
                for (Iterator it = voices.iterator(); it.hasNext();) {
                    Voice v = (Voice) it.next();
                    if (v instanceof InterpolatingVoice) {
                        // do not list interpolating voice
                    } else if (v instanceof UnitSelectionVoice){
                        outputWriter.println(v.getName() + " " 
                                			+ v.getLocale() + " " 
                                			+ v.gender().toString() + " " 
                                			+ "unitselection" + " "
                                			+((UnitSelectionVoice)v).getDomain());}
                    else if (v instanceof HMMVoice)
                    {
                        	outputWriter.println(v.getName() + " " 
                        	        			+ v.getLocale()+ " " 
                        	        			+ v.gender().toString()+ " "
                        	        			+ "hmm");
                    }
                    else
                    {
                        outputWriter.println(v.getName() + " " 
                                            + v.getLocale()+ " " 
                                            + v.gender().toString() + " "
                                            + "other");
                    }
                }
                // Empty line marks end of info:
                outputWriter.println();
                return true;
            } else if (inputLine.startsWith("MARY LIST AUDIOFILEFORMATTYPES")) {
                logger.debug("InfoRequest " + inputLine);
                AudioFileFormat.Type[] audioTypes = AudioSystem.getAudioFileTypes();
                for (int t=0; t<audioTypes.length; t++) {
                    outputWriter.println(audioTypes[t].getExtension()+" "+audioTypes[t].toString());
                }
                // Empty line marks end of info:
                outputWriter.println();
                return true;
            } else if (inputLine.startsWith("MARY EXAMPLETEXT")) {
                logger.debug("InfoRequest " + inputLine);
                // send an example text for a given data type
                StringTokenizer st = new StringTokenizer(inputLine);
                // discard two tokens (MARY and EXAMPLETEXT)
                st.nextToken();
                st.nextToken();
                if (st.hasMoreTokens()) {
                    String typeName = st.nextToken();
                    try {
                        MaryDataType type = MaryDataType.get(typeName);
                        // if we get here, the type exists
                        assert type != null;
                        String exampleText = type.exampleText();
                        if (exampleText != null)
                            outputWriter.println(exampleText.trim());
                    } catch (Error err) {} // type doesn't exist
                }
                // upon failure, simply return nothing
                outputWriter.println();
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
                        String text = ((de.dfki.lt.mary.unitselection.UnitSelectionVoice) v).getExampleText();
                        outputWriter.println(text);
                    }
                }
                // upon failure, simply return nothing
                outputWriter.println();
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
                
                outputWriter.println(audioEffectClass);
                    
                // upon failure, simply return nothing
                outputWriter.println();
                return true;
            }
            else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTHELPTEXTLINEBREAK"))
            {
                logger.debug("InfoRequest " + inputLine);
                
                outputWriter.println(BaseAudioEffect.strLineBreak);
                
                // upon failure, simply return nothing
                outputWriter.println();
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
                            outputWriter.println(audioEffectParams.trim());
                        }
                     
                        // upon failure, simply return nothing
                        outputWriter.println();
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
                            outputWriter.println(audioEffectFull.trim());
                        }
                     
                        // upon failure, simply return nothing
                        outputWriter.println();
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
                            outputWriter.println(audioEffectParams);
                        }
                     
                        // upon failure, simply return nothing
                        outputWriter.println();
                        return true;
                    }
                }
                
                return false;
            }
            else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTHELPTEXT "))
            {
                int zz = MaryProperties.effectClasses().size();
                
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
                            outputWriter.println(helpText.trim());
                        }
                     
                        // upon failure, simply return nothing
                        outputWriter.println();
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
                            
                            outputWriter.println(strRet.trim());
                        }
                     
                        // upon failure, simply return nothing
                        outputWriter.println();
                        return true;
                    }
                }
                
                return false;
                
            }
            else
                return false;
        }

        private boolean handleSynthesisRequest(String inputLine, PrintWriter outputWriter) throws Exception {
            int id = 0;
            // * if MARY ..., then
            if (inputLine.startsWith("MARY")) {
                StringTokenizer t = new StringTokenizer(inputLine);
                String helper = null;
                MaryDataType inputType = null;
                MaryDataType outputType = null;
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
                        if (MaryDataType.exists(helper)) {
                            inputType = MaryDataType.get(helper);
                        } else {
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
                        if (MaryDataType.exists(helper)) {
                            outputType = MaryDataType.get(helper);
                        } else {
                            throw new Exception("Invalid output type: " + helper);
                        }
                    } else {
                        throw new Exception("Expected OUT=<OUTPUTTYPE>");
                    }
                } else { // OUT is required
                    throw new Exception("Expected OUT=<OUTPUTTYPE>");
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
                                && (inputType.getLocale() != null || outputType.getLocale() != null)) {
                                // Locale-specific interpretation of gender
                                Locale locale = inputType.getLocale();
                                if (locale == null)
                                    locale = outputType.getLocale();
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
                        Locale locale = inputType.getLocale();
                        if (locale == null)
                            locale = outputType.getLocale();
                        if (locale == null)
                            locale = Locale.GERMAN;
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

                Request request = new Request(inputType, outputType, voice, effects, style, id, audioFileFormat, streamingAudio);
                outputWriter.println(id);
                //   -- create new clientMap entry
                Object[] value = new Object[2];
                value[0] = client;
                value[1] = request;
                clientMap.put(id, value);
                return true;
            }
            return false;
        }

        private boolean handleNumberRequest(String inputLine, Reader reader)
            throws Exception {
            // * if number
            int id = 0;
            try {
                id = Integer.parseInt(inputLine);
            } catch (NumberFormatException e) {
                return false;
            }
            //   -- find corresponding infoSocket and request in clientMap
            Socket infoSocket = null;
            Request request = null;
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
                infoSocket = (Socket) value[0];
                request = (Request) value[1];
            }
            // Verify that the request is non-null and that the
            // corresponding socket comes from the same IP address:
            if (request == null
                || infoSocket == null
                || !infoSocket.getInetAddress().equals(client.getInetAddress())) {
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
            //   -- send off to new request
            RequestHandler rh = new RequestHandler(request, infoSocket, client, reader);
            rh.start();
            return true;
        }
    }
}
