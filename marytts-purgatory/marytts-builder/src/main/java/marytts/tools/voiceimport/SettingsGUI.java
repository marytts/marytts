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
package marytts.tools.voiceimport;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

public class SettingsGUI {

	private final JFrame frame = new JFrame("Settings Editor");
	private DatabaseLayout db;
	private String[][] tableProps;
	private PropTableModel tableModel;
	private JTable table;
	private String[] compNames;
	private JScrollPane scrollPane;
	private JComboBox componentsComboBox;
	private final Map<String, String> comps2HelpText;
	private String simpleModeHelpText;
	private String guiText;
	private boolean wasSaved;

	public SettingsGUI(DatabaseLayout db, SortedMap<String, String> props, String simpleModeHelpText, String guiText) {
		this.db = db;
		this.simpleModeHelpText = simpleModeHelpText;
		this.guiText = guiText;
		comps2HelpText = null;
		Set<String> propSet = props.keySet();
		List<String[]> propList = new ArrayList<String[]>();
		for (String key : props.keySet()) {
			Object value = props.get(key);
			if (value instanceof String) {
				// this is a global prop
				if (db.isEditable((String) value)) {
					String[] keyAndValue = new String[2];
					keyAndValue[0] = key;
					keyAndValue[1] = (String) value;
					propList.add(keyAndValue);
				}
			} else {
				// these are props for a component
				if (value instanceof SortedMap) {
					SortedMap<String, String> newLocalProps = new TreeMap<String, String>();
					SortedMap<String, String> mapValue = (SortedMap) value;
					for (String nextKey : mapValue.keySet()) {
						String nextValue = mapValue.get(nextKey);
						String[] keyAndValue = new String[2];
						keyAndValue[0] = nextKey;
						keyAndValue[1] = nextValue;
						propList.add(keyAndValue);
					}
				}
			}
		}// end of loop over props
		tableProps = new String[propList.size()][];
		for (int i = 0; i < propList.size(); i++) {
			tableProps[i] = (String[]) propList.get(i);
		}

		display(null, true);
	}

	public SettingsGUI(DatabaseLayout db, String[][] props, String selectedComp, Map<String, String> comps2HelpText) {
		this.db = db;
		this.tableProps = props;
		this.comps2HelpText = comps2HelpText;
		display(selectedComp, false);
	}

