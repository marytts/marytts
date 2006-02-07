/*
 * IzPack - Copyright 2001-2006 Julien Ponge, All Rights Reserved.
 * Portions Copyright (c) 2006 DFKI GmbH.
 * http://www.izforge.com/izpack/
 * http://developer.berlios.de/projects/izpack/
 * 
 * Copyright 2002 Jan Blok
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.izforge.izpack.Pack;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.installer.ResourceManager;

/**
 * The license panel.
 * 
 * @author Julien Ponge, Marc Schröder
 */
public class LicensePanel extends IzPanel implements ActionListener
{

    /**
     * 
     */
    private static final long serialVersionUID = 3691043187997552949L;

    /** The license text. */
    private String license;

    /** The names of the packs covered by this license. */
    private List packsCovered;
    private List selectedPacksConcerned;
    
    private JLabel infoLabel;
    
    /** The text area. */
    private JTextArea textArea;

    /** The radio buttons. */
    private JRadioButton yesRadio, noRadio;

    /** The scrolling container. */
    private JScrollPane scroller;

    private static int instanceCount = 0;

    protected int instanceNumber = 0;

    /**
     * The constructor.
     * 
     * @param parent The parent window.
     * @param idata The installation data.
     */
    public LicensePanel(InstallerFrame parent, InstallData idata)
    {
        super(parent, idata);

        instanceNumber = instanceCount++;

        // We initialize our layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // We load the licence
        loadLicense();

        // We put our components

        infoLabel = LabelFactory.create(parent.langpack.getString("LicencePanel.info"),
                parent.icons.getImageIcon("history"), JLabel.TRAILING);
        add(infoLabel);

        add(Box.createRigidArea(new Dimension(0, 3)));

        textArea = new JTextArea(license);
        textArea.setMargin(new Insets(2, 2, 2, 2));
        textArea.setCaretPosition(0);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scroller = new JScrollPane(textArea);
        scroller.setAlignmentX(LEFT_ALIGNMENT);
        add(scroller);

        ButtonGroup group = new ButtonGroup();

        yesRadio = new JRadioButton(parent.langpack.getString("LicencePanel.agree"), false);
        group.add(yesRadio);
        add(yesRadio);
        yesRadio.addActionListener(this);

        noRadio = new JRadioButton(parent.langpack.getString("LicencePanel.notagree"), true);
        group.add(noRadio);
        add(noRadio);
        noRadio.addActionListener(this);
    }

    /** Loads the licence text. */
    private void loadLicense()
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
            parent.unlockNextButton();
        else
            parent.lockNextButton();
    }

    /**
     * Indicates wether the panel has been validated or not.
     * 
     * @return true if the user has agreed.
     */
    public boolean isValidated()
    {
        if (noRadio.isSelected())
        {
            parent.exit();
            return false;
        }
        else
            return (yesRadio.isSelected());
    }

    /** Called when the panel becomes active. */
    public void panelActivate()
    {
        selectedPacksConcerned = new ArrayList();
        for (Iterator it = idata.selectedPacks.iterator(); it.hasNext(); ) {
            Pack pack = (Pack) it.next();
            if (packsCovered.contains(pack.name))
                selectedPacksConcerned.add(pack.name);
        }
        if (selectedPacksConcerned.isEmpty()) {
            // If none of the selected packs is concerned by this license, skip this panel:
            parent.skipPanel();
        } else if (selectedPacksConcerned.size() == 1) {
            String info = "The pack '"+ ((String)selectedPacksConcerned.get(0)) + "' is covered by the following license. Please read it carefully:";
            infoLabel.setText(info);
        } else { // several
            StringBuffer buf = new StringBuffer("The packs '");
            Iterator it = selectedPacksConcerned.iterator();
            String name = (String) it.next();
            buf.append(name); buf.append("'");
            while (it.hasNext()) {
                buf.append(", '"); buf.append((String)it.next()); buf.append("'");
            }
            buf.append(" are covered by the following license. Please read it carefully:");
            infoLabel.setText(buf.toString());
        }

        if (!yesRadio.isSelected()) parent.lockNextButton();
    }
}
