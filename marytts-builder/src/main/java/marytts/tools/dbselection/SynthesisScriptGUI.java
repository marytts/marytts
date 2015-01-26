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
package marytts.tools.dbselection;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 * Simple Synthesis script GUI.
 * 
 * @author Marcela Charfuelan, Holmer Hemsen.
 */
public class SynthesisScriptGUI extends JPanel implements TableModelListener {

	String colNames[] = { "Unwanted", "No.", "Selected Sentences" };
	Object[][] data = null;
	DefaultTableModel dtm;
	private static JTextArea output;
	private static JFrame frame;
	private static JTextField[] fields = new JTextField[16];
	private static String argsSelector[] = new String[29];

	// Initialisation with default values
	private static String locale = "en_US";
	// mySql database args
	private static String mysqlHost = "localhost";
	private static String mysqlUser = "marcela";
	private static String mysqlPasswd = "wiki123";
	private static String mysqlDB = "wiki";
	private static String tableName = "test";
	private static String actualTableName = locale + "_test_selectedSentences";
	// Additonal for databaseSelector
	private static String currentDir = System.getProperty("user.dir");
	private static String tableDesc = "add here a table description";
	private static String feaDefFile = currentDir + "/" + locale + "_featureDefinition.txt";
	private static String stop = "numSentences 30 simpleDiphones simpleProsody";
	private static String covConfFile = currentDir + "/covDef.config";
	private static String initFile = currentDir + "/init.bin";
	private static String overallLogFile = currentDir + "/overallLog.txt";
	private static String selecDir = currentDir + "/selection";
	private static String vectorsOnDisk = "false";
	private static String logCovDevelopment = "false";
	private static String verbose = "false";

	private static DBHandler wikiToDB = null;
	private static int selIds[] = null;
	private static int numWanted = 0;
	private static int numUnwanted = 0;
	private static String saveFile;
	private static boolean mysqlInfo = false;
	private static boolean connectionProblems = false;

	SynthesisScriptGUI() {
		super();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		output = new JTextArea(5, 40);
		output.setEditable(false);
		dtm = new DefaultTableModel(data, colNames);

		// Get the sentences from DB
		selIds = null;
		if (!connectionProblems && mysqlInfo) {
			// check if this table name exist in the DB
			if (wikiToDB.tableExist(actualTableName)) {
				selIds = wikiToDB.getIdListOfSelectedSentences(actualTableName, "unwanted=false");
				if (selIds != null) {
					String str;
					numWanted = selIds.length;
					numUnwanted = 0;
					for (int i = 0; i < selIds.length; i++) {
						str = wikiToDB.getSelectedSentence(actualTableName, selIds[i]);
						dtm.addRow(new Object[] { Boolean.FALSE, (i + 1), str });
					}
				} else
					output.append("There are not selected sentences in TABLE = " + actualTableName);
			} else
				output.append("\nERROR TABLE = " + actualTableName + " does not exist\n");
		}

		if (selIds == null || !mysqlInfo || connectionProblems) {
			for (int i = 0; i < 30; i++) {
				dtm.addRow(new Object[] { Boolean.FALSE, "", "" });
			}
		}
		if (!mysqlInfo)
			output.append("Please use the options menu to Enter/correct the mysql DB parameters.\n");
		if (connectionProblems)
			output.append("Problems trying to connect to the DB, please revise your parameters.\n");

		dtm.addTableModelListener(this);

		JTable table = new JTable(dtm);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		JScrollPane sp = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		TableColumn tc = table.getColumnModel().getColumn(0);
		tc.setCellEditor(table.getDefaultEditor(Boolean.class));
		tc.setCellRenderer(table.getDefaultRenderer(Boolean.class));

		tc.setResizable(true);
		tc.setPreferredWidth(70);
		table.getColumnModel().getColumn(1).setPreferredWidth(50);
		table.getColumnModel().getColumn(2).setPreferredWidth(1000);

		add(sp);
		add(new JScrollPane(output));

		setPreferredSize(new Dimension(1000, 700));

	}

	public void tableChanged(TableModelEvent event) {
		int command = event.getType();
		int row = event.getLastRow();

		// output.append("event=" + command + " row=" + event.getLastRow() );
		if (selIds != null) {
			boolean res = (Boolean) dtm.getValueAt(row, 0);
			// Marc the sentence as unwanted in both the dbselection table and selectedSentences table
			if (res) {
				numUnwanted++;
				numWanted--;
				output.append("id=" + selIds[row] + " set as unwanted (No. wanted=" + numWanted + " No. unwanted=" + numUnwanted
						+ ")\n");
				wikiToDB.setUnwantedSentenceRecord(actualTableName, selIds[row], res);
			} else {
				numWanted++;
				numUnwanted--;
				output.append("id=" + selIds[row] + " set as wanted (No. wanted=" + numWanted + " No. unwanted=" + numUnwanted
						+ ")\n");
				wikiToDB.setUnwantedSentenceRecord(actualTableName, selIds[row], res);
			}
		}
	}

