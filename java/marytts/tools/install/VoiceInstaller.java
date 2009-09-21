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
package marytts.tools.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class VoiceInstaller extends Thread
{
    private String filter;
    private boolean exitOnClose;

    public VoiceInstaller()
    {
        this(null, true);
    }
    
    /**
     * Create a voice installer that shows only voices
     * whose name (and optionally, version) matches the ones given in filter.
     * @param filter
     */
    public VoiceInstaller(String filter, boolean exitOnClose)
    {
        this.filter = filter;
        this.exitOnClose = exitOnClose;
    }
    
    public void run()
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
        File infoDir = new File(maryBase+"/installed-voices");
        if (!infoDir.exists()) infoDir.mkdir();
        
        SortedMap<String, InstallableVoice> voiceMap = new TreeMap<String, InstallableVoice>();
        
        // Already-installed voices:
        String[] infoFilenames = infoDir.list(new FilenameFilter() {
            public boolean accept(File dir, String filename) { return filename.endsWith(".voice"); }
        });
        for (String n : infoFilenames) {
            voiceMap.put(n.substring(0, n.lastIndexOf('.')), null);
        }

        // Downloaded voices:
        String[] archiveFilenames = archiveDir.list(new FilenameFilter() {
            public boolean accept(File dir, String filename) { return filename.endsWith(".zip"); }
        });
        for (String n : archiveFilenames) {
            voiceMap.put(n.substring(0, n.lastIndexOf('.')), null);
        }
        
        try {
            // Available voices:
            URL voicesBaseURL = new URL("http://mary.dfki.de/download/voices");
            URL voicesList = new URL(voicesBaseURL.toString()+"/voices-list.txt");
            System.out.println("Trying to connect to "+voicesList.toString());
            HttpURLConnection connection = (HttpURLConnection) voicesList.openConnection();
            connection.connect();
            // Make sure response code is in the 200 range.
            if (connection.getResponseCode() / 100 != 2) {
                throw new Exception("Cannot download voice list: "+connection.getResponseMessage());
            }
            BufferedReader fromVoicesList = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            while ((line = fromVoicesList.readLine()) != null) {
                System.out.println("Read from voices-list: '"+line+"'");
                // Skip empty lines and comments:
                if (line.trim().equals("") || line.trim().startsWith("#")) {
                    continue;
                }
                try {
                    StringTokenizer st = new StringTokenizer(line);
                    String name = st.nextToken();
                    String version = st.nextToken();
                    String urlString = st.nextToken();
                    URL url = new URL(urlString);
                    String sizeString = st.nextToken();
                    int size = Integer.parseInt(sizeString);
                    urlString = st.nextToken();
                    URL license = new URL(urlString);
                    String md5sum = st.nextToken();
                    String archiveFilename = archiveDir.getAbsolutePath()+"/"+name+"-"+version+".zip";
                    String infoFilename = infoDir.getAbsolutePath()+"/"+name+"-"+version+".voice";
                    voiceMap.put(name+"-"+version, new InstallableVoice(name, version, archiveFilename, infoFilename, url, size, license, md5sum));
                } catch (Exception e) {
                    System.err.println("Problem with voice description read from list -- ignoring: '"+line+"'");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        List<InstallableVoice> voices = new ArrayList<InstallableVoice>();
        for (String voice : voiceMap.keySet()) {
            if (voiceMap.get(voice) == null) {
                String name;
                String version;
                int iMinus = voice.lastIndexOf('-');
                int iLastDot = voice.lastIndexOf('.');
                if (iLastDot == -1) iLastDot = voice.length();
                if (iMinus != -1) {
                    name = voice.substring(0, iMinus);
                    version = voice.substring(iMinus+1, iLastDot);
                } else { // just a name, no version
                    name = voice.substring(0, iLastDot);
                    version = "";
                }
                String infoFilename = infoDir.getAbsolutePath()+"/"+voice+".voice";
                String archiveFilename = archiveDir.getAbsolutePath()+"/"+voice+".zip";

                URL url = null;
                int size = -1;
                if (new File(archiveFilename).exists()) {
                    size = (int) new File(archiveFilename).length();
                }
                voiceMap.put(voice, new InstallableVoice(name, version, archiveFilename, infoFilename, url, size, null, null));
            }
            if (filter == null || voice.contains(filter)) {
                System.out.println("added voice: "+voice);
                voices.add(voiceMap.get(voice));
            }
        }
        
        DownloadManager manager = new DownloadManager(voices, exitOnClose);
        manager.setVisible(true);
        
        try {
            while (manager.isVisible()) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
        }
    }
    
    // Run the Download Manager.
    public static void main(String[] args) throws Exception {
        new VoiceInstaller().start();
    }
}

