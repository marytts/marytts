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

package de.dfki.lt.mary.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

/**
 * @author oytun.turk
 *
 */
public class MaryAudioEffectsBox {
    MaryAudioEffectControl [] effectControls;
    
    public JPanel mainPanel;
    public JLabel effectsBoxLabel;
    public JScrollPane scrollPane;
    public JPanel effectControlsPanel;
    
    public String [] effectNames;
    public String [] exampleParams;
    public String [] helpTexts;
    public char chEffectSeparator;
    
    //availableEffects is a one large string produced by the server in the following format:
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
    public MaryAudioEffectsBox(String availableEffects, String lineBreak)
    {
        if (availableEffects!=null || availableEffects!="")
        {
            int numEffects = parseAvailableEffects(availableEffects);

            mainPanel = new JPanel();
            effectsBoxLabel = new JLabel("Audio Effects:");
            effectControlsPanel = new JPanel();

            if (numEffects>0)
            {
                effectControls = new MaryAudioEffectControl[numEffects];

                for (int i=0; i<effectControls.length; i++)
                    effectControls[i] = new MaryAudioEffectControl(effectNames[i], exampleParams[i], helpTexts[i], lineBreak);
            }
            else
                effectControls = null;
        }
        else
            effectControls = null;
    }
    
    public boolean hasEffects()
    {
        if (effectControls!=null)
            return true;
        else
            return false;
    }
    
    //Parse the XML-like full effect set string from the server
    protected int parseAvailableEffects(String availableEffects)
    {
        effectNames = null;
        exampleParams = null;
        helpTexts = null;
        chEffectSeparator = ' ';
       
        int ind1, ind2, ind3, ind4;
        String strEffectName, strParams, strExampleParams, strHelpText;
        
        String effectSeparatorStartTag = "<EffectSeparator>";
        String effectSeparatorEndTag = "</EffectSeparator>";
        String effectStartTag = "<Effect>";
        String effectEndTag = "</Effect>";
        String nameStartTag = "<Name>";
        String nameEndTag = "</Name>";
        String paramStartTag = "<Param>";
        String paramEndTag = "</Param>";
        String sampleParamStartTag = "<SampleParam>";
        String sampleParamEndTag = "</SampleParam>";
        String helpTextStartTag = "<HelpText>";
        String helpTextEndTag = "</HelpText>";
        
        int totalEffects = 0;
        int currentIndex = 0;
        
        ind1 = availableEffects.indexOf(effectSeparatorStartTag, currentIndex);
        ind2 = availableEffects.indexOf(effectSeparatorEndTag, currentIndex);
        
        if (ind1>-1 && ind2>-1)
        {
            String strTmp = availableEffects.substring(ind1+effectSeparatorStartTag.length(), ind2);
            chEffectSeparator = strTmp.charAt(0);
            
            while (true)
            {
                ind1 = availableEffects.indexOf(effectStartTag, currentIndex);
                ind2 = availableEffects.indexOf(effectEndTag, currentIndex);

                if (ind1>-1 && ind2>-1)
                {
                    ind3 = availableEffects.indexOf(nameStartTag, currentIndex);
                    ind4 = availableEffects.indexOf(nameEndTag, currentIndex);

                    if (ind3>-1 && ind4>-1)
                    {
                        strEffectName = availableEffects.substring(ind3+nameStartTag.length(), ind4);

                        ind3 = availableEffects.indexOf(sampleParamStartTag, currentIndex);
                        ind4 = availableEffects.indexOf(sampleParamEndTag, currentIndex);


                        if (ind3>-1 && ind4>-1)
                        {
                            strParams = availableEffects.substring(ind3+paramStartTag.length(), ind4);

                            ind3 = availableEffects.indexOf(sampleParamStartTag, currentIndex);
                            ind4 = availableEffects.indexOf(sampleParamEndTag, currentIndex);

                            if (ind3>-1 && ind4>-1)
                            {
                                strExampleParams = availableEffects.substring(ind3+sampleParamStartTag.length(), ind4);

                                ind3 = availableEffects.indexOf(helpTextStartTag, currentIndex);
                                ind4 = availableEffects.indexOf(helpTextEndTag, currentIndex);

                                if (ind3>-1 && ind4>-1)
                                {
                                    strHelpText = availableEffects.substring(ind3+helpTextStartTag.length(), ind4);

                                    totalEffects++;
                                }
                                else
                                    break;
                            }
                            else
                                break;
                        }
                        else
                            break;
                    }
                    else
                        break;

                    currentIndex = ind2+1;

                    if (currentIndex>=availableEffects.length()-1)
                        break;
                }
                else
                    break;
            }
        }
        
        if (totalEffects>0)
        {
            effectNames = new String[totalEffects];
            exampleParams = new String[totalEffects];
            helpTexts = new String[totalEffects];
            
            currentIndex = 0;
            for (int i=0; i<totalEffects; i++)
            {
                ind1 = availableEffects.indexOf(effectStartTag, currentIndex);
                ind2 = availableEffects.indexOf(effectEndTag, currentIndex);

                if (ind1>-1 && ind2>-1)
                {
                    ind3 = availableEffects.indexOf(nameStartTag, currentIndex);
                    ind4 = availableEffects.indexOf(nameEndTag, currentIndex);

                    if (ind3>-1 && ind4>-1)
                    {
                        strEffectName = availableEffects.substring(ind3+nameStartTag.length(), ind4);

                        ind3 = availableEffects.indexOf(sampleParamStartTag, currentIndex);
                        ind4 = availableEffects.indexOf(sampleParamEndTag, currentIndex);


                        if (ind3>-1 && ind4>-1)
                        {
                            strExampleParams = availableEffects.substring(ind3+sampleParamStartTag.length(), ind4);

                            ind3 = availableEffects.indexOf(helpTextStartTag, currentIndex);
                            ind4 = availableEffects.indexOf(helpTextEndTag, currentIndex);

                            if (ind3>-1 && ind4>-1)
                            {
                                strHelpText = availableEffects.substring(ind3+helpTextStartTag.length(), ind4);
                                
                                effectNames[i] = strEffectName;
                                exampleParams[i] = strExampleParams;
                                helpTexts[i] = strHelpText;
                            }
                            else
                                break;
                        }
                        else
                            break;
                    }
                    else
                        break;

                    currentIndex = ind2+1;

                    if (currentIndex>=availableEffects.length()-1)
                        break;
                }
                else
                    break;
            }

        }

        return totalEffects;
    }
    
