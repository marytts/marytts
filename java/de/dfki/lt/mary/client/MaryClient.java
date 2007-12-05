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
package de.dfki.lt.mary.client;

// General Java Classes
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import com.sun.speech.freetts.audio.AudioPlayer;

import de.dfki.lt.mary.Version;

/**
 * A socket client implementing the MARY protocol.
 * It can be used as a command line client or from within java code.
 * @author Marc Schr&ouml;der
 * @see MaryGUIClient A GUI interface to this client
 * @see de.dfki.lt.mary.MaryServer Description of the MARY protocol
 */

public class MaryClient {
    // Default values which can be overridden from the command line.
    private final String DEFAULT_HOST = "cling.dfki.uni-sb.de";
    private final int DEFAULT_PORT = 59125;
    private String host;
    private int port;
    private String serverVersion = "unknown";
    private boolean serverCanStream = false;
    private boolean doProfile = false;
    private boolean beQuiet = false;
    private Vector allVoices = null;
    private Map voicesByLocaleMap = null;
    private Vector allDataTypes = null;
    private Vector inputDataTypes = null;
    private Vector outputDataTypes = null;
    private Map serverExampleTexts = new HashMap();
    private Map voiceExampleTexts = new HashMap();
    private Map audioEffectsMap = new HashMap();
    private String[] audioFileFormatTypes = null;
    private String[] serverVersionInfo = null;

