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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import marytts.Version;
import marytts.client.AudioEffectControlData;
import marytts.client.AudioEffectsBoxData;
import marytts.client.MaryClient;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * An HTTP client implementing the MARY protocol.
 * It can be used as a command line client or from within java code.
 * @author Marc Schr&ouml;der, oytun.turk
 * @see MaryGUIHttpClient A GUI interface to this client
 * @see marytts.server.MaryServer Description of the MARY protocol
 */

public class MaryHttpClient
{
    private MaryFormData data = new MaryFormData();
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
        data.hostAddress = serverAddress;
        
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
        data.hostAddress = serverAddress;
        
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

        String[] info;
        if (!beQuiet)
            System.err.println("Mary TTS client " + Version.specificationVersion() + " (impl. " + Version.implementationVersion() + ")");
        
        try {
            fillServerVersion();
        } catch (IOException e1) {
            throw new IOException("MARY client cannot connect to MARY server at\n"+
                    data.hostAddress.getFullAddress()+"\n"+
                    "Make sure that you have started the mary server\n"+
                    "or specify a different host or port using \n"+
            "maryclient -Dserver.host=my.host.com -Dserver.port=12345", e1);
        }
        
        if (!beQuiet) 
        {
            System.err.print("Connected to " + data.hostAddress.getFullAddress() + ", ");
            System.err.println(data.serverVersionInfo);
            
            if (!data.serverCanStream) {
                System.err.println("Server version " + data.serverVersionNo + " cannot stream audio, defaulting to non-streaming");
            }
        }
        
        fillVoices();
        //Limited domain example texts
        if (data.allVoices!=null && data.allVoices.size()>0)
        {
            if (data.allVoices.elementAt(data.voiceSelected).isLimitedDomain())
            {
                data.limitedDomainExampleTexts = getVoiceExampleTextsLimitedDomain(data.allVoices.elementAt(data.voiceSelected).name());
            }
        }
        //

