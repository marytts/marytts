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
package de.dfki.lt.mary.recsessionmgr;

import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.dfki.lt.mary.recsessionmgr.debug.Test;
import de.dfki.lt.mary.recsessionmgr.gui.AdminWindow;
import de.dfki.lt.mary.recsessionmgr.gui.Splash;

public class Redstart {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // Determine the voice building directory in the following order:
        // 1. System property "user.dir"
        // 2. First command line argument
        // 3. current directory
        // 4. Prompt user via gui.
        // Do a sanity check -- do they exist, do they have a wav/ subdirectory?
        
        String voiceBuildingDir = null;
        Vector<String> candidates = new Vector<String>();
        candidates.add(System.getProperty("user.dir"));
        if (args.length > 0) candidates.add(args[0]);
        candidates.add("."); // current directory
        for (String dir: candidates) {
            if (dir != null 
                    && new File(dir).isDirectory()
                    && new File(dir+"/text").isDirectory()) {
                voiceBuildingDir = dir;
                break;
            }
        }
        if (voiceBuildingDir == null) { // need to ask user
            JFrame window = new JFrame("This is the Frames's Title Bar!");
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Choose Voice Building Directory");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            System.out.println("Opening GUI....... ");
            //outDir.setText(file.getAbsolutePath());
            //System.exit(0);
            int returnVal = fc.showOpenDialog(window);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (file != null)
                    voiceBuildingDir = file.getAbsolutePath(); 
            }
        }
        if (voiceBuildingDir == null) {
            System.err.println("Could not get a voice building directory -- exiting.");
            System.exit(0);
        }
        
        File textDir =  new File(System.getProperty("user.dir")+System.getProperty("file.separator")+"text");
        //System.out.println(System.getProperty("user.dir")+System.getProperty("file.separator")+"wav");
        if(!textDir.exists()){
            int choose = JOptionPane.showOptionDialog(null,
                    "Before beginning a new recording session, make sure that all text files (transcriptions) are available in 'text' directory of your specified location.",
                    "Could not find transcriptions",
                    JOptionPane.OK_OPTION, 
                    JOptionPane.ERROR_MESSAGE, 
                    null,
                    new String[] {"OK"},
                    null);
            System.err.println("Could not find 'text' directory in user specified location -- exiting.");
            System.exit(0);
        }
        
        // Display splash screen

        Splash splash = null;
        try {
            splash = new Splash();
            splash.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Test.output("OS Name: " + System.getProperty("os.name"));
        Test.output("OS Architecture: " + System.getProperty("os.arch"));
        Test.output("OS Version: " + System.getProperty("os.version"));

        System.out.println("Welcome to Redstart, your recording session manager.");
        
        // TESTCODE
        Test.output("|Redstart.main| voiceFolderPath = " + voiceBuildingDir);
                        
        AdminWindow adminWindow = new AdminWindow(voiceBuildingDir);
        if (splash != null) splash.setVisible(false);
        adminWindow.setVisible(true);
    }

}
