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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import de.dfki.lt.signalproc.effects.BaseAudioEffect;

/**
 * @author oytun.turk
 * 
 * A MaryEffectControl consists of a checkbox, a text pane, a text field, and a button
 * The checkbox indicates whether the effect will be applied or not
 * The label contains the name of the effect
 * The text field contains the parameters of the effect
 * The button shows help information about the usage of the effect when clicked
 */
public class MaryAudioEffectControl {
    public JPanel mainPanel;
    public JCheckBox chkEnabled;
    public JTextField txtParams;
    public JButton btnHelp;
    public String strHelpText;
    public String strExampleParams;
    public String strLineBreak;
    private boolean isVisible; //This can be used for not showing a specific effect for specific voices
    public boolean isHelpWindowOpen;
    private JFrame helpWindow; //Window to show help context
    
    //Create a Mary audio effect with help text
    public MaryAudioEffectControl(String strEffectNameIn, String strExampleParams, String strHelpTextIn, String lineBreak)
    { 
        mainPanel = new JPanel();
        chkEnabled = new JCheckBox();
        txtParams = new JTextField("Parameters");
        btnHelp = new JButton("?");
        strLineBreak = lineBreak;
        isVisible = true;
        isHelpWindowOpen = false;
 
        init(strEffectNameIn, strExampleParams, strHelpTextIn);
    }

    public void init(String strEffectNameIn, String strExampleParamsIn, String strHelpTextIn)
    {
        setEffectName(strEffectNameIn);
        setExampleParams(strExampleParamsIn);
        setHelpText(strHelpTextIn); 
        setEffectParamsToExample();
    }
    
    public void setVisible(boolean bShow)
    {
        if (bShow)
            isVisible = true;
        else
            isVisible = false;
    }
    
    public boolean isVisible()
    {
        return isVisible;
    }
    
    public void show()
    {
        mainPanel.removeAll();
        mainPanel.validate();
        
        if (isVisible)
        {
            GridBagLayout g = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();

            mainPanel.setLayout(g);

            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridx = 0;
            c.gridy = 0;
            g.setConstraints(chkEnabled, c);
            chkEnabled.setPreferredSize(new Dimension(100,25));
            mainPanel.add(chkEnabled);

            c.gridx = 1;
            g.setConstraints(chkEnabled, c);
            txtParams.setPreferredSize(new Dimension(150,25));
            mainPanel.add(txtParams);

            c.gridx = GridBagConstraints.RELATIVE;
            g.setConstraints(btnHelp, c);
            btnHelp.setPreferredSize(new Dimension(45,25));
            mainPanel.add(btnHelp);

            btnHelp.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!isHelpWindowOpen)
                    {
                        isHelpWindowOpen = true;
                        helpWindow = new JFrame("Help: " + chkEnabled.getText() + " Effect");
                        JTextArea helpText = new JTextArea(parseLineBreaks(strHelpText));
                        helpText.setEditable(false);

                        helpWindow.getContentPane().add(helpText, BorderLayout.WEST);
                        helpWindow.pack();
                        helpWindow.setLocation(btnHelp.getLocation().x, btnHelp.getLocation().y);
                        helpWindow.setVisible(true);
                        
                        helpWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                            public void windowClosing(WindowEvent winEvt) {
                                // Perhaps ask user if they want to save any unsaved files first.
                                isHelpWindowOpen = false;
                            }
                        });
                    }
                    else
                    {
                        if (helpWindow!=null)
                            helpWindow.requestFocus();
                    }
                }
            });
        }
        
        mainPanel.validate();
    }
    
    //Parse string according to line break string
    public String parseLineBreaks(String strInput)
    {
        String strOut = "";
        String strTmp;
        
        for (int i=0; i<strInput.length()-strLineBreak.length(); i++)
        {
            strTmp = strInput.substring(i, i+strLineBreak.length());
            
            if (strTmp.equals(strLineBreak)==false)
                strOut += strInput.substring(i, i+1);
            else
            {
                strOut += System.getProperty("line.separator");
                i += strLineBreak.length()-1;
            }
        }
        
        strTmp = strInput.substring(strInput.length()-strLineBreak.length(), strInput.length());
        if (strTmp.compareTo(strLineBreak)!=0)
            strOut += strTmp;
        else
            strOut += System.getProperty("line.separator");
        
        return strOut;
    }
    
    public boolean isSelected()
    {
        return chkEnabled.isSelected();
    }
    
    public void setEffectName(String strEffectName)
    {
        chkEnabled.setText(strEffectName);
    }
    
    public void setHelpText(String strHelpTextIn)
    {
        strHelpText = strHelpTextIn;
    }
    
    public void setEffectParams(String strEffectParams)
    {
        txtParams.setText(strEffectParams);
    }
    
    public void setEffectParamsToExample()
    {
        setEffectParams(strExampleParams);
    }
    
    public void setExampleParams(String strExampleParamsIn)
    {
        strExampleParams = strExampleParamsIn;
    }
    
    public String getEffectName()
    {
        return chkEnabled.getText();
    }
    
    public String getEffectParam()
    {
        return txtParams.getText();
    }
}
