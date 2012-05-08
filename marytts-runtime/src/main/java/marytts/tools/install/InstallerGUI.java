/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.tools.install;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import marytts.Version;
import marytts.tools.install.ComponentDescription.Status;
import marytts.util.MaryUtils;

/**
 *
 * @author  marc
 */
public class InstallerGUI extends javax.swing.JFrame implements VoiceUpdateListener
{
    private Map<String, LanguageComponentDescription> languages;
    private Map<String, VoiceComponentDescription> voices;
    private LanguageComponentDescription currentLanguage = null;
    private String version = Version.specificationVersion();
    
    /** Creates new form InstallerGUI */
    public InstallerGUI() {
        this(null);
    }
    
    
    /**
     * Creates new installer gui and fills it with content from the given URL.
     * @param maryComponentURL
     */
    public InstallerGUI(String maryComponentURL)
    {
        this.languages = new TreeMap<String, LanguageComponentDescription>();
        this.voices = new TreeMap<String, VoiceComponentDescription>();
        initComponents();
        customInitComponents();
        if (maryComponentURL != null) {
            setAndUpdateFromMaryComponentURL(maryComponentURL);
        }
    }
    
    public void setAndUpdateFromMaryComponentURL(String maryComponentURL) {
        try {
            URL url = new URL(maryComponentURL);
            // if this doesn't fail then it's OK, we can set it
            tfComponentListURL.setText(maryComponentURL);
            updateFromMaryComponentURL();
        } catch (MalformedURLException e) {
            // ignore, treat as unset value
        }
    }
    
