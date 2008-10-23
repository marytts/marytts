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
package marytts.client.http;

// General Java Classes
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import marytts.Version;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.BufferingHttpClientHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.HttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

/**
 * A socket client implementing the MARY protocol.
 * It can be used as a command line client or from within java code.
 * @author Marc Schr&ouml;der, oytun.turk
 * @see MaryGUIClient A GUI interface to this client
 * @see marytts.server.MaryServer Description of the MARY protocol
 */

public class MaryHttpClient {
    // Default values which can be overridden from the command line.
    private final String DEFAULT_HOST = "cling.dfki.uni-sb.de";
    private final int DEFAULT_PORT = 59125;
    private String host;
    private int port;
    private String serverVersion = "unknown";
    private boolean serverCanStream = false;
    private boolean doProfile = false;
    private boolean beQuiet = false;
    private Vector<MaryHttpClient.Voice> allVoices = null;
    private Map<Locale, Vector<MaryHttpClient.Voice>> voicesByLocaleMap = null;
    private Vector<MaryHttpClient.DataType> allDataTypes = null;
    private Vector<MaryHttpClient.DataType> inputDataTypes = null;
    private Vector<MaryHttpClient.DataType> outputDataTypes = null;
    private Map<String, String> serverExampleTexts = new HashMap<String, String>();
    private Map<String, String> voiceExampleTexts = new HashMap<String, String>();
    private String audioEffects;
    private Map<String, String> audioEffectHelpTextsMap = new HashMap<String, String>();
    private String[] audioFileFormatTypes = null;
    private String[] serverVersionInfo = null;

    private MaryHttpRequester maryHttpRequester;
    
