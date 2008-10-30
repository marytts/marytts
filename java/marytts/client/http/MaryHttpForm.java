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

package marytts.client.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author oytun.turk
 *
 * This class nests all the information and functions that a Mary client needs
 * to receive/send data from/to server.
 * To be able to use the functionality provided by this class, all Mary clients should either:
 * (i) extend this class (Example: MaryHttpClient, MaryWebHttpClient)
 * (ii) use an object of type MaryHttpForm or of a derived class (Example: MaryGUIHttpClient)
 * 
 */
public class MaryHttpForm {
    // Default values which can be overridden from the command line.
    private final String DEFAULT_HOST = "cling.dfki.uni-sb.de";
    private final int DEFAULT_PORT = 59125;
    protected String host;
    protected int port;
    protected String[] serverVersionInfo = null;
    protected String serverVersionNo = "unknown";
    protected boolean serverCanStream = false;
    
    protected Vector<MaryHttpForm.Voice> allVoices = null;
    protected Map<Locale, Vector<MaryHttpForm.Voice>> voicesByLocaleMap = null;
    protected Vector<MaryHttpForm.DataType> allDataTypes = null;
    protected Vector<MaryHttpForm.DataType> inputDataTypes = null;
    protected Vector<MaryHttpForm.DataType> outputDataTypes = null;
    protected Map<String, String> serverExampleTexts = new HashMap<String, String>();
    protected Map<String, String> voiceExampleTexts = new HashMap<String, String>();
    protected String audioEffects;
    protected Map<String, String> audioEffectHelpTextsMap = new HashMap<String, String>();
    protected String[] audioFileFormatTypes = null;

    protected MaryHttpRequester httpRequester;
    
    public MaryHttpForm()
    {
        this("");
    }
    
    public MaryHttpForm(String httpRequestHeader)
    {
        String serverHost = System.getProperty("server.host", DEFAULT_HOST);
        int serverPort = 0;
        String helperString = System.getProperty("server.port");
        if (helperString != null)
            serverPort = Integer.decode(helperString).intValue();
        else
            serverPort = DEFAULT_PORT;
        
        initialise(serverHost, serverPort, httpRequestHeader);
    }
    
    public MaryHttpForm(String serverHost, int serverPort)
    {
        initialise(serverHost, serverPort, "");
    }
    
    public MaryHttpForm(String serverHost, int serverPort, String httpRequestHeader)
    {
        initialise(serverHost, serverPort, httpRequestHeader);
    }
    
    public void initialise(String serverHost, int serverPort, String httpRequestHeader)
    {
        if (serverHost == null || serverPort == 0)
            throw new IllegalArgumentException("Illegal server host or port");
        
        this.host = serverHost;
        this.port = serverPort;
        
        httpRequester = new MaryHttpRequester(httpRequestHeader);
    }
    
    public void fillServerVersion() throws IOException, InterruptedException
    {
        // Expect 3 lines of the kind
        // Mary TTS server
        // Specification version 1.9.1
        // Implementation version 20030207
        serverVersionInfo = httpRequester.requestStringArray(host, port, "MARY VERSION");  

        serverVersionNo = "unknown";
        
        if (serverVersionInfo!=null)
        {
            StringTokenizer st = new StringTokenizer(serverVersionInfo[0]);
            while (st.hasMoreTokens()) {
                String t = st.nextToken();
                if (t.matches("^[0-9].*")) {
                    serverVersionNo = t;
                    break;
                }
            }
        }

        if (serverVersionNo.equals("unknown")
                || serverVersionNo.compareTo("3.0.1") < 0) {
            serverCanStream = false;
        } else {
            serverCanStream = true;
        }
    }
    
    public boolean isServerNotOlderThan(String serverVersionToCompare) throws IOException, InterruptedException
    {
        fillServerVersion();
        
        int tmp = serverVersionNo.compareToIgnoreCase(serverVersionToCompare);
        
        if (tmp>=0)
            return true;
        else
            return false;
    }
    