    /**
     * The simplest way to create a mary client. It will connect to the
     * MARY server running at DFKI. Only use this for testing purposes!
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public MaryClient() throws IOException
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
    public MaryClient(String host, int port)
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
    public MaryClient(String host, int port, boolean profile, boolean quiet)
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
    private void initialise(String serverHost, int serverPort, boolean profile, boolean quiet)
    throws IOException {
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
                System.err.println("Server version "+serverVersion+" cannot stream audio, defaulting to non-streaming");
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
    public void streamAudio(String input, String inputType, String audioType, String defaultVoiceName, String defaultStyle, String defaultEffects, AudioPlayer audioPlayer, AudioPlayerListener listener)
    throws UnknownHostException, IOException
    {
        _process(input, inputType, "AUDIO", audioType, defaultVoiceName, defaultStyle, defaultEffects, audioPlayer, 0, true, listener);
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
    public void process(String input, String inputType, String outputType,
        String audioType, String defaultVoiceName, String defaultStyle, String defaultEffects, OutputStream output)
        throws UnknownHostException, IOException
    {
        _process(input, inputType, outputType, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, 0, false, null);
    }
    
    public void process(String input, String inputType, String outputType,
            String audioType, String defaultVoiceName, OutputStream output)
            throws UnknownHostException, IOException
    {
        process( input,  inputType,  outputType, audioType,  defaultVoiceName,  "", "", output);
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
    public void process(String input, String inputType, String outputType,
        String audioType, String defaultVoiceName, String defaultStyle, String defaultEffects, OutputStream output, long timeout)
        throws UnknownHostException, IOException
    {
        _process(input, inputType, outputType, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, timeout, false, null);
    }

    public void process(String input, String inputType, String outputType,
         String audioType, String defaultVoiceName, OutputStream output, long timeout)
         throws UnknownHostException, IOException
    {
        process(input,  inputType, outputType, audioType,  defaultVoiceName, "",  "", output, timeout);
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
    public void process(String input, String inputType, String defaultVoiceName, String defaultStyle,  String defaultEffects, AudioPlayer player)
        throws UnknownHostException, IOException
    {
        _process(input, inputType, "AUDIO", "AU", defaultVoiceName, defaultStyle, defaultEffects, player, 0, false, null);
    }
    
    public void process(String input, String inputType, String defaultVoiceName, AudioPlayer player)
    throws UnknownHostException, IOException
    {
        process(input, inputType, defaultVoiceName, "", "", player);
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
    public void process(String input, String inputType, String defaultVoiceName, String defaultStyle, String defaultEffects, AudioPlayer player, long timeout)
        throws UnknownHostException, IOException
    {
        _process(input, inputType, "AUDIO", "AU", defaultVoiceName, defaultStyle, defaultEffects, player, timeout, false, null);
    }

    public void process(String input, String inputType, String defaultVoiceName, AudioPlayer player, long timeout)
    throws UnknownHostException, IOException
    {
        process( input, inputType, defaultVoiceName, "", "", player, timeout);
    }
    private void _process(String input, String inputType, String outputType, String audioType, 
            String defaultVoiceName, String defaultStyle, String defaultEffects, 
            Object output, long timeout, boolean streamingAudio, AudioPlayerListener playerListener)
        throws UnknownHostException, IOException
    {
        boolean isAudioPlayer;
        if (output instanceof AudioPlayer) {
            isAudioPlayer = true;
        } else if (output instanceof OutputStream) {
            isAudioPlayer = false;
        } else {
            throw new IllegalArgumentException("Expected OutputStream or AudioPlayer, got " + output.getClass().getName());
        }
        final long startTime = System.currentTimeMillis();
        // Socket Client
        final Socket maryInfoSocket;
        try {
            maryInfoSocket = new Socket(host, port);
        } catch (SocketException se) {
            throw new RuntimeException("Cannot connect to " + host + ":" + port, se);
        }
        final PrintWriter toServerInfo = new PrintWriter(new OutputStreamWriter(maryInfoSocket.getOutputStream(), "UTF-8"), true);
        final BufferedReader fromServerInfo = new BufferedReader(new InputStreamReader(maryInfoSocket.getInputStream(), "UTF-8"));

        // Formulate Request to Server:
        //System.err.println("Writing request to server.");
        toServerInfo.print("MARY IN=" + inputType + " OUT=" + outputType);
        if (audioType != null) {
        	if (streamingAudio && serverCanStream) {
        		toServerInfo.print(" AUDIO=STREAMING_"+audioType);
        	} else {
        		toServerInfo.print(" AUDIO=" + audioType);
        	}
        }
        if (defaultVoiceName != null) {
            toServerInfo.print(" VOICE=" + defaultVoiceName);
        }
        
        if (defaultStyle != "") {
            toServerInfo.print(" STYLE=" + defaultStyle);
        }
        
        if (defaultEffects != "") {
            toServerInfo.print(" EFFECTS=" + defaultEffects);
        }
        toServerInfo.println();
        
        // Receive a request ID:
        //System.err.println("Reading reply from server.");
        String helper = fromServerInfo.readLine();
        //System.err.println("Read from Server: " + helper);
        int id = -1;
        try {
            id = Integer.parseInt(helper);
        } catch (NumberFormatException e) {
            // Whatever we read from the server, it was not a number
            StringBuffer message = new StringBuffer("Server replied:\n");
            message.append(helper);
            message.append("\n");
            while ((helper = fromServerInfo.readLine()) != null) {
                message.append(helper);
                message.append("\n");
            }
            throw new IOException(message.toString());
        }

        //System.err.println("Read id " + id + " from server.");
        final Socket maryDataSocket = new Socket(host, port);
        //System.err.println("Created second socket.");
        final PrintWriter toServerData = new PrintWriter(new OutputStreamWriter(maryDataSocket.getOutputStream(), "UTF-8"), true);
        toServerData.println(id);
        //System.err.println("Writing to server:");
        //System.err.print(input);
        toServerData.println(input.trim());
        maryDataSocket.shutdownOutput();

        // Check for warnings from server:
        final WarningReader warningReader = new WarningReader(fromServerInfo);
        warningReader.start();
        
        // Read from Server and copy into OutputStream output:
        // (as we only do low-level copying of bytes here,
        //  we do not need to distinguish between text and audio)
        final InputStream fromServerStream = maryDataSocket.getInputStream();

        // If timeout is > 0, create a timer. It will close the input stream,
        // thus causing an IOException in the reading code.
        final Timer timer;
        if (timeout <= 0) {
            timer = null;
        } else {
            timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                public void run() {
                    System.err.println("Timer closes socket");
                    try {
                        maryDataSocket.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            };
            timer.schedule(timerTask, timeout);
        }

        if (isAudioPlayer) {
            final AudioPlayer player = (AudioPlayer) output;
            final AudioPlayerListener listener = playerListener;
            Thread t = new Thread() {
                public void run() 
                {
                    try {
                        AudioPlayerWriter apw = new AudioPlayerWriter(player, fromServerStream, startTime);
                        apw.write();
                        if (timer != null) {
                            timer.cancel();
                        }
                        if (listener != null) listener.playerFinished();

                        toServerInfo.close();
                        fromServerInfo.close();
                        maryInfoSocket.close();
                        toServerData.close();
                        maryDataSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }                    

                    try {
                        warningReader.join();
                    } catch (InterruptedException ie) {}
                    if (warningReader.getWarnings().length() > 0) { // there are warnings
                        String warnings = warningReader.getWarnings(); 
                        System.err.println(warnings);
                        if (listener != null) listener.playerException(new IOException(warnings));
                    }

                        if (doProfile) {
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
        } else { // output is an OutputStream
            OutputStream os = (OutputStream) output;
            InputStream bis = new BufferedInputStream(fromServerStream);
            byte[] bbuf = new byte[1024];
            int nr;
            while ((nr = bis.read(bbuf, 0, bbuf.length)) != -1) {
                //System.err.println("Read " + nr + " bytes from server.");
                os.write(bbuf, 0, nr);
            }
            os.flush();
            

            if (timeout > 0) {
                timer.cancel();
            }

            toServerInfo.close();
            fromServerInfo.close();
            maryInfoSocket.close();
            toServerData.close();
            maryDataSocket.close();

            try {
                warningReader.join();
            } catch (InterruptedException ie) {}
            if (warningReader.getWarnings().length() > 0) // there are warnings
                throw new IOException(warningReader.getWarnings());

            if (doProfile) {
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
    public String[] getServerVersionInfo() throws IOException, UnknownHostException
    {
        if (serverVersionInfo == null) {
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
        }
        return serverVersionInfo;
    }

    /**
     * Obtain a list of all data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryClient.DataType objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getAllDataTypes() throws IOException, UnknownHostException
    {
        if (allDataTypes == null) {
            fillDataTypes();
        }
        assert allDataTypes != null && allDataTypes.size() > 0;
        return allDataTypes;
    }

    /**
     * Obtain a list of input data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryClient.DataType objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getInputDataTypes() throws IOException, UnknownHostException
    {
        if (inputDataTypes == null) {
            fillDataTypes();
        }
        assert inputDataTypes != null && inputDataTypes.size() > 0;
        return inputDataTypes;
    }

    /**
     * Obtain a list of output data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryClient.DataType objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getOutputDataTypes() throws IOException, UnknownHostException
    {
        if (outputDataTypes == null) {
            fillDataTypes();
        }
        assert outputDataTypes != null && outputDataTypes.size() > 0;
        return outputDataTypes;
    }

    private void fillDataTypes() throws UnknownHostException, IOException {
        allDataTypes = new Vector();
        inputDataTypes = new Vector();
        outputDataTypes = new Vector();
        Socket marySocket = new Socket(host, port);
        // Expect a variable number of lines of the kind
        // RAWMARYXML INPUT OUTPUT
        // TEXT_DE LOCALE=de INPUT
        // AUDIO OUTPUT
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
            new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
            "MARY LIST DATATYPES");
        if (info.length() == 0)
            throw new IOException("Could not get list of data types from Mary server");
        String[] typeStrings = info.split("\n");
        marySocket.close();
        for (int i=0; i<typeStrings.length; i++) {
            StringTokenizer st = new StringTokenizer(typeStrings[i]);
            if (!st.hasMoreTokens()) continue; // ignore this type
            String name = st.nextToken();
            boolean isInputType = false;
            boolean isOutputType = false;
            Locale locale = null;
            while (st.hasMoreTokens()) {
                String t = st.nextToken();
                if (t.startsWith("LOCALE=")) {
                    locale = string2locale(t.substring("LOCALE=".length()).trim());
                } else if (t.equals("INPUT")) {
                    isInputType = true;
                } else if (t.equals("OUTPUT")) {
                    isOutputType = true;
                }
            }
            DataType dt = new DataType(name, locale, isInputType, isOutputType);
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
     * @return a Vector of MaryClient.Voice objects.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getVoices() throws IOException, UnknownHostException
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
     * @return a Vector of MaryClient.Voice objects, or null if no voices exist for
     * that locale.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getVoices(Locale locale) throws IOException, UnknownHostException
    {
        if (allVoices == null) {
            fillVoices();
        }
        return (Vector) voicesByLocaleMap.get(locale);
    }

    /**
     * Provide a list of general domain voices known to the server.
     * If the information is not yet available, query the server for it.
     * This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getGeneralDomainVoices() throws IOException, UnknownHostException
    {
        Vector voices = getVoices();
        Vector requestedVoices = new Vector();
        for (Iterator it = voices.iterator(); it.hasNext(); ) {
            Voice v = (Voice) it.next();
            if (!v.isLimitedDomain()) {
                requestedVoices.add(v);
            }
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
     * @return a Vector of MaryClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getLimitedDomainVoices() throws IOException, UnknownHostException
    {
        Vector voices = getVoices();
        Vector requestedVoices = new Vector();
        for (Iterator it = voices.iterator(); it.hasNext(); ) {
            Voice v = (Voice) it.next();
            if (v.isLimitedDomain()) {
                requestedVoices.add(v);
            }
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
     * @return a Vector of MaryClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getGeneralDomainVoices(Locale locale) throws IOException, UnknownHostException
    {
        Vector voices = getVoices(locale);
        Vector requestedVoices = new Vector();
        for (Iterator it = voices.iterator(); it.hasNext(); ) {
            Voice v = (Voice) it.next();
            if (!v.isLimitedDomain()) {
                requestedVoices.add(v);
            }
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
     * @return a Vector of MaryClient.Voice objects, or null if no such voices exist.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public Vector getLimitedDomainVoices(Locale locale) throws IOException, UnknownHostException
    {
        Vector voices = getVoices(locale);
        Vector requestedVoices = new Vector();
        for (Iterator it = voices.iterator(); it.hasNext(); ) {
            Voice v = (Voice) it.next();
            if (v.isLimitedDomain()) {
                requestedVoices.add(v);
            }
        }
        if (!requestedVoices.isEmpty())
            return requestedVoices;
        else
            return null;
    }

    private void fillVoices() throws IOException, UnknownHostException
    {
        allVoices = new Vector();
        voicesByLocaleMap = new HashMap();
        Socket marySocket = new Socket(host, port);
        // Expect a variable number of lines of the kind
        // de7 de female
        // us2 en male
        // dfki-stadium-emo de male limited
        String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
            new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
            "MARY LIST VOICES");
        if (info.length() == 0)
            throw new IOException("Could not get voice list from Mary server");
        String[] voiceStrings = info.split("\n");
        marySocket.close();
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
            Voice voice;
            if (!st.hasMoreTokens()){ //assume domain is general
                voice = new Voice(name, locale, gender,"general");}
            else{ //read in the domain
                String domain = st.nextToken();
                voice = new Voice(name, locale, gender,domain);}
            allVoices.add(voice);
            Vector localeVoices = null;
            if (voicesByLocaleMap.containsKey(locale)) {
                localeVoices = (Vector) voicesByLocaleMap.get(locale);
            } else {
                localeVoices = new Vector();
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
    public String getVoiceExampleText(String voicename) 
    								throws IOException, UnknownHostException {
          if (!voiceExampleTexts.containsKey(voicename)) {
                Socket marySocket = new Socket(host, port);
                String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                    new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                    "MARY VOICE EXAMPLETEXT " + voicename);
                if (info.length() == 0)
                    throw new IOException("Could not get example text from Mary server");
                
                voiceExampleTexts.put(voicename, info.replaceAll("\n", System.getProperty("line.separator")));
                marySocket.close();
                
            }
          
            return (String) voiceExampleTexts.get(voicename);
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
    public String getServerExampleText(String dataType) throws IOException, UnknownHostException
    {
        if (!serverExampleTexts.containsKey(dataType)) {
            Socket marySocket = new Socket(host, port);
            String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                "MARY EXAMPLETEXT " + dataType);
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");
            serverExampleTexts.put(dataType, info.replaceAll("\n", System.getProperty("line.separator")));
            marySocket.close();
            
        }
        return (String) serverExampleTexts.get(dataType);
    }

    /**
     * Request the available audio effects for a voice from the server
     * @param voicename the voice
     * @return A string of available audio effects and default parameters, i.e. "FIRFilter,Robot(amount=50)"
     * @throws IOException
     * @throws UnknownHostException
     */
    public String getAudioEffects(String voicename) 
                                    throws IOException, UnknownHostException {
          if (!audioEffectsMap.containsKey(voicename)) 
          {
                Socket marySocket = new Socket(host, port);
                String info = getServerInfo(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                    new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                    "MARY VOICE AUDIOEFFECTS " + voicename);
                if (info.length() == 0)
                    return "";
                
                audioEffectsMap.put(voicename, info.replaceAll("\n", System.getProperty("line.separator")));
                marySocket.close();
            }
          
            return (String)audioEffectsMap.get(voicename);
        }
    
