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

package marytts.server.http;

import java.util.Collection;
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
import marytts.datatypes.MaryDataType;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.signalproc.effects.BaseAudioEffect;
import marytts.signalproc.effects.EffectsApplier;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;

import org.apache.log4j.Logger;

/**
 * @author oytun.turk
 *
 */
public class BaselineRequestProcessor {
    protected static Logger logger;
    
    public BaselineRequestProcessor()
    {
        logger = Logger.getLogger("server");
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
        String output = "";
        AudioFileFormat.Type[] audioTypes = AudioSystem.getAudioFileTypes();
        for (int t=0; t<audioTypes.length; t++)
            output += audioTypes[t].getExtension() + " " + audioTypes[t].toString() + System.getProperty("line.separator");

        return output;
    }

    public String getExampleText(String parameters)
    {
        return getExampleText(parameters, null);
    }
    
    public String getExampleText(String parameters, Map<String, String> pairs)
    {
        String output = "";
        StringTokenizer st = new StringTokenizer(parameters);
        if (st.hasMoreTokens()) 
        {
            String typeName = st.nextToken();
            try {
                //Next should be locale:
                Locale locale = MaryUtils.string2locale(st.nextToken());
                MaryDataType type = MaryDataType.get(typeName);
                if (type != null) {
                    String exampleText = type.exampleText(locale);
                    if (exampleText != null)
                        output += exampleText.trim() + System.getProperty("line.separator");

                    if (pairs!=null)
                    {
                        if (type.isInputType())
                            pairs.put("INPUT_TEXT", output);
                        else
                            pairs.put("OUTPUT_TEXT", output);
                    }
                }
            } catch (Error err) {} // type doesn't exist
        }

        return output;
    }

    public Vector<String> getDefaultVoiceExampleTexts()
    {
        String defaultVoiceName = getDefaultVoiceName();
        Vector<String> defaultVoiceExampleTexts = null;
        defaultVoiceExampleTexts = StringUtils.processVoiceExampleText(getVoiceExampleText(defaultVoiceName, null));
        if (defaultVoiceExampleTexts==null) //Try for general domain
        {
            String str = getExampleText("TEXT" + " " + Voice.getVoice(defaultVoiceName).getLocale());
            if (str!=null && str.length()>0)
            {
                defaultVoiceExampleTexts = new Vector<String>();
                defaultVoiceExampleTexts.add(str);
            }
        }
        
        return defaultVoiceExampleTexts;
    }
    
    public String getVoiceExampleText(String parameters, Map<String, String> pairs)
    {
        String output = "";
        StringTokenizer st = new StringTokenizer(parameters);

        if (st.hasMoreTokens()) 
        {
            String voiceName = st.nextToken();
            Voice v = Voice.getVoice(voiceName);
            
            if (v != null) 
            {
                String text = "";
                if (v instanceof marytts.unitselection.UnitSelectionVoice)
                    output += ((marytts.unitselection.UnitSelectionVoice)v).getExampleText();
                
                if (pairs!=null)
                    pairs.put("INPUT_TEXT", output);
            }
        }

        return output;
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
    }

    public String getAudioEffectHelpTextLineBreak()
    {
        return BaseAudioEffect.strLineBreak;
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
                    String helpText = ae.getHelpText();
                    output = helpText.trim();
                }

                break;
            }
        }
        
        return output;
    }

    public String isHmmAudioEffect(String effectName)
    {
        String output = "";

        for (int i=0; i<MaryProperties.effectNames().size(); i++)
        {
            if (effectName.compareTo(MaryProperties.effectNames().elementAt(i))==0)
            {   
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
                    output = "no";

                    if (ae.isHMMEffect())
                        output = "yes";
                }

                break;
            }
        }
        
        return output;
    }
    
    protected String toRequestedAudioEffectsString(Map<String, String> keyValuePairs)
    {
        String effects = "";
        StringTokenizer tt;
        Set<String> keys = keyValuePairs.keySet();
        String currentKey;
        String currentEffectName, currentEffectParams;
        for (Iterator<String> it = keys.iterator(); it.hasNext();)
        {
            currentKey = it.next();
            if (currentKey.startsWith("effect_"))
            {
                if (currentKey.endsWith("_selected"))
                {
                    if (keyValuePairs.get(currentKey).compareTo("on")==0)
                    {
                        if (effects.length()>0)
                            effects += "+";
                        
                        tt = new StringTokenizer(currentKey, "_");
                        if (tt.hasMoreTokens()) tt.nextToken(); //Skip "effects_"
                        if (tt.hasMoreTokens()) //The next token is the effect name
                        {
                            currentEffectName = tt.nextToken();

                            currentEffectParams = keyValuePairs.get("effect_" + currentEffectName + "_parameters");
                            if (currentEffectParams!=null && currentEffectParams.length()>0)
                                effects += currentEffectName + "(" + currentEffectParams + ")";
                            else
                                effects += currentEffectName;
                        }
                    }
                }
            }
        }
        
        return effects;
    }
}