    public InputStream requestInputStream(String input, String inputType, String outputType, String locale, String audioType, 
                                          String defaultVoiceName, String defaultStyle, String defaultEffects,
                                          boolean streamingAudio) throws IOException, InterruptedException
    {
        String toServer = "MARY IN=" + inputType + " OUT=" + outputType + " LOCALE="+ locale;
        
        if (audioType != null) 
        {
            if (streamingAudio && serverCanStream)
                toServer += " AUDIO=STREAMING_"+audioType;
            else
                toServer += " AUDIO=" + audioType;
        }
        
        if (defaultVoiceName != null) 
            toServer += " VOICE=" + defaultVoiceName;
        
        if (defaultStyle != "")
            toServer += " STYLE=" + defaultStyle;
        
        if (defaultEffects != "")
            toServer += " EFFECTS=" + defaultEffects;

        toServer += System.getProperty("line.separator");
        toServer += input.trim() + System.getProperty("line.separator");
        
        return httpRequester.requestInputStream(host, port, toServer);
    }
    
    /**
     * Obtain a list of all data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryHttpForm.DataType> getAllDataTypes() throws IOException, InterruptedException
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
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryHttpForm.DataType> getInputDataTypes() throws IOException, InterruptedException
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
    public Vector<MaryHttpForm.DataType> getOutputDataTypes() throws IOException, InterruptedException
    {
        if (outputDataTypes == null)
            fillDataTypes();
       
        assert outputDataTypes != null && outputDataTypes.size() > 0;
        
        return outputDataTypes;
    }

    protected void fillDataTypes() throws IOException, InterruptedException 
    {
        allDataTypes = new Vector<MaryHttpForm.DataType>();
        inputDataTypes = new Vector<MaryHttpForm.DataType>();
        outputDataTypes = new Vector<MaryHttpForm.DataType>();
        
        // Expect a variable number of lines of the kind
        // RAWMARYXML INPUT OUTPUT
        // TEXT_DE LOCALE=de INPUT
        // AUDIO OUTPUT
        String info = httpRequester.requestString(host, port, "MARY LIST DATATYPES");
        
        if (info.length() == 0)
            throw new IOException("Could not get list of data types from Mary server");
        
        String[] typeStrings = info.split("\n");

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
    public Vector<MaryHttpForm.Voice> getVoices() throws IOException, InterruptedException
    {
        if (allVoices == null)
            fillVoices();
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
    public Vector<MaryHttpForm.Voice> getVoices(Locale locale) throws IOException, InterruptedException
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
    public Vector<MaryHttpForm.Voice> getGeneralDomainVoices() throws IOException, InterruptedException
    {
        Vector<MaryHttpForm.Voice> voices = getVoices();
        Vector<MaryHttpForm.Voice> requestedVoices = new Vector<MaryHttpForm.Voice>();
        
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
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryHttpForm.Voice> getLimitedDomainVoices() throws IOException, InterruptedException
    {
        Vector<MaryHttpForm.Voice> voices = getVoices();
        Vector<MaryHttpForm.Voice> requestedVoices = new Vector<MaryHttpForm.Voice>();
        
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
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryHttpForm.Voice> getGeneralDomainVoices(Locale locale) throws IOException, InterruptedException
    {
        Vector<MaryHttpForm.Voice> voices = getVoices(locale);
        Vector<MaryHttpForm.Voice> requestedVoices = new Vector<MaryHttpForm.Voice>();
        
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
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryHttpForm.Voice> getLimitedDomainVoices(Locale locale) throws IOException, InterruptedException
    {
        Vector<MaryHttpForm.Voice> voices = getVoices(locale);
        Vector<MaryHttpForm.Voice> requestedVoices = new Vector<MaryHttpForm.Voice>();
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

    protected void fillVoices() throws IOException, InterruptedException
    {
        allVoices = new Vector<MaryHttpForm.Voice>();
        voicesByLocaleMap = new HashMap<Locale,Vector<MaryHttpForm.Voice>>();

        // Expect a variable number of lines of the kind
        // de7 de female
        // us2 en male
        // dfki-stadium-emo de male limited
        String info = httpRequester.requestString(host, port, "MARY LIST VOICES");
        
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
            Vector<MaryHttpForm.Voice> localeVoices = null;
            if (voicesByLocaleMap.containsKey(locale)) {
                localeVoices = voicesByLocaleMap.get(locale);
            } else {
                localeVoices = new Vector<MaryHttpForm.Voice>();
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
    public String getVoiceExampleText(String voicename) throws IOException, InterruptedException 
    {
        if (!voiceExampleTexts.containsKey(voicename)) 
        {
            String info = httpRequester.requestString(host, port, "MARY VOICE EXAMPLETEXT " + voicename);
            
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");

            voiceExampleTexts.put(voicename, info.replaceAll("\n", System.getProperty("line.separator")));
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
    public String getServerExampleText(String dataType, String locale) throws IOException, InterruptedException
    {
        if (!serverExampleTexts.containsKey(dataType+" "+locale)) 
        {
            String info = httpRequester.requestString(host, port, "MARY EXAMPLETEXT " + dataType + " " + locale);
            
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");
            
            serverExampleTexts.put(dataType+" "+locale, info.replaceAll("\n", System.getProperty("line.separator")));

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
    public String getAudioEffects() throws IOException, InterruptedException 
    {
        String info = httpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTS");
        
        if (info.length() == 0)
            return "";

        audioEffects = info;

        return audioEffects;
    }
    
    public String getAudioEffectHelpTextLineBreak() throws IOException, InterruptedException 
    {
        String info = httpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTHELPTEXTLINEBREAK");
        if (info.length() == 0)
            return "";

        return info.trim();
    }
    
    public String requestEffectParametersChange(String effectName, String strParamNew) throws IOException, InterruptedException 
    {
        String info = httpRequester.requestString(host, port, "MARY VOICE SETAUDIOEFFECTPARAM " + effectName + "_" + strParamNew);
        
        if (info.length() == 0)
            return "";
        
        return requestEffectParametersAsString(effectName);
    }
    
    public String requestEffectParametersAsString(String effectName) throws IOException, InterruptedException 
    { 
        String info = httpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTPARAM " + effectName);
        
        if (info.length() == 0)
            return "";

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }

    public String requestFullEffectAsString(String effectName) throws IOException, InterruptedException 
    {
        String info = httpRequester.requestString(host, port, "MARY VOICE GETFULLAUDIOEFFECT " + effectName);
        
        if (info.length() == 0)
            return "";

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }
    
    public String requestEffectHelpText(String effectName) throws IOException, InterruptedException 
    {
        if (!audioEffectHelpTextsMap.containsKey(effectName))
        {
            String info = httpRequester.requestString(host, port, "MARY VOICE GETAUDIOEFFECTHELPTEXT " + effectName);
            
            if (info.length() == 0)
                return "";

            audioEffectHelpTextsMap.put(effectName, info.replaceAll("\n", System.getProperty("line.separator")));
        }
        
        return audioEffectHelpTextsMap.get(effectName);
    }
    
    public boolean isHMMEffect(String effectName) throws IOException, InterruptedException
    {
        String info = httpRequester.requestString(host, port, "MARY VOICE ISHMMAUDIOEFFECT " + effectName);
        
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
    public String[] getAudioFileFormatTypes() throws IOException, InterruptedException
    {
        fillAudioFileFormatTypes();

        return audioFileFormatTypes;
    }
    
    protected void fillAudioFileFormatTypes()  throws IOException, InterruptedException
    {
        if (audioFileFormatTypes == null) 
            audioFileFormatTypes = httpRequester.requestStringArray(host, port, "MARY LIST AUDIOFILEFORMATTYPES");
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
    
    // get the server host to which this client connects
    public String getHost() { return host; }
    // get the server port to which this client connects
    public int getPort() { return port; }
    

    
    /**
     * An abstraction of server info about available voices.
     * @author Marc Schr&ouml;der
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

}