	public static void main(String[] args) {

		// Create and set up the window.
		frame = new JFrame("Synthesis script GUI");

		SynthesisScriptGUI select = new SynthesisScriptGUI();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		select.setOpaque(true); // content panes must be opaque
		frame.setLocation(3, 3);
		frame.setJMenuBar(select.createMenuBar());
		frame.setContentPane(select);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
		select.printHelp();

	}

	public void loadTable() {

		if (wikiToDB != null)
			wikiToDB.closeDBConnection();
		wikiToDB = null;
		mysqlInfo = false;
		connectionProblems = false;
		wikiToDB = new DBHandler(locale);
		if (wikiToDB.createDBConnection(mysqlHost, mysqlDB, mysqlUser, mysqlPasswd))
			mysqlInfo = true;
		else {
			connectionProblems = true;
			wikiToDB = null;
		}
		SynthesisScriptGUI select = new SynthesisScriptGUI();
		frame.setContentPane(select);
		// Display the window.
		frame.pack();
		frame.setVisible(true);
		printSettings();
	}

	public void printSettings() {
		output.append("\nCURRENT SETTINGS:\n");
		output.append("Locale:      " + locale + "\n");
		output.append("Mysql Host:  " + mysqlHost + "\n");
		output.append("Mysql user:  " + mysqlUser + "\n");
		output.append("Mysql paswd: " + mysqlPasswd + "\n");
		output.append("Data base:   " + mysqlDB + "\n");
		output.append("Table name:  " + actualTableName + "\n");
	}

	public void printTableProperties() {
		if (wikiToDB != null) {
			String table[] = wikiToDB.getTableDescription(actualTableName);
			output.append("\nTABLE PROPERTIES: \nPROPERTY=tableName: " + table[0] + "\n");
			output.append("PROPERTY=description: " + table[1] + "\n");
			output.append("PROPERTY=stopCriterion: " + table[2] + "\n");
			output.append("PROPERTY=featuresDefinitionFileName: " + table[3] + "\n");
			output.append("PROPERTY=featuresDefinitionFile:\n" + table[4] + "\n");
			output.append("PROPERTY=covDefConfigFileName: " + table[5] + "\n");
			output.append("PROPERTY=covDefConfigFile:\n" + table[6] + "\n");
			int propLength = 0;
			for (int i = 0; i < 7; i++)
				propLength += table[i].length();
			// to point to the beginning of the properties description
			output.setCaretPosition((output.getDocument().getLength() - propLength));
		} else {
			output.append("\nERROR NO Mysql PARAMETERS: please select a table first.\n");
		}
	}

	public void printHelp() {
		output.append("\n SYNTHESIS SCRIPT OPTIONS:\n");
		output.append("1. Run DatabaseSelector: Creates a new selection table or adds sentences to an already existing one.\n"
				+ "   - After running the DatabaseSelector the selected sentences are loaded.\n");
		output.append("2. Load selected sentences table: reads mysql parameters and load a selected sentences table.\n");
		output.append("   - Once the sentences are loaded, use the checkboxes to mark sentences as unwanted/wanted.\n"
				+ "   - Sentences marked as unwanted can be unselected and set as wanted again. \n"
				+ "   - The DB is updated every time a checkbox is selected. \n"
				+ "   - There is no need to save changes. Changes can be made before the window is updated or the program exits.\n");
		output.append("3. Save synthesis script as: saves the selected sentences, without unwanted, in a file.\n");
		output.append("4. Print table properties: prints the properties used to generate the list of sentences.\n");
		output.append("5. Update window: presents the table without the sentences marked as unwanted.\n");
		output.append("6. Help: presents this description.\n");
		output.append("7. Exit: terminates the program.\n");
		// to point to the end of the JTextArea
		// output.setCaretPosition(output.getDocument().getLength());
	}

	public JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("Options");
		menuBar.add(fileMenu);

		JMenuItem runDatabaseSelectorAction = new JMenuItem("Run DatabaseSelector");
		JMenuItem mysqlInfoAction = new JMenuItem("Load selected sentences table");
		JMenuItem saveScriptAction = new JMenuItem("Save synthesis script as");
		JMenuItem printTablePropertiesAction = new JMenuItem("Print table properties");
		JMenuItem updateListAction = new JMenuItem("Update window");
		JMenuItem showListOfTablesAction = new JMenuItem("Show list of tables for this local");
		JMenuItem helpAction = new JMenuItem("Help");
		JMenuItem exitAction = new JMenuItem("Exit");