        //Input text
        if (data.allVoices!=null && data.allVoices.size()>0 && data.inputDataTypes!=null && data.inputDataTypes.size()>0)
        {
            if (data.allVoices.elementAt(data.voiceSelected).isLimitedDomain())
                data.inputText = data.limitedDomainExampleTexts.get(data.limitedDomainExampleTextSelected);
            else
                data.inputText = getServerExampleText(data.inputDataTypes.get(data.inputTypeSelected).name(), data.allVoices.elementAt(data.voiceSelected).getLocale().toString());
        } 
        //

    }
    
    
    
    ///////////////////////////////////////////////////////////////////////
    //////////////////////// Information requests /////////////////////////
    ///////////////////////////////////////////////////////////////////////
    
    
    
    
    
    
    
    
    /**
     * Get the audio file format types known by the server, one per line.
     * Each line has the format: <code>extension name</code>
     * @return
     * @throws IOException
     * @throws UnknownHostException
     */
    public Vector<String> getAudioFileFormatTypes() throws IOException, InterruptedException
    {
        fillAudioFileFormatAndOutTypes();

        return data.audioFileFormatTypes;
    }
    
    public Vector<String> getAudioOutTypes() throws IOException, InterruptedException
    {
        fillAudioFileFormatAndOutTypes();

        return data.audioOutTypes;
    }
    
    protected void fillAudioFileFormatAndOutTypes()  throws IOException, InterruptedException
    {
        if (data.audioFileFormatTypes==null || data.audioOutTypes==null) {
            String audioFormatInfo = serverInfoRequest("audioformats", null);
            data.audioOutTypes = new Vector<String>(Arrays.asList(StringUtils.toStringArray(audioFormatInfo)));
            data.audioFileFormatTypes = new Vector<String>();
            for (String af : data.audioOutTypes) {
                if (af.endsWith("_FILE")) {
                    String typeName = af.substring(0, af.indexOf("_"));
                    try {
                        AudioFileFormat.Type type = MaryAudioUtils.getAudioFileFormatType(typeName);
                        data.audioFileFormatTypes.add(typeName+" "+type.getExtension());
                    } catch (Exception e) {}
                }
            }
        }
    }

    public void fillServerVersion() throws IOException
    {
        data.toServerVersionInfo(serverInfoRequest("version", null));
    }
    
    
    public boolean isServerVersionAtLeast(String version)
    throws IOException
    {
        if (data.serverVersionNo.equals("unknown"))
            fillServerVersion();
        return data.isServerVersionAtLeast(version);
    }
    
    public InputStream requestInputStream(String input, String inputType, String outputType, String locale, String audioType, 
                                          String defaultVoiceName, String defaultStyle, Map<String, String> effects, //String defaultEffects,
                                          boolean streamingAudio, String outputTypeParams) throws IOException
    {
        
        StringBuilder params = new StringBuilder();
        params.append("INPUT_TEXT=").append(URLEncoder.encode(input, "UTF-8"));
        params.append("&INPUT_TYPE=").append(URLEncoder.encode(inputType, "UTF-8"));
        params.append("&OUTPUT_TYPE=").append(URLEncoder.encode(outputType, "UTF-8"));
        params.append("&LOCALE=").append(URLEncoder.encode(locale, "UTF-8"));
        if (audioType != null) {
            params.append("&AUDIO=").append(URLEncoder.encode((streamingAudio && data.serverCanStream) ? audioType+"_STREAM" : audioType + "_FILE", "UTF-8"));
        }
        if (outputTypeParams != null) {
            params.append("&OUTPUT_TYPE_PARAMS=").append(URLEncoder.encode(outputTypeParams, "UTF-8"));
        }
        
        if (defaultVoiceName != null) {
            params.append("&VOICE=").append(URLEncoder.encode(defaultVoiceName, "UTF-8"));
        }
        
        if (defaultStyle != null) {
            params.append("&STYLE=").append(URLEncoder.encode(defaultStyle, "UTF-8"));
        }
        
        if (effects != null) {
            for (String key : effects.keySet()) {
                params.append("&").append(key).append("=").append(URLEncoder.encode(effects.get(key), "UTF-8"));
            }
        }
        
      //to make HTTP Post request with HttpURLConnection
        URL url = new URL(data.hostAddress.getHttpAddress()+"/process");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        conn.setRequestMethod("POST");
        conn.setAllowUserInteraction(false); // no user interact [like pop up]
        conn.setDoOutput(true); // want to send
        conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        OutputStream ost = conn.getOutputStream();
        PrintWriter pw = new PrintWriter(ost);
        pw.print(params.toString()); // here we "send" our body!
        pw.flush();
        pw.close();

        //and InputStream from here will be body
        return conn.getInputStream();
        
    }
    
    /**
     * Obtain a list of all data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryClient.DataType> getAllDataTypes() throws IOException
    {
        if (data.allDataTypes == null)
            fillDataTypes();
        
        assert data.allDataTypes != null && data.allDataTypes.size() > 0;
        
        return data.allDataTypes;
    }

    /**
     * Obtain a list of input data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @return a Vector of MaryHttpClient.DataType objects.
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryClient.DataType> getInputDataTypes() throws IOException
    {
        if (data.inputDataTypes == null)
            fillDataTypes();
       
        assert data.inputDataTypes != null && data.inputDataTypes.size() > 0;
        
        return data.inputDataTypes;
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
    public Vector<MaryClient.DataType> getOutputDataTypes() throws IOException
    {
        if (data.outputDataTypes == null)
            fillDataTypes();
       
        assert data.outputDataTypes != null && data.outputDataTypes.size() > 0;
        
        return data.outputDataTypes;
    }

    protected void fillDataTypes() throws IOException 
    {  
        data.toDataTypes(serverInfoRequest("datatypes", null));
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
    public Vector<MaryClient.Voice> getVoices() throws IOException
    {
        if (data.allVoices == null)
            fillVoices();
        
        assert data.allVoices != null && data.allVoices.size() > 0;
        
        return data.allVoices;
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
    public Vector<MaryClient.Voice> getVoices(Locale locale) throws IOException
    {
        if (data.allVoices == null)
            fillVoices();

        return data.voicesByLocaleMap.get(locale);
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
    public Vector<MaryClient.Voice> getGeneralDomainVoices() throws IOException
    {
        Vector<MaryClient.Voice> voices = getVoices();
        Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();
        
        for (MaryClient.Voice v: voices) 
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
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryClient.Voice> getLimitedDomainVoices() throws IOException
    {
        Vector<MaryClient.Voice> voices = getVoices();
        Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();
        
        for (MaryClient.Voice v : voices) 
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
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryClient.Voice> getGeneralDomainVoices(Locale locale) throws IOException
    {
        Vector<MaryClient.Voice> voices = getVoices(locale);
        Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();
        
        for (MaryClient.Voice v : voices) 
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
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryClient.Voice> getLimitedDomainVoices(Locale locale) throws IOException
    {
        Vector<MaryClient.Voice> voices = getVoices(locale);
        Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();
        for (MaryClient.Voice v : voices) 
        {
            if (v.isLimitedDomain())
                requestedVoices.add(v);
        }
        
        if (!requestedVoices.isEmpty())
            return requestedVoices;
        else
            return null;
    }

    protected void fillVoices() throws IOException
    {
        data.toVoices(serverInfoRequest("voices", null));
    }
    
    /**
     * Request the example text of a limited domain
     * unit selection voice from the server
     * @param voicename the voice
     * @return the example text
     * @throws IOException
     * @throws UnknownHostException
     */
    public String getVoiceExampleTextLimitedDomain(String voicename) throws IOException 
    {
        if (!data.voiceExampleTextsLimitedDomain.containsKey(voicename)) 
        {
            Map<String,String> queryItems = new HashMap<String, String>();
            queryItems.put("voice", voicename);
            String info = serverInfoRequest("exampletext", queryItems);
            
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");

            data.voiceExampleTextsLimitedDomain.put(voicename, info.replaceAll("\n", System.getProperty("line.separator")));
        }

        return data.voiceExampleTextsLimitedDomain.get(voicename);
    }
    
    public Vector<String> getVoiceExampleTextsLimitedDomain(String voicename) throws IOException
    {
        return StringUtils.processVoiceExampleText(getVoiceExampleTextLimitedDomain(voicename));
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
    public String getServerExampleText(String dataType, String locale) throws IOException
    {
        if (!data.serverExampleTexts.containsKey(dataType+" "+locale)) 
        {
            Map<String,String> queryItems = new HashMap<String, String>();
            queryItems.put("datatype", dataType);
            queryItems.put("locale", locale);
            String info = serverInfoRequest("exampletext", queryItems);
            
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");
            
            data.serverExampleTexts.put(dataType+" "+locale, info.replaceAll("\n", System.getProperty("line.separator")));
        }
        
        data.currentExampleText = data.serverExampleTexts.get(dataType+" "+locale);
        
        return data.currentExampleText;
    }

    /**
     * Request the available audio effects for a voice from the server
     * @param voicename the voice
     * @return A string of available audio effects and default parameters, i.e. "FIRFilter,Robot(amount=50)"
     * @throws IOException
     * @throws UnknownHostException
     */
    public String getDefaultAudioEffects() throws IOException 
    {
        String defaultAudioEffects = serverInfoRequest("audioeffects", null);

        return defaultAudioEffects;
    }

    public String getAudioEffects() throws IOException 
    {
        if (data.audioEffects==null)
            data.audioEffects = getDefaultAudioEffects();

        return data.audioEffects;
    }
    
    
    public String requestDefaultEffectParameters(String effectName) throws IOException 
    {
        Map<String, String> queryItems = new HashMap<String, String>();
        queryItems.put("effect", effectName);
        
        String info = serverInfoRequest("audioeffect-default-param", queryItems);

        if (info==null || info.length() == 0)
            return "";

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }

    public String requestFullEffect(String effectName, String currentEffectParameters) throws IOException 
    {
        Map<String, String> queryItems = new HashMap<String, String>();
        queryItems.put("effect", effectName);
        queryItems.put("params", currentEffectParameters);
        
        String info = serverInfoRequest("audioeffect-full", queryItems);

        if (info.length() == 0)
            return "";

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }
    
    public String requestEffectHelpText(String effectName) throws IOException
    {
        if (!data.audioEffectHelpTextsMap.containsKey(effectName))
        {
            Map<String, String> queryItems = new HashMap<String, String>();
            queryItems.put("effect", effectName);
            
            String info = serverInfoRequest("audioeffect-help", queryItems);
            
            if (info.length() == 0)
                return "";

            data.audioEffectHelpTextsMap.put(effectName, info.replaceAll("\n", System.getProperty("line.separator")));
        }
        
        return data.audioEffectHelpTextsMap.get(effectName);
    }
    
    public boolean isHMMEffect(String effectName) throws IOException
    {
        Map<String, String> queryItems = new HashMap<String, String>();
        queryItems.put("effect", effectName);
        
        String info = serverInfoRequest("audioeffect-is-hmm-effect", queryItems);
        
        if (info.length() == 0)
            return false;

        boolean bRet = false;
        info = info.toLowerCase();
        if (info.indexOf("yes")>-1)
            bRet = true;
        
        return bRet;
    }
    
    
    
    private String serverInfoRequest(String request, Map<String,String>queryItems)
    throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(data.hostAddress.getHttpAddress()).append("/").append(request);
        if (queryItems != null) {
            url.append("?");
            boolean first = true;
            for (String key : queryItems.keySet()) {
                if (first) first = false;
                else url.append("&");
                url.append(key).append("=");
                url.append(URLEncoder.encode(queryItems.get(key), "UTF-8"));
            }
        }
        return serverInfoRequest(new URL(url.toString()));
        
    }

    private String serverInfoRequest(URL url)
    throws IOException
    {
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("GET");
        http.connect();

        if(http.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(http.getResponseCode() + ":" + http.getResponseMessage());
        }
        return FileUtils.getStreamAsString(http.getInputStream(), "UTF-8");
/*  The following is example code if we were to use HttpClient:
 *         HttpClient httpclient = new DefaultHttpClient();

        HttpGet httpget = new HttpGet("http://www.google.com/"); 

        System.out.println("executing request " + httpget.getURI());

        // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);
        System.out.println(responseBody);
*/
    }
    
