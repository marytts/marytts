/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.server.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import javax.sound.sampled.AudioSystem;

import marytts.Version;
import marytts.client.http.Address;
import marytts.datatypes.MaryDataType;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.signalproc.effects.BaseAudioEffect;
import marytts.signalproc.effects.EffectsApplier;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;
import marytts.vocalizations.VocalizationSynthesizer;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.FileContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.entity.ContentListener;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * Provides baseline functionality to process http requests to the Mary server.
 * 
 * @author Oytun T&uuml;rk, Marc Schröder
 */
public abstract class BaseHttpRequestHandler extends SimpleNHttpRequestHandler implements HttpRequestHandler  
{
    private final boolean useFileChannels = true;
    
    protected static Logger logger;
    private int runningNumber = 1;
    private Map<String,Object[]> requestMap;

    public BaseHttpRequestHandler()
    {
        super();
        logger = MaryUtils.getLogger("server");
        requestMap = Collections.synchronizedMap(new HashMap<String, Object[]>());

    }
    

    /**
     * The entry point of all HttpRequestHandlers.
     * When this method returns, the response is sent to the client.
     */
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
    throws HttpException, IOException
    {
        try {
            Header[] tmp = request.getHeaders("Host");
            Address serverAddressAtClient = getServerAddressAtClient(tmp[0].getValue());
            String uri = request.getRequestLine().getUri();
            
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!(method.equals("GET") || method.equals("POST"))) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            String absPath = null;
            String query = null;
            if (uri != null && uri.length()>0) {
                if (!uri.startsWith("/")) {
                    throw new HttpException("Unexpected uri: '"+uri+"' does not start with a slash");
                }
                int q = uri.indexOf('?');
                if (q == -1) {
                    absPath = uri;
                } else {
                    absPath = uri.substring(0, q);
                    query = uri.substring(q+1);
                }
            }
            Map<String,String> queryItems = null;
            if (query != null && query.length() > 0) {
                queryItems = MaryHttpServerUtils.toKeyValuePairs(query, true);
            }
            
            //Try and get parameters from different HTTP POST requests if you have not been able to do this above
            if (method.equals("POST") && queryItems == null 
                    && request instanceof HttpEntityEnclosingRequest) {
                try {
                    String postQuery = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
                    queryItems = MaryHttpServerUtils.toKeyValuePairs(postQuery, true);
                } catch (Exception e) {
                    logger.debug("Cannot read post query", e);
                    MaryHttpServerUtils.errorInternalServerError(response, "Cannot read post query", e);
                }
            }

            //Parse request and create appropriate response
            handleClientRequest(absPath, queryItems, response, serverAddressAtClient);

        } catch (RuntimeException re) {
            logger.warn("runtime exception in handle():", re);
        }
    }

    protected abstract void handleClientRequest(String absPath, Map<String,String> queryItems, HttpResponse response, Address serverAddressAtClient)
    throws IOException;
    
    protected Address getServerAddressAtClient(String fullHeader)
    {
        String fullAddress = fullHeader.trim();
        int index = fullAddress.indexOf('?');
        
        if (index>0)
            fullAddress = fullAddress.substring(0, index);
        
        return new Address(fullAddress);
    }
    
    

    
    public ConsumingNHttpEntity entityRequest(
            final HttpEntityEnclosingRequest request,
            final HttpContext context) throws HttpException, IOException {
        return new ConsumingNHttpEntityTemplate(
                request.getEntity(),
                new FileWriteListener(useFileChannels));
    }
    
    
    
    
  
    
    
    
    

    
    public String getMaryVersion()
    {
        String output = "Mary TTS server " + Version.specificationVersion() + " (impl. " + Version.implementationVersion() + ")";

        return output;
    }

    public String getDataTypes()
    {
        String output = "";
        
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

        return output;
    }

    public String getVoices()
    {
        String output = "";
        Collection<Voice> voices = Voice.getAvailableVoices();
        for (Iterator<Voice> it = voices.iterator(); it.hasNext();) 
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
        
        return output;
    }
    
    public String getDefaultVoiceName()
    {
        String defaultVoiceName = "";
        String allVoices = getVoices();
        if (allVoices!=null && allVoices.length()>0)
        {
            StringTokenizer tt = new StringTokenizer(allVoices, System.getProperty("line.separator"));
            if (tt.hasMoreTokens())
            {
                defaultVoiceName = tt.nextToken();
                StringTokenizer tt2 = new StringTokenizer(defaultVoiceName, " ");
                if (tt2.hasMoreTokens())
                    defaultVoiceName = tt2.nextToken();
            }
        }
        
        return defaultVoiceName;
    }

    public String getAudioFileFormatTypes()
    {
        return MaryRuntimeUtils.getAudioFileFormatTypes();
    }

    public String getExampleText(String datatype, Locale locale)
    {
        MaryDataType type = MaryDataType.get(datatype);
        String exampleText = type.exampleText(locale);
        if (exampleText != null)
            return exampleText.trim() + System.getProperty("line.separator");
        return "";
    }

    public Vector<String> getDefaultVoiceExampleTexts()
    {
        String defaultVoiceName = getDefaultVoiceName();
        Vector<String> defaultVoiceExampleTexts = null;
        defaultVoiceExampleTexts = StringUtils.processVoiceExampleText(getVoiceExampleText(defaultVoiceName));
        if (defaultVoiceExampleTexts==null) //Try for general domain
        {
            String str = getExampleText("TEXT", Voice.getVoice(defaultVoiceName).getLocale());
            if (str!=null && str.length()>0)
            {
                defaultVoiceExampleTexts = new Vector<String>();
                defaultVoiceExampleTexts.add(str);
            }
        }
        
        return defaultVoiceExampleTexts;
    }
    
    public String getVoiceExampleText(String voiceName)
    {
        Voice v = Voice.getVoice(voiceName);
        if (v instanceof marytts.unitselection.UnitSelectionVoice)
            return ((marytts.unitselection.UnitSelectionVoice)v).getExampleText();
        return "";
    }
    
    /**
     * For the voice with the given name, return the list of vocalizations supported by this voice,
     * one vocalization per line.
     * These values can be used in the "name" attribute of the vocalization tag.
     * @param voiceName
     * @return the list of vocalizations, or the empty string if the voice does not support vocalizations.
     */
    public String getVocalizations(String voiceName) {
        Voice v = Voice.getVoice(voiceName);
        if (v == null || !v.hasVocalizationSupport()) {
            return "";
        }
        VocalizationSynthesizer vs = v.getVocalizationSynthesizer();
        assert vs != null;
        String[] vocalizations = vs.listAvailableVocalizations();
        assert vocalizations != null;
        return StringUtils.toString(vocalizations);
    }

    public String getDefaultAudioEffects()
    {
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
        /*
        String audioEffectClass = "<EffectSeparator>" + EffectsApplier.chEffectSeparator + "</EffectSeparator>";

        for (int i=0; i<MaryProperties.effectClasses().size(); i++)
        {
            audioEffectClass += "<Effect>";
            audioEffectClass += "<Name>" + MaryProperties.effectNames().elementAt(i) + "</Name>";
            audioEffectClass += "<Param>" + MaryProperties.effectSampleParams().elementAt(i) + "</Param>";
            audioEffectClass += "<SampleParam>" + MaryProperties.effectSampleParams().elementAt(i) + "</SampleParam>";
            audioEffectClass += "<HelpText>" + MaryProperties.effectHelpTexts().elementAt(i) + "</HelpText>";
            audioEffectClass += "</Effect>";
        }

        return audioEffectClass;
        */
        // Marc, 8.1.09: Simplified format
        // name params
        StringBuilder sb = new StringBuilder();
        Vector<String> names = MaryProperties.effectNames();
        Vector<String> params = MaryProperties.effectSampleParams();
        for (int i=0; i<MaryProperties.effectClasses().size(); i++) {
            sb.append(names.elementAt(i)).append(" ").append(params.elementAt(i)).append("\n");
        }
        return sb.toString();
    }


    public String getAudioEffectDefaultParam(String effectName)
    {
        String output = "";
        boolean bFound = false;
        for (int i=0; i<MaryProperties.effectNames().size(); i++)
        {
            //int tmpInd = inputLine.indexOf(MaryProperties.effectNames().elementAt(i));
            //if (tmpInd>-1)
            if (effectName.compareTo(MaryProperties.effectNames().elementAt(i))==0)
            {   
                //the request is about the parameters of a specific audio effect
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
                    String audioEffectParams = ae.getExampleParameters();
                    output = audioEffectParams.trim();
                }

                break;
            }
        }

        return output;
    }
    
    public String getFullAudioEffect(String effectName, String currentEffectParams)
    {
        String output = "";

        for (int i=0; i<MaryProperties.effectNames().size(); i++)
        {
            if (effectName.compareTo(MaryProperties.effectNames().elementAt(i))==0)
            {   
                //the request is about the parameters of a specific audio effect
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
                    ae.setParams(currentEffectParams);
                    output = ae.getFullEffectAsString();
                }

                break;
            }
        }
        return output;
    }

    public String getAudioEffectHelpText(String effectName)
    {
        for (int i=0; i<MaryProperties.effectNames().size(); i++) {
            if (effectName.equals(MaryProperties.effectNames().elementAt(i))) {   
                BaseAudioEffect ae = null;
                try {
                    ae = (BaseAudioEffect)Class.forName(MaryProperties.effectClasses().elementAt(i)).newInstance();
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (ClassNotFoundException e) {
                }

                if (ae!=null) {
                    return ae.getHelpText().trim();
                }
            }
        }
        return "";
    }

    public String isHmmAudioEffect(String effectName)
    {
        String output = "";

        for (int i=0; i<MaryProperties.effectNames().size(); i++) {
            if (effectName.equals(MaryProperties.effectNames().elementAt(i))) {   
                BaseAudioEffect ae = null;
                try {
                    ae = (BaseAudioEffect)Class.forName(MaryProperties.effectClasses().elementAt(i)).newInstance();
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (ClassNotFoundException e) {
                }

                if (ae!=null) {
                    output = "no";
                    if (ae.isHMMEffect())
                        output = "yes";
                }
                break;
            }
        }
        return output;
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



    
}