		fileMenu.add(runDatabaseSelectorAction);
		fileMenu.add(mysqlInfoAction);
		// fileMenu.add(loadTableAction);
		fileMenu.add(saveScriptAction);
		fileMenu.add(printTablePropertiesAction);
		fileMenu.add(updateListAction);
		fileMenu.add(showListOfTablesAction);
		fileMenu.addSeparator();
		fileMenu.add(helpAction);
		fileMenu.add(exitAction);

		runDatabaseSelectorAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runDatabaseSelector();
			}
		});
		mysqlInfoAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				readMysqlParameters();
			}
		});
		showListOfTablesAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				showListOfTables();
			}
		});
		saveScriptAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveSynthesisScriptAs();
			}
		});
		printTablePropertiesAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				printTableProperties();
			}
		});
		updateListAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				loadTable();
			}
		});
		helpAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				printHelp();
			}
		});
		exitAction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				wikiToDB.closeDBConnection();
				System.exit(1);
			}
		});
		return menuBar;
	}

	private void saveSynthesisScriptAs() {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Save as");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int returnVal = fc.showSaveDialog(SynthesisScriptGUI.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			saveFile = file.getAbsolutePath();
			output.append("Saving synthesis script to file:" + saveFile + "\n");

			int sel[] = wikiToDB.getIdListOfSelectedSentences(actualTableName, "unwanted=false");

			if (sel != null) {
				// saving sentences in a file
				try {
					PrintWriter selectedLog = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(saveFile)),
							"UTF-8"));
					String str;
					for (int i = 0; i < sel.length; i++) {
						str = wikiToDB.getSelectedSentence(actualTableName, sel[i]);
						selectedLog.println(sel[i] + " " + str);
					}
					selectedLog.close();
				} catch (Exception e) {
					System.out.println(e);
				}
			} else
				System.out.println("No selected sentences to save.");
		}
	}

	public void readMysqlParameters() {

		final JFrame f = new JFrame("Read Mysql parameters");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(createMysqlParamTextForm(false), BorderLayout.NORTH);
		JPanel p = new JPanel();

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				locale = fields[0].getText();
				mysqlHost = fields[1].getText();
				mysqlUser = fields[2].getText();
				mysqlPasswd = fields[3].getText();
				mysqlDB = fields[4].getText();
				tableName = fields[5].getText();
				actualTableName = locale + "_" + tableName + "_selectedSentences";

				if (locale.length() > 0 && mysqlHost.length() > 0 && mysqlDB.length() > 0 && mysqlUser.length() > 0
						&& mysqlPasswd.length() > 0)
					loadTable();
				else {
					if (locale.length() == 0)
						output.append("\nERROR PARAMETER: locale is empty\n");
					if (mysqlHost.length() == 0)
						output.append("\nERROR PARAMETER: Mysql host name is empty\n");
					if (mysqlDB.length() == 0)
						output.append("\nERROR PARAMETER: Mysql Data base name is empty\n");
					if (mysqlUser.length() == 0)
						output.append("\nERROR PARAMETER: Mysql user name is empty\n");
					if (mysqlPasswd.length() == 0)
						output.append("\nERROR PARAMETER: Mysql password is empty\n");
				}

				f.dispose();
			}
		});
		JButton cancelJButton = new JButton("Cancel");
		cancelJButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				f.dispose();
			}
		});
		p.add(okButton);
		p.add(cancelJButton);
		f.getContentPane().add(p, BorderLayout.SOUTH);
		f.pack();
		f.setVisible(true);
	}

	public JPanel createMysqlParamTextForm(boolean noTableName) {

		JPanel textForm = new JPanel();
		textForm.setLayout(new BorderLayout());

		String[] labels = { "Locale", "Host", "User Name", "Password", "Database Name", "Table Name" };
		String[] descs = { locale, mysqlHost, mysqlUser, mysqlPasswd, mysqlDB, tableName };
		String[] tips = {
				"Wikipedia Language",
				"mysql DB host",
				"mysql user",
				"mysql password",
				"Name of mysql data base",
				"Name of the selected sentences table (without \"locale\" and without sufix \"_selectedSentences\"). "
						+ "\nCheck available tables with options --> \"Show list of tables for this local\"." };

		JPanel labelPanel = new JPanel(new GridLayout(labels.length, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(labels.length, 1));
		textForm.add(labelPanel, BorderLayout.WEST);
		textForm.add(fieldPanel, BorderLayout.CENTER);
		int maxLabels = labels.length;
		if (noTableName)
			maxLabels--;
		for (int i = 0; i < maxLabels; i++) {
			fields[i] = new JTextField();
			if (i < descs.length) {
				fields[i].setText(descs[i]);
				fields[i].setToolTipText(tips[i]);
				fields[i].setColumns(15);
			}
			JLabel lab = new JLabel(labels[i], JLabel.RIGHT);
			lab.setLabelFor(fields[i]);

			labelPanel.add(lab);
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p.add(fields[i]);
			fieldPanel.add(p);
		}
		return textForm;
	}

	/** Run DatabaseSelector program */
	public void runDatabaseSelector() {

		final JFrame f = new JFrame("Run DatabaseSelector");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(createDatabaseselectorTextForm(), BorderLayout.NORTH);
		JPanel p = new JPanel();

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				locale = fields[0].getText();
				argsSelector[0] = "-locale";
				argsSelector[1] = locale;
				mysqlHost = fields[1].getText();
				argsSelector[2] = "-mysqlHost";
				argsSelector[3] = mysqlHost;
				mysqlUser = fields[2].getText();
				argsSelector[4] = "-mysqlUser";
				argsSelector[5] = mysqlUser;
				mysqlPasswd = fields[3].getText();
				argsSelector[6] = "-mysqlPasswd";
				argsSelector[7] = mysqlPasswd;
				mysqlDB = fields[4].getText();
				argsSelector[8] = "-mysqlDB";
				argsSelector[9] = mysqlDB;
				tableName = fields[5].getText();
				argsSelector[10] = "-tableName";
				argsSelector[11] = tableName;
				actualTableName = locale + "_" + tableName + "_selectedSentences";

				tableDesc = fields[6].getText();
				argsSelector[12] = "-tableDescription";
				argsSelector[13] = tableDesc;
				feaDefFile = fields[7].getText();
				argsSelector[14] = "-featDef";
				argsSelector[15] = feaDefFile;
				stop = fields[8].getText();
				argsSelector[16] = "-stop";
				argsSelector[17] = stop;
				covConfFile = fields[9].getText();
				argsSelector[18] = "-coverageConfig";
				argsSelector[19] = covConfFile;
				initFile = fields[10].getText();
				argsSelector[20] = "-initFile";
				argsSelector[21] = initFile;
				overallLogFile = fields[11].getText();
				argsSelector[22] = "-overallLog";
				argsSelector[23] = overallLogFile;
				selecDir = fields[12].getText();
				argsSelector[24] = "-selectionDir";
				argsSelector[25] = selecDir;
				// The following options do not have argument
				vectorsOnDisk = fields[13].getText();
				if (vectorsOnDisk.contentEquals("true"))
					argsSelector[26] = "-vectorsOnDisk";
				else
					argsSelector[26] = "";
				logCovDevelopment = fields[14].getText();
				if (logCovDevelopment.contentEquals("true"))
					argsSelector[27] = "-logCoverageDevelopment";
				else
					argsSelector[27] = "";
				verbose = fields[15].getText();
				if (verbose.contentEquals("true"))
					argsSelector[28] = "-verbose";
				else
					argsSelector[28] = "";

				// The SelectionFunctionProgram will check if the params are ok
				try {
					output.append("\n-----------------------------------------------\n"
							+ "RUNNING: DatabaseSelector, when finished (if no errors) the selected sentences will be loaded.");
					output.setCaretPosition(output.getDocument().getLength());
					DatabaseSelector.main(argsSelector);
					loadTable();
					// int sel[] = wikiToDB.getIdListOfType("selectedSentences", null);
					// not sure if we need to make another table???
				} catch (Exception ex) {
					// ex.printStackTrace();
					output.append("\nERROR RUNNING: DatabaseSelector, please check the parameters. \n");
					output.append(ex.getMessage() + "\n");
					output.setCaretPosition(output.getDocument().getLength());
				}
				// once read the parameters close the window
				f.dispose();
				// int sel[] = wikiToDB.getIdListOfType("selectedSentences", null);
				// not sure if we need to make another table???

			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				f.dispose();
			}
		});
		p.add(okButton);
		p.add(cancelButton);
		f.getContentPane().add(p, BorderLayout.SOUTH);
		f.pack();
		f.setVisible(true);
	}

	public JPanel createDatabaseselectorTextForm() {

		JPanel textForm = new JPanel();
		textForm.setLayout(new BorderLayout());

		String[] labels = { "Locale", "Host", "User Name", "Password", "Database Name", "Table Name", "Table description",
				"Feature definition file", "Stop criterion", "Coverage config file", "Init file", "overallLog",
				"Selection directory", "Vectors on disk", "logCoverageDevelopment", "verbose" };
		String[] descs = { locale, mysqlHost, mysqlUser, mysqlPasswd, mysqlDB, tableName, tableDesc, feaDefFile, stop,
				covConfFile, initFile, overallLogFile, selecDir, vectorsOnDisk, logCovDevelopment, verbose };
		String[] tips = {
				"Wikipedia Language",
				"mysql DB host",
				"mysql user",
				"mysql password",
				"Name of mysql data base",
				"Name of the selected sentences table (without \"locale\" and without sufix \"_selectedSentences\"). The table will be created if it does not exist.",
				"Short description/characteristics of the selected sentences table",
				"The feature definition for the features, the FeatureMakerMaryServer should have created one for this locale.",
				"Which of three stop criterion to use (individually or combined): numSentences n simpleDiphones simpleProsody",
				"The config file for the coverage definition. Default config file is ./covDef.config.",
				"The file containing the coverage data (this file is created when the program is run the first time). Default init file is ./init.bin",
				"Log file for all runs of the program: date, settings and results of the current run are appended to the end of the file.",
				"The directory where all selection data is stored. Default directory is ./selection",
				"If true the feature vectors are NOT loaded into memory when running the program.",
				"If true the coverage development over time is stored.",
				"If true there will be more output on the command line during the execution of the program." };

		JPanel labelPanel = new JPanel(new GridLayout(labels.length, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(labels.length, 1));
		textForm.add(labelPanel, BorderLayout.WEST);
		textForm.add(fieldPanel, BorderLayout.CENTER);
		for (int i = 0; i < labels.length; i++) {
			fields[i] = new JTextField();
			if (i < descs.length) {
				fields[i].setText(descs[i]);
				fields[i].setToolTipText(tips[i]);
				fields[i].setColumns(40);
			}
			JLabel lab = new JLabel(labels[i], JLabel.RIGHT);
			lab.setLabelFor(fields[i]);
			labelPanel.add(lab);
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p.add(fields[i]);
			fieldPanel.add(p);
		}
		return textForm;
	}

	// show list of tables for this local
	public void showListOfTables() {
		final JFrame f = new JFrame("Read Mysql parameters");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(createMysqlParamTextForm(true), BorderLayout.NORTH);
		JPanel p = new JPanel();

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				locale = fields[0].getText();
				mysqlHost = fields[1].getText();
				mysqlUser = fields[2].getText();
				mysqlPasswd = fields[3].getText();
				mysqlDB = fields[4].getText();

				if (locale.length() > 0 && mysqlHost.length() > 0 && mysqlDB.length() > 0 && mysqlUser.length() > 0
						&& mysqlPasswd.length() > 0) {
					if (wikiToDB != null)
						wikiToDB.closeDBConnection();
					wikiToDB = null;
					mysqlInfo = false;
					connectionProblems = false;
					wikiToDB = new DBHandler(locale);
					if (wikiToDB.createDBConnection(mysqlHost, mysqlDB, mysqlUser, mysqlPasswd)) {
						mysqlInfo = true;
						ArrayList<String> tables = wikiToDB.getListOfTables();
						output.append("\nLIST OF " + locale + " TABLES IN DB:\n");
						for (int i = 0; i < tables.size(); i++)
							output.append("   " + tables.get(i) + "\n");
						// to point to the end of the JTextArea
						output.setCaretPosition(output.getDocument().getLength());
					} else {
						connectionProblems = true;
						wikiToDB = null;
					}
				} else {
					if (locale.length() == 0)
						output.append("\nERROR PARAMETER: locale is empty\n");
					if (mysqlHost.length() == 0)
						output.append("\nERROR PARAMETER: Mysql host name is empty\n");
					if (mysqlDB.length() == 0)
						output.append("\nERROR PARAMETER: Mysql Data base name is empty\n");
					if (mysqlUser.length() == 0)
						output.append("\nERROR PARAMETER: Mysql user name is empty\n");
					if (mysqlPasswd.length() == 0)
						output.append("\nERROR PARAMETER: Mysql password is empty\n");
				}

				f.dispose();
			}
		});
		JButton cancelJButton = new JButton("Cancel");
		cancelJButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				f.dispose();
			}
		});
		p.add(okButton);
		p.add(cancelJButton);
		f.getContentPane().add(p, BorderLayout.SOUTH);
		f.pack();
		f.setVisible(true);
	}
}