	/**
	 * Show a frame displaying the help file.
	 * 
	 * @param selectedComp
	 *            selectedComp
	 * @param simpleMode
	 *            simpleMode
	 */
	public void display(String selectedComp, boolean simpleMode) {
		wasSaved = false;
		// final JFrame frame = new JFrame("Settings Editor");
		GridBagLayout gridBagLayout = new GridBagLayout();
		GridBagConstraints gridC = new GridBagConstraints();
		frame.getContentPane().setLayout(gridBagLayout);
		if (simpleMode) {
			JLabel guiTextLabel = new JLabel(guiText);
			gridC.gridx = 0;
			gridC.gridy = 0;
			gridBagLayout.setConstraints(guiTextLabel, gridC);
			frame.getContentPane().add(guiTextLabel);
			String[] columnNames = { "Property", "Value" };
			tableModel = new PropTableModel(columnNames, tableProps);
		} else {
			compNames = db.getCompNamesForDisplay();
			componentsComboBox = new JComboBox(compNames);
			componentsComboBox.setSelectedItem(selectedComp);
			componentsComboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JComboBox cb = (JComboBox) e.getSource();
					String compName = (String) cb.getSelectedItem();
					updateTable(compName);
				}
			});
			gridC.gridx = 0;
			gridC.gridy = 0;
			gridBagLayout.setConstraints(componentsComboBox, gridC);
			frame.getContentPane().add(componentsComboBox);

			// build a new JTable
			String[] columnNames = { "Property", "Value" };
			String[][] currentProps = getPropsForCompName(selectedComp);
			tableModel = new PropTableModel(columnNames, currentProps);
		}
		table = new JTable(tableModel);
		// set the focus traversal keys for the table
		Set<KeyStroke> forwardKeys = new HashSet<KeyStroke>();
		forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false));
		table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys);
		Set<KeyStroke> backwardKeys = new HashSet<KeyStroke>();
		backwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_MASK + KeyEvent.SHIFT_DOWN_MASK, false));
		table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardKeys);
		// table.setPreferredScrollableViewportSize(new Dimension(600, 500));
		scrollPane = new JScrollPane(table);
		gridC.gridy = 1;
		// resize scroll pane:
		gridC.weightx = 1;
		gridC.weighty = 1;
		gridC.fill = GridBagConstraints.HORIZONTAL;
		scrollPane.setPreferredSize(new Dimension(600, 300));
		gridBagLayout.setConstraints(scrollPane, gridC);

		frame.getContentPane().add(scrollPane);
		gridC.gridy = 2;
		// do not resize buttons:
		gridC.weightx = 0;
		gridC.weighty = 0;
		JButton helpButton = new JButton("Help");
		helpButton.setMnemonic(KeyEvent.VK_H);
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread("DisplayHelpGUIThread") {
					public void run() {
						if (componentsComboBox == null) {
							new HelpGUI(simpleModeHelpText).display();
						} else {
							String helpText = (String) comps2HelpText.get(componentsComboBox.getSelectedItem());
							new HelpGUI(helpText).display();
						}
					}
				}.start();
			}
		});
		JButton saveButton = new JButton("Save");
		saveButton.setMnemonic(KeyEvent.VK_S);
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopTableEditing();
				updateProps();
				wasSaved = true;
				frame.setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				wasSaved = false;
				frame.setVisible(false);
			}
		});
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(helpButton);
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		gridBagLayout.setConstraints(buttonPanel, gridC);
		frame.getContentPane().add(buttonPanel);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				wasSaved = false;
				frame.setVisible(false);
			}
		});
		frame.pack();
		frame.setVisible(true);
		do {
			try {
				Thread.sleep(10); // OK, this is ugly, but I don't mind today...
			} catch (InterruptedException e) {
			}
		} while (frame.isVisible());

		frame.dispose();
	}

	public boolean wasSaved() {
		return wasSaved;
	}

	private String[][] getPropsForCompName(String name) {
		if (name.equals("Global properties"))
			name = "db";
		List<String[]> propList = new ArrayList<String[]>();
		for (int i = 0; i < tableProps.length; i++) {
			String[] keyAndValue = tableProps[i];
			// System.err.println(keyAndValue[0]+" --- "+name);
			if (keyAndValue[0].startsWith(name + ".")) {
				propList.add(keyAndValue);
			}
		}
		String[][] result = new String[propList.size()][];
		for (int i = 0; i < propList.size(); i++) {
			result[i] = propList.get(i);
		}
		return result;
	}

	private void updateProps() {
		db.updateProps(tableProps);
	}

	private void stopTableEditing() {
		if (table.isEditing()) {
			TableCellEditor ed = table.getCellEditor();
			assert ed != null;
			boolean success = ed.stopCellEditing(); // we first try to save
			if (!success) {
				ed.cancelCellEditing();
			}
			assert !table.isEditing();
		}
	}

	private void updateTable(String compName) {
		// First, make sure that any field that is currently being edited is saved or discarded:
		stopTableEditing();
		// Then, update the table model:
		String[][] currentProps = getPropsForCompName(compName);
		tableModel.setProps(currentProps);
		table.tableChanged(new TableModelEvent(tableModel));
		// int hsize = currentProps.length*20;
		// scrollPane.setPreferredSize(new Dimension(600,hsize));
		// frame.pack();
	}

	private class PropTableModel extends AbstractTableModel {
		private String[] columnNames;
		private boolean DEBUG = false;
		private String[][] props;

		public PropTableModel(String[] columnNames, String[][] props) {
			this.columnNames = columnNames;
			this.props = props;
		}

		public void setProps(String[][] props) {
			this.props = props;
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return props.length;
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {
			return props[row][col];
		}

		/*
		 * JTable uses this method to determine the default renderer/ editor for each cell.
		 */
		public Class getColumnClass(int c) {
			return String.class;
		}

		public boolean isCellEditable(int row, int col) {
			// Note that the data/cell address is constant,
			// no matter where the cell appears onscreen.
			return col != 0;
		}

		public void setValueAt(Object value, int row, int col) {
			if (DEBUG) {
				System.out.println("Setting value at " + row + "," + col + " to " + value);
			}
			props[row][col] = (String) value;
			fireTableCellUpdated(row, col);
		}

		private void printDebugData() {
			int numRows = getRowCount();
			int numCols = getColumnCount();

			for (int i = 0; i < numRows; i++) {
				System.out.print("    row " + i + ":");
				for (int j = 0; j < numCols; j++) {
					System.out.print("  " + props[i][j]);
				}
				System.out.println();
			}
			System.out.println("--------------------------");
		}
	}

	class HelpButtonActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			new Thread("DisplayHelpGUIThread") {
				public void run() {

				}
			}.start();
		}
	}

}
