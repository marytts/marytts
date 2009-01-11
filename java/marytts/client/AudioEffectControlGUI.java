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

package marytts.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * 
 * An AudioEffectControlGUI consists of a checkbox, a text pane, a text field, and a button.
 * <p>
 * The checkbox indicates whether the effect will be applied or not.
 * <p>
 * The label contains the name of the effect.
 * <p>
 * The text field contains the parameters of the effect.
 * <p>
 * The button shows help information about the usage of the effect when clicked.
 * <p>
 * 
 * @author Oytun T&uumlrk
 */
public class AudioEffectControlGUI {
    
    private AudioEffectControlData data; //All data this control has about the audio effect 
    public JPanel mainPanel;
    public JCheckBox chkEnabled;
    public JTextField txtParams;
    public JButton btnHelp;
    
    private boolean isVisible; //This can be used for not showing a specific effect for specific voices
    public boolean isHelpWindowOpen;
    private JFrame helpWindow; //Window to show help context
    
    //Create a Mary audio effect with help text
    public AudioEffectControlGUI(AudioEffectControlData dataIn)
    { 
        data = dataIn;
        
        mainPanel = new JPanel();
        chkEnabled = new JCheckBox();
        txtParams = new JTextField("Parameters");

        btnHelp = new JButton("?");

        isVisible = true;
        isHelpWindowOpen = false;
        
    }
    
    public void setVisible(boolean bShow) { isVisible = bShow; }
    public boolean getVisible() { return isVisible; }
    
    public AudioEffectControlData getData() { return data; }

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
            chkEnabled.setText(data.getEffectName());
            chkEnabled.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    data.setSelected(((JCheckBox)e.getSource()).isSelected());     
                }
            });
            mainPanel.add(chkEnabled);
            
            c.gridx = 1;
            g.setConstraints(chkEnabled, c);
            txtParams.setPreferredSize(new Dimension(150,25));
            txtParams.setText(data.getParams());
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
                        JTextArea helpTextArea = new JTextArea(data.getHelpText());
                        helpTextArea.setEditable(false);

                        helpWindow.getContentPane().add(helpTextArea, BorderLayout.WEST);
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
}
