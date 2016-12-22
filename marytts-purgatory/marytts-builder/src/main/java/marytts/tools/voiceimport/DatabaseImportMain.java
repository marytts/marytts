/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package marytts.tools.voiceimport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.apache.commons.io.FileUtils;

import marytts.util.io.BasenameList;

/**
 * The single purpose of the DatabaseImportMain class is to provide a main which executes the sequence of database import and
 * conversion operations.
 * 
 * @author sacha, anna
 * 
 */
public class DatabaseImportMain extends JFrame {
	protected VoiceImportComponent[] components;
	protected String[][] groups2Comps;
	protected JCheckBox[] checkboxes;
	protected JButton runButton;
	protected DatabaseLayout db = null;
	protected BasenameList bnl = null;
	protected String currentComponent;

	public DatabaseImportMain(String title, VoiceImportComponent[] components, DatabaseLayout db, String[][] groups2Comps) {
		super(title);
		this.components = components;
		this.checkboxes = new JCheckBox[components.length];
		this.db = db;
		this.bnl = db.getBasenames();
		this.groups2Comps = groups2Comps;
		currentComponent = "global properties";
		setupGUI();
	}

	protected void setupGUI() {
		// A scroll pane containing one labelled checkbox per component,
		// and a "run selected components" button below.
		GridBagLayout gridBagLayout = new GridBagLayout();
		GridBagConstraints gridC = new GridBagConstraints();
		getContentPane().setLayout(gridBagLayout);

		JPanel checkboxPane = new JPanel();
		checkboxPane.setLayout(new BoxLayout(checkboxPane, BoxLayout.Y_AXIS));
		// checkboxPane.setPreferredSize(new Dimension(300, 300));
		int compIndex = 0;
		for (int j = 0; j < groups2Comps.length; j++) {
			String[] nextGroup = groups2Comps[j];
			JPanel groupPane = new JPanel();
			groupPane.setLayout(new BoxLayout(groupPane, BoxLayout.Y_AXIS));
			groupPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(nextGroup[0]),
					BorderFactory.createEmptyBorder(1, 1, 1, 1)));
			for (int i = 1; i < nextGroup.length; i++) {
				JButton configButton = new JButton();
				Icon configIcon = new ImageIcon(DatabaseImportMain.class.getResource("configure.png"), "Configure");
				configButton.setIcon(configIcon);
				configButton.setPreferredSize(new Dimension(configIcon.getIconWidth(), configIcon.getIconHeight()));
				configButton.addActionListener(new ConfigButtonActionListener(nextGroup[i]));
				configButton.setBorderPainted(false);
				// System.out.println("Adding checkbox for "+components[i].getClass().getName());
				checkboxes[compIndex] = new JCheckBox(nextGroup[i]);
				checkboxes[compIndex].setFocusable(true);
				// checkboxes[i].setPreferredSize(new Dimension(200, 30));
				JPanel line = new JPanel();
				line.setLayout(new BorderLayout(5, 0));
				line.add(configButton, BorderLayout.WEST);
				line.add(checkboxes[compIndex], BorderLayout.CENTER);
				groupPane.add(line);
				compIndex++;
			}
			checkboxPane.add(groupPane);
		}
		gridC.gridx = 0;
		gridC.gridy = 0;
		gridC.fill = GridBagConstraints.BOTH;
		JScrollPane scrollPane = new JScrollPane(checkboxPane);
		scrollPane.setPreferredSize(new Dimension(450, 300));
		gridBagLayout.setConstraints(scrollPane, gridC);
		getContentPane().add(scrollPane);

		JButton helpButton = new JButton("Help");
		helpButton.setMnemonic(KeyEvent.VK_H);
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				displayHelpGUI();
			}
		});
		JButton settingsButton = new JButton("Settings");
		settingsButton.setMnemonic(KeyEvent.VK_S);
		settingsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				currentComponent = "Global properties";
				displaySettingsGUI();
			}
		});
		runButton = new JButton("Run");
		runButton.setMnemonic(KeyEvent.VK_R);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				runSelectedComponents();
			}
		});

		JButton quitAndSaveButton = new JButton("Quit");
		quitAndSaveButton.setMnemonic(KeyEvent.VK_Q);
		quitAndSaveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					askIfSave();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				System.exit(0);
			}
		});

		gridC.gridy = 1;
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		// buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
		// runButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
		buttonPanel.add(runButton);
		// helpButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
		buttonPanel.add(helpButton);
		// settingsButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
		buttonPanel.add(settingsButton);
		// buttonPanel.add(Box.createHorizontalGlue());
		// quitAndSaveButton.setAlignmentX(JButton.RIGHT_ALIGNMENT);
		buttonPanel.add(quitAndSaveButton);
		gridBagLayout.setConstraints(buttonPanel, gridC);
		getContentPane().add(buttonPanel);

		// getContentPane().setPreferredSize(new Dimension(300, 300));
		// End program when closing window:
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				try {
					askIfSave();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				System.exit(0);
			}
		});
	}

	protected void displayHelpGUI() {
		Thread helpGUIThread = new Thread("DisplayHelpGUIThread") {
			public void run() {
				boolean ok = new HelpGUI(DatabaseImportMain.class.getResourceAsStream("help_import_main.html")).display();
				if (!ok) {
					System.out.println("Error displaying helpfile " + "help_import_main.html");
				}
			}
		};
		helpGUIThread.start();
	}

	protected void displaySettingsGUI() {
		new Thread("DisplaySettingsGUIThread") {
			public void run() {
				Map<String, String> comps2HelpText = db.getComps2HelpText();
				new SettingsGUI(db, db.getAllPropsForDisplay(), currentComponent, comps2HelpText);
			}
		}.start();
	}

	/**
	 * Run the selected components in a different thread.
	 * 
	 */
	protected void runSelectedComponents() {
		new Thread("RunSelectedComponentsThread") {
			public void run() {
				try {
					runButton.setEnabled(false);
					for (int i = 0; i < components.length; i++) {
						if (checkboxes[i].isSelected()) {
							boolean success = false;
							Container parent = checkboxes[i].getParent();
							final JProgressBar progress = new JProgressBar();
							final VoiceImportComponent oneComponent = components[i];
							if (oneComponent.getProgress() != -1) {
								progress.setStringPainted(true);
								new Thread("ProgressThread") {
									public void run() {
										int percent = 0;
										while (progress.isVisible()) {
											progress.setValue(percent);
											try {
												Thread.sleep(500);
											} catch (InterruptedException ie) {
											}
											percent = oneComponent.getProgress();
										}
									}
								}.start();
							} else {
								progress.setIndeterminate(true);
							}
							parent.add(progress, BorderLayout.EAST);
							progress.setVisible(true);
							parent.validate();
							try {
								success = oneComponent.compute();
							} catch (Exception exc) {
								checkboxes[i].setBackground(Color.RED);
								throw new Exception("The component " + checkboxes[i].getText()
										+ " produced the following exception: ", exc);
							} finally {
								checkboxes[i].setSelected(false);
								progress.setVisible(false);
							}
							if (success) {
								checkboxes[i].setBackground(Color.GREEN);
							} else {
								checkboxes[i].setBackground(Color.RED);
							}
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					runButton.setEnabled(true);
				}

			}
		}.start();
	}

	protected void askIfSave() throws IOException {
		if (bnl.hasChanged()) {
			int answer = JOptionPane.showOptionDialog(this, "Do you want to save the list of basenames?", "Save?",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (answer == JOptionPane.YES_OPTION) {
				JFileChooser fc = new JFileChooser();
				fc.setSelectedFile(new File(db.getProp(db.BASENAMEFILE)));
				int returnVal = fc.showSaveDialog(this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					bnl.write(fc.getSelectedFile());
				}
			}
		} else {
			System.exit(0);
		}
	}

	protected void closeGUI() {
		// This doesn't work because Java cannot close the X11 connection, see
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4420885
		int answer = JOptionPane.showOptionDialog(this, "Do you want to close this GUI?\n\n"
				+ "The components that are currently running will continue to run,\n"
				+ "but you will not be able to save settings or select/deselect\n" + "components to run.\n"
				+ "This may be useful when running this tool using 'nohup' on a server\n"
				+ "because it allows you to log off without stopping the process.", "Really close GUI?",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (answer == JOptionPane.YES_OPTION) {
			this.setVisible(false);
			this.dispose();
		}
	}

	public static String[][] readComponentList(InputStream fileIn) throws IOException {
		List<String> groups = new ArrayList<String>();
		Map<String, String> groups2Names = new HashMap<String, String>();
		Map<String, List<String>> groups2Components = new HashMap<String, List<String>>();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(fileIn, "UTF-8"));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#") || line.equals(""))
					continue;
				// System.out.println(line);
				String[] lineSplit = line.split(" ");
				if (lineSplit[0].equals("group")) {
					// we have a group
					// line looks like "group basic_data basic data files"
					groups.add(lineSplit[1]);
					StringBuilder nameBuf = new StringBuilder();
					for (int i = 2; i < lineSplit.length; i++) {
						nameBuf.append(lineSplit[i] + " ");
					}
					groups2Names.put(lineSplit[1], nameBuf.toString().trim());
				} else {
					// we have a component
					// line looks like
					// "marytts.tools.voiceimport.WaveformTimelineMaker basic_data"
					if (groups2Components.containsKey(lineSplit[1])) {
						List<String> comps = groups2Components.get(lineSplit[1]);
						comps.add(lineSplit[0]);
					} else {
						List<String> comps = new ArrayList<String>();
						comps.add(lineSplit[0]);
						groups2Components.put(lineSplit[1], comps);
					}
				}
			}
			in.close();
		} catch (IOException e) {
			throw new IOException("Problem reading list of voice import components -- importMain.config seems broken", e);
		}
		String[][] result = new String[groups.size()][];
		for (int i = 0; i < groups.size(); i++) {
			String groupKey = groups.get(i);
			String groupName = groups2Names.get(groupKey);
			List<String> components = groups2Components.get(groupKey);
			if (components == null) // group is empty
				continue;
			String[] group = new String[components.size() + 1];
			group[0] = groupName;
			for (int j = 0; j < components.size(); j++) {
				group[j + 1] = components.get(j);
			}
			result[i] = group;
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		File voiceDir = determineVoiceBuildingDir(args);
		if (voiceDir == null) {
			throw new IllegalArgumentException("Cannot determine voice building directory.");
		}
		File wavDir = new File(voiceDir, "wav");
		// System.out.println(System.getProperty("user.dir")+System.getProperty("file.separator")+"wav");
		assert wavDir.exists() : "no wav dir at " + wavDir.getAbsolutePath();

		/* Read the list of components */
		File importMainConfigFile = new File(voiceDir, "importMain.config");
		if (!importMainConfigFile.exists()) {
			FileUtils.copyInputStreamToFile(DatabaseImportMain.class.getResourceAsStream("importMain.config"),
					importMainConfigFile);
		}
		assert importMainConfigFile.exists();

		String[][] groups2comps = readComponentList(new FileInputStream(importMainConfigFile));

		VoiceImportComponent[] components = createComponents(groups2comps);

		/* Load DatabaseLayout */
		File configFile = new File(voiceDir, "database.config");

		DatabaseLayout db = new DatabaseLayout(configFile, components);
		if (!db.isInitialized())
			return;

		if (args.length > 0) { // non-gui mode: arguments are expected to be component names, in order or application
			for (String compName : args) {
				VoiceImportComponent component = null;
				for (VoiceImportComponent comp : components) {
					if (comp.getName().equals(compName)) {
						component = comp;
						break;
					}
				}
				if (component != null) {
					System.out.println("Running " + compName);
					component.compute();
				} else {
					throw new IllegalArgumentException("No such voice import component: " + compName);
				}

			}
		} else {
			/* Display GUI */
			String voicename = db.getProp(db.VOICENAME);
			DatabaseImportMain importer = new DatabaseImportMain("Database import: " + voicename, components, db, groups2comps);
			importer.pack();
			// Center window on screen:
			importer.setLocationRelativeTo(null);
			importer.setVisible(true);
		}

	}

	/**
	 * @param groups2comps
	 *            groups2comps
	 * @return compsList.toArray(new VoiceImportComponent[compsList.size()])
	 * @throws InstantiationException
	 *             InstantiationException
	 * @throws IllegalAccessException
	 *             IllegalAccessException
	 * @throws ClassNotFoundException
	 *             ClassNotFoundException
	 */
	private static VoiceImportComponent[] createComponents(String[][] groups2comps) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		/* Create component classes */
		List<VoiceImportComponent> compsList = new ArrayList<VoiceImportComponent>();
		// loop over the groups
		for (int i = 0; i < groups2comps.length; i++) {
			// get the components for this group
			String[] nextComps = groups2comps[i];
			// loop over the components (first element is group name; ignore)
			for (int j = 1; j < nextComps.length; j++) {
				// get the class name of this component
				String className = nextComps[j];
				// System.out.println(className);
				// create a new instance of this class and store in compsList
				compsList.add((VoiceImportComponent) Class.forName(className).newInstance());
				// remove "de.dfki...." from class name and store in groups2comps
				nextComps[j] = className.substring(className.lastIndexOf('.') + 1);
			}
		}
		return compsList.toArray(new VoiceImportComponent[compsList.size()]);
	}

	private static File determineVoiceBuildingDir(String[] args) {
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
			if (dir != null && new File(dir).isDirectory() && new File(dir + "/wav").isDirectory()) {
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
			// outDir.setText(file.getAbsolutePath());
			// System.exit(0);
			int returnVal = fc.showOpenDialog(window);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				if (file != null)
					voiceBuildingDir = file.getAbsolutePath();
			}
		}
		System.setProperty("user.dir", voiceBuildingDir);
		if (voiceBuildingDir != null) {
			return new File(voiceBuildingDir);
		} else {
			return null;
		}
	}

	class ConfigButtonActionListener implements ActionListener {
		private String comp;

		public ConfigButtonActionListener(String comp) {
			this.comp = comp;
		}

		public void actionPerformed(ActionEvent ae) {
			currentComponent = comp;
			displaySettingsGUI();
		}
	}

}