    public void addLanguagesAndVoices(InstallFileParser p)
    {
        for (LanguageComponentDescription desc : p.getLanguageDescriptions()) {
            if (languages.containsKey(desc.getName())) {
                LanguageComponentDescription existing = languages.get(desc.getName());
                // Check if one is an update of the other
                if (existing.getStatus() == Status.INSTALLED) {
                    if (desc.isUpdateOf(existing)) {
                        existing.setAvailableUpdate(desc);
                    }
                } else if (desc.getStatus() == Status.INSTALLED) {
                    languages.put(desc.getName(), desc);
                    if (existing.isUpdateOf(desc)) {
                        desc.setAvailableUpdate(existing);
                    }
                } else { // both not installed: show only higher version number
                    if (desc.getVersion().compareTo(existing.getVersion()) > 0) {
                        languages.put(desc.getName(), desc);
                    } // else leave existing as is
                }
            } else { // no such entry yet
                languages.put(desc.getName(), desc);
            }
        }
        for (VoiceComponentDescription desc : p.getVoiceDescriptions()) {
            if (voices.containsKey(desc.getName())) {
                VoiceComponentDescription existing = voices.get(desc.getName());
                // Check if one is an update of the other
                if (existing.getStatus() == Status.INSTALLED) {
                    if (desc.isUpdateOf(existing)) {
                        existing.setAvailableUpdate(desc);
                    }
                } else if (desc.getStatus() == Status.INSTALLED) {
                    voices.put(desc.getName(), desc);
                    if (existing.isUpdateOf(desc)) {
                        desc.setAvailableUpdate(existing);
                    }
                } else { // both not installed: show only higher version number
                    if (desc.getVersion().compareTo(existing.getVersion()) > 0) {
                        voices.put(desc.getName(), desc);
                    } // else leave existing as is
                }
            } else { // no such entry yet
                voices.put(desc.getName(), desc);
            }
        }
        updateLanguagesTable();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        pDownload = new javax.swing.JPanel();
        tfComponentListURL = new javax.swing.JTextField();
        bUpdate = new javax.swing.JButton();
        pInstallButtons = new javax.swing.JPanel();
        bInstall = new javax.swing.JButton();
        bUninstall = new javax.swing.JButton();
        bUninstall1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        spLanguages = new javax.swing.JScrollPane();
        pLanguages = new javax.swing.JPanel();
        spVoices = new javax.swing.JScrollPane();
        pVoices = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        menuBar1 = new javax.swing.JMenuBar();
        menuTools1 = new javax.swing.JMenu();
        miProxy1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("MARY TTS Installer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                InstallerGUI.this.windowClosing(evt);
            }
        });

        pDownload.setBorder(javax.swing.BorderFactory.createTitledBorder("Download languages and voices from:"));
        // hack so that SVN checkout from "trunk" will look for "latest" directory on server:
        if (version.equals("trunk")) {
            version = "latest";
        }
        tfComponentListURL.setText("https://raw.github.com/marytts/marytts/master/download/marytts-components.xml");
        tfComponentListURL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfComponentListURLActionPerformed(evt);
            }
        });

        bUpdate.setText("Update");
        bUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bUpdateActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pDownloadLayout = new org.jdesktop.layout.GroupLayout(pDownload);
        pDownload.setLayout(pDownloadLayout);
        pDownloadLayout.setHorizontalGroup(
            pDownloadLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pDownloadLayout.createSequentialGroup()
                .add(tfComponentListURL, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 540, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 73, Short.MAX_VALUE)
                .add(bUpdate)
                .addContainerGap())
        );
        pDownloadLayout.setVerticalGroup(
            pDownloadLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pDownloadLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(tfComponentListURL, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(bUpdate))
        );

        bInstall.setText("Install selected");
        bInstall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bInstallActionPerformed(evt);
            }
        });

        bUninstall.setText("Uninstall selected");
        bUninstall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bUninstallActionPerformed(evt);
            }
        });

        bUninstall1.setText("Quit");
        bUninstall1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pInstallButtonsLayout = new org.jdesktop.layout.GroupLayout(pInstallButtons);
        pInstallButtons.setLayout(pInstallButtonsLayout);
        pInstallButtonsLayout.setHorizontalGroup(
            pInstallButtonsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, pInstallButtonsLayout.createSequentialGroup()
                .add(146, 146, 146)
                .add(bInstall, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(14, 14, 14)
                .add(bUninstall)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bUninstall1)
                .add(194, 194, 194))
        );
        pInstallButtonsLayout.setVerticalGroup(
            pInstallButtonsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pInstallButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .add(pInstallButtonsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(bUninstall)
                    .add(bInstall)
                    .add(bUninstall1))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        spLanguages.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pLanguages.setLayout(new javax.swing.BoxLayout(pLanguages, javax.swing.BoxLayout.Y_AXIS));

        spLanguages.setViewportView(pLanguages);

        spVoices.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pVoices.setLayout(new javax.swing.BoxLayout(pVoices, javax.swing.BoxLayout.Y_AXIS));

        spVoices.setViewportView(pVoices);

        jLabel1.setText("Languages");

        jLabel2.setText("Voices");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spLanguages, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 340, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .add(21, 21, 21)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spVoices, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 369, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spVoices, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
                    .add(spLanguages, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)))
        );

        menuTools1.setText("Tools");
        miProxy1.setText("Proxy settings...");
        miProxy1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miProxy1ActionPerformed(evt);
            }
        });

        menuTools1.add(miProxy1);

        menuBar1.add(menuTools1);

        setJMenuBar(menuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, 0, 730, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, pInstallButtons, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, pDownload, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(pDownload, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pInstallButtons, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tfComponentListURLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfComponentListURLActionPerformed
        updateFromMaryComponentURL();
    }//GEN-LAST:event_tfComponentListURLActionPerformed

    private void miProxy1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miProxy1ActionPerformed
        ProxyPanel prp = new ProxyPanel(System.getProperty("http.proxyHost"), System.getProperty("http.proxyPort"));
        final JOptionPane optionPane = new JOptionPane(prp, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, new String[] {"OK", "Cancel"}, "OK");
        final JDialog dialog = new JDialog((Frame)null, "", true);
        dialog.setContentPane(optionPane);
        optionPane.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        String prop = e.getPropertyName();

                        if (dialog.isVisible() 
                         && (e.getSource() == optionPane)
                         && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            dialog.setVisible(false);
                        }
                    }
                });
        dialog.pack();
        dialog.setVisible(true);

        if ("OK".equals(optionPane.getValue())) {
            System.setProperty("http.proxyHost",prp.getProxyHost());
            System.setProperty("http.proxyPort",prp.getProxyPort());
        }

    }//GEN-LAST:event_miProxy1ActionPerformed

    private void bUninstallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bUninstallActionPerformed
        uninstallSelectedLanguagesAndVoices();
    }//GEN-LAST:event_bUninstallActionPerformed

    private void bInstallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bInstallActionPerformed
        installSelectedLanguagesAndVoices();
    }//GEN-LAST:event_bInstallActionPerformed

    private void bUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bUpdateActionPerformed
        updateFromMaryComponentURL();
    }//GEN-LAST:event_bUpdateActionPerformed

    private void updateFromMaryComponentURL() throws HeadlessException {
        String urlString = tfComponentListURL.getText().trim().replaceAll(" ", "%20");
        try {
            URL url = new URL(urlString);
            InstallFileParser p = new InstallFileParser(url);
            addLanguagesAndVoices(p);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            String message = sw.toString();
            JOptionPane.showMessageDialog(this, "Problem retrieving component list:\n"+message);
        }
    }

    private void windowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_windowClosing
        confirmExit();
    }//GEN-LAST:event_windowClosing

    private void quitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitActionPerformed
        confirmExit();
    }//GEN-LAST:event_quitActionPerformed
    
    
    private void customInitComponents()
    {
        bUpdate.requestFocusInWindow();
        
        // Set up the authentication dialog in case it will be used:
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                PasswordPanel passP = new PasswordPanel();
                final JOptionPane optionPane = new JOptionPane(passP, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new String[] {"OK", "Cancel"}, "OK");
                final JDialog dialog = new JDialog((Frame)null, "", true);
                dialog.setContentPane(optionPane);
                optionPane.addPropertyChangeListener(
                    new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            String prop = e.getPropertyName();

                            if (dialog.isVisible() 
                                && (e.getSource() == optionPane)
                                && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            dialog.setVisible(false);
                        }
                    }
                });
                dialog.pack();
                dialog.setVisible(true);
                if ("OK".equals(optionPane.getValue())) {
                    return new PasswordAuthentication(passP.getUser(), passP.getPassword());
                }
                return null;
            }
        });
    }

    private void updateLanguagesTable()
    {
        pLanguages.removeAll();
        for (String dName : languages.keySet()) {
            ComponentDescription desc = languages.get(dName);
            pLanguages.add(new ShortDescriptionPanel(desc, this));
        }
        pLanguages.add(Box.createVerticalGlue());
        if (languages.size() > 0) {
            pLanguages.getComponent(0).requestFocusInWindow();
            updateVoices(languages.get(languages.keySet().iterator().next()), true);
        }
    }
    
    public void updateVoices(LanguageComponentDescription newLanguage, boolean forceUpdate)
    {
        if (currentLanguage != null && currentLanguage.equals(newLanguage) && !forceUpdate) {
            return;
        }
        currentLanguage = newLanguage;
        List<VoiceComponentDescription> lVoices = getVoicesForLanguage(currentLanguage);
        pVoices.removeAll();
        for (ComponentDescription desc : lVoices) {
            pVoices.add(new ShortDescriptionPanel(desc, null));
        }
        pVoices.add(Box.createVerticalGlue());
        pVoices.repaint();
        this.pack();
        
    }
    
    private HashSet<ComponentDescription> getAllInstalledComponents() {
        HashSet<ComponentDescription> components = new HashSet<ComponentDescription>();
        for (ComponentDescription component : languages.values()) {
            if (component.getStatus().equals(Status.INSTALLED)) {
                components.add(component);
            }
        }
        for (ComponentDescription component : voices.values()) {
            if (component.getStatus().equals(Status.INSTALLED)) {
                components.add(component);
            }
        }
        return components;
    }
    
    private List<VoiceComponentDescription> getVoicesForLanguage(LanguageComponentDescription language)
    {
        List<VoiceComponentDescription> lVoices = new ArrayList<VoiceComponentDescription>();
        for (String vName : voices.keySet()) {
            VoiceComponentDescription v = voices.get(vName);
            if (v.getDependsLanguage().equals(language.getName())) {
                lVoices.add(v);
            }
        }
        return lVoices;
    }
    
    private void confirmExit()
    {
        if (getComponentsSelectedForInstallation().size()+getComponentsSelectedForUninstall().size() == 0) {
            // Exit without further ado
            this.setVisible(false);
            System.exit(0);
        }
        int choice = JOptionPane.showConfirmDialog(this, "Discard selection and exit?", "Exit program", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            this.setVisible(false);
            System.exit(0);
        }
    }
    
    private List<ComponentDescription> getComponentsSelectedForInstallation() {
        List<ComponentDescription> toInstall = new ArrayList<ComponentDescription>();
        for (String langName : languages.keySet()) {
            LanguageComponentDescription lang = languages.get(langName);
            if (lang.isSelected() && (lang.getStatus() != Status.INSTALLED || lang.isUpdateAvailable())) {
                toInstall.add(lang);
                System.out.println(lang.getName()+" selected for installation");
            }
            // Show voices with corresponding language:
            List<VoiceComponentDescription> lVoices = getVoicesForLanguage(lang);
            for (VoiceComponentDescription voice : lVoices) {
                if (voice.isSelected() && (voice.getStatus() != Status.INSTALLED || voice.isUpdateAvailable())) {
                    toInstall.add(voice);
                    System.out.println(voice.getName()+" selected for installation");
                }
            }
        }
        return toInstall;
    }
    
    public void installSelectedLanguagesAndVoices()
    {
        long downloadSize = 0;
        List<ComponentDescription> toInstall = getComponentsSelectedForInstallation();
        if (toInstall.size() == 0) {
            JOptionPane.showMessageDialog(this, "You have not selected any installable components");
            return;
        }
        // Verify if all dependencies are met
        // There are the following ways of meeting a dependency:
        // - the component with the right name and version number is already installed;
        // - the component with the right name and version number is selected for installation;
        // - an update of the component with the right version number is selected for installation.
        Map<String, String> unmetDependencies = new TreeMap<String, String>(); // map name to problem description
        for (ComponentDescription cd : toInstall) {
            if (cd instanceof VoiceComponentDescription) {
                // Currently have dependencies only for voice components
                VoiceComponentDescription vcd = (VoiceComponentDescription) cd;
                String depLang = vcd.getDependsLanguage();
                String depVersion = vcd.getDependsVersion();
                // Two options for fulfilling the dependency: either it is already installed, or it is in toInstall
                LanguageComponentDescription lcd = languages.get(depLang);
                if (lcd == null) {
                    unmetDependencies.put(depLang, "-- no such language component");
                } else if (lcd.getStatus() == Status.INSTALLED) {
                    if (lcd.getVersion().compareTo(depVersion) < 0) {
                        ComponentDescription update = lcd.getAvailableUpdate();
                        if (update == null) {
                            unmetDependencies.put(depLang, "version "+depVersion+" is required by "+vcd.getName()+",\nbut older version "+lcd.getVersion()+" is installed and no update is available");
                        } else if (update.getVersion().compareTo(depVersion) < 0) {
                            unmetDependencies.put(depLang, "version "+depVersion+" is required by "+vcd.getName()+",\nbut only version "+update.getVersion()+" is available as an update");
                        } else if (!toInstall.contains(lcd)) {
                            unmetDependencies.put(depLang, "version "+depVersion+" is required by "+vcd.getName()+",\nbut older version "+lcd.getVersion()+" is installed\nand update to version "+update.getVersion()+" is not selected for installation");
                        }
                    }
                } else if (!toInstall.contains(lcd)) {
                    if (lcd.getVersion().compareTo(depVersion) >= 0) {
                        unmetDependencies.put(depLang, "is required  by "+vcd.getName()+"\nbut is not selected for installation");
                    } else {
                        unmetDependencies.put(depLang, "version "+depVersion+" is required by "+vcd.getName()+",\nbut only older version "+lcd.getVersion()+" is available");
                    }
                }
            }
        }
        // Any unmet dependencies?
        if (unmetDependencies.size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (String compName : unmetDependencies.keySet()) {
                buf.append("Component ").append(compName).append(" ").append(unmetDependencies.get(compName)).append("\n");
            }
            JOptionPane.showMessageDialog(this, buf.toString(), "Dependency problem", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        for (ComponentDescription cd : toInstall) {
            if (cd.getStatus() == Status.AVAILABLE) {
                downloadSize += cd.getPackageSize();
            } else if (cd.getStatus() == Status.INSTALLED && cd.isUpdateAvailable()) {
                if (cd.getAvailableUpdate().getStatus() == Status.AVAILABLE) {
                    downloadSize += cd.getAvailableUpdate().getPackageSize();
                }
            }
        }
        int returnValue = JOptionPane.showConfirmDialog(this, "Install "+toInstall.size()+" components?\n("
                +MaryUtils.toHumanReadableSize(downloadSize)+" to download)", "Proceed with installation?", JOptionPane.YES_NO_OPTION);
        if (returnValue != JOptionPane.YES_OPTION) {
            System.err.println("Aborting installation.");
            return;
        }
        System.out.println("Check license(s)");
        boolean accepted = showLicenses(toInstall);
        if (accepted) {
            System.out.println("Starting installation");
            showProgressPanel(toInstall, true);
        }
    }
    
    
    /**
     * Show the licenses for the components in toInstall
     * @param toInstall the components to install
     * @return true if all licenses were accepted, false otherwise
     */
    private boolean showLicenses(List<ComponentDescription> toInstall) {
        Map<URL, SortedSet<ComponentDescription>> licenseGroups = new HashMap<URL, SortedSet<ComponentDescription>>();
        // Group components by their license:
        for (ComponentDescription cd : toInstall) {
            URL licenseURL = cd.getLicenseURL(); // may be null
            // null is an acceptable key for HashMaps, so it's OK.
            SortedSet<ComponentDescription> compsUnderLicense = licenseGroups.get(licenseURL);
            if (compsUnderLicense == null) {
                compsUnderLicense = new TreeSet<ComponentDescription>();
                licenseGroups.put(licenseURL, compsUnderLicense);
            }
            assert compsUnderLicense != null;
            compsUnderLicense.add(cd);
        }
        // Now show license for each group
        for (URL licenseURL : licenseGroups.keySet()) {
            if (licenseURL == null) {
                continue;
            }
            URL localURL = LicenseRegistry.getLicense(licenseURL);
            SortedSet<ComponentDescription> comps = licenseGroups.get(licenseURL);
            System.out.println("Showing license "+licenseURL+ " for "+comps.size()+" components");
            LicensePanel licensePanel = new LicensePanel(localURL, comps);
            final JOptionPane optionPane = new JOptionPane(licensePanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, new String[] {"Reject", "Accept"}, "Reject");
            optionPane.setPreferredSize(new Dimension(800,600));
            final JDialog dialog = new JDialog((Frame)null, "Do you accept the following license?", true);
            dialog.setContentPane(optionPane);
            optionPane.addPropertyChangeListener(
                    new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            String prop = e.getPropertyName();

                            if (dialog.isVisible() 
                             && (e.getSource() == optionPane)
                             && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                                dialog.setVisible(false);
                            }
                        }
                    });
            dialog.pack();
            dialog.setVisible(true);
            
            if (!"Accept".equals(optionPane.getValue())) {
                System.out.println("License not accepted. Installation of component cannot proceed.");
                return false;
            }
            System.out.println("License accepted.");
        }
        return true;
    }
    
    private List<ComponentDescription> getComponentsSelectedForUninstall() {
        List<ComponentDescription> toUninstall = new ArrayList<ComponentDescription>();
        for (String langName : languages.keySet()) {
            LanguageComponentDescription lang = languages.get(langName);
            if (lang.isSelected() && lang.getStatus() == Status.INSTALLED) {
                toUninstall.add(lang);
                System.out.println(lang.getName()+" selected for uninstall");
            }
            // Show voices with corresponding language:
            List<VoiceComponentDescription> lVoices = getVoicesForLanguage(lang);
            for (VoiceComponentDescription voice : lVoices) {
                if (voice.isSelected() && voice.getStatus() == Status.INSTALLED) {
                    toUninstall.add(voice);
                    System.out.println(voice.getName()+" selected for uninstall");
                }
            }
        }
        findAndStoreSharedFiles(toUninstall);
        return toUninstall;
    }
    
    /**
     * For all components to be uninstalled, find any shared files required by components that will <i>not</i> be uninstalled, and
     * store them in the component (using {@link ComponentDescription#setSharedFiles(List)}).
     * {@link ComponentDescription#uninstall()} can then check and refrain from removing those shared files.
     * 
     * @param uninstallComponents
     *            selected for uninstallation
     */
    private void findAndStoreSharedFiles(List<ComponentDescription> uninstallComponents) {
        // first, find out which components are *not* selected for removal:
        Set<ComponentDescription> retainComponents = getAllInstalledComponents();
        retainComponents.removeAll(uninstallComponents);

        // if all components are selected for removal, there is nothing to do here:
        if (retainComponents.isEmpty()) {
            return;
        }

        // otherwise, list all unique files required by retained components:
        Set<String> retainFiles = new TreeSet<String>();
        for (ComponentDescription retainComponent : retainComponents) {
            retainFiles.addAll(retainComponent.getInstalledFileNames());
        }

        // finally, store shared files in components to be removed (queried later):
        for (ComponentDescription uninstallComponent : uninstallComponents) {
            Set<String> sharedFiles = new HashSet<String>(uninstallComponent.getInstalledFileNames());
            sharedFiles.retainAll(retainFiles);
            if (!sharedFiles.isEmpty()) {
                uninstallComponent.setSharedFiles(sharedFiles);
            }
        }
    }
    
    public void uninstallSelectedLanguagesAndVoices()
    {
        List<ComponentDescription> toUninstall = getComponentsSelectedForUninstall();
        if (toUninstall.size() == 0) {
            JOptionPane.showMessageDialog(this, "You have not selected any uninstallable components");
            return;
        }
        int returnValue = JOptionPane.showConfirmDialog(this, "Uninstall "+toUninstall.size()+" components?\n", "Proceed with uninstall?", JOptionPane.YES_NO_OPTION);
        if (returnValue != JOptionPane.YES_OPTION) {
            System.err.println("Aborting uninstall.");
            return;
        }
        System.out.println("Starting uninstall");
        showProgressPanel(toUninstall, false);

    }
    
    
    private void showProgressPanel(List<ComponentDescription> comps, boolean install)
    {
        final ProgressPanel pp = new ProgressPanel(comps, install);
        final JOptionPane optionPane = new JOptionPane(pp, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new String[] {"Abort"}, "Abort");
        //optionPane.setPreferredSize(new Dimension(640,480));
        final JDialog dialog = new JDialog((Frame)null, "Progress", false);
        dialog.setContentPane(optionPane);
        optionPane.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        String prop = e.getPropertyName();

                        if (dialog.isVisible() 
                                && (e.getSource() == optionPane)
                                && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            pp.requestExit();
                            dialog.setVisible(false);
                        }
                    }
                });
        dialog.pack();
        dialog.setVisible(true);
        new Thread(pp).start();
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    throws Exception
    {
        String maryBase = System.getProperty("mary.base");
        if (maryBase == null || !new File(maryBase).isDirectory()) {
            JFrame window = new JFrame("This is the Frames's Title Bar!");
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Please indicate MARY TTS installation directory");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(window);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (file != null)
                    maryBase = file.getAbsolutePath(); 
            }
        }
        if (maryBase == null || !new File(maryBase).isDirectory()) {
            System.out.println("No MARY base directory -- exiting.");
            System.exit(0);
        }
        System.setProperty("mary.base", maryBase);
        
        File archiveDir = new File(maryBase+"/download");
        if (!archiveDir.exists()) archiveDir.mkdir();
        System.setProperty("mary.downloadDir", archiveDir.getPath());
        File infoDir = new File(maryBase+"/installed");
        if (!infoDir.exists()) infoDir.mkdir();
        System.setProperty("mary.installedDir", infoDir.getPath());

        InstallerGUI g = new InstallerGUI();

        File[] componentDescriptionFiles = infoDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });
        for (File cd : componentDescriptionFiles) {
            try {
                g.addLanguagesAndVoices(new InstallFileParser(cd.toURI().toURL()));
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        componentDescriptionFiles = archiveDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });
        for (File cd : componentDescriptionFiles) {
            try {
                g.addLanguagesAndVoices(new InstallFileParser(cd.toURI().toURL()));
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        
        if (args.length > 0) {
            g.setAndUpdateFromMaryComponentURL(args[0]);
        }
        
        g.setVisible(true);
    
    
    
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bInstall;
    private javax.swing.JButton bUninstall;
    private javax.swing.JButton bUninstall1;
    private javax.swing.JButton bUpdate;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JMenuBar menuBar1;
    private javax.swing.JMenu menuTools1;
    private javax.swing.JMenuItem miProxy1;
    private javax.swing.JPanel pDownload;
    private javax.swing.JPanel pInstallButtons;
    private javax.swing.JPanel pLanguages;
    private javax.swing.JPanel pVoices;
    private javax.swing.JScrollPane spLanguages;
    private javax.swing.JScrollPane spVoices;
    private javax.swing.JTextField tfComponentListURL;
    // End of variables declaration//GEN-END:variables
    

}
