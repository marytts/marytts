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
package marytts.tools.redstart;

import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Redstart {

	/**
	 * @param args
	 *            args
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
		if (args.length > 0)
			candidates.add(args[0]);
		candidates.add("."); // current directory
		for (String dir : candidates) {
			if (dir != null && new File(dir).isDirectory() && new File(dir + "/text").isDirectory()) {
				voiceBuildingDir = dir;
				break;
			}
		}
		if (voiceBuildingDir == null) { // need to ask user
			JFrame window = new JFrame("This is the Frames's Title Bar!");
			JFileChooser fc = new JFileChooser(new File("."));
			fc.setDialogTitle("Choose Voice Building Directory");
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			System.out.println("Opening GUI....... ");
			// outDir.setText(file.getAbsolutePath());
			// System.exit(0);
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

		File textDir = new File(voiceBuildingDir + System.getProperty("file.separator") + "text");
		// System.out.println(System.getProperty("user.dir")+System.getProperty("file.separator")+"wav");
		if (!textDir.exists()) {
			JOptionPane
					.showOptionDialog(
							null,
							"Before beginning a new recording session, make sure that all text files (transcriptions) are available in 'text' directory of your specified location.",
							"Could not find transcriptions", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null,
							new String[] { "OK" }, null);
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
		if (splash != null)
			splash.setVisible(false);
		adminWindow.setVisible(true);
	}
}
