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
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.client.AudioEffectsBoxData;
import marytts.server.http.Address;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

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
public class MaryHtmlForm {
    // Default values which can be overridden from the command line.
    private final String DEFAULT_HOST = "cling.dfki.uni-sb.de";
    private final int DEFAULT_PORT = 59125;
    protected Address hostAddress = null;
    protected String serverVersionInfo = null;
    protected String serverVersionNo = "unknown";
    protected boolean serverCanStream = false;
    
    public Vector<MaryHtmlForm.Voice> allVoices;
    public Map<Locale, Vector<MaryHtmlForm.Voice>> voicesByLocaleMap;
    public Map<String, Vector<String>> limitedDomainVoices;
    public Vector<MaryHtmlForm.DataType> allDataTypes;
    public Vector<MaryHtmlForm.DataType> inputDataTypes;
    public Vector<MaryHtmlForm.DataType> outputDataTypes;
    public Map<String, String> serverExampleTexts;
    public String currentExampleText;
    public Map<String, String> voiceExampleTextsLimitedDomain;
    public Map<String, String> voiceExampleTextsGeneralDomain;
    public Map<String, String> audioEffectHelpTextsMap;
    public String[] audioFileFormatTypes;
    public String inputText;
    public String outputText;
    public boolean isOutputText;
    public int voiceSelected;
    public int inputTypeSelected;
    public int outputTypeSelected;
    public int audioFormatSelected;
    public int limitedDomainExampleTextSelected;
    public String audioEffects;
    public String audioEffectsHelpTextLineBreak;
    public AudioEffectsBoxData effectsBoxData;
    public Map<String, String> keyValuePairs;
    public Vector<String> limitedDomainExampleTexts;
    
    protected MaryHttpRequester httpRequester;
    
    public MaryHtmlForm() throws IOException, InterruptedException
    {
        String serverHost = System.getProperty("server.host", DEFAULT_HOST);
        int serverPort = 0;
        String helperString = System.getProperty("server.port");
        if (helperString != null)
            serverPort = Integer.decode(helperString).intValue();
        else
            serverPort = DEFAULT_PORT;
        
        Address serverAddress = new Address(serverHost, serverPort);
        initialiseFromParameterString(serverAddress, null, null, null, null, null, null, null, null);
    }
    
    public MaryHtmlForm(Address serverAddress) throws IOException, InterruptedException
    {
        initialiseFromParameterString(serverAddress, null, null, null, null, null, null, null, null);
    }
    
    public MaryHtmlForm(String fullParameters,
                        String versionIn,
                        String voicesIn,
                        String dataTypesIn,
                        String audioFileFormatTypesIn,
                        String audioEffectHelpTextLineBreakIn,
                        String defaultAudioEffects,
                        Vector<String> defaultVoiceExampleTexts) throws IOException, InterruptedException
    {
        String serverHost = System.getProperty("server.host", DEFAULT_HOST);
        int serverPort = 0;
        String helperString = System.getProperty("server.port");
        if (helperString != null)
            serverPort = Integer.decode(helperString).intValue();
        else
            serverPort = DEFAULT_PORT;
        
        Address serverAddress = new Address(serverHost, serverPort);
        
        initialiseFromParameterString(serverAddress, 
                   fullParameters, 
                   versionIn, 
                   voicesIn, 
                   dataTypesIn, 
                   audioFileFormatTypesIn, 
                   audioEffectHelpTextLineBreakIn, 
                   defaultAudioEffects,
                   defaultVoiceExampleTexts);
    }
    
    public MaryHtmlForm(Address serverAddress,
                        String fullParameters,
                        String versionIn,
                        String voicesIn,
                        String dataTypesIn,
                        String audioFileFormatTypesIn,
                        String audioEffectHelpTextLineBreakIn,
                        String defaultAudioEffects,
                        Vector<String> defaultVoiceExampleTexts) throws IOException, InterruptedException
    {
        initialiseFromParameterString(serverAddress, 
                   fullParameters, 
                   versionIn, 
                   voicesIn, 
                   dataTypesIn, 
                   audioFileFormatTypesIn, 
                   audioEffectHelpTextLineBreakIn, 
                   defaultAudioEffects, 
                   defaultVoiceExampleTexts);
    }
    