    /**
     * The simplest way to create a mary client. It will connect to the
     * MARY server running at DFKI. Only use this for testing purposes!
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public MaryHttpClient() throws IOException
    {
        String serverHost = System.getProperty("server.host", DEFAULT_HOST);
        int serverPort = 0;
        String helperString = System.getProperty("server.port");
        if (helperString != null)
            serverPort = Integer.decode(helperString).intValue();
        else
            serverPort = DEFAULT_PORT;
        boolean profile = Boolean.getBoolean("mary.client.profile");
        boolean quiet = Boolean.getBoolean("mary.client.quiet");
        initialise(serverHost, serverPort, profile, quiet);
    }

    /**
     * The typical way to create a mary client. It will connect to the
     * MARY client running at the given host and port.
     * This constructor reads two system properties:
     * <ul>
     * <li><code>mary.client.profile</code> (=true/false) - 
     * determines whether profiling (timing) information is calculated;</li>
     * <li><code>mary.client.quiet</code> (=true/false) - 
     * tells the client not to print any of the normal information to stderr.</li>
     * </ul>
     * @param host the host where to contact a MARY server
     * @param port the socket port where to contact a MARY server
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public MaryHttpClient(String host, int port)
    throws IOException
    {
        boolean profile = Boolean.getBoolean("mary.client.profile");
        boolean quiet = Boolean.getBoolean("mary.client.quiet");
        initialise(host, port, profile, quiet);
    }

    /**
     * An alternative way to create a mary client, which works with applets.
     * It will connect to the MARY client running at the given host and port.
     * Note that in applets, the host must be the same as the one from which
     * the applet was loaded; otherwise, a security exception is thrown.
     * @param host the host where to contact a MARY server
     * @param port the socket port where to contact a MARY server
     * @param profile determines whether profiling (timing) information is calculated
     * @param quiet tells the client not to print any of the normal information to stderr
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public MaryHttpClient(String host, int port, boolean profile, boolean quiet)
    throws IOException
    {
        initialise(host, port, profile, quiet);
    }

    /**
     * Initialise a connection to the MARY server at the specified host and port.
     * @param serverHost the host where to contact a MARY server
     * @param serverPort the socket port where to contact a MARY server
     * @param profile whether to do profiling
     * @param quiet whether to refrain from printing information to stderr
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    private void initialise(String serverHost, int serverPort, boolean profile, boolean quiet) throws IOException 
    {
        maryHttpRequester = new MaryHttpRequester();
        
        // This must work for applets too, so no system property queries here:
        if (serverHost == null || serverPort == 0)
            throw new IllegalArgumentException("Illegal server host or port");
        this.host = serverHost;
        this.port = serverPort;
        doProfile = profile;
        beQuiet = quiet;
        String[] info;
        if (!beQuiet) {
            System.err.println("Mary TTS client " + Version.specificationVersion() + " (impl. " + Version.implementationVersion() + ")");
        }
        try {
            info = getServerVersionInfo();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("MARY client cannot connect to MARY server at\n"+
            serverHost+":"+serverPort+"\n"+
            "Make sure that you have started the mary server\n"+
            "or specify a different host or port using \n"+
            "maryclient -Dserver.host=my.host.com -Dserver.port=12345");
        }
        // Version number is, on the first line, the first token that starts with a digit:
        StringTokenizer st = new StringTokenizer(info[0]);
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if (t.matches("^[0-9].*")) {
                this.serverVersion = t;
                break;
            }
        }
        if (serverVersion.equals("unknown")
                || serverVersion.compareTo("3.0.1") < 0) {
            serverCanStream = false;
        } else {
            serverCanStream = true;
        }
        if (!beQuiet) {
            System.err.print("Connected to " + serverHost + ":" + serverPort + ", ");
            for (int i=0; i<info.length; i++) {
                System.err.println(info[i]);
            }
            if (!serverCanStream) {
                System.err.println("Server version " + serverVersion + " cannot stream audio, defaulting to non-streaming");
            }
        }
    }
    
    /**
     * Call the mary client to stream audio via the given audio player. The server will
     * provide audio data as it is being generated. If the connection to the server is
     * not too slow, streaming will be attractive because it reduces considerably the
     * amount of time one needs to wait for the first audio to play. 
     * @param input a textual representation of the input data 
     * @param inputType the name of the input data type, e.g. TEXT or RAWMARYXML.
     * @param audioType the name of the audio format, e.g. "WAVE" or "MP3".
     * @param defaultVoiceName the name of the voice to use, e.g. de7 or us1.
     * @param audioPlayer the FreeTTS audio player with which to play the synthesised audio data. The
     * given audio player must already be instanciated. See the package
     * <code>com.sun.speech.freetts.audio</code> in FreeTTS for implementations of AudioPlayer.
     * @param listener a means for letting calling code know that the AudioPlayer has finished.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     * @see #getInputDataTypes()
     * @see #getVoices()
     */
    public void streamAudio(String input, String inputType, String locale, String audioType, String defaultVoiceName, String defaultStyle, String defaultEffects, marytts.util.data.audio.AudioPlayer audioPlayer, AudioPlayerListener listener)
    throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, "AUDIO", locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, audioPlayer, 0, true, listener);
    }

    /**
     * The standard way to call the MARY client when the output is to
     * go to an output stream. 
     * @param input a textual representation of the input data 
     * @param inputType the name of the input data type, e.g. TEXT or RAWMARYXML. 
     * @param outputType the name of the output data type, e.g. AUDIO or ACOUSTPARAMS.
     * @param audioType the name of the audio format, e.g. "WAVE" or "MP3".
     * @param defaultVoiceName the name of the voice to use, e.g. de7 or us1.
     * @param audioEffects the audio effects and their parameters to be applied as a post-processing step, e.g. Robot(Amount=100), Whisper(amount=50)
     * @param output the output stream into which the data from the server is to be written.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     * @see #getInputDataTypes()
     * @see #getOutputDataTypes()
     * @see #getVoices()
     */
    public void process(String input, String inputType, String outputType, String locale,
        String audioType, String defaultVoiceName, String defaultStyle, String defaultEffects, OutputStream output)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, 0, false, null);
    }
    
    public void process(String input, String inputType, String outputType, String locale,
            String audioType, String defaultVoiceName, OutputStream output)
            throws UnknownHostException, IOException, Exception
    {
        process( input,  inputType,  outputType, locale, audioType,  defaultVoiceName,  "", "", output);
    }

    /**
     * An alternative way to call the MARY client when the output is to
     * go to an output stream, with a timeout. 
     * @param input a textual representation of the input data 
     * @param inputType the name of the input data type, e.g. TEXT or RAWMARYXML.
     * @param outputType the name of the output data type, e.g. AUDIO or ACOUSTPARAMS.
     * @param audioType the name of the audio format, e.g. "WAVE" or "MP3".
     * @param defaultVoiceName the name of the voice to use, e.g. de7 or us1.
     * @param audioEffects the audio effects and their parameters to be applied as a post-processing step, e.g. Robot(Amount=100), Whisper(amount=50)
     * @param output the output stream into which the data from the server is to be written.
     * @param timeout if >0, sets a timer to as many milliseconds; if processing is not finished by then,
     * the connection with the Mary server is forcefully cut, resulting in an IOException.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     * @see #getInputDataTypes()
     * @see #getOutputDataTypes()
     * @see #getVoices()
     */
    public void process(String input, String inputType, String outputType, String locale,
        String audioType, String defaultVoiceName, String defaultStyle, String defaultEffects, OutputStream output, long timeout)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, timeout, false, null);
    }

    public void process(String input, String inputType, String outputType, String locale,
         String audioType, String defaultVoiceName, OutputStream output, long timeout)
         throws UnknownHostException, IOException, Exception
    {
        process(input,  inputType, outputType, locale, audioType,  defaultVoiceName, "",  "", output, timeout);
    }
    
    
    /**
     * The easiest way to call the MARY client when the output is to
     * be played via a FreeTTS audio player. 
     * @param input a textual representation of the input data 
     * @param inputType the name of the input data type, e.g. TEXT or RAWMARYXML. See #getInputDataTypes().
     * @param defaultVoiceName the name of the voice to use, e.g. de7 or us1. See #getVoices().
     * @param audioEffects the audio effects and their parameters to be applied as a post-processing step, e.g. Robot(Amount=100), Whisper(amount=50)
     * @param player the FreeTTS audio player with which to play the synthesised audio data. The
     * given audio player must already be instanciated. See the package
     * <code>com.sun.speech.freetts.audio</code> in FreeTTS for implementations of AudioPlayer.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    @Deprecated
    public void process(String input, String inputType, String locale, String defaultVoiceName, String defaultStyle,  String defaultEffects, com.sun.speech.freetts.audio.AudioPlayer player)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, "AUDIO", locale, "AU", defaultVoiceName, defaultStyle, defaultEffects, player, 0, false, null);
    }
    
    @Deprecated
    public void process(String input, String inputType, String locale, String defaultVoiceName, com.sun.speech.freetts.audio.AudioPlayer player)
    throws UnknownHostException, IOException, Exception
    {
        process(input, inputType, locale, defaultVoiceName, "", "", player);
    }

    /**
     * An alternative way to call the MARY client when the output is to
     * be played via a FreeTTS audio player, with a timeout. 
     * @param input a textual representation of the input data 
     * @param inputType the name of the input data type, e.g. TEXT or RAWMARYXML.
     * @param defaultVoiceName the name of the voice to use, e.g. de7 or us1.
     * @param audioEffects the audio effects and their parameters to be applied as a post-processing step, e.g. Robot(Amount=100), Whisper(amount=50)
     * @param player the FreeTTS audio player with which to play the synthesised audio data. The
     * given audio player must already be instanciated. See the package
     * <code>com.sun.speech.freetts.audio</code> in FreeTTS for implementations of AudioPlayer.
     * @param timeout if >0, sets a timer to as many milliseconds; if processing is not finished by then,
     * the connection with the Mary server is forcefully cut, resulting in an IOException.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     * @see #getInputDataTypes()
     * @see #getVoices()
     */
    @Deprecated
    public void process(String input, String inputType, String locale, String defaultVoiceName, String defaultStyle, String defaultEffects, com.sun.speech.freetts.audio.AudioPlayer player, long timeout)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, "AUDIO", locale, "AU", defaultVoiceName, defaultStyle, defaultEffects, player, timeout, false, null);
    }

    @Deprecated
    public void process(String input, String inputType, String locale, String defaultVoiceName, com.sun.speech.freetts.audio.AudioPlayer player, long timeout)
    throws UnknownHostException, IOException, Exception
    {
        process( input, inputType, locale, defaultVoiceName, "", "", player, timeout);
    }

    private void _process(String input, String inputType, String outputType, String locale, String audioType, 
            String defaultVoiceName, String defaultStyle, String defaultEffects, 
            Object output, long timeout, boolean streamingAudio, AudioPlayerListener playerListener)
        throws UnknownHostException, IOException, Exception
    {
        boolean isFreettsAudioPlayer = false;
        boolean isMaryAudioPlayer = false;
        if (output instanceof com.sun.speech.freetts.audio.AudioPlayer) {
            isFreettsAudioPlayer = true;
        } else if (output instanceof marytts.util.data.audio.AudioPlayer) {
            isMaryAudioPlayer = true;
        } else if (output instanceof OutputStream) {
        } else {
            throw new IllegalArgumentException("Expected OutputStream or AudioPlayer, got " + output.getClass().getName());
        }
        final long startTime = System.currentTimeMillis();
        // Socket Client
        /*
        final Socket maryInfoSocket;
        try {
            maryInfoSocket = new Socket(host, port);
        } catch (SocketException se) {
            throw new RuntimeException("Cannot connect to " + host + ":" + port, se);
        }
        final PrintWriter toServerInfo = new PrintWriter(new OutputStreamWriter(maryInfoSocket.getOutputStream(), "UTF-8"), true);
        final BufferedReader fromServerInfo = new BufferedReader(new InputStreamReader(maryInfoSocket.getInputStream(), "UTF-8"));
        */
        
        // Formulate Request to Server:
        //System.err.println("Writing request to server.");
        //String toServerInfo;
        String toServer = "";
        //toServerInfo.print("MARY IN=" + inputType + " OUT=" + outputType + " LOCALE="+ locale);
        //toServerInfo = "MARY IN=" + inputType + " OUT=" + outputType + " LOCALE="+ locale;
        toServer += "MARY IN=" + inputType + " OUT=" + outputType + " LOCALE="+ locale;
        if (audioType != null) {
            if (streamingAudio && serverCanStream) {
                //toServerInfo.print(" AUDIO=STREAMING_"+audioType);
                //toServerInfo += " AUDIO=STREAMING_"+audioType;
                toServer += " AUDIO=STREAMING_"+audioType;
            } else {
                //toServerInfo.print(" AUDIO=" + audioType);
                //toServerInfo += " AUDIO=" + audioType;
                toServer += " AUDIO=" + audioType;
            }
        }
        if (defaultVoiceName != null) {
            //toServerInfo.print(" VOICE=" + defaultVoiceName);
            //toServerInfo += " VOICE=" + defaultVoiceName;
            toServer += " VOICE=" + defaultVoiceName;
        }
        
        if (defaultStyle != "") {
            //toServerInfo.print(" STYLE=" + defaultStyle);
            //toServerInfo += " STYLE=" + defaultStyle;
            toServer += " STYLE=" + defaultStyle;
        }
        
        if (defaultEffects != "") {
            //toServerInfo.print(" EFFECTS=" + defaultEffects);
            //toServerInfo += " EFFECTS=" + defaultEffects;
            toServer += " EFFECTS=" + defaultEffects;
        }
        //toServerInfo.println();
        //toServerInfo += System.getProperty("line.separator");
        toServer += System.getProperty("line.separator");
        
        //String[] fromServerInfo = maryHttpRequester.requestMultipleLines(host, port, toServerInfo);
        
        // Receive a request ID:
        //System.err.println("Reading reply from server.");
        //String helper = fromServerInfo.readLine();
        
        /*
        int lineCount = 0;
        String helper = fromServerInfo[lineCount++];
        
        //System.err.println("Read from Server: " + helper);
        int id = -1;
        try {
            id = Integer.parseInt(helper);
        } catch (NumberFormatException e) {
            // Whatever we read from the server, it was not a number
            StringBuffer message = new StringBuffer("Server replied:\n");
            message.append(helper);
            message.append("\n");
            
            //while ((helper = fromServerInfo.readLine()) != null) {
            //    message.append(helper);
            //    message.append("\n");
            //}
            
            
            for (int i=lineCount; i<fromServerInfo.length; i++)
            {
                helper = fromServerInfo[i];
                message.append(helper);
                message.append("\n");
            }
            
            throw new IOException(message.toString());
        }
        */

        //System.err.println("Read id " + id + " from server.");
        //final Socket maryDataSocket = new Socket(host, port);
        //System.err.println("Created second socket.");
        //final PrintWriter toServerData = new PrintWriter(new OutputStreamWriter(maryDataSocket.getOutputStream(), "UTF-8"), true);
        //toServerData.println(id);
        //String toServerData = String.valueOf(id) + System.getProperty("line.separator");
        //toServer += String.valueOf(id) + System.getProperty("line.separator");
        
        //System.err.println("Writing to server:");
        //System.err.print(input);
        //toServerData.println(input.trim());
        //toServerData += input.trim() + System.getProperty("line.separator");
        toServer += input.trim() + System.getProperty("line.separator");
        //maryDataSocket.shutdownOutput();
        
        //final InputStream fromServerStream = maryHttpRequester.requestInputStream(host, port, toServerData);
        final InputStream fromServerStream = maryHttpRequester.requestInputStream(host, port, toServer);
        
        //final InputStream fromServerStream = null;
        //int[] xx = maryHttpRequester.requestIntArray(host, port, toServer);
        //System.out.println(xx.toString());
        
        // Check for warnings from server:
        //final WarningReader warningReader = new WarningReader(fromServerInfo);
        //warningReader.start();
        
        // Read from Server and copy into OutputStream output:
        // (as we only do low-level copying of bytes here,
        //  we do not need to distinguish between text and audio)
       // final InputStream fromServerStream = maryDataSocket.getInputStream();

        // If timeout is > 0, create a timer. It will close the input stream,
        // thus causing an IOException in the reading code.
        final Timer timer;
        if (timeout <= 0) {
            timer = null;
        } else {
            timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                public void run() {
                    System.err.println("Timer closes connection");
                    /*
                    try {
                        maryDataSocket.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    */
                }
            };
            timer.schedule(timerTask, timeout);
        }



        if (isFreettsAudioPlayer) 
        {
            final com.sun.speech.freetts.audio.AudioPlayer player = (com.sun.speech.freetts.audio.AudioPlayer) output;
            final AudioPlayerListener listener = playerListener;
            Thread t = new Thread() {
                public void run() 
                {
                    try {
                        AudioPlayerWriter apw = new AudioPlayerWriter(player, fromServerStream, startTime);
                        apw.write();
                        
                        if (timer != null)
                            timer.cancel();
                        
                        if (listener != null) listener.playerFinished();

                        //toServerInfo.close();
                        //fromServerInfo.close();
                        //maryInfoSocket.close();
                        //toServerData.close();
                        //maryDataSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }                    

                    //try {
                    //    warningReader.join();
                    //} catch (InterruptedException ie) {}
                    //if (warningReader.getWarnings().length() > 0) { // there are warnings
                    //    String warnings = warningReader.getWarnings(); 
                    //    System.err.println(warnings);
                    //    if (listener != null) 
                    //        listener.playerException(new IOException(warnings));
                    //}

                    if (doProfile) 
                    {
                        long endTime = System.currentTimeMillis();
                        long processingTime = endTime - startTime;
                        System.err.println("Processed request in " + processingTime + " ms.");
                    }
                }
            };
            if (streamingAudio) {
                t.start();
            } else {
                
                t.run(); // execute code in the current thread
                
            }
        } 
        else if (isMaryAudioPlayer) 
        {
            final marytts.util.data.audio.AudioPlayer player = (marytts.util.data.audio.AudioPlayer) output;
            final AudioPlayerListener listener = playerListener;
            Thread t = new Thread() {
                public void run() 
                {
                    try {
                        InputStream in = fromServerStream;
                        if (doProfile)
                            System.err.println("After "+(System.currentTimeMillis()-startTime)+" ms: Trying to read data from server");
                        while (false && in.available() < 46) { // at least the audio header should be there
                            Thread.yield();
                        }
                        if (doProfile)
                            System.err.println("After "+(System.currentTimeMillis()-startTime)+" ms: Got at least the header");
                        in = new BufferedInputStream(in);
                        in.mark(1000);
                         AudioInputStream fromServerAudio = AudioSystem.getAudioInputStream(in);
                         if (fromServerAudio.getFrameLength() == 0) { // weird bug under Java 1.4
                             //in.reset();
                             fromServerAudio = new AudioInputStream(in, fromServerAudio.getFormat(), AudioSystem.NOT_SPECIFIED);
                         }
                         //System.out.println("Audio framelength: "+fromServerAudio.getFrameLength());
                         //System.out.println("Audio frame size: "+fromServerAudio.getFormat().getFrameSize());
                         //System.out.println("Audio format: "+fromServerAudio.getFormat());
                         if (doProfile)
                             System.err.println("After "+(System.currentTimeMillis()-startTime)+" ms: Audio available: "+in.available());
                         AudioFormat audioFormat = fromServerAudio.getFormat();
                         if (!audioFormat.getEncoding().equals(Encoding.PCM_SIGNED)) { // need conversion, e.g. for mp3
                             audioFormat = new AudioFormat(fromServerAudio.getFormat().getSampleRate(), 16, 1, true, false);
                             fromServerAudio = AudioSystem.getAudioInputStream(audioFormat, fromServerAudio);
                         }
                        player.setAudio(fromServerAudio);
                        player.run(); // not start(), i.e. execute in this thread
                        
                        if (timer != null)
                            timer.cancel();
                        
                        if (listener != null) 
                            listener.playerFinished();

                        //toServerInfo.close();
                        //fromServerInfo.close();
                        //maryInfoSocket.close();
                        //toServerData.close();
                        //maryDataSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }                    

                    //try {
                    //    warningReader.join();
                    //} catch (InterruptedException ie) {}
                    //if (warningReader.getWarnings().length() > 0) { // there are warnings
                    //    String warnings = warningReader.getWarnings(); 
                    //    System.err.println(warnings);
                    //    if (listener != null) listener.playerException(new IOException(warnings));
                    //}

                    if (doProfile) 
                    {
                        long endTime = System.currentTimeMillis();
                        long processingTime = endTime - startTime;
                        System.err.println("Processed request in " + processingTime + " ms.");
                    }
                }
            };
            
            if (streamingAudio)
                t.start();
            else
                t.run(); // execute code in the current thread
        } 
        else // output is an OutputStream
        { 
            OutputStream os = (OutputStream) output;
            InputStream bis = new BufferedInputStream(fromServerStream);
            byte[] bbuf = new byte[1024];
            int nr;
            while ((nr = bis.read(bbuf, 0, bbuf.length)) != -1) 
            {
                //System.err.println("Read " + nr + " bytes from server.");
                os.write(bbuf, 0, nr);
            }
            os.flush();
            
            if (timeout > 0)
                timer.cancel();
            
            //toServerInfo.close();
            //fromServerInfo.close();
            //maryInfoSocket.close();
            //toServerData.close();
            //maryDataSocket.close();
            
            //try {
            //    warningReader.join();
            //} catch (InterruptedException ie) {}
            //if (warningReader.getWarnings().length() > 0) // there are warnings
            //    throw new IOException(warningReader.getWarnings());

            if (doProfile) 
            {
                long endTime = System.currentTimeMillis();
                long processingTime = endTime - startTime;
                System.err.println("Processed request in " + processingTime + " ms.");
            }
        }
    }

    /** get the server host to which this client connects */
    public String getHost() { return host; }
    /** get the server port to which this client connects */
    public int getPort() { return port; }

    /**
     * From an open server connection, read one chunk of info data. Writes the 
     * infoCommand to the server, then reads from the server until an empty line
     * or eof is read.
     * @param toServer
     * @param fromServer
     * @param infoCommand the one-line request to send to the server
     * @return a string representing the server response, lines being separated by a '\n' character.
     * @throws IOException if communication with the server fails
     */
    private String getServerInfo(PrintWriter toServerInfo, BufferedReader fromServerInfo, String infoCommand)
    throws IOException {
        toServerInfo.println(infoCommand);
        StringBuffer result = new StringBuffer();
        String line = null;
        // Read until either end of file or an empty line
        while((line = fromServerInfo.readLine()) != null && !line.equals("")) {
            result.append(line);
            result.append("\n");
        }
        return result.toString();
    }

    /**
     * From an open server connection, read one chunk of info data. Writes the 
     * infoCommand to the server, then reads from the server until an empty line
     * or eof is read.
     * @param toServer
     * @param fromServer
     * @param infoCommand the one-line request to send to the server
     * @return an array of Strings representing the server response, one string for one line
     * @throws IOException if communication with the server fails
     */
    private String[] getServerInfoLines(PrintWriter toServerInfo, BufferedReader fromServerInfo, String infoCommand)
    throws IOException {
        toServerInfo.println(infoCommand);
        Vector<String> result = new Vector<String>();
        String line = null;
        // Read until either end of file or an empty line
        while((line = fromServerInfo.readLine()) != null && !line.equals("")) {
            result.add(line);
        }
        return result.toArray(new String[0]);
    }


    /**
     * Get the version info from the server. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a string array, each String representing one line of info
     * without the trailing newline character.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public String[] getServerVersionInfo() throws Exception
    {
        if (serverVersionInfo == null) 
        {
            /*
            Socket marySocket = new Socket(host, port);
            // Expect 3 lines of the kind
            // Mary TTS server
            // Specification version 1.9.1
            // Implementation version 20030207
            String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY VERSION");
            if (info.length() == 0)
                throw new IOException("Could not get version info from Mary server");
            serverVersionInfo = info.split("\n");
            marySocket.close();
            */
            
            // Expect 3 lines of the kind
            // Mary TTS server
            // Specification version 1.9.1
            // Implementation version 20030207
            serverVersionInfo = maryHttpRequester.requestStringArray(host, port, "MARY VERSION");  
        }
        
        return serverVersionInfo;
    }
    
    public String getServerVersionNo()
    {
        String serverVersionNo = "";
        String[] serverInfo = null;
        try {
            serverInfo = getServerVersionInfo();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        if (serverInfo!=null)
        {
            int startIndex = 0;
            int endIndex = serverInfo[0].length();
            int dotIndex = serverInfo[0].indexOf('.');
            if (dotIndex>-1)
            {
                int spaceIndex1 = serverInfo[0].lastIndexOf(' ', dotIndex);
                if (spaceIndex1<0)
                    spaceIndex1=-1;
                int spaceIndex2 = serverInfo[0].indexOf(' ', dotIndex);
                if (spaceIndex2<0)
                    spaceIndex2=serverInfo[0].length();
                
                startIndex = spaceIndex1+1;
                endIndex = spaceIndex2;
            }
            
            serverVersionNo = serverInfo[0].substring(startIndex, endIndex);
        }
        else
            serverVersionNo = "";
        
        return serverVersionNo;
    }
    
    public boolean isServerNotOlderThan(String serverVersionToCompare)
    {
        String currentServer = getServerVersionNo();
        int tmp = currentServer.compareToIgnoreCase(serverVersionToCompare);
        
        if (tmp>=0)
            return true;
        else
            return false;
    }

    /**
     * Obtain a list of all data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryHttpClient.DataType objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.DataType> getAllDataTypes() throws Exception
    {
        if (allDataTypes == null)
            fillDataTypes();
        
        assert allDataTypes != null && allDataTypes.size() > 0;
        
        return allDataTypes;
    }

    /**
     * Obtain a list of input data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryHttpClient.DataType objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.DataType> getInputDataTypes() throws Exception
    {
        if (inputDataTypes == null)
            fillDataTypes();
       
        assert inputDataTypes != null && inputDataTypes.size() > 0;
        
        return inputDataTypes;
    }

    /**
     * Obtain a list of output data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryHttpClient.DataType objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.DataType> getOutputDataTypes() throws Exception
    {
        if (outputDataTypes == null)
            fillDataTypes();
       
        assert outputDataTypes != null && outputDataTypes.size() > 0;
        
        return outputDataTypes;
    }

    private void fillDataTypes() throws Exception 
    {
        allDataTypes = new Vector<MaryHttpClient.DataType>();
        inputDataTypes = new Vector<MaryHttpClient.DataType>();
        outputDataTypes = new Vector<MaryHttpClient.DataType>();
        
        //Socket marySocket = new Socket(host, port);
        // Expect a variable number of lines of the kind
        // RAWMARYXML INPUT OUTPUT
        // TEXT_DE LOCALE=de INPUT
        // AUDIO OUTPUT
        String info;
        /*
        info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
            new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
            "MARY LIST DATATYPES");
            */
        info = maryHttpRequester.requestString(host, port, "MARY LIST DATATYPES");
        
        if (info.length() == 0)
            throw new IOException("Could not get list of data types from Mary server");
        String[] typeStrings = info.split("\n");
        //marySocket.close();
        for (int i=0; i<typeStrings.length; i++) 
        {
            StringTokenizer st = new StringTokenizer(typeStrings[i]);
            if (!st.hasMoreTokens()) continue; // ignore this type
            String name = st.nextToken();
            boolean isInputType = false;
            boolean isOutputType = false;
            Locale locale = null;
            while (st.hasMoreTokens()) 
            {
                String t = st.nextToken();
                if (t.equals("INPUT")) {
                    isInputType = true;
                } else if (t.equals("OUTPUT")) {
                    isOutputType = true;
                }
            }
            DataType dt = new DataType(name, isInputType, isOutputType);
            allDataTypes.add(dt);
            if (dt.isInputType()) {
                inputDataTypes.add(dt);
            }
            if (dt.isOutputType()) {
                outputDataTypes.add(dt);
            }
        }
    }

    /**
     * Provide a list of voices known to the server. If the information is not yet
     * available, query the server for it. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryHttpClient.Voice objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.Voice> getVoices() throws Exception
    {
        if (allVoices == null) {
            fillVoices();
        }
        assert allVoices != null && allVoices.size() > 0;
        return allVoices;
    }
    
    /**
     * Provide a list of voices known to the server for the given locale.
     * If the information is not yet available, query the server for it.
     * This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @param locale the requested voice locale
     * @return a Vector of MaryHttpClient.Voice objects, or null if no voices exist for
     * that locale.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.Voice> getVoices(Locale locale) throws Exception
    {
        if (allVoices == null)
            fillVoices();

        return voicesByLocaleMap.get(locale);
    }

    /**
     * Provide a list of general domain voices known to the server.
     * If the information is not yet available, query the server for it.
     * This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.Voice> getGeneralDomainVoices() throws Exception
    {
        Vector<MaryHttpClient.Voice> voices = getVoices();
        Vector<MaryHttpClient.Voice> requestedVoices = new Vector<MaryHttpClient.Voice>();
        
        for (Voice v: voices) 
        {
            if (!v.isLimitedDomain())
                requestedVoices.add(v);
        }
        
        if (!requestedVoices.isEmpty())
            return requestedVoices;
        else
            return null;
    }

    /**
     * Provide a list of limited domain voices known to the server.
     * If the information is not yet available, query the server for it.
     * This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.Voice> getLimitedDomainVoices() throws Exception
    {
        Vector<MaryHttpClient.Voice> voices = getVoices();
        Vector<MaryHttpClient.Voice> requestedVoices = new Vector<MaryHttpClient.Voice>();
        
        for (Voice v : voices) 
        {
            if (v.isLimitedDomain())
                requestedVoices.add(v);
        }
        
        if (!requestedVoices.isEmpty())
            return requestedVoices;
        else
            return null;
    }

    /**
     * Provide a list of general domain voices known to the server.
     * If the information is not yet available, query the server for it.
     * This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @param locale the requested voice locale
     * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.Voice> getGeneralDomainVoices(Locale locale) throws Exception
    {
        Vector<MaryHttpClient.Voice> voices = getVoices(locale);
        Vector<MaryHttpClient.Voice> requestedVoices = new Vector<MaryHttpClient.Voice>();
        
        for (Voice v : voices) 
        {
            if (!v.isLimitedDomain())
                requestedVoices.add(v);
        }
        
        if (!requestedVoices.isEmpty())
            return requestedVoices;
        else
            return null;
    }

    /**
     * Provide a list of limited domain voices known to the server.
     * If the information is not yet available, query the server for it.
     * This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @param locale the requested voice locale
     * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector<MaryHttpClient.Voice> getLimitedDomainVoices(Locale locale) throws Exception
    {
        Vector<MaryHttpClient.Voice> voices = getVoices(locale);
        Vector<MaryHttpClient.Voice> requestedVoices = new Vector<MaryHttpClient.Voice>();
        for (Voice v : voices) 
        {
            if (v.isLimitedDomain())
                requestedVoices.add(v);
        }
        
        if (!requestedVoices.isEmpty())
            return requestedVoices;
        else
            return null;
    }

    private void fillVoices() throws Exception
    {
        allVoices = new Vector<MaryHttpClient.Voice>();
        voicesByLocaleMap = new HashMap<Locale,Vector<MaryHttpClient.Voice>>();
        //Socket marySocket = new Socket(host, port);
        // Expect a variable number of lines of the kind
        // de7 de female
        // us2 en male
        // dfki-stadium-emo de male limited
        String info;
        /* 
         info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
            new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
            "MARY LIST VOICES");
            */
        info = maryHttpRequester.requestString(host, port, "MARY LIST VOICES");
        
        if (info.length() == 0)
            throw new IOException("Could not get voice list from Mary server");
        String[] voiceStrings = info.split("\n");
        //marySocket.close();
        for (int i=0; i<voiceStrings.length; i++) {
            StringTokenizer st = new StringTokenizer(voiceStrings[i]);
            if (!st.hasMoreTokens()) continue; // ignore entry
            String name = st.nextToken();
            if (!st.hasMoreTokens()) continue; // ignore entry
            String localeString = st.nextToken();
            Locale locale = string2locale(localeString);
            assert locale != null;
            if (!st.hasMoreTokens()) continue; // ignore entry
            String gender = st.nextToken();
            
            Voice voice = null;
            if (isServerNotOlderThan("3.5.0"))
            {
                String synthesizerType;
                if (!st.hasMoreTokens())
                    synthesizerType = "non-specified";
                else
                    synthesizerType = st.nextToken();

                if (!st.hasMoreTokens())
                { 
                    //assume domain is general
                    voice = new Voice(name, locale, gender, "general");
                }
                else
                { 
                    //read in the domain
                    String domain = st.nextToken();
                    voice = new Voice(name, locale, gender, domain);
                }

                voice.setSynthesizerType(synthesizerType);
            }
            else
            {
                if (!st.hasMoreTokens())
                { 
                    //assume domain is general
                    voice = new Voice(name, locale, gender, "general");
                }
                else
                { 
                    //read in the domain
                    String domain = st.nextToken();
                    voice = new Voice(name, locale, gender, domain);
                }
            }
            
            allVoices.add(voice);
            Vector<MaryHttpClient.Voice> localeVoices = null;
            if (voicesByLocaleMap.containsKey(locale)) {
                localeVoices = voicesByLocaleMap.get(locale);
            } else {
                localeVoices = new Vector<MaryHttpClient.Voice>();
                voicesByLocaleMap.put(locale, localeVoices);
            }
            localeVoices.add(voice);
        }
    }

    /**
     * Request the example text of a limited domain
     * unit selection voice from the server
     * @param voicename the voice
     * @return the example text
     * @throws IOException
     * @throws UnknownHostException
     */
    public String getVoiceExampleText(String voicename) throws Exception 
    {
        if (!voiceExampleTexts.containsKey(voicename)) 
        {
            /*
            Socket marySocket = new Socket(host, port);
            String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                    new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                    "MARY VOICE EXAMPLETEXT " + voicename);
                    */
            String info = maryHttpRequester.requestString(host, port, "MARY VOICE EXAMPLETEXT " + voicename);
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");

            voiceExampleTexts.put(voicename, info.replaceAll("\n", System.getProperty("line.separator")));
            //marySocket.close();
        }

        return voiceExampleTexts.get(voicename);
    }
    
    /**
     * Request an example text for a given data type from the server.
     * @param dataType the string representation of the data type,
     * e.g. "RAWMARYXML". This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return the example text, or null if none could be obtained.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public String getServerExampleText(String dataType, String locale) throws Exception
    {
        if (!serverExampleTexts.containsKey(dataType+" "+locale)) 
        {
            /*
            Socket marySocket = new Socket(host, port);
            String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY EXAMPLETEXT " + dataType + " " + locale);
                */
            String info = maryHttpRequester.requestString(host, port, "MARY EXAMPLETEXT " + dataType + " " + locale);
            
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");
            serverExampleTexts.put(dataType+" "+locale, info.replaceAll("\n", System.getProperty("line.separator")));
            
            //marySocket.close();
        }
        
        return serverExampleTexts.get(dataType+" "+locale);
    }

    /**
     * Request the available audio effects for a voice from the server
     * @param voicename the voice
     * @return A string of available audio effects and default parameters, i.e. "FIRFilter,Robot(amount=50)"
     * @throws IOException
     * @throws UnknownHostException
     */
    public String getAudioEffects() throws Exception 
    {
        /*
        Socket marySocket = new Socket(host, port);
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY VOICE GETAUDIOEFFECTS");
                */
        String info = maryHttpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTS");
        
        if (info.length() == 0)
            return "";

        audioEffects = info;

        //marySocket.close();

        return audioEffects;
    }
    
    public String getAudioEffectHelpTextLineBreak() throws Exception 
    {
        /*
        Socket marySocket = new Socket(host, port);
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY VOICE GETAUDIOEFFECTHELPTEXTLINEBREAK");
                */
        String info = maryHttpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTHELPTEXTLINEBREAK");
        if (info.length() == 0)
            return "";

        //marySocket.close();

        return info.trim();
    }
    
    public String requestEffectParametersChange(String effectName, String strParamNew) throws Exception 
    {
        /*
        Socket marySocket = new Socket(host, port);
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY VOICE SETAUDIOEFFECTPARAM " + effectName + "_" + strParamNew);
                */
        String info = maryHttpRequester.requestString(host, port, "MARY VOICE SETAUDIOEFFECTPARAM " + effectName + "_" + strParamNew);
        
        if (info.length() == 0)
            return "";
        
        //marySocket.close();
        
        return requestEffectParametersAsString(effectName);
    }
    
    public String requestEffectParametersAsString(String effectName) throws Exception 
    {
        /*
        Socket marySocket = new Socket(host, port);
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY VOICE GETAUDIOEFFECTPARAM " + effectName);
                */
        
        String info = maryHttpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTPARAM " + effectName);
        
        if (info.length() == 0)
            return "";

        //marySocket.close();

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }

    public String requestFullEffectAsString(String effectName) throws Exception 
    {
        /*
        Socket marySocket = new Socket(host, port);
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY VOICE GETFULLAUDIOEFFECT " + effectName);
                */
        
        String info = maryHttpRequester.requestString(host, port, "MARY VOICE GETFULLAUDIOEFFECT " + effectName);
        
        if (info.length() == 0)
            return "";

        //marySocket.close();

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }
    
    public String requestEffectHelpText(String effectName) throws Exception 
    {
        if (!audioEffectHelpTextsMap.containsKey(effectName))
        {
            /*
            Socket marySocket = new Socket(host, port);
            String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                    new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                    "MARY VOICE GETAUDIOEFFECTHELPTEXT " + effectName);
                    */
            
            String info = maryHttpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTHELPTEXT " + effectName);
            
            if (info.length() == 0)
                return "";

            audioEffectHelpTextsMap.put(effectName, info.replaceAll("\n", System.getProperty("line.separator")));
            
            //marySocket.close();
        }
        
        return audioEffectHelpTextsMap.get(effectName);
    }
    
    public boolean isHMMEffect(String effectName) throws Exception
    {
        /*
        Socket marySocket = new Socket(host, port);
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY VOICE ISHMMAUDIOEFFECT " + effectName);
                */
        
        String info = maryHttpRequester.requestString(host, port, "MARY VOICE ISHMMAUDIOEFFECT " + effectName);
        
        //marySocket.close();
        
        if (info.length() == 0)
            return false;

        boolean bRet = false;
        info = info.toLowerCase();
        if (info.indexOf("yes")>-1)
            bRet = true;
        
        return bRet;
    }
    
    /**
     * Get the audio file format types known by the server, one per line.
     * Each line has the format: <code>extension name</code>
     * @return
     * @throws IOException
     * @throws UnknownHostException
     */
    public String[] getAudioFileFormatTypes() throws Exception
    {
        if (audioFileFormatTypes == null) 
        {
            /*
            Socket marySocket = new Socket(host, port);
            
            audioFileFormatTypes = getServerInfoLines(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                    new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                    "MARY LIST AUDIOFILEFORMATTYPES");
                    */
            
            audioFileFormatTypes = maryHttpRequester.requestStringArray(host, port, "MARY LIST AUDIOFILEFORMATTYPES");
            
            //marySocket.close();
        }
        
        return audioFileFormatTypes;
    }

    public static void usage() 
    {
        System.err.println("usage:");
        System.err.println("java [properties] " + MaryHttpClient.class.getName() + " [inputfile]");
        System.err.println();
        System.err.println("Properties are: -Dinput.type=INPUTTYPE");
        System.err.println("                -Doutput.type=OUTPUTTYPE");
        System.err.println("                -Daudio.type=AUDIOTYPE");
        System.err.println("                -Dvoice.default=male|female|de1|de2|de3|...");
        System.err.println("                -Dserver.host=HOSTNAME");
        System.err.println("                -Dserver.port=PORTNUMBER");
        System.err.println(
            "where INPUTTYPE is one of TEXT_DE, TEXT_EN, RAWMARYXML, TOKENISED_DE, PREPROCESSED_DE, CHUNKED_DE,");
        System.err.println(
            "                          PHONEMISED_DE, INTONISED_DE, POSTPROCESSED_DE, ACOUSTPARAMS or MBROLA,");
        System.err.println("     OUTPUTTYPE is one of TOKENISED_DE, PREPROCESSED_DE, CHUNKED_DE, PHONEMISED_DE");
        System.err.println("                          INTONISED_DE, POSTPROCESSED_DE, ACOUSTPARAMS, MBROLA, or AUDIO,");
        System.err.println("and AUDIOTYPE is one of AIFC, AIFF, AU, SND, WAVE, MP3, and Vorbis.");
        System.err.println("The default values for input.type and output.type are TEXT_DE and AUDIO,");
        System.err.println("respectively; the default audio.type is WAVE.");
        System.err.println();
        System.err.println("inputfile must be of type input.type.");
        System.err.println("If no inputfile is given, the program will read from standard input.");
        System.err.println();
        System.err.println("The output is written to standard output, so redirect or pipe as appropriate.");
    }

    public static void main(String[] args) throws IOException 
    {
        if (args.length > 0 && args[0].equals("-h")) 
        {
            usage();
            System.exit(1);
        }
        
        MaryHttpClient mc = new MaryHttpClient();
        BufferedReader inputReader = null;
        // read requested input/output type from properties:
        String inputType = System.getProperty("input.type", "TEXT_DE");
        String outputType = System.getProperty("output.type", "AUDIO");
        String locale = System.getProperty("locale", "en_US");
        String audioType = System.getProperty("audio.type", "WAVE");
        if (!(audioType.equals("AIFC")
            || audioType.equals("AIFF")
            || audioType.equals("AU")
            || audioType.equals("SND")
            || audioType.equals("WAVE")
            || audioType.equals("MP3")
            || audioType.equals("Vorbis"))) {
                System.err.println("Invalid value '" + audioType + "' for property 'audio.type'");
                System.err.println();
                usage();
                System.exit(1);
        }
        String defaultVoiceName = System.getProperty("voice.default", "de7");
        String defaultStyle = "";
        String defaultEffects = "";

        if (args.length > 0) {
            File file = new File(args[0]);
            inputReader = new BufferedReader(new FileReader(file));
        } else { // no Filename, read from stdin:
            inputReader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        }

        // Read input into a string:
        StringBuffer sb = new StringBuffer(1024);
        char[] buf = new char[1024];
        int nr;
        while ((nr = inputReader.read(buf)) != -1) {
            sb.append(buf, 0, nr);
        }

        try {
            mc.process(sb.toString(), inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, System.out);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * This helper method converts a string (e.g., "en_US") into a 
     * proper Locale object.
     * @param localeString a string representation of the locale
     * @return a Locale object.
     */
    public static Locale string2locale(String localeString)
    {
        Locale locale = null;
        StringTokenizer localeST = new StringTokenizer(localeString, "_");
        String language = localeST.nextToken();
        String country = "";
        String variant = "";
        if (localeST.hasMoreTokens()) {
            country = localeST.nextToken();
            if (localeST.hasMoreTokens()) {
                variant = localeST.nextToken();
            }
         }
        locale = new Locale(language, country, variant);
        return locale;
    }

    /**
     * An abstraction of server info about available voices.
     * @author Marc Schr&ouml;der
     *
     *
     */
    public static class Voice
    {
        private String name;
        private Locale locale;
        private String gender;
        private String domain;
        private String synthesizerType;
     
        private boolean isLimitedDomain;
        Voice(String name, Locale locale, String gender, String domain)
        {
            this.name = name;
            this.locale = locale;
            this.gender = gender;
            this.domain = domain;
            if (domain == null || domain.equals("general")){
                isLimitedDomain = false;}
            else {isLimitedDomain = true;}
            
            this.synthesizerType = "not-specified";
        }
        public Locale getLocale() { return locale; }
        public String name() { return name; }
        public String gender() { return gender; }
        public String synthesizerType() {return synthesizerType;}
        public void setSynthesizerType(String synthesizerTypeIn) {synthesizerType = synthesizerTypeIn;}
        public String toString() { return name + " (" + locale.getDisplayLanguage() + ", " + gender
            + (isLimitedDomain ? ", " + domain : "") +")";}
        public boolean isLimitedDomain() { return isLimitedDomain; }
        public boolean isHMMVoice() {
            if (synthesizerType.compareToIgnoreCase("hmm")==0)
                return true;
            else
                return false;
        }
    }
    
    /**
     * An abstraction of server info about available data types.
     * @author Marc Schr&ouml;der
     *
     *
     */
    public static class DataType
    {
        private String name;
        private boolean isInputType;
        private boolean isOutputType;
        DataType(String name, boolean isInputType, boolean isOutputType) {
            this.name = name;
            this.isInputType = isInputType;
            this.isOutputType = isOutputType;
        }
        public String name() { return name; }
        public boolean isInputType() { return isInputType; }
        public boolean isOutputType() { return isOutputType; }
        public boolean isTextType() { return !name.equals("AUDIO"); }
        public String toString() { return name; }
    }

    public class AudioPlayerWriter
    {
        protected com.sun.speech.freetts.audio.AudioPlayer player;
        protected InputStream in;
        protected long startTime;
        public AudioPlayerWriter(com.sun.speech.freetts.audio.AudioPlayer player, InputStream in)
        {
            this.player = player;
            this.in = in;
            this.startTime = System.currentTimeMillis();
        }
        public AudioPlayerWriter(com.sun.speech.freetts.audio.AudioPlayer player, InputStream in, long startTime)
        {
            this.player = player;
            this.in = in;
            this.startTime = startTime;
        }
        
        public void write() throws IOException, UnsupportedAudioFileException
        {
            // Read from Server and copy into audio player:
            if (doProfile)
                System.err.println("After "+(System.currentTimeMillis()-startTime)+" ms: Trying to read data from server");
            while (false && in.available() < 46) { // at least the audio header should be there
                Thread.yield();
            }
            if (doProfile)
                System.err.println("After "+(System.currentTimeMillis()-startTime)+" ms: Got at least the header");
            in = new BufferedInputStream(in);
            in.mark(1000);
             AudioInputStream fromServerAudio = AudioSystem.getAudioInputStream(in);
             if (fromServerAudio.getFrameLength() == 0) { // weird bug under Java 1.4
                 //in.reset();
                 fromServerAudio = new AudioInputStream(in, fromServerAudio.getFormat(), AudioSystem.NOT_SPECIFIED);
             }
             //System.out.println("Audio framelength: "+fromServerAudio.getFrameLength());
             //System.out.println("Audio frame size: "+fromServerAudio.getFormat().getFrameSize());
             //System.out.println("Audio format: "+fromServerAudio.getFormat());
             if (doProfile)
                 System.err.println("After "+(System.currentTimeMillis()-startTime)+" ms: Audio available: "+fromServerAudio.available());
             AudioFormat audioFormat = fromServerAudio.getFormat();
             if (!audioFormat.getEncoding().equals(Encoding.PCM_SIGNED)) { // need conversion, e.g. for mp3
                 audioFormat = new AudioFormat(fromServerAudio.getFormat().getSampleRate(), 16, 1, true, false);
                 fromServerAudio = AudioSystem.getAudioInputStream(audioFormat, fromServerAudio);
             }
             player.reset();
             player.setAudioFormat(audioFormat);
             player.setVolume(0.9f);
             // number of bytes to be written:
             player.begin(AudioSystem.NOT_SPECIFIED); // bytes per frame
             int nr;
             byte[] bbuf = new byte[1024];
             boolean ok = true;
             if (doProfile)
                 System.err.println("After "+(System.currentTimeMillis()-startTime)+" ms: Start playing");
             boolean first = true;
             while (ok && (nr = fromServerAudio.read(bbuf, 0, bbuf.length)) != -1) {
                 if (doProfile && first) {
                     first = false;
                     System.err.println("Time to audio: "+(System.currentTimeMillis()-startTime)+" ms");
                 }
                 ok = player.write(bbuf, 0, nr);
             }
             if (ok) {
                 ok = player.end();
             }
             
             player.drain();
        }
    }
    
    /**
     * A means of letting a caller code know
     * that the audioplayer has finished.
     * @author Marc Schr&ouml;der
     *
     */
    public static interface AudioPlayerListener
    {
        /**
         * Notify the listener that the audio player has finished.
         *
         */
        public void playerFinished();
        
        /**
         * Inform the listener that the audio player has thrown an exception.
         * @param e the exception thrown
         */
        public void playerException(Exception e);
    }
    
    public static class WarningReader extends Thread
    {
        protected BufferedReader in;
        protected StringBuffer warnings;
        public WarningReader(BufferedReader in)
        {
            this.in = in;
            warnings = new StringBuffer();
        }
        
        public String getWarnings() { return warnings.toString(); }
        
        public void run()
        {
            char[] cbuf = new char[1024];
            int nr;
            try {
                while ((nr = in.read(cbuf)) != -1) {
                    // warnings from the server
                    warnings.append(cbuf, 0, nr);
                }
            } catch (IOException ioe) {
            }
        }
    }
}
