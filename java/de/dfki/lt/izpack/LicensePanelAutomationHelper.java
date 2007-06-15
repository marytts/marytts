/*
 * IzPack - Copyright 2001-2006 Julien Ponge, All Rights Reserved.
 *  * Portions Copyright (c) 2006 DFKI GmbH.
 * http://www.izforge.com/izpack/
 * http://developer.berlios.de/projects/izpack/
 * 
 * Copyright 2003 Jonathan Halliday
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dfki.lt.izpack;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.n3.nanoxml.XMLElement;

import com.izforge.izpack.Pack;
import com.izforge.izpack.gui.ButtonFactory;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelAutomation;
import com.izforge.izpack.installer.ResourceManager;

/**
 * Functions to support automated usage of the TargetPanel
 * 
 * @author Jonathan Halliday
 * @author Julien Ponge
 */
public class LicensePanelAutomationHelper implements PanelAutomation, ActionListener
{
    JFrame frame;
    JPanel panel;
    JTextArea textArea;
    JScrollPane scroller;
    JRadioButton yesRadio;
    JRadioButton noRadio;
    JButton nextButton;
    String license;
    boolean nextButtonClicked = false;
    Object lock = new Object();
    
    /** The names of the packs covered by this license. */
    private List packsCovered;
    private List selectedPacksConcerned;
    
    private JLabel infoLabel;
    

    private static int instanceCount = 0;

    protected int instanceNumber = 0;

    

    
    public LicensePanelAutomationHelper() {

        instanceNumber = instanceCount++;

    }
    
    /**
     * Asks to make the XML panel data.
     * 
     * @param idata The installation data.
     * @param panelRoot The tree to put the data in.
     */
    public void makeXMLData(AutomatedInstallData idata, XMLElement panelRoot)
    {
    }

    /**
     * Asks to run in the automated mode.
     * 
     * @param idata The installation data.
     * @param panelRoot The XML tree to read the data from.
     * @return true if successful, false if failed
     */
    public boolean runAutomated(AutomatedInstallData idata, XMLElement panelRoot)
    {
        // We load the licence and the list of packs covered by this license
        loadLicense(idata);

        selectedPacksConcerned = new ArrayList();
        assert idata.selectedPacks != null;
        assert packsCovered != null;
        for (Iterator it = idata.selectedPacks.iterator(); it.hasNext(); ) {
            Pack pack = (Pack) it.next();
            if (packsCovered.contains(pack.name))
                selectedPacksConcerned.add(pack.name);
        }
        
        String infoText;
        if (selectedPacksConcerned.isEmpty()) {
            // If none of the selected packs is concerned by this license, skip this panel:
            return true;
        } else if (selectedPacksConcerned.size() == 1) {
            infoText = "The pack '"+ ((String)selectedPacksConcerned.get(0)) + "' is covered by the following license. Please read it carefully:";
        } else { // several
            StringBuffer buf = new StringBuffer("The packs '");
            Iterator it = selectedPacksConcerned.iterator();
            String name = (String) it.next();
            buf.append(name); buf.append("'");
            while (it.hasNext()) {
                buf.append(", '"); buf.append((String)it.next()); buf.append("'");
            }
            buf.append(" are covered by the following license. Please read it carefully:");
            infoText = buf.toString();
        }

        frame = new JFrame(System.getProperty("licensepanel.title", ""));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(1);}
        });
        panel = new JPanel();
        Dimension dim = new Dimension(640,480);
        frame.getRootPane().setMaximumSize(dim);
        frame.getRootPane().setMinimumSize(dim);
        frame.getRootPane().setPreferredSize(dim);
        frame.getContentPane().add(panel);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width-dim.width)/2,(screenSize.height-dim.height)/2);
        // We initialize our layout
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // We put our components

        infoLabel = LabelFactory.create(infoText,
                JLabel.TRAILING);
        panel.add(infoLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        textArea = new JTextArea(license);
        textArea.setMargin(new Insets(2, 2, 2, 2));
        textArea.setCaretPosition(0);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scroller = new JScrollPane(textArea);
        scroller.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.add(scroller);

        ButtonGroup group = new ButtonGroup();

        yesRadio = new JRadioButton(idata.langpack.getString("LicencePanel.agree"), false);
        group.add(yesRadio);
        panel.add(yesRadio);
        yesRadio.addActionListener(this);

        noRadio = new JRadioButton(idata.langpack.getString("LicencePanel.notagree"), true);
        group.add(noRadio);
        panel.add(noRadio);
        noRadio.addActionListener(this);

        ButtonFactory.useButtonIcons(false);
        ButtonFactory.useHighlightButtons(false);
        nextButton = ButtonFactory.createButton("Install",
                null, null);
        nextButton.setEnabled(false);
        panel.add(nextButton);
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nextButtonClicked = true;
                synchronized (lock) {
                    lock.notifyAll();
                }
                
            }
        });
        frame.pack();
        frame.setVisible(true);
        frame.toFront();

        synchronized (lock) {
            while(!nextButtonClicked) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {}
            }
        }
        frame.setVisible(false);
        return true;
    }

    /** Loads the license text. */
    private void loadLicense(AutomatedInstallData idata)
    {
        try
        {
            // We read it
            String resName = "LicensePanel.license."+instanceNumber;
            //license = ResourceManager.getInstance().getTextResource(resName);
            // Need to set UTF-8 encoding manually:
            InputStream in = ResourceManager.getInstance().getInputStream(resName);
            ByteArrayOutputStream infoData = new ByteArrayOutputStream();
            byte[] buffer = new byte[5120];
            int bytesInBuffer;
            while ((bytesInBuffer = in.read(buffer)) != -1)
                infoData.write(buffer, 0, bytesInBuffer);
            license = infoData.toString("UTF-8");
        }
        catch (Exception err)
        {
            license = "Error : could not load the license text !";
        }
        try
        {
            // We read it
            String variableName = "LicensePanel.packsCovered."+instanceNumber;
            packsCovered = Arrays.asList(idata.getVariable(variableName)
                .split("\\s+"));
        }
        catch (Exception err)
        {
            err.printStackTrace();
            packsCovered = new ArrayList();
        }
    }
    
    /**
     * Actions-handling method (here it allows the installation).
     * 
     * @param e The event.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (yesRadio.isSelected())
            unlockNextButton();
        else
            lockNextButton();
    }


    /** Locks the 'next' button. */
    public void lockNextButton() {
        nextButton.setEnabled(false);
    }


    /** Unlocks the 'next' button. */
    public void unlockNextButton() {
        nextButton.setEnabled(true);
        nextButton.requestFocus();
    }

}