    public void show()
    {  
        mainPanel.removeAll();
        mainPanel.validate();
        
        effectControlsPanel.removeAll();
        effectControlsPanel.validate();
        
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        mainPanel.setLayout(g);
        
        c.fill = GridBagConstraints.VERTICAL;
        g.setConstraints(mainPanel, c);

        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 200;
        c.ipady = 20;
        c.fill = GridBagConstraints.CENTER;
        g.setConstraints(effectsBoxLabel, c);
        mainPanel.add(effectsBoxLabel);
        
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = 0;
        c.ipady = 0;
        g.setConstraints(effectControlsPanel, c);
        mainPanel.add(effectControlsPanel);
        
        if (effectControls!=null && effectControls.length>0)
        {  
            effectControlsPanel.setLayout(g);
            
            c.gridx = 0;
            c.fill = GridBagConstraints.BOTH;
            
            int totalShown = 0;
            for (int i=0; i<effectControls.length; i++)
            {
                if (effectControls[i].isVisible())
                {
                    c.gridy = totalShown;
                    g.setConstraints(effectControls[i].mainPanel, c);                    
                    effectControlsPanel.add(effectControls[i].mainPanel);
                    effectControls[i].show();

                    totalShown++;
                }
            }
        }

        //Add the scroll pane  
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = 300;
        c.ipady = 105;
        scrollPane = new JScrollPane(effectControlsPanel);
        scrollPane.setViewportView(effectControlsPanel);
        g.setConstraints(scrollPane, c);
        mainPanel.add(scrollPane);
        effectControlsPanel.validate();
        mainPanel.validate();
    }
    
    public int getTotalEffects()
    {
        if (effectControls!=null)
            return effectControls.length;
        else
            return 0;
    }
}