    /**
     * Get the audio file format types known by the server, one per line.
     * Each line has the format: <code>extension name</code>
     * @return
     * @throws IOException
     * @throws UnknownHostException
     */
    public String[] getAudioFileFormatTypes()
    throws IOException, UnknownHostException
    {
        if (audioFileFormatTypes == null) {
            Socket marySocket = new Socket(host, port);
            audioFileFormatTypes = getServerInfoLines(new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true),
                    new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8")),
                    "MARY LIST AUDIOFILEFORMATTYPES");
            
        }
        return audioFileFormatTypes;
    }
    

    public static void usage() {
        System.err.println("usage:");
        System.err.println("java [properties] " + MaryClient.class.getName() + " [inputfile]");
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

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equals("-h")) {
            usage();
            System.exit(1);
        }
        MaryClient mc = new MaryClient();
        BufferedReader inputReader = null;
        // read requested input/output type from properties:
        String inputType = System.getProperty("input.type", "TEXT_DE");
        String outputType = System.getProperty("output.type", "AUDIO");
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
            mc.process(sb.toString(), inputType, outputType, audioType, defaultVoiceName, defaultStyle, defaultEffects, System.out);
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
        }
        public Locale getLocale() { return locale; }
        public String name() { return name; }
        public String gender() { return gender; }
        public String toString() { return name + " (" + locale.getDisplayLanguage() + ", " + gender
        	+ (isLimitedDomain ? ", " + domain : "") +")";}
        public boolean isLimitedDomain() { return isLimitedDomain; }
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
        private Locale locale;
        private boolean isInputType;
        private boolean isOutputType;
        DataType(String name, Locale locale, boolean isInputType, boolean isOutputType) {
            this.name = name;
            this.locale = locale;
            this.isInputType = isInputType;
            this.isOutputType = isOutputType;
        }
        public String name() { return name; }
        public Locale getLocale() { return locale; }
        public boolean isInputType() { return isInputType; }
        public boolean isOutputType() { return isOutputType; }
        public boolean isTextType() { return !name.equals("AUDIO"); }
        public String toString() { return name; }
    }

    public class AudioPlayerWriter
    {
        protected AudioPlayer player;
        protected InputStream in;
        protected long startTime;
        public AudioPlayerWriter(AudioPlayer player, InputStream in)
        {
            this.player = player;
            this.in = in;
            this.startTime = System.currentTimeMillis();
        }
        public AudioPlayerWriter(AudioPlayer player, InputStream in, long startTime)
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