    public MaryHtmlForm(Address serverAddress,
            Map<String, String> keyValuePairsIn,
            String versionIn,
            String voicesIn,
            String dataTypesIn,
            String audioFileFormatTypesIn,
            String audioEffectHelpTextLineBreakIn,
            String defaultAudioEffects,
            Vector<String> defaultVoiceExampleTexts) throws IOException, InterruptedException
    {
        initialiseFromKeyValuePairs(serverAddress, 
                keyValuePairsIn, 
                versionIn, 
                voicesIn, 
                dataTypesIn, 
                audioFileFormatTypesIn, 
                audioEffectHelpTextLineBreakIn, 
                defaultAudioEffects, 
                defaultVoiceExampleTexts);
    }
   
    public void init()
    {
        httpRequester = new MaryHttpRequester();
        hostAddress = null;
        String serverVersionInfo = null;
        String serverVersionNo = "unknown";
        boolean serverCanStream = false;
        
        allVoices = null;
        voicesByLocaleMap = null;
        limitedDomainVoices = new HashMap<String, Vector<String>>();
        allDataTypes = null;
        inputDataTypes = null;
        outputDataTypes = null;
        serverExampleTexts = new HashMap<String, String>();
        currentExampleText = "";
        voiceExampleTextsLimitedDomain = new HashMap<String, String>();
        voiceExampleTextsGeneralDomain = new HashMap<String, String>();
        audioEffectHelpTextsMap = new HashMap<String, String>();
        audioFileFormatTypes = null;
        inputText = "";
        outputText = "";
        isOutputText = false;
        voiceSelected = 0;
        inputTypeSelected = 0;
        outputTypeSelected = 0;
        audioFormatSelected = 0;
        limitedDomainExampleTextSelected = 0;
        audioEffects = "";
        audioEffectsHelpTextLineBreak = "";
        effectsBoxData = null;
        keyValuePairs = new HashMap<String, String>();
        limitedDomainExampleTexts = null;
    }
    
    public void initialiseFromParameterString(Address serverAddress, 
                           String fullParameters,
                           String versionIn,
                           String voicesIn,
                           String dataTypesIn,
                           String audioFileFormatTypesIn,
                           String audioEffectHelpTextLineBreakIn,
                           String defaultAudioEffects,
                           Vector<String> defaultVoiceExampleTexts) throws IOException, InterruptedException
    {
        init();
        
        hostAddress = serverAddress;
        
        toServerVersionInfo(versionIn);
        toVoices(voicesIn);
        toDataTypes(dataTypesIn);
        toAudioFileFormatTypes(audioFileFormatTypesIn);
        toAudioEffectsHelpTextLineBreak(audioEffectHelpTextLineBreakIn);
        toAudioEffects(defaultAudioEffects);
        toSelections(fullParameters, defaultVoiceExampleTexts);
    }

    public void initialiseFromKeyValuePairs(Address serverAddress, 
            Map<String, String> keyValuePairsIn,
            String versionIn,
            String voicesIn,
            String dataTypesIn,
            String audioFileFormatTypesIn,
            String audioEffectHelpTextLineBreakIn,
            String defaultAudioEffects,
            Vector<String> defaultVoiceExampleTexts) throws IOException, InterruptedException
            {
        
        init();
        hostAddress = serverAddress;


        toServerVersionInfo(versionIn);
        toVoices(voicesIn);
        toDataTypes(dataTypesIn);
        toAudioFileFormatTypes(audioFileFormatTypesIn);
        toAudioEffectsHelpTextLineBreak(audioEffectHelpTextLineBreakIn);
        toAudioEffects(defaultAudioEffects);
        toSelections(keyValuePairsIn, defaultVoiceExampleTexts);
    }
    