/*    private String getFromServer(String key) throws IOException
    {
        return getFromServer(key, null);
    }
    
    //This handles each request one by one
    private String getFromServer(String key, String params) throws IOException
    {
        if (data.keyValuePairs==null)
            data.keyValuePairs = new HashMap<String, String>();

        Map<String, String> singleKeyValuePair = new HashMap<String, String>();

        if (params==null || params.length()<1)
            singleKeyValuePair.put(key, "?");
        else 
            singleKeyValuePair.put(key, "? " + params);

        singleKeyValuePair = httpRequester.request(data.hostAddress, singleKeyValuePair);

        data.keyValuePairs.put(key, singleKeyValuePair.get(key));

        return data.keyValuePairs.get(key);
    }

  */  
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    ///////////////////////////////////////////////////////////////////////
    //////////////////////// Actual synthesis requests ////////////////////
    ///////////////////////////////////////////////////////////////////////
    
    
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
        _process(input, inputType, "AUDIO", locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, audioPlayer, 0, true, null, listener);
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
     * @param outputTypeParams any additional parameters, e.g. for output type TARGETFEATURES, the space-separated list of features to produce. Can be null. 
     * @param output the output stream into which the data from the server is to be written.
     * @throws IOException if communication with the server fails
     * @throws UnknownHostException if the host could not be found
     * @see #getInputDataTypes()
     * @see #getOutputDataTypes()
     * @see #getVoices()
     */
    public void process(String input, String inputType, String outputType, String locale,
        String audioType, String defaultVoiceName, String defaultStyle, Map<String, String> defaultEffects, String outputTypeParams, OutputStream output)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, 0, false, outputTypeParams, null);
    }
    
    public void process(String input, String inputType, String outputType, String locale,
            String audioType, String defaultVoiceName, OutputStream output)
            throws UnknownHostException, IOException, Exception
    {
        process( input,  inputType,  outputType, locale, audioType,  defaultVoiceName,  "", null, null, output);
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
     * @param outputTypeParams any additional parameters, e.g. for output type TARGETFEATURES, the space-separated list of features to produce. Can be null. 
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
        String audioType, String defaultVoiceName, String defaultStyle, Map<String, String> defaultEffects, String outputTypeParams,
        OutputStream output, long timeout)
        throws UnknownHostException, IOException, Exception
    {
        _process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, timeout, false, outputTypeParams, null);
    }

    public void process(String input, String inputType, String outputType, String locale,
         String audioType, String defaultVoiceName, OutputStream output, long timeout)
         throws UnknownHostException, IOException, Exception
    {
        process(input,  inputType, outputType, locale, audioType,  defaultVoiceName, "",  null, null, output, timeout);
    }

    private void _process(String input, String inputType, String outputType, String locale, String audioType, 
                          String defaultVoiceName, String defaultStyle, Map<String, String> defaultEffects, 
                          Object output, long timeout, boolean streamingAudio, String outputTypeParams, AudioPlayerListener playerListener)
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
                                                                streamingAudio, outputTypeParams);

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
        System.err.println("                -Dlocale=LOCALE");
        System.err.println("                -Daudio.type=AUDIOTYPE");
        System.err.println("                -Dvoice.default=male|female|de1|de2|de3|...");
        System.err.println("                -Dserver.host=HOSTNAME");
        System.err.println("                -Dserver.port=PORTNUMBER");
        System.err.println(
            "where INPUTTYPE is one of TEXT, RAWMARYXML, TOKENS, WORDS, POS,");
        System.err.println(
            "                          PHONEMES, INTONATION, ALLOPHONES, ACOUSTPARAMS or MBROLA,");
        System.err.println("     OUTPUTTYPE is one of TOKENS, WORDS, POS, PHONEMES,");
        System.err.println("                          INTONATION, ALLOPHONES, ACOUSTPARAMS, MBROLA, or AUDIO,");
        System.err.println("     LOCALE is the language and/or the country (e.g., de, en_US);");
        System.err.println("and AUDIOTYPE is one of AIFF, AU, WAVE, MP3, and Vorbis.");
        System.err.println("The default values for input.type and output.type are TEXT and AUDIO,");
        System.err.println("respectively; default locale is en_US; the default audio.type is WAVE.");
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
        String inputType = System.getProperty("input.type", "TEXT");
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
        String defaultVoiceName = System.getProperty("voice.default");
        String defaultStyle = "";
        Map<String, String> defaultEffects = null;
        String outputTypeParams = System.getProperty("output.type.params"); // null if not present
        
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
            mc.process(sb.toString(), inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, outputTypeParams, System.out);
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
