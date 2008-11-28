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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.HashMap;
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

import marytts.Version;
import marytts.client.MaryGUIClient;

/**
 * An HTTP client implementing the MARY protocol.
 * It can be used as a command line client or from within java code.
 * @author Marc Schr&ouml;der, oytun.turk
 * @see MaryGUIClient A GUI interface to this client
 * @see marytts.server.MaryServer Description of the MARY protocol
 */

public class MaryHttpClient extends MaryBaseClient {
    
    protected boolean beQuiet = false;
    private boolean doProfile = false;

    /**
     * The simplest way to create a mary client. It will connect to the
     * MARY server running at DFKI. Only use this for testing purposes!
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     */
    public MaryHttpClient() throws IOException, InterruptedException
    {
        super();
        
        boolean profile = Boolean.getBoolean("mary.client.profile");
        boolean quiet = Boolean.getBoolean("mary.client.quiet");
        
        initialise(profile, quiet);
    }
    
    public MaryHttpClient(boolean quiet) throws IOException, InterruptedException
    {
        super();
        
        boolean profile = Boolean.getBoolean("mary.client.profile");
        
        initialise(profile, quiet);
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
    public MaryHttpClient(Address serverAddress) throws IOException, InterruptedException
    {
        super(serverAddress);
        
        boolean profile = Boolean.getBoolean("mary.client.profile");
        boolean quiet = Boolean.getBoolean("mary.client.quiet");
        
        initialise(profile, quiet);
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
    public MaryHttpClient(Address serverAddress, boolean profile, boolean quiet) throws IOException, InterruptedException
    {
        super(serverAddress);
        
        initialise(profile, quiet);
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
    private void initialise(boolean profile, boolean quiet) throws IOException, InterruptedException
    {  
        // This must work for applets too, so no system property queries here:
        doProfile = profile;
        beQuiet = quiet;
        httpRequester = new MaryHttpRequester(beQuiet);
        
        String[] info;
        if (!beQuiet)
            System.err.println("Mary TTS client " + Version.specificationVersion() + " (impl. " + Version.implementationVersion() + ")");
        
        try {
            fillServerVersion();
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new IOException("MARY client cannot connect to MARY server at\n"+
                    hostAddress.getFullAddress()+"\n"+
                    "Make sure that you have started the mary server\n"+
                    "or specify a different host or port using \n"+
            "maryclient -Dserver.host=my.host.com -Dserver.port=12345");
        } catch (InterruptedException e2) {
            e2.printStackTrace();
            throw new InterruptedException("MARY client cannot connect to MARY server at\n"+
                    hostAddress.getFullAddress()+"\n"+
                    "Make sure that you have started the mary server\n"+
                    "or specify a different host or port using \n"+
            "maryclient -Dserver.host=my.host.com -Dserver.port=12345");
        }
        
        if (!beQuiet) 
        {
            System.err.print("Connected to " + hostAddress.getFullAddress() + ", ");
            System.err.println(serverVersionInfo);
            
            if (!serverCanStream) {
                System.err.println("Server version " + serverVersionNo + " cannot stream audio, defaulting to non-streaming");
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
    public void streamAudio(String input, String inputType, String locale, String audioType, String defaultVoiceName, String defaultStyle, Map<String, String> defaultEffects, marytts.util.data.audio.AudioPlayer audioPlayer, AudioPlayerListener listener)
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
        String audioType, String defaultVoiceName, String defaultStyle, Map<String, String> defaultEffects, OutputStream output)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, 0, false, null);
    }
    
    public void process(String input, String inputType, String outputType, String locale,
            String audioType, String defaultVoiceName, OutputStream output)
            throws UnknownHostException, IOException, Exception
    {
        process( input,  inputType,  outputType, locale, audioType,  defaultVoiceName,  "", null, output);
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
        String audioType, String defaultVoiceName, String defaultStyle, Map<String, String> defaultEffects, OutputStream output, long timeout)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, timeout, false, null);
    }

    public void process(String input, String inputType, String outputType, String locale,
         String audioType, String defaultVoiceName, OutputStream output, long timeout)
         throws UnknownHostException, IOException, Exception
    {
        process(input,  inputType, outputType, locale, audioType,  defaultVoiceName, "",  null, output, timeout);
    }

    private void _process(String input, String inputType, String outputType, String locale, String audioType, 
                          String defaultVoiceName, String defaultStyle, Map<String, String> defaultEffects, 
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
        
        final InputStream fromServerStream = requestInputStream(input, inputType, outputType, locale, audioType, 
                                                                defaultVoiceName, defaultStyle, defaultEffects, 
                                                                streamingAudio);

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

    public static void main(String[] args) throws IOException, InterruptedException
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
        Map<String, String> defaultEffects = null;

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
