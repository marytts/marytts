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
    
    public MaryAudioEffectsBox(String [] effectNames, String [] exampleParams, String [] helpTexts)
    {
        assert effectNames.length == helpTexts.length;
        assert effectNames.length == exampleParams.length;
        
        int numEffects = effectNames.length;
        
        mainPanel = new JPanel();
        effectsBoxLabel = new JLabel("Audio Effects:");
        effectControlsPanel = new JPanel();
        
        if (numEffects>0)
        {
            effectControls = new MaryAudioEffectControl[numEffects];

            for (int i=0; i<effectControls.length; i++)
                effectControls[i] = new MaryAudioEffectControl(effectNames[i], exampleParams[i], helpTexts[i]);
        }
        else
            effectControls = null;
    }
    
    public void show()
    {  
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
            c.fill = GridBagConstraints.WEST;
            
            for (int i=0; i<effectControls.length; i++)
            {
                c.gridy = i;
                g.setConstraints(effectControls[i].mainPanel, c);
                effectControlsPanel.add(effectControls[i].mainPanel);
                effectControls[i].show();
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
    }
}