    public void toServerVersionInfo(String info)
    {
        serverVersionInfo = info;

        serverVersionNo = "unknown";
        
        if (serverVersionInfo!=null)
        {
            StringTokenizer st = new StringTokenizer(serverVersionInfo);
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
    
    public void toVoices(String info) throws IOException, InterruptedException
    {
        allVoices = null;
        voicesByLocaleMap = null;
        limitedDomainVoices = null;

        if (info!=null && info.length()>0)
        {
            allVoices = new Vector<MaryHtmlForm.Voice>();
            voicesByLocaleMap = new HashMap<Locale,Vector<MaryHtmlForm.Voice>>();
            limitedDomainVoices = new HashMap<String, Vector<String>>();
            String[] voiceStrings = info.split("\n");

            for (int i=0; i<voiceStrings.length; i++) 
            {
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
                Vector<MaryHtmlForm.Voice> localeVoices = null;
                if (voicesByLocaleMap.containsKey(locale))
                    localeVoices = voicesByLocaleMap.get(locale);
                else 
                {
                    localeVoices = new Vector<MaryHtmlForm.Voice>();
                    voicesByLocaleMap.put(locale, localeVoices);
                }
                localeVoices.add(voice);

                if (voice.isLimitedDomain)
                {
                    String exampleText = getVoiceExampleTextLimitedDomain(voice.name());
                    limitedDomainVoices.put(voice.name(), StringUtils.processVoiceExampleText(exampleText));
                }
            }
        }
    }
    
    public void toDataTypes(String info)
    {
        allDataTypes = null;
        inputDataTypes = null;
        outputDataTypes = null;

        if (info!=null && info.length()>0)
        {
            allDataTypes = new Vector<MaryHtmlForm.DataType>();
            inputDataTypes = new Vector<MaryHtmlForm.DataType>();
            outputDataTypes = new Vector<MaryHtmlForm.DataType>();

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
    }
    
    public void toAudioFileFormatTypes(String info)
    {
        if (info!=null && info.length()>0)
            audioFileFormatTypes = StringUtils.toStringArray(info);
        else
            audioFileFormatTypes = null;
    }
    
    public void toAudioEffectsHelpTextLineBreak(String strLineBreak)
    {
        if (strLineBreak!=null && strLineBreak.length()>0)
            audioEffectsHelpTextLineBreak = strLineBreak;
        else
            audioEffectsHelpTextLineBreak = null;
    }
    
    public void toAudioEffects(String availableAudioEffects)
    {
        if (availableAudioEffects!=null && availableAudioEffects.length()>0)
            audioEffects = availableAudioEffects;
        else
            audioEffects = null;
        
        if (audioEffects!=null && audioEffects.length()>0)
            effectsBoxData = new AudioEffectsBoxData(audioEffects, audioEffectsHelpTextLineBreak);
        else
            effectsBoxData = null;
    }
    
    public void toSelections(String fullParameters, Vector<String> defaultVoiceExampleTexts) throws IOException, InterruptedException
    {        
        keyValuePairs = MaryHttpClientUtils.toKeyValuePairs(fullParameters, false);
        
        toSelections(keyValuePairs, defaultVoiceExampleTexts);
    }
    
    //Parse fullParamaters which is of the form key1=value1&key2=value2...
    public void toSelections(Map<String, String> keyValuePairsIn, Vector<String> defaultVoiceExampleTexts) throws IOException, InterruptedException
    {
        keyValuePairs = keyValuePairsIn;
        
        inputTypeSelected = 0;
        inputText = "";
        if (outputDataTypes!=null && outputDataTypes.size()>0)
            outputTypeSelected = outputDataTypes.size()-1;
        else
            outputTypeSelected = 0;
        
        isOutputText = false;
        outputText = "";
        audioFormatSelected = 0;
        voiceSelected = 0;
        limitedDomainExampleTextSelected = 0;
        
        if (effectsBoxData==null)
        {
            if (audioEffectsHelpTextLineBreak==null)
                getAudioEffectHelpTextLineBreak();
            if (audioEffects==null)
                getAudioEffects();
            
            effectsBoxData = new AudioEffectsBoxData(audioEffects, audioEffectsHelpTextLineBreak);
        }
        
        if (keyValuePairs!=null)
        {
            int i;
            String selected;
            
            //Input type selected
            selected = keyValuePairs.get("INPUT_TYPE");
            if (selected!=null)
            {
                for (i=0; i<inputDataTypes.size(); i++)
                {
                    if (inputDataTypes.get(i).name().compareTo(selected)==0)
                    {
                        inputTypeSelected = i;
                        break;
                    }
                }
            }
            //
        
            //Output type selected
            selected = keyValuePairs.get("OUTPUT_TYPE");
            if (selected!=null)
            {
                for (i=0; i<outputDataTypes.size(); i++)
                {
                    if (outputDataTypes.get(i).name().compareTo(selected)==0)
                    {
                        outputTypeSelected = i;
                        break;
                    }
                }

                //Check if output type contains AUDIO
                if (outputDataTypes.get(outputTypeSelected).name().contains("AUDIO"))
                    isOutputText = false;
                else
                    isOutputText = true;
            }
            //
            
            //Voice selected
            selected = keyValuePairs.get("voice");
            if (selected!=null)
            {
                for (i=0; i<allVoices.size(); i++)
                {
                    if (allVoices.get(i).name().compareTo(selected)==0)
                    {
                        voiceSelected = i;
                        break;
                    }
                }
            }
            //
        
            //Limited domain example texts
            if (allVoices!=null && allVoices.size()>0)
            {
                if (allVoices.elementAt(voiceSelected).isLimitedDomain())
                {
                    if (defaultVoiceExampleTexts==null)
                        limitedDomainExampleTexts = getVoiceExampleTextsLimitedDomain(allVoices.elementAt(voiceSelected).name());
                    else
                        limitedDomainExampleTexts = defaultVoiceExampleTexts;

                    selected = keyValuePairs.get("exampletext");
                    if (selected!=null)
                    {
                        for (i=0; i<limitedDomainExampleTexts.size(); i++)
                        {
                            if (limitedDomainExampleTexts.get(i).compareTo(selected)==0)
                            {
                                limitedDomainExampleTextSelected = i;
                                break;
                            }
                        }
                    }
                }
            }
            //

            //Input text
            selected = keyValuePairs.get("INPUT_TEXT");
            if (selected!=null)
                inputText = selected;
            else
            {
                if (allVoices!=null && allVoices.size()>0 && inputDataTypes!=null && inputDataTypes.size()>0)
                {
                    if (allVoices.elementAt(voiceSelected).isLimitedDomain())
                        inputText = limitedDomainExampleTexts.get(limitedDomainExampleTextSelected);
                    else
                        inputText = getServerExampleText(inputDataTypes.get(inputTypeSelected).name(), allVoices.elementAt(voiceSelected).getLocale().toString());
                } 
            }
            //
            
            //Output text if non-audio output
            if (isOutputText)
            {
                selected = keyValuePairs.get("OUTPUT_TEXT");
                if (selected!=null)
                    outputText = selected;
            }
            //
        
            //Audio format selected
            selected = keyValuePairs.get("audioformat");
            int spaceInd;
            if (selected!=null)
            {
                for (i=0; i<audioFileFormatTypes.length; i++)
                {
                    spaceInd = audioFileFormatTypes[i].indexOf(' ');
                    String typeName = audioFileFormatTypes[i].substring(spaceInd+1);
                    if (typeName.compareTo(selected)==0)
                    {
                        audioFormatSelected = i;
                        break;
                    }
                }
            }
            //
        
            //Audio effects
            String currentEffectName;
            for (i=0; i<effectsBoxData.getTotalEffects(); i++)
            {
                currentEffectName = effectsBoxData.getControlData(i).getEffectName();

                //Check box
                selected = keyValuePairs.get("effect_" + currentEffectName + "_selected"); 
                if (selected!=null && selected.compareTo("on")==0) //Effect is selected
                    effectsBoxData.getControlData(i).setSelected(true);
                else //If not found, effect is not selected
                    effectsBoxData.getControlData(i).setSelected(false);
                //

                //Parameters
                selected = keyValuePairs.get("effect_" + currentEffectName + "_parameters"); 
                if (selected!=null) //Effect paramaters is there
                    effectsBoxData.getControlData(i).setParams(selected);
                else //If not found, something is wrong, set parameters to default
                    effectsBoxData.getControlData(i).setEffectParamsToExample();
                // 
            }
            //
        } 
        else
        {
            //Limited domain example texts
            if (allVoices!=null && allVoices.size()>0)
            {
                if (allVoices.elementAt(voiceSelected).isLimitedDomain())
                {
                    if (defaultVoiceExampleTexts==null)
                        limitedDomainExampleTexts = getVoiceExampleTextsLimitedDomain(allVoices.elementAt(voiceSelected).name());
                    else
                        limitedDomainExampleTexts = defaultVoiceExampleTexts;
                }
            }
            //

            //Input text
            if (allVoices!=null && allVoices.size()>0 && inputDataTypes!=null && inputDataTypes.size()>0)
            {
                if (allVoices.elementAt(voiceSelected).isLimitedDomain())
                    inputText = limitedDomainExampleTexts.get(limitedDomainExampleTextSelected);
                else
                    inputText = getServerExampleText(inputDataTypes.get(inputTypeSelected).name(), allVoices.elementAt(voiceSelected).getLocale().toString());
            } 
            //
        }
    }
    
    public void fillServerVersion() throws IOException, InterruptedException
    {
        toServerVersionInfo(getFromServer("VERSION"));
    }
    
    public String getFromServer(String key) throws IOException, InterruptedException
    {
        return getFromServer(key, null);
    }
    
    //This handles each request one by one
    public String getFromServer(String key, String params) throws IOException, InterruptedException
    {
        if (keyValuePairs==null)
            keyValuePairs = new HashMap<String, String>();

        Map<String, String> singleKeyValuePair = new HashMap<String, String>();

        if (params==null || params.length()<1)
            singleKeyValuePair.put(key, "?");
        else 
            singleKeyValuePair.put(key, "? " + params);

        singleKeyValuePair = httpRequester.request(hostAddress, singleKeyValuePair);

        keyValuePairs.put(key, singleKeyValuePair.get(key));

        return keyValuePairs.get(key);
    }
    
    public boolean isServerNotOlderThan(String serverVersionToCompare) throws IOException, InterruptedException
    {
        if (serverVersionNo.compareTo("unknown")==0)
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
        keyValuePairs.put("SYNTHESIS_OUTPUT", "?");
        
        keyValuePairs.put("INPUT_TEXT", input);
        keyValuePairs.put("INPUT_TYPE", inputType);
        keyValuePairs.put("OUTPUT_TYPE", outputType);
        keyValuePairs.put("LOCALE", locale);
        
        if (audioType != null)
            keyValuePairs.put("AUDIO", ( (streamingAudio && serverCanStream) ? ("STREAMING_"+audioType) : (audioType) ) );
        
        if (defaultVoiceName != null) 
            keyValuePairs.put("VOICE", defaultVoiceName);
        
        keyValuePairs.put("STYLE", defaultStyle);
        keyValuePairs.put("EFFECTS", defaultEffects);
        
        return httpRequester.requestInputStream(hostAddress, keyValuePairs);
    }
    
    /**
     * Obtain a list of all data types known to the server. If the information is not
     * yet available, the server is queried. This is optional information
     * which is not required for the normal operation of the client, but
     * may help to avoid incompatibilities.
     * @throws Exception if communication with the server fails
     */
    public Vector<MaryHtmlForm.DataType> getAllDataTypes() throws IOException, InterruptedException
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
    public Vector<MaryHtmlForm.DataType> getInputDataTypes() throws IOException, InterruptedException
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
    public Vector<MaryHtmlForm.DataType> getOutputDataTypes() throws IOException, InterruptedException
    {
        if (outputDataTypes == null)
            fillDataTypes();
       
        assert outputDataTypes != null && outputDataTypes.size() > 0;
        
        return outputDataTypes;
    }

    protected void fillDataTypes() throws IOException, InterruptedException 
    {  
        toDataTypes(getFromServer("DATA_TYPES"));
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
    public Vector<MaryHtmlForm.Voice> getVoices() throws IOException, InterruptedException
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
    public Vector<MaryHtmlForm.Voice> getVoices(Locale locale) throws IOException, InterruptedException
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
    public Vector<MaryHtmlForm.Voice> getGeneralDomainVoices() throws IOException, InterruptedException
    {
        Vector<MaryHtmlForm.Voice> voices = getVoices();
        Vector<MaryHtmlForm.Voice> requestedVoices = new Vector<MaryHtmlForm.Voice>();
        
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
    public Vector<MaryHtmlForm.Voice> getLimitedDomainVoices() throws IOException, InterruptedException
    {
        Vector<MaryHtmlForm.Voice> voices = getVoices();
        Vector<MaryHtmlForm.Voice> requestedVoices = new Vector<MaryHtmlForm.Voice>();
        
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
    public Vector<MaryHtmlForm.Voice> getGeneralDomainVoices(Locale locale) throws IOException, InterruptedException
    {
        Vector<MaryHtmlForm.Voice> voices = getVoices(locale);
        Vector<MaryHtmlForm.Voice> requestedVoices = new Vector<MaryHtmlForm.Voice>();
        
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
    public Vector<MaryHtmlForm.Voice> getLimitedDomainVoices(Locale locale) throws IOException, InterruptedException
    {
        Vector<MaryHtmlForm.Voice> voices = getVoices(locale);
        Vector<MaryHtmlForm.Voice> requestedVoices = new Vector<MaryHtmlForm.Voice>();
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
        toVoices(getFromServer("VOICES"));
    }
    
    /**
     * Request the example text of a limited domain
     * unit selection voice from the server
     * @param voicename the voice
     * @return the example text
     * @throws IOException
     * @throws UnknownHostException
     */
    public String getVoiceExampleTextLimitedDomain(String voicename) throws IOException, InterruptedException 
    {
        if (!voiceExampleTextsLimitedDomain.containsKey(voicename)) 
        {
            String info = getFromServer("VOICE_EXAMPLE_TEXT", voicename);
            
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");

            voiceExampleTextsLimitedDomain.put(voicename, info.replaceAll("\n", System.getProperty("line.separator")));
        }

        return voiceExampleTextsLimitedDomain.get(voicename);
    }
    
    public Vector<String> getVoiceExampleTextsLimitedDomain(String voicename) throws IOException, InterruptedException
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
    public String getServerExampleText(String dataType, String locale) throws IOException, InterruptedException
    {
        if (!serverExampleTexts.containsKey(dataType+" "+locale)) 
        {
            String info = getFromServer("EXAMPLE_TEXT", dataType + " " + locale);
            
            if (info.length() == 0)
                throw new IOException("Could not get example text from Mary server");
            
            serverExampleTexts.put(dataType+" "+locale, info.replaceAll("\n", System.getProperty("line.separator")));
        }
        
        currentExampleText = serverExampleTexts.get(dataType+" "+locale);
        
        return currentExampleText;
    }

    /**
     * Request the available audio effects for a voice from the server
     * @param voicename the voice
     * @return A string of available audio effects and default parameters, i.e. "FIRFilter,Robot(amount=50)"
     * @throws IOException
     * @throws UnknownHostException
     */
    public String getDefaultAudioEffects() throws IOException, InterruptedException 
    {
        String defaultAudioEffects = getFromServer("DEFAULT_AUDIO_EFFECTS");

        return defaultAudioEffects;
    }

    public String getAudioEffects() throws IOException, InterruptedException 
    {
        if (audioEffects==null)
            audioEffects = getDefaultAudioEffects();

        return audioEffects;
    }
    
    public AudioEffectsBoxData getAudioEffectsBox() throws IOException, InterruptedException
    {
        if (effectsBoxData==null)
            effectsBoxData = new AudioEffectsBoxData(getAudioEffects(), getAudioEffectHelpTextLineBreak());
        
        return effectsBoxData;
    }
    
    public String getAudioEffectHelpTextLineBreak() throws IOException, InterruptedException 
    {
        if (audioEffectsHelpTextLineBreak==null)
        {
            String info = getFromServer("AUDIO_EFFECT_HELP_TEXT_LINE_BREAK");
            if (info==null || info.length() == 0)
                audioEffectsHelpTextLineBreak = null;
            else
                audioEffectsHelpTextLineBreak = info.trim();
        }

        return audioEffectsHelpTextLineBreak;
    }
    
    public String requestDefaultEffectParameters(String effectName) throws IOException, InterruptedException 
    { 
        String info = getFromServer("AUDIO_EFFECT_DEFAULT_PARAM", effectName);

        if (info==null || info.length() == 0)
            return "";

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }

    public String requestFullEffect(String effectName, String currentEffectParameters) throws IOException, InterruptedException 
    {
        String info = getFromServer("FULL_AUDIO_EFFECT", effectName + " " + currentEffectParameters);

        if (info.length() == 0)
            return "";

        return info.replaceAll("\n", System.getProperty("line.separator"));
    }
    
    public String requestEffectHelpText(String effectName) throws IOException, InterruptedException 
    {
        if (!audioEffectHelpTextsMap.containsKey(effectName))
        {
            String info = getFromServer("AUDIO_EFFECT_HELP_TEXT", effectName);
            
            if (info.length() == 0)
                return "";

            audioEffectHelpTextsMap.put(effectName, info.replaceAll("\n", System.getProperty("line.separator")));
        }
        
        return audioEffectHelpTextsMap.get(effectName);
    }
    
    public boolean isHMMEffect(String effectName) throws IOException, InterruptedException
    {
        String info = getFromServer("IS_HMM_AUDIO_EFFECT", effectName);
        
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
            toAudioFileFormatTypes(getFromServer("AUDIO_FILE_FORMAT_TYPES"));  
    }
    
    //Check if all selections are appropriately made, i.e. no array bounds exceeded etc
    public void checkAndCorrectSelections()
    {
        audioFormatSelected = MathUtils.CheckLimits(audioFormatSelected, 0, audioFileFormatTypes.length-1);
        inputTypeSelected = MathUtils.CheckLimits(inputTypeSelected, 0, inputDataTypes.size()-1);
        outputTypeSelected = MathUtils.CheckLimits(outputTypeSelected, 0, outputDataTypes.size()-1);
        voiceSelected = MathUtils.CheckLimits(voiceSelected, 0, allVoices.size()-1);
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
    public String getHost() { return hostAddress.host; }
    // get the server port to which this client connects
    public int getPort() { return hostAddress.port; }

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
